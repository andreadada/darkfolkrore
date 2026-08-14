package com.darkfolklore.core.fae;

import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.knowledge.lore.LoreEngine;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.reputation.ReputationFaction;
import com.darkfolklore.core.society.village.VillageKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Uses existing food/flowers as fae offerings and returns a bounded crop blessing; no custom fae item or entity. */
public final class FaeBargainEngine {
    public static final FaeBargainEngine INSTANCE = new FaeBargainEngine();
    private static final TagKey<Item> OFFERINGS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("darkfolklore", "fae_offering"));

    private FaeBargainEngine() {}

    @SubscribeEvent
    public void onOffering(PlayerInteractEvent.RightClickBlock event) {
        if (!FolkloreConfig.FAE_BARGAINS.get()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level) || !player.isShiftKeyDown()) return;
        BlockState altar = level.getBlockState(event.getPos());
        if (!altar.is(BlockTags.FLOWERS)) return;
        ItemStack offering = player.getMainHandItem();
        if (offering.isEmpty() || !offering.is(OFFERINGS)) return;

        String region = VillageKey.at(level, event.getPos()).serialized();
        long now = level.getGameTime();
        FaeBargainSavedData bargains = FaeBargainSavedData.get(player.getServer());
        if (!bargains.ready(region, now)) {
            player.displayClientMessage(Component.literal("The local fae are not ready to bargain again yet."), true);
            return;
        }

        int grown = blessNearbyGrowth(level, event.getPos(), 8);
        if (grown <= 0) {
            player.displayClientMessage(Component.literal("No nearby living growth answers the offering."), true);
            return;
        }

        if (!player.getAbilities().instabuild) consumeOffering(player, offering);
        bargains.mark(region, now + FolkloreConfig.FAE_BARGAIN_COOLDOWN.get());
        LoreEngine.INSTANCE.grant(player, "darkfolklore:fae", 8);
        FolkloreSavedData.get(player.getServer()).addReputation(player.getUUID(), ReputationFaction.FAE, 2);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, event.getPos().getX() + .5, event.getPos().getY() + .8,
                event.getPos().getZ() + .5, 16, 1.2, .5, 1.2, .02);
        player.displayClientMessage(Component.literal(
                "The offering is accepted. Nearby living growth stirs (" + grown + ")."), false);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static void consumeOffering(ServerPlayer player, ItemStack offering) {
        Item item = offering.getItem();
        offering.shrink(1);
        ItemStack remainder = item == Items.MILK_BUCKET ? new ItemStack(Items.BUCKET)
                : item == Items.HONEY_BOTTLE ? new ItemStack(Items.GLASS_BOTTLE) : ItemStack.EMPTY;
        if (!remainder.isEmpty() && !player.getInventory().add(remainder)) player.spawnAtLocation(remainder);
    }

    @SubscribeEvent
    public void onTick(ServerTickEvent.Post event) {
        if (!FolkloreConfig.FAE_BARGAINS.get()) return;
        if (event.getServer().getTickCount() % 1200 == 0) {
            FaeBargainSavedData.get(event.getServer()).prune(event.getServer().overworld().getGameTime());
        }
    }

    private static int blessNearbyGrowth(ServerLevel level, BlockPos center, int limit) {
        int grown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-7, -2, -7), center.offset(7, 3, 7))) {
            if (grown >= limit) break;
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BonemealableBlock growable)) continue;
            if (!growable.isValidBonemealTarget(level, pos, state)) continue;
            if (!growable.isBonemealSuccess(level, level.random, pos, state)) continue;
            growable.performBonemeal(level, level.random, pos, state);
            grown++;
        }
        return grown;
    }
}
