package com.github.c0c0tier.dungeon_defenders.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.function.BiFunction;

// SimpleTowerBlock avec une orientation : la roue applique déjà la rotation choisie à tout bloc
// qui déclare HORIZONTAL_FACING (ModNetworking#handlePlaceTower).
public class DirectionalTowerBlock extends SimpleTowerBlock {

    public DirectionalTowerBlock(Properties properties, BiFunction<BlockPos, BlockState, ? extends BlockEntity> blockEntityFactory) {
        super(properties, blockEntityFactory);
        this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    protected SimpleTowerBlock recreate(Properties properties) {
        return new DirectionalTowerBlock(properties, blockEntityFactory());
    }
}
