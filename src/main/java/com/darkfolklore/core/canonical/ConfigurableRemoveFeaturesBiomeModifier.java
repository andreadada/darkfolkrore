package com.darkfolklore.core.canonical;

import com.darkfolklore.core.config.FolkloreConfig;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/** Removes audited duplicate worldgen only while canonicalization is enabled. */
public record ConfigurableRemoveFeaturesBiomeModifier(
        HolderSet<Biome> biomes,
        HolderSet<PlacedFeature> features,
        GenerationStep.Decoration step
) implements BiomeModifier {
    public static final MapCodec<ConfigurableRemoveFeaturesBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(ConfigurableRemoveFeaturesBiomeModifier::biomes),
            PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(ConfigurableRemoveFeaturesBiomeModifier::features),
            GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(ConfigurableRemoveFeaturesBiomeModifier::step)
    ).apply(instance, ConfigurableRemoveFeaturesBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (FolkloreConfig.CANONICALIZATION.get() && phase == Phase.REMOVE && biomes.contains(biome)) {
            builder.getGenerationSettings().getFeatures(step).removeIf(features::contains);
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return FolkloreBiomeModifiers.REMOVE_FEATURES.get();
    }
}
