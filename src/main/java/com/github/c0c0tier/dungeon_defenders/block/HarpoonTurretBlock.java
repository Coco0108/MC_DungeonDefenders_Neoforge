package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTurretBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.HarpoonTurretBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

// "Harpoon Turret" (nom repris du plan Excel — Squire) : première tour de la catégorie
// "Turret", tire dans un cône orienté selon HORIZONTAL_FACING (voir
// AbstractTurretBlockEntity). Contrairement à SpikeBlockadeBlock, déclare une vraie propriété
// de BlockState : la rotation choisie dans la roue (TowerWheelScreen) est appliquée par
// ModNetworking.handlePlaceTower, qui gère déjà HORIZONTAL_FACING génériquement (code écrit par
// anticipation lors de la roue, jamais exercé jusqu'ici — premier vrai consommateur).
public class HarpoonTurretBlock extends BaseEntityBlock {

    public static final MapCodec<HarpoonTurretBlock> CODEC = simpleCodec(HarpoonTurretBlock::new);

    public HarpoonTurretBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HarpoonTurretBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide()
                ? null
                : createTickerHelper(blockEntityType, DungeonDefendersMod.HARPOON_TURRET_BE.get(), AbstractTurretBlockEntity::serverTick);
    }
}
