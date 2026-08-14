package com.darkfolklore.core.encounter;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.canonical.CanonicalDefinition;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.*;

/** Player-invoked escape hatch for deliberately finding otherwise rare encounters. */
@EventBusSubscriber(modid = DarkFolkloreCore.MOD_ID)
public final class RitualEngine {
    private static final int MAX_COOLDOWNS = 4096;
    private static final int[][] OFFSETS = {
            {2, 0}, {-2, 0}, {0, 2}, {0, -2},
            {3, 2}, {-3, 2}, {3, -2}, {-3, -2}
    };
    private static final Map<RitualKey, Long> COOLDOWNS = new LinkedHashMap<>();

    private RitualEngine() {}

    @SubscribeEvent
    public static void onUseFocus(PlayerInteractEvent.RightClickBlock event) {
        if (!FolkloreConfig.RITUALS.get() || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()
                || !(player.level() instanceof ServerLevel level)) return;

        String blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(event.getPos()).getBlock()).toString();
        String itemId = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
        RitualDefinition ritual = ThreatPolicyManager.INSTANCE.rituals().stream()
                .filter(RitualDefinition::enabled)
                .filter(value -> value.focusBlock().equals(blockId) && value.activationItem().equals(itemId))
                .min(Comparator.comparing(RitualDefinition::id)).orElse(null);
        if (ritual == null) return;
        consumeInteraction(event);

        EncounterPolicy encounter = ThreatPolicyManager.INSTANCE.encounter(ritual.encounterId()).orElse(null);
        if (encounter == null) {
            player.sendSystemMessage(Component.literal("The ritual pattern is incomplete."));
            return;
        }
        ResourceLocation entityId = ResourceLocation.tryParse(encounter.entityId());
        if (entityId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
            player.sendSystemMessage(Component.literal("Nothing answers this ritual in the current world."));
            return;
        }
        if (ritual.requiresNight() && !level.isNight()) {
            player.sendSystemMessage(Component.literal("The rite needs darkness before it can take hold."));
            return;
        }

        String concept = FolkloreDataManager.INSTANCE.canonical().resolve(encounter.entityId())
                .map(CanonicalDefinition::concept).orElse(encounter.entityId());
        int knowledge = FolkloreSavedData.get(player.getServer()).lore(player.getUUID(), concept).points();
        if (knowledge < ritual.requiredKnowledgePoints()) {
            player.sendSystemMessage(Component.literal("Your field notes are not complete enough to perform this rite."));
            return;
        }

        long now = level.getGameTime();
        RitualKey key = new RitualKey(player.getUUID(), ritual.id());
        long readyAt = COOLDOWNS.getOrDefault(key, 0L);
        if (readyAt > now) {
            long seconds = Math.max(1L, (readyAt - now + 19L) / 20L);
            player.sendSystemMessage(Component.literal("The ritual site is still quiet. Try again in about " + seconds + " seconds."));
            return;
        }
        if (!hasCosts(player, ritual.itemCosts())) {
            player.sendSystemMessage(Component.literal("The offering is incomplete."));
            return;
        }

        List<BlockPos> positions = findSpawnPositions(level, event.getPos(), ritual.spawnCount());
        if (positions.size() < ritual.spawnCount()) {
            player.sendSystemMessage(Component.literal("There is not enough clear ground around the ritual focus."));
            return;
        }

        if (!player.getAbilities().instabuild) consumeCosts(player, ritual.itemCosts());
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        int spawned = 0;
        for (BlockPos pos : positions) {
            Entity entity = type.create(level);
            if (!(entity instanceof LivingEntity living)) continue;
            living.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(living) || !level.addFreshEntity(living)) continue;
            spawned++;
        }
        if (spawned == 0) {
            player.sendSystemMessage(Component.literal("The offering is consumed, but the manifestation fails."));
            return;
        }

        rememberCooldown(key, now + ritual.cooldownTicks(), now);
        player.sendSystemMessage(Component.literal(spawned == 1
                ? "The rite takes hold. Something answers."
                : "The rite takes hold. Several presences answer."));
        DarkFolkloreCore.LOGGER.info("[ritual] player={} ritual={} encounter={} spawned={}",
                player.getUUID(), ritual.id(), encounter.id(), spawned);
    }

    public static void clearRuntimeState() {
        COOLDOWNS.clear();
    }

    private static List<BlockPos> findSpawnPositions(ServerLevel level, BlockPos focus, int count) {
        List<BlockPos> result = new ArrayList<>();
        for (int[] offset : OFFSETS) {
            if (result.size() >= count) break;
            BlockPos pos = focus.offset(offset[0], 1, offset[1]);
            if (!level.hasChunkAt(pos)) continue;
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
            if (level.getBlockState(pos.below()).isAir()) continue;
            result.add(pos);
        }
        return result;
    }

    private static boolean hasCosts(ServerPlayer player, Map<String, Integer> costs) {
        for (Map.Entry<String, Integer> cost : costs.entrySet()) {
            int total = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(cost.getKey())) total += stack.getCount();
            }
            if (total < cost.getValue()) return false;
        }
        return true;
    }

    private static void consumeCosts(ServerPlayer player, Map<String, Integer> costs) {
        for (Map.Entry<String, Integer> cost : costs.entrySet()) {
            int remaining = cost.getValue();
            for (ItemStack stack : player.getInventory().items) {
                if (remaining <= 0) break;
                if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(cost.getKey())) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    private static void rememberCooldown(RitualKey key, long readyAt, long now) {
        COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (COOLDOWNS.size() >= MAX_COOLDOWNS) COOLDOWNS.remove(COOLDOWNS.keySet().iterator().next());
        COOLDOWNS.put(key, readyAt);
    }

    private static void consumeInteraction(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private record RitualKey(UUID player, String ritual) {}
}
