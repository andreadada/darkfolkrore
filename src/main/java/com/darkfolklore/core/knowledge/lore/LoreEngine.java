package com.darkfolklore.core.knowledge.lore;

import com.darkfolklore.core.api.event.ConfirmedLivingDeathEvent;
import com.darkfolklore.core.api.event.KnowledgeChangedEvent;
import com.darkfolklore.core.canonical.CanonicalDefinition;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.magic.MagicIntegrationDefinition;
import com.darkfolklore.core.network.FolkloreNetwork;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.society.SecretFacts;
import com.darkfolklore.core.traits.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.EnumSet;
import java.util.Set;

public final class LoreEngine {
    public static final LoreEngine INSTANCE = new LoreEngine();
    private LoreEngine() {}

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (!FolkloreConfig.KNOWLEDGE.get() || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof LivingEntity target)
                || TraitResolver.creatureTraits(target).isEmpty()) return;
        String concept = concept(target);
        if (FolkloreSavedData.get(player.getServer()).lore(player.getUUID(), concept).points() == 0) {
            grant(player, concept, 1);
        }
    }

    @SubscribeEvent
    public void onConfirmedDeath(ConfirmedLivingDeathEvent event) {
        if (!FolkloreConfig.KNOWLEDGE.get() || !(event.source().getEntity() instanceof ServerPlayer player)) return;
        Set<CreatureTrait> traits = TraitResolver.creatureTraits(event.entity());
        if (traits.isEmpty()) return;
        grant(player, concept(event.entity()), 15);
        grant(player, "darkfolklore:monster_lore", 3);
    }

    @SubscribeEvent
    public void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (!FolkloreConfig.KNOWLEDGE.get() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getOriginalStack();
        Set<ItemTrait> traits = TraitResolver.itemTraits(stack);
        if (traits.contains(ItemTrait.ARCHAEOLOGICAL_LORE)) {
            String concept = FolkloreDataManager.INSTANCE.canonical()
                    .resolve(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                    .map(CanonicalDefinition::concept).orElse("darkfolklore:forbidden_lore");
            discoverOnce(player, concept, 10);
        }
        if (!traits.isEmpty()) discoverMagicSynergies(player, traits);
    }

    private void discoverMagicSynergies(ServerPlayer player, Set<ItemTrait> pickedUpTraits) {
        EnumSet<ItemTrait> inventoryTraits = EnumSet.copyOf(pickedUpTraits);
        for (ItemStack stack : player.getInventory().items) inventoryTraits.addAll(TraitResolver.itemTraits(stack));
        for (MagicIntegrationDefinition definition : FolkloreDataManager.INSTANCE.magic()) {
            if (inventoryTraits.containsAll(definition.requiredTraits())) {
                discoverOnce(player, definition.knowledgeReward(), definition.knowledgePoints());
            }
        }
    }

    public LoreProgress grant(ServerPlayer player, String concept, int points) {
        FolkloreSavedData data = FolkloreSavedData.get(player.getServer());
        LoreProgress before = data.lore(player.getUUID(), concept);
        LoreProgress after = data.addLore(player.getUUID(), concept, points);
        if (after.points() != before.points()) {
            NeoForge.EVENT_BUS.post(new KnowledgeChangedEvent(player, concept, before, after));
            if (after.stage() != before.stage()) {
                FolkloreNetwork.sendLoreToast(player, concept, after.stage().name());
            }
        }
        return after;
    }

    public void discoverOnce(ServerPlayer player, String concept, int points) {
        FolkloreSavedData data = FolkloreSavedData.get(player.getServer());
        if (data.lore(player.getUUID(), concept).points() == 0) grant(player, concept, points);
    }

    private static String concept(Entity entity) {
        return FolkloreDataManager.INSTANCE.canonical()
                .resolve(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString())
                .map(CanonicalDefinition::concept).orElseGet(() -> SecretFacts.canonicalConcept(entity));
    }
}
