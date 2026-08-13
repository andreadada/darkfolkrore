package com.darkfolklore.core.ward;

import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.persistence.WorldPosition;
import com.darkfolklore.core.traits.ItemTrait;
import com.darkfolklore.core.traits.TraitResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Set;
import java.util.UUID;

/** Explicit ward ritual using only existing tagged items and an existing door. */
public final class WardEngine {
    public static final WardEngine INSTANCE = new WardEngine();

    private WardEngine() {}

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!FolkloreConfig.WARDS.get()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level) || !player.isShiftKeyDown()
                || !level.getBlockState(event.getPos()).is(BlockTags.DOORS)) return;

        ItemStack ritual = player.getMainHandItem();
        Set<ItemTrait> ritualTraits = TraitResolver.itemTraits(ritual);
        if (!ritualTraits.contains(ItemTrait.RITUAL_COMPONENT)) return;

        ItemStack focus = player.getOffhandItem();
        Set<ItemTrait> focusTraits = TraitResolver.itemTraits(focus);
        WardType type = wardType(focusTraits);
        if (type == null) {
            player.displayClientMessage(Component.literal(
                    "The ritual needs garlic, a holy/spiritual/fae focus, or a second ritual component in the off hand."), true);
            return;
        }

        BlockPos anchor = normalizedDoorAnchor(level, event.getPos());
        int strength = strength(type, focusTraits);
        long now = level.getGameTime();
        WardRecord ward = new WardRecord(UUID.randomUUID(), type, WorldPosition.of(level, anchor),
                9, strength, player.getUUID(), now, now + FolkloreConfig.WARD_LIFETIME.get());
        if (!WardSavedData.get(player.getServer()).add(ward)) {
            player.displayClientMessage(Component.literal(
                    "This threshold cannot accept your ward right now; no ritual components were consumed."), true);
            return;
        }

        if (!player.getAbilities().instabuild) {
            ritual.shrink(1);
            focus.shrink(1);
        }
        level.sendParticles(ParticleTypes.ENCHANT, anchor.getX() + .5, anchor.getY() + 1,
                anchor.getZ() + .5, 18, .7, 1.0, .7, .05);
        player.displayClientMessage(Component.literal(
                "A " + type.name().toLowerCase() + " ward settles over this threshold."), false);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static BlockPos normalizedDoorAnchor(ServerLevel level, BlockPos clicked) {
        BlockPos below = clicked.below();
        if (level.hasChunkAt(below)
                && level.getBlockState(below).is(BlockTags.DOORS)
                && level.getBlockState(below).getBlock() == level.getBlockState(clicked).getBlock()) {
            return below;
        }
        return clicked;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!FolkloreConfig.WARDS.get()) return;
        if (event.getServer().getTickCount() % 1200 == 0) {
            WardSavedData.get(event.getServer()).prune(event.getServer().overworld().getGameTime());
        }
    }

    public int strengthAt(ServerLevel level, double x, double y, double z, WardType type) {
        if (!FolkloreConfig.WARDS.get()) return 0;
        return WardSavedData.get(level.getServer()).strengthAt(level.dimension().location().toString(),
                x, y, z, type, level.getGameTime());
    }

    public boolean blocksManifestation(ServerLevel level, double x, double y, double z, WardType type, int threshold) {
        return strengthAt(level, x, y, z, type) >= threshold;
    }

    private static WardType wardType(Set<ItemTrait> traits) {
        int categories = (traits.contains(ItemTrait.GARLIC) ? 1 : 0)
                + (traits.contains(ItemTrait.HOLY) ? 1 : 0)
                + (traits.contains(ItemTrait.SPIRITUAL) || traits.contains(ItemTrait.SOUL) ? 1 : 0)
                + (traits.contains(ItemTrait.FAE) ? 1 : 0);
        if (categories > 1 || (categories == 0 && traits.contains(ItemTrait.RITUAL_COMPONENT))) return WardType.GENERAL;
        if (traits.contains(ItemTrait.GARLIC)) return WardType.VAMPIRE;
        if (traits.contains(ItemTrait.HOLY)) return WardType.UNDEAD;
        if (traits.contains(ItemTrait.SPIRITUAL) || traits.contains(ItemTrait.SOUL)) return WardType.SPIRIT;
        if (traits.contains(ItemTrait.FAE)) return WardType.FAE;
        return null;
    }

    private static int strength(WardType type, Set<ItemTrait> traits) {
        int base = switch (type) {
            case VAMPIRE -> 70;
            case UNDEAD -> 70;
            case SPIRIT -> 65;
            case FAE -> 65;
            case GENERAL -> 60;
        };
        if (traits.contains(ItemTrait.HOLY) && traits.contains(ItemTrait.GARLIC)) base += 10;
        return Math.min(100, base);
    }
}
