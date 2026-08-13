package com.darkfolklore.core.encounter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Optional;

/** Finds already-loaded, ground-backed manifestation positions without chunk generation or chunk loading. */
public final class EncounterSpawnFinder {
    private EncounterSpawnFinder() {}

    public static Optional<BlockPos> aroundPlayer(ServerLevel level, ServerPlayer player,
                                                   int minimumDistance, int maximumDistance, long salt) {
        int span = Math.max(8, maximumDistance - minimumDistance);
        for (int attempt = 0; attempt < 12; attempt++) {
            long mixed = mix(salt + 0x9E3779B97F4A7C15L * attempt);
            double angle = ((mixed >>> 11) * 0x1.0p-53) * Math.PI * 2.0;
            int distance = minimumDistance + Math.floorMod((int) mixed, span + 1);
            int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            BlockPos probe = new BlockPos(x, player.getBlockY(), z);
            if (!level.hasChunkAt(probe)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos below = pos.below();
            if (!level.hasChunkAt(pos) || !level.hasChunkAt(below) || !level.getWorldBorder().isWithinBounds(pos)) continue;
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) continue;
            if (!level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) continue;
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
            return Optional.of(pos);
        }
        return Optional.empty();
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
