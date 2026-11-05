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

/** Places a crystal on a small, irregular patch of disturbed forest floor. */
public final class LifeEchoSiteFeature extends Feature<NoneFeatureConfiguration> {
    public LifeEchoSiteFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        if (!level.isEmptyBlock(origin) || !isNaturalGround(level.getBlockState(origin.below()))) return false;

        RandomSource random = context.random();
        int radiusX = 4 + random.nextInt(3);
        int radiusZ = 3 + random.nextInt(3);
        double driftX = random.nextDouble() * 0.45D - 0.225D;
        double driftZ = random.nextDouble() * 0.45D - 0.225D;

        level.setBlock(origin, ModBlocks.LIFE_ECHO.get().defaultBlockState(), 2);
        level.setBlock(origin.below(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
        for (int[] offset : new int[][]{{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{1,1}}) {
            if (random.nextFloat() > 0.58F) continue;
            BlockPos p = origin.offset(offset[0], -1, offset[1]);
            if (isNaturalGround(level.getBlockState(p))) {
                level.setBlock(p, random.nextBoolean() ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                        : Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 2);
            }
        }

        for (int dx = -radiusX; dx <= radiusX; dx++) {
            for (int dz = -radiusZ; dz <= radiusZ; dz++) {
                if (dx == 0 && dz == 0) continue;
                double nx = (dx - driftX) / radiusX;
                double nz = (dz - driftZ) / radiusZ;
                double edgeNoise = (random.nextFloat() - 0.5D) * 0.34D;
                double shape = nx * nx + nz * nz + edgeNoise;
                if (shape > 1.0D || (shape > 0.78D && random.nextFloat() < 0.38F)) continue;

                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos floor = new BlockPos(x, surfaceY - 1, z);
                BlockPos surface = floor.above();
                if (!isNaturalGround(level.getBlockState(floor)) || !level.isEmptyBlock(surface)) continue;

                float roll = random.nextFloat();
                double center = Math.sqrt(nx * nx + nz * nz);
                if (center < 0.52D && roll < 0.26F) {
                    level.setBlock(floor, random.nextBoolean() ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                            : Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 2);
                } else if (center < 0.78D && roll < 0.48F) {
                    level.setBlock(floor, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
                } else if (roll < 0.73F) {
                    level.setBlock(floor, Blocks.ROOTED_DIRT.defaultBlockState(), 2);
                } else if (roll < 0.88F) {
                    level.setBlock(floor, Blocks.PODZOL.defaultBlockState(), 2);
                } else {
                    level.setBlock(floor, Blocks.COARSE_DIRT.defaultBlockState(), 2);
                }
                if (random.nextFloat() < (center > 0.68D ? 0.24F : 0.11F)) {
                    level.setBlock(surface, Blocks.MOSS_CARPET.defaultBlockState(), 2);
                }
            }
        }

        for (int i = 0; i < 5; i++) {
            int dx = random.nextInt(radiusX * 2 + 1) - radiusX;
            int dz = random.nextInt(radiusZ * 2 + 1) - radiusZ;
            if (dx * dx + dz * dz < 5) continue;
            int x = origin.getX() + dx;
            int z = origin.getZ() + dz;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos p = new BlockPos(x, y, z);
            if (!level.isEmptyBlock(p) || !isNaturalGround(level.getBlockState(p.below()))) continue;
            level.setBlock(p, random.nextFloat() < 0.65F
                    ? Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState()
                    : Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
        }
        return true;
    }

    private static boolean isNaturalGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.PODZOL)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.ROOTED_DIRT);
    }
}