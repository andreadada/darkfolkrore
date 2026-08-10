package com.darkfolklore.core.canonical;

import com.darkfolklore.core.config.FolkloreConfig;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/** Removes audited duplicate natural-spawn entries only while canonicalization is enabled. */
public record ConfigurableRemoveSpawnsBiomeModifier(
        HolderSet<Biome> biomes,
        HolderSet<EntityType<?>> entityTypes
) implements BiomeModifier {
    public static final MapCodec<ConfigurableRemoveSpawnsBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(ConfigurableRemoveSpawnsBiomeModifier::biomes),
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE)
                    .fieldOf("entity_types").forGetter(ConfigurableRemoveSpawnsBiomeModifier::entityTypes)
    ).apply(instance, ConfigurableRemoveSpawnsBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (!FolkloreConfig.CANONICALIZATION.get() || phase != Phase.REMOVE || !biomes.contains(biome)) {
            return;
        }
        for (MobCategory category : MobCategory.values()) {
            builder.getMobSpawnSettings().getSpawner(category).removeIf(spawn ->
                    entityTypes.contains(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(spawn.type)));
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return FolkloreBiomeModifiers.REMOVE_SPAWNS.get();
    }
}
