package com.darkfolklore.core.society;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/** Keeps persisted beliefs and rumor work limited to entities that can participate in society. */
public final class SocialEntityClassifier {
    private SocialEntityClassifier() {}

    public static boolean isSocial(Entity entity) {
        if (entity instanceof Player || entity instanceof AbstractVillager) return true;
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && id.getNamespace().equals("mca");
    }
}
