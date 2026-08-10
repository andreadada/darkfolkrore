package com.darkfolklore.core.traits;

import com.darkfolklore.core.compat.CompatibilityManager;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.Set;

public final class TraitResolver {
    private TraitResolver() {}

    public static Set<ItemTrait> itemTraits(ItemStack stack) {
        EnumSet<ItemTrait> result = EnumSet.noneOf(ItemTrait.class);
        if (stack == null || stack.isEmpty()) return result;
        for (ItemTrait trait : ItemTrait.values()) {
            if (stack.is(FolkloreTags.item(trait))) result.add(trait);
        }
        return Set.copyOf(result);
    }

    public static Set<CreatureTrait> creatureTraits(Entity entity) {
        EnumSet<CreatureTrait> result = EnumSet.noneOf(CreatureTrait.class);
        result.addAll(staticCreatureTraits(entity));
        CompatibilityManager compat = CompatibilityManager.INSTANCE;
        if (compat.isVampire(entity).isTrue()) result.add(CreatureTrait.VAMPIRE);
        if (compat.isWerewolf(entity).isTrue()) result.add(CreatureTrait.WEREWOLF);
        if (compat.isHunter(entity).isTrue()) result.add(CreatureTrait.HUNTER);
        addSupernaturalUmbrella(result);
        return Set.copyOf(result);
    }

    public static Set<CreatureTrait> staticCreatureTraits(Entity entity) {
        EnumSet<CreatureTrait> result = EnumSet.noneOf(CreatureTrait.class);
        for (CreatureTrait trait : CreatureTrait.values()) {
            if (entity.getType().is(FolkloreTags.entity(trait))) result.add(trait);
        }
        if (entity.getType().is(EntityTypeTags.UNDEAD)) result.add(CreatureTrait.UNDEAD);
        addSupernaturalUmbrella(result);
        return Set.copyOf(result);
    }

    private static void addSupernaturalUmbrella(EnumSet<CreatureTrait> result) {
        if (!result.isEmpty() && (result.contains(CreatureTrait.VAMPIRE)
                || result.contains(CreatureTrait.WEREWOLF)
                || result.contains(CreatureTrait.FAE)
                || result.contains(CreatureTrait.SPIRIT)
                || result.contains(CreatureTrait.DEMON)
                || result.contains(CreatureTrait.CRYPTID))) {
            result.add(CreatureTrait.SUPERNATURAL);
        }
    }
}
