package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.entity.SpikeBlockadeBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// "Spike Blockade" (nom repris du plan Excel du joueur, feuille Tours — Squire) : un mur qui
// bloque le passage (gratuit, un bloc plein bloque déjà naturellement) et pique les monstres à
// son contact (voir SpikeBlockadeBlockEntity). Premier tower du mod, remplace l'ancien
// SpikeTrapBlock (piège au sol au mécanisme différent, supprimé — voir doc/02-gameplay.md).
public class SpikeBlockadeBlock extends BaseEntityBlock {

    public static final MapCodec<SpikeBlockadeBlock> CODEC = simpleCodec(SpikeBlockadeBlock::new);

    public SpikeBlockadeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpikeBlockadeBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide()
                ? null
                : createTickerHelper(blockEntityType, DungeonDefendersMod.SPIKE_BLOCKADE_BE.get(), SpikeBlockadeBlockEntity::serverTick);
    }
}
