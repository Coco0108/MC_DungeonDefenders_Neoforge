package com.github.c0c0tier.dungeon_defenders.atelier;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A {@link BlocModele} that turns when placed ({@code "orientation": "horizontale"}): its model is drawn front towards the
 * north, and the block state {@code facing} says where that front looks, like a furnace. Placed, it faces the player, so a
 * wall piece (modelled against the south face) ends up with its back against the wall the player looks at. The build turns
 * the model ({@code y} of the block state); here, the shape turns with it. Structures can rotate and mirror it.
 *
 * <p>Repris tel quel de l'Atelier (objet/BlocModeleOriente.java).
 */
public class BlocModeleOriente extends BlocModele {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    public BlocModeleOriente(BlockBehaviour.Properties properties, List<double[]> boxes) {
        super(properties, boxes);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            shapes.put(facing, boxes.stream().map(b -> turn(b, facing)).map(b -> Block.box(b[0], b[1], b[2], b[3], b[4], b[5]))
                    .reduce(Shapes.empty(), Shapes::or).optimize());
        }
    }

    /**
     * A box of the north-facing model, turned clockwise (seen from above) as many quarter turns as {@code y} in the block
     * state: 90 for east, 180 for south, 270 for west. A quarter turn takes (x, z) to (16 - z, x).
     */
    static double[] turn(double[] box, Direction facing) {
        double[] b = box.clone();
        int turns = Math.floorMod(facing.get2DDataValue() - Direction.NORTH.get2DDataValue(), 4);
        for (int i = 0; i < turns; i++) {
            b = new double[] {16 - b[5], b[1], b[0], 16 - b[2], b[4], b[3]};
        }
        return b;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
