package com.github.c0c0tier.dungeon_defenders.atelier;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Un {@link BlocModele} au modèle animé ({@code anime} dans sa fiche) : son {@link BlocAnimeEntity} est dessiné par
 * GeckoLib ; le modèle converti ne sert plus qu'à l'objet, aux particules et à la forme. Repris de
 * {@code objet/BlocAnime.java} de l'Atelier, sans ses interactions de test (clic droit pour ouvrir, coup pour
 * {@code hurt}) : ici, c'est un décor ; ses rôles se jouent par {@code /dd_bloc_anim <pos> <role>}.
 */
public class BlocAnime extends BlocModele implements EntityBlock {
    private final Supplier<BlockEntityType<BlocAnimeEntity>> type;

    public BlocAnime(BlockBehaviour.Properties properties, List<double[]> boxes, Supplier<BlockEntityType<BlocAnimeEntity>> type) {
        super(properties, boxes);
        this.type = type;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
