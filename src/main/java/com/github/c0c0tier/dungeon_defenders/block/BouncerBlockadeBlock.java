package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractBlockadeBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.BouncerBlockadeBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// "Bouncer Blockade" (nom repris du plan Excel du joueur, feuille Tours — Squire) : même patron
// que SpikeBlockadeBlock, voir BouncerBlockadeBlockEntity pour le comportement (dégâts + repousse).
public class BouncerBlockadeBlock extends BaseEntityBlock {

    public static final MapCodec<BouncerBlockadeBlock> CODEC = simpleCodec(BouncerBlockadeBlock::new);

    public BouncerBlockadeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BouncerBlockadeBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // Même raisonnement que SpikeBlockadeBlock : hitbox à 1,5 bloc de haut pour qu'un monstre
    // ne puisse pas sauter sur le blockade et continuer son chemin par-dessus.
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.5D, 1.0D);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.5D, 1.0D);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide()
                ? null
                : createTickerHelper(blockEntityType, DungeonDefendersMod.BOUNCER_BLOCKADE_BE.get(), AbstractBlockadeBlockEntity::serverTick);
    }
}
