package ru.amir.traces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A decorative, non-cubic cluster of life-echo crystals. */
public final class LifeEchoBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(5.0D, 0.0D, 5.0D, 11.0D, 2.0D, 11.0D),
            Block.box(6.0D, 1.0D, 6.0D, 10.0D, 12.0D, 10.0D),
            Block.box(7.0D, 12.0D, 7.0D, 9.0D, 15.0D, 9.0D),
            Block.box(7.5D, 15.0D, 7.5D, 8.5D, 16.0D, 8.5D),
            Block.box(3.0D, 0.0D, 7.0D, 6.0D, 6.0D, 10.0D),
            Block.box(3.5D, 6.0D, 7.5D, 5.5D, 9.0D, 9.5D),
            Block.box(10.0D, 0.0D, 6.0D, 13.0D, 5.0D, 9.0D),
            Block.box(10.5D, 5.0D, 6.5D, 12.5D, 8.0D, 8.5D),
            Block.box(6.0D, 0.0D, 10.0D, 9.0D, 4.0D, 13.0D),
            Block.box(6.5D, 4.0D, 10.5D, 8.5D, 6.5D, 12.5D),
            Block.box(3.0D, 0.0D, 3.0D, 6.0D, 5.0D, 6.0D));

    public LifeEchoBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }
}
