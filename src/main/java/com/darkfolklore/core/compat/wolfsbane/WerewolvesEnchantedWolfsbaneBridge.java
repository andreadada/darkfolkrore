package com.darkfolklore.core.compat.wolfsbane;

import de.teamlapen.werewolves.blocks.entity.WolfsbaneDiffuserBlockEntity;
import de.teamlapen.werewolves.effects.WolfsbaneEffect;
import de.teamlapen.werewolves.util.Helper;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Direct bridge for exactly Werewolves 2.0.3.3 and Enchanted 4.2.7.
 * Loaded only by {@link WolfsbaneIntegration} after fail-closed version and registry checks.
 */
public final class WerewolvesEnchantedWolfsbaneBridge {
    private static final Item CANONICAL_FLOWER = requiredItem(WolfsbaneSemantics.CANONICAL_ITEM);
    private static final Block CANONICAL_CROP = requiredBlock(WolfsbaneSemantics.CANONICAL_BLOCK);
    private static final Item FINDER = requiredItem(WolfsbaneSemantics.FINDER_ITEM);
    private static final Set<Block> DIFFUSERS = Set.of(
            requiredBlock(WolfsbaneSemantics.DIFFUSER_NORMAL_BLOCK),
            requiredBlock(WolfsbaneSemantics.DIFFUSER_LONG_BLOCK),
            requiredBlock(WolfsbaneSemantics.DIFFUSER_IMPROVED_BLOCK)
    );
    private static final int FINDER_HORIZONTAL_RADIUS = 32;
    private static final int FINDER_VERTICAL_RADIUS = 8;
    private static final int FINDER_COOLDOWN_TICKS = 60;

    public WerewolvesEnchantedWolfsbaneBridge() {}

    /** Mirrors the exact native diffuser interaction for the canonical flower only. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDiffuserFuel(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() != CANONICAL_FLOWER) {
            return;
        }
        Level level = event.getLevel();
        if (!DIFFUSERS.contains(level.getBlockState(event.getPos()).getBlock())) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (!(blockEntity instanceof WolfsbaneDiffuserBlockEntity diffuser)) {
            return;
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        event.setCanceled(true);
        if (level.isClientSide) {
            return;
        }

        Player player = event.getEntity();
        if (diffuser.getFuelTime() > 0) {
            player.sendSystemMessage(Component.translatable("block.vampirism.garlic_diffuser.already_fueled"));
            return;
        }
        diffuser.onFueled();
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        player.sendSystemMessage(Component.translatable("block.vampirism.garlic_diffuser.successfully_fueled"));
    }

    /** Gives Enchanted's crop the same Werewolves effect as contact with the legacy flower block. */
    @SubscribeEvent
    public void onCanonicalCropContact(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living) || entity.level().isClientSide
                || entity.level().getDifficulty() == Difficulty.PEACEFUL || !Helper.isWerewolf(entity)
                || !touchesCanonicalCrop(entity)) {
            return;
        }
        living.addEffect(WolfsbaneEffect.createWolfsbaneEffect(living, 45, 1));
    }

    /** Adds the plant-locating behavior the native finder name implies; native diffuser highlighting remains intact. */
    @SubscribeEvent
    public void onFinderUse(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() != FINDER) {
            return;
        }
        Level level = event.getLevel();
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        event.setCanceled(true);
        if (level.isClientSide) {
            return;
        }

        Player player = event.getEntity();
        BlockPos found = findNearestLoadedCanonicalCrop(level, player.blockPosition());
        if (found == null) {
            player.sendSystemMessage(Component.translatable("message.darkfolklore.wolfsbane_finder.not_found",
                    FINDER_HORIZONTAL_RADIUS));
        } else {
            int distance = Mth.ceil(Math.sqrt(found.distSqr(player.blockPosition())));
            player.sendSystemMessage(Component.translatable("message.darkfolklore.wolfsbane_finder.found",
                    found.getX(), found.getY(), found.getZ(), distance));
        }
        player.getCooldowns().addCooldown(stack.getItem(), FINDER_COOLDOWN_TICKS);
    }

    private static boolean touchesCanonicalCrop(Entity entity) {
        AABB bounds = entity.getBoundingBox();
        int minX = Mth.floor(bounds.minX + 1.0E-7D);
        int minY = Mth.floor(bounds.minY + 1.0E-7D);
        int minZ = Mth.floor(bounds.minZ + 1.0E-7D);
        int maxX = Mth.floor(bounds.maxX - 1.0E-7D);
        int maxY = Mth.floor(bounds.maxY - 1.0E-7D);
        int maxZ = Mth.floor(bounds.maxZ - 1.0E-7D);
        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            if (entity.level().getBlockState(pos).getBlock() == CANONICAL_CROP) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos findNearestLoadedCanonicalCrop(Level level, BlockPos origin) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        Set<Long> loadedChunks = loadedChunksInSearchArea(level, origin);
        for (BlockPos candidate : BlockPos.withinManhattan(origin, FINDER_HORIZONTAL_RADIUS,
                FINDER_VERTICAL_RADIUS, FINDER_HORIZONTAL_RADIUS)) {
            if (!level.isInWorldBounds(candidate)
                    || !loadedChunks.contains(ChunkPos.asLong(candidate.getX() >> 4, candidate.getZ() >> 4))) {
                continue;
            }
            if (level.getBlockState(candidate).getBlock() != CANONICAL_CROP) {
                continue;
            }
            double distance = candidate.distSqr(origin);
            if (distance < nearestDistance) {
                nearest = candidate.immutable();
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static Set<Long> loadedChunksInSearchArea(Level level, BlockPos origin) {
        int minChunkX = (origin.getX() - FINDER_HORIZONTAL_RADIUS) >> 4;
        int maxChunkX = (origin.getX() + FINDER_HORIZONTAL_RADIUS) >> 4;
        int minChunkZ = (origin.getZ() - FINDER_HORIZONTAL_RADIUS) >> 4;
        int maxChunkZ = (origin.getZ() + FINDER_HORIZONTAL_RADIUS) >> 4;
        Set<Long> loaded = new HashSet<>((maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1));
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (level.getChunkSource().getChunkNow(chunkX, chunkZ) != null) {
                    loaded.add(ChunkPos.asLong(chunkX, chunkZ));
                }
            }
        }
        return loaded;
    }

    private static Item requiredItem(String id) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElseThrow();
    }

    private static Block requiredBlock(String id) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id)).orElseThrow();
    }
}
