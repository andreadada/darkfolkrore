package com.darkfolklore.core.investigation;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.magic.MagicTradition;
import com.darkfolklore.core.traits.ItemTrait;
import com.darkfolklore.core.traits.TraitResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves a deliberately curated existing pack item to one of Dark Folklore's
 * five investigative magical practices without importing optional provider classes.
 */
public final class MagicPracticeResolver {
    private static final Map<MagicTradition, TagKey<Item>> TOOL_TAGS;

    static {
        EnumMap<MagicTradition, TagKey<Item>> tags = new EnumMap<>(MagicTradition.class);
        for (MagicTradition tradition : MagicTradition.values()) {
            tags.put(tradition, TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(DarkFolkloreCore.MOD_ID,
                            "investigation_tools/" + tradition.name().toLowerCase())));
        }
        TOOL_TAGS = Map.copyOf(tags);
    }

    private MagicPracticeResolver() {}

    public static Optional<MagicTradition> resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        for (MagicTradition tradition : MagicTradition.values()) {
            if (stack.is(TOOL_TAGS.get(tradition))) return Optional.of(tradition);
        }

        // Fallback remains trait-aware and namespace-aware so datapacks can add
        // semantically equivalent implements without opening every item in a magic
        // mod as an analysis tool.
        String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        Set<ItemTrait> traits = TraitResolver.itemTraits(stack);
        if (namespace.equals("enchanted") && (traits.contains(ItemTrait.RITUAL_COMPONENT)
                || traits.contains(ItemTrait.WOLFSBANE) || traits.contains(ItemTrait.GARLIC)
                || traits.contains(ItemTrait.VERVAIN))) return Optional.of(MagicTradition.WITCHCRAFT);
        if (namespace.equals("occultism") && (traits.contains(ItemTrait.SPIRITUAL)
                || traits.contains(ItemTrait.SOUL) || traits.contains(ItemTrait.RITUAL_COMPONENT))) {
            return Optional.of(MagicTradition.SPIRIT);
        }
        if (namespace.equals("malum") && (traits.contains(ItemTrait.SOUL)
                || traits.contains(ItemTrait.SPIRITUAL))) return Optional.of(MagicTradition.SOUL);
        if (namespace.equals("eidolon_repraised") && (traits.contains(ItemTrait.SOUL)
                || traits.contains(ItemTrait.CURSED) || traits.contains(ItemTrait.RITUAL_COMPONENT)
                || traits.contains(ItemTrait.RITUAL_WEAPON))) return Optional.of(MagicTradition.FORBIDDEN_THEURGY);
        if (namespace.equals("feywild") && (traits.contains(ItemTrait.FAE)
                || traits.contains(ItemTrait.RITUAL_COMPONENT))) return Optional.of(MagicTradition.FAE);
        return Optional.empty();
    }

    public static String knowledgeConcept(MagicTradition tradition) {
        return switch (tradition) {
            case WITCHCRAFT -> "darkfolklore:witchcraft";
            case SPIRIT -> "darkfolklore:spirit_magic";
            case SOUL -> "darkfolklore:soul_magic";
            case FORBIDDEN_THEURGY -> "darkfolklore:forbidden_lore";
            case FAE -> "darkfolklore:fae_lore";
        };
    }
}
