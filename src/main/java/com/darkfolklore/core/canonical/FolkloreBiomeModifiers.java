package com.darkfolklore.core.canonical;

import com.darkfolklore.core.DarkFolkloreCore;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class FolkloreBiomeModifiers {
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, DarkFolkloreCore.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<ConfigurableRemoveFeaturesBiomeModifier>> REMOVE_FEATURES =
            SERIALIZERS.register("remove_features_when_canonicalization_enabled", () -> ConfigurableRemoveFeaturesBiomeModifier.CODEC);
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<ConfigurableRemoveSpawnsBiomeModifier>> REMOVE_SPAWNS =
            SERIALIZERS.register("remove_spawns_when_canonicalization_enabled", () -> ConfigurableRemoveSpawnsBiomeModifier.CODEC);

    private FolkloreBiomeModifiers() {
    }

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }
}
