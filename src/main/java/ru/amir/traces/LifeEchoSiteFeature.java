package ru.amir.traces;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class LifeEchoSiteFeature extends Feature<NoneFeatureConfiguration> {
    public LifeEchoSiteFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        BlockState ground = level.getBlockState(origin.below());
        if (!level.isEmptyBlock(origin) || !isNaturalGround(ground)) return false;

        level.setBlock(origin, ModBlocks.LIFE_ECHO.get().defaultBlockState(), 2);
        level.setBlock(origin.below(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);

        RandomSource random = context.random();
        int rotation = random.nextInt(4);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared == 0 || distanceSquared > 8) continue;

                int xOffset = rotateX(dx, dz, rotation);
                int zOffset = rotateZ(dx, dz, rotation);
                int x = origin.getX() + xOffset;
                int z = origin.getZ() + zOffset;
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos floorPos = new BlockPos(x, surfaceY - 1, z);
                BlockPos surfacePos = floorPos.above();
                BlockState floor = level.getBlockState(floorPos);
                if (!isNaturalGround(floor) || !level.isEmptyBlock(surfacePos)) continue;

                float roll = random.nextFloat();
                if (distanceSquared <= 4 && roll < 0.46F) {
                    level.setBlock(floorPos, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
                } else if (distanceSquared <= 4 && roll < 0.72F) {
                    level.setBlock(floorPos, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
                } else if (roll < 0.84F) {
                    level.setBlock(floorPos, Blocks.ROOTED_DIRT.defaultBlockState(), 2);
                } else if (roll < 0.93F) {
                    level.setBlock(floorPos, Blocks.PODZOL.defaultBlockState(), 2);
                } else {
                    level.setBlock(floorPos, Blocks.COARSE_DIRT.defaultBlockState(), 2);
                }

                if (random.nextFloat() < (distanceSquared > 4 ? 0.35F : 0.18F)) {
                    level.setBlock(surfacePos, Blocks.MOSS_CARPET.defaultBlockState(), 2);
                }
            }
        }

        placeBrokenMarker(level, origin, -2, -1, random);
        placeBrokenMarker(level, origin, 2, 1, random);
        return true;
    }

    private static void placeBrokenMarker(WorldGenLevel level, BlockPos origin,
                                          int xOffset, int zOffset, RandomSource random) {
        if (random.nextBoolean()) return;
        int x = origin.getX() + xOffset;
        int z = origin.getZ() + zOffset;
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos floorPos = new BlockPos(x, surfaceY - 1, z);
        BlockPos markerPos = floorPos.above();
        if (isNaturalGround(level.getBlockState(floorPos)) && level.isEmptyBlock(markerPos)) {
            level.setBlock(markerPos, Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState(), 2);
        }
    }

    private static boolean isNaturalGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.PODZOL)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.ROOTED_DIRT);
    }

    private static int rotateX(int x, int z, int rotation) {
        return switch (rotation) {
            case 1 -> -z;
            case 2 -> -x;
            case 3 -> z;
            default -> x;
        };
    }

    private static int rotateZ(int x, int z, int rotation) {
        return switch (rotation) {
            case 1 -> x;
            case 2 -> -z;
            case 3 -> -x;
            default -> z;
        };
    }
}
