package com.darkfolklore.core.trophy;

import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.knowledge.lore.LoreEngine;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.reputation.ReputationFaction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import java.util.Map;

/** Gives existing provider trophies research/proof meaning without changing their item implementation. */
public final class TrophyEngine {
    public static final TrophyEngine INSTANCE = new TrophyEngine();
    private static final Map<String, TrophyDefinition> TROPHIES = Map.of(
            "cnc:wendigo_head", new TrophyDefinition("darkfolklore:wendigo", 25, 4),
            "cnc:chupacabra_head", new TrophyDefinition("darkfolklore:chupacabra", 25, 4)
    );

    private TrophyEngine() {}

    @SubscribeEvent
    public void onPickup(ItemEntityPickupEvent.Post event) {
        if (!FolkloreConfig.TROPHY_RESEARCH.get()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getOriginalStack();
        TrophyDefinition definition = TROPHIES.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (definition == null) return;
        FolkloreSavedData data = FolkloreSavedData.get(player.getServer());
        int current = data.lore(player.getUUID(), definition.concept()).points();
        if (current >= definition.researchFloor()) return;
        LoreEngine.INSTANCE.grant(player, definition.concept(), definition.researchFloor() - current);
        data.addReputation(player.getUUID(), ReputationFaction.HUNTERS, definition.hunterReputation());
        player.displayClientMessage(Component.literal(
                "This trophy is credible proof and research material for " + definition.concept() + "."), true);
    }

    public record TrophyDefinition(String concept, int researchFloor, int hunterReputation) {}
}
