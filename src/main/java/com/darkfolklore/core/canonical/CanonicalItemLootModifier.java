package com.darkfolklore.core.canonical;

import com.darkfolklore.core.config.FolkloreConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Rewrites only newly generated loot stacks. Existing stacks and registries are deliberately
 * untouched, which keeps legacy content loadable while routing future acquisition.
 */
public final class CanonicalItemLootModifier extends LootModifier {
    public static final MapCodec<CanonicalItemLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            IGlobalLootModifier.LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(modifier -> modifier.conditions),
            Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC)
                    .fieldOf("replacements")
                    .forGetter(CanonicalItemLootModifier::replacements)
    ).apply(instance, CanonicalItemLootModifier::new));

    private final Map<ResourceLocation, ResourceLocation> replacements;

    public CanonicalItemLootModifier(LootItemCondition[] conditions, Map<ResourceLocation, ResourceLocation> replacements) {
        super(conditions);
        this.replacements = Map.copyOf(replacements);
    }

    public Map<ResourceLocation, ResourceLocation> replacements() {
        return replacements;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!FolkloreConfig.CANONICALIZATION.get()) {
            return generatedLoot;
        }
        for (int index = 0; index < generatedLoot.size(); index++) {
            ItemStack original = generatedLoot.get(index);
            ResourceLocation sourceId = BuiltInRegistries.ITEM.getKey(original.getItem());
            ResourceLocation targetId = replacements.get(sourceId);
            if (targetId == null) {
                continue;
            }

            var target = BuiltInRegistries.ITEM.getOptional(targetId);
            if (target.isEmpty()) {
                continue;
            }
            ItemStack replacement = new ItemStack(target.get(), original.getCount());
            replacement.applyComponents(original.getComponentsPatch());
            generatedLoot.set(index, replacement);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return FolkloreLootModifiers.CANONICALIZE_ITEMS.get();
    }
}
