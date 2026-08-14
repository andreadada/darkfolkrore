package com.darkfolklore.core.magic;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.investigation.MagicPracticeResolver;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.Set;

/** Resolves one item to one or more gameplay disciplines without importing optional provider classes. */
public final class MagicDisciplineResolver {
    private static final TagKey<Item> BLOOD_TOOLS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(DarkFolkloreCore.MOD_ID, "investigation_tools/blood"));
    private static final TagKey<Item> RITUAL_FOCI = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(DarkFolkloreCore.MOD_ID, "recipe/ritual_focus"));

    private MagicDisciplineResolver() {}

    public static Set<MagicDiscipline> resolveAll(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Set.of();
        EnumSet<MagicDiscipline> result = EnumSet.noneOf(MagicDiscipline.class);
        MagicPracticeResolver.resolve(stack).ifPresent(tradition -> result.add(switch (tradition) {
            case WITCHCRAFT -> MagicDiscipline.WITCHCRAFT;
            case SPIRIT -> MagicDiscipline.SPIRITUALISM;
            case SOUL -> MagicDiscipline.SOUL_MAGIC;
            case FORBIDDEN_THEURGY -> MagicDiscipline.NECROMANCY;
            case FAE -> MagicDiscipline.FAE_MAGIC;
        }));
        if (stack.is(BLOOD_TOOLS)) result.add(MagicDiscipline.BLOOD_MAGIC);
        if (stack.is(RITUAL_FOCI)) result.add(MagicDiscipline.RITUAL_MAGIC);
        return Set.copyOf(result);
    }
}
