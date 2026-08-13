package com.darkfolklore.core.endgame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;

public final class DemonInvocationMultiblock {
    public static final ResourceLocation FRAME = ResourceLocation.parse("the_day_of_the_beast:demon_frame");
    public static final ResourceLocation FRAME_UNCLOCKED = ResourceLocation.parse("the_day_of_the_beast:demon_frame_unclocked");
    public static final ResourceLocation BRICKS = ResourceLocation.parse("the_day_of_the_beast:demon_bricks");
    public static final ResourceLocation BROKEN_BRICKS = ResourceLocation.parse("the_day_of_the_beast:demon_broken_bricks");
    private static final int MAX_REPORTED_MISSING = 12;
    private DemonInvocationMultiblock() {}

    public static boolean isFrame(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return FRAME.equals(id) || FRAME_UNCLOCKED.equals(id);
    }

    public static ValidationResult validate(ServerLevel level, BlockPos frame) {
        List<MissingBlock> missing = new ArrayList<>();
        checkEither(level, frame, frame, List.of(FRAME, FRAME_UNCLOCKED), missing);
        int[] sides = {-3, 3};
        for (int dx : sides) for (int dz : sides) {
            check(level, frame, frame.offset(dx, -1, dz), BROKEN_BRICKS, missing);
            for (int y = 0; y <= 4; y++) check(level, frame, frame.offset(dx, y, dz), BRICKS, missing);
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int distance = 1; distance <= 3; distance++) {
                check(level, frame, frame.relative(direction, distance).below(), BROKEN_BRICKS, missing);
            }
        }
        return new ValidationResult(missing.isEmpty(), List.copyOf(missing));
    }

    private static void check(ServerLevel level, BlockPos origin, BlockPos pos, ResourceLocation expected, List<MissingBlock> missing) {
        if (missing.size() >= MAX_REPORTED_MISSING) return;
        if (!level.hasChunkAt(pos)) { missing.add(new MissingBlock(relative(origin, pos), expected.toString(), "unloaded")); return; }
        ResourceLocation found = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        if (!expected.equals(found)) missing.add(new MissingBlock(relative(origin, pos), expected.toString(), found.toString()));
    }

    private static void checkEither(ServerLevel level, BlockPos origin, BlockPos pos, List<ResourceLocation> expected, List<MissingBlock> missing) {
        if (!level.hasChunkAt(pos)) { missing.add(new MissingBlock(relative(origin, pos), expected.toString(), "unloaded")); return; }
        ResourceLocation found = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        if (!expected.contains(found)) missing.add(new MissingBlock(relative(origin, pos), expected.toString(), found.toString()));
    }

    private static String relative(BlockPos origin, BlockPos pos) {
        return (pos.getX()-origin.getX()) + "," + (pos.getY()-origin.getY()) + "," + (pos.getZ()-origin.getZ());
    }

    public record MissingBlock(String relativePosition, String expected, String found) {}
    public record ValidationResult(boolean valid, List<MissingBlock> missing) {}
}
