package com.github.c0c0tier.dungeon_defenders.atelier;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A block that is not a cube: the converted model of a monster in its hiding pose ({@code "modele": true} in its sheet).
 * Its shape, for selection and collision, is the union of the boxes of its model; it never hides the faces of its
 * neighbours (registered with {@code noOcclusion}) and lets the sky light through, like any block that does not fill its cube.
 *
 * <p>Repris tel quel de l'Atelier (objet/BlocModele.java).
 */
public class BlocModele extends Block {
    private final VoxelShape shape;

    public BlocModele(BlockBehaviour.Properties properties, List<double[]> boxes) {
        super(properties);
        this.shape = boxes.stream().map(b -> Block.box(b[0], b[1], b[2], b[3], b[4], b[5]))
                .reduce(Shapes.empty(), Shapes::or).optimize();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }
}
