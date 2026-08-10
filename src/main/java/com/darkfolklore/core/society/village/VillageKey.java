package com.darkfolklore.core.society.village;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public record VillageKey(String dimension, int regionX, int regionZ) {
    private static final int REGION_CHUNKS = 8;

    public static VillageKey at(ServerLevel level, BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        return new VillageKey(level.dimension().location().toString(),
                Math.floorDiv(chunk.x, REGION_CHUNKS), Math.floorDiv(chunk.z, REGION_CHUNKS));
    }

    public String serialized() {
        return dimension + "|" + regionX + "|" + regionZ;
    }

    public static VillageKey parse(String value) {
        String[] parts = value.split("\\|", -1);
        if (parts.length != 3) throw new IllegalArgumentException("Invalid village key " + value);
        return new VillageKey(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}
