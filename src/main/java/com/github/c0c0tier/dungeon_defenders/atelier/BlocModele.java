package com.github.c0c0tier.dungeon_defenders.atelier;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A block that is not a cube: the converted model of a monster in its hiding pose ({@code "modele": true} in its sheet).
 * Its shape, for selection and collision, is the union of the boxes of its model; it never hides the faces of its
 * neighbours (registered with {@code noOcclusion}) and lets the sky light through, like any block that does not fill its cube.
 *
 * <p>Repris de l'Atelier (objet/BlocModele.java), avec en plus {@link #isPathfindable} (2026-11-28).
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

    /**
     * Pour les monstres, un bloc modèle qui a une forme est un obstacle, pas de l'air. Par défaut, le pathfinding vanilla
     * traverse tout ce qui n'est pas un cube plein : il menait les monstres droit dans la margelle du puits (14 px de
     * haut, vue au même niveau que le sol, donc sans saut) et à travers les passerelles (planches en haut du bloc, vues
     * comme du vide), d'où ils tombaient dans l'eau (vu par le gametest {@code map_paths_*}, 2026-11-28). Bloqué, le bloc
     * est contourné, ou franchi par-dessus : le pathfinding pose alors le monstre sur le haut de sa forme (il saute sur
     * la margelle, il marche sur la passerelle). Un bloc sans forme (décor traversable) reste traversé.
     */
    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return type == PathComputationType.LAND ? shape.isEmpty() : super.isPathfindable(state, type);
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
