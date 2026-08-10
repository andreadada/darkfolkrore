package com.darkfolklore.core.persistence;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public record WorldPosition(String dimension, int x, int y, int z) {
    public static WorldPosition of(ServerLevel level, BlockPos pos) {
        return new WorldPosition(level.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockPos blockPos() {
        return new BlockPos(x, y, z);
    }

    public double distanceSquared(BlockPos other) {
        return blockPos().distSqr(other);
    }
}
