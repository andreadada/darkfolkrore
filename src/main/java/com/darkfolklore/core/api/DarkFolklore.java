package com.darkfolklore.core.api;

import com.darkfolklore.core.canonical.CanonicalDefinition;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.knowledge.lore.LoreProgress;
import com.darkfolklore.core.knowledge.social.*;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.reputation.ReputationFaction;
import com.darkfolklore.core.traits.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Stable facade for addons; external mod implementation types never cross this boundary. */
public final class DarkFolklore {
    private DarkFolklore() {}

    public static boolean isVampire(Entity entity) { return creatureTraits(entity).contains(CreatureTrait.VAMPIRE); }
    public static boolean isWerewolf(Entity entity) { return creatureTraits(entity).contains(CreatureTrait.WEREWOLF); }
    public static boolean isHunter(Entity entity) { return creatureTraits(entity).contains(CreatureTrait.HUNTER); }
    public static Set<CreatureTrait> creatureTraits(Entity entity) { return TraitResolver.creatureTraits(entity); }
    public static Set<ItemTrait> itemTraits(ItemStack stack) { return TraitResolver.itemTraits(stack); }

    public static Optional<CanonicalDefinition> resolveCanonicalEntity(EntityType<?> type) {
        return FolkloreDataManager.INSTANCE.canonical().resolve(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
    }

    public static Optional<CanonicalDefinition> resolveCanonicalItem(ItemStack stack) {
        return stack.isEmpty() ? Optional.empty() : FolkloreDataManager.INSTANCE.canonical()
                .resolve(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    public static LoreProgress lore(ServerPlayer player, String concept) {
        return FolkloreSavedData.get(player.getServer()).lore(player.getUUID(), concept);
    }

    public static Optional<SocialKnowledgeRecord> socialKnowledge(MinecraftServer server, UUID observer,
                                                                  UUID subject, SecretType secret) {
        return FolkloreSavedData.get(server).social(new SocialKnowledgeKey(observer, subject, secret));
    }

    public static int reputation(MinecraftServer server, UUID holder, ReputationFaction faction) {
        return FolkloreSavedData.get(server).reputation(holder).get(faction);
    }
}
