package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.entity.SpawnerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class SpawnerBlock extends BaseEntityBlock {

    public static final MapCodec<SpawnerBlock> CODEC = simpleCodec(SpawnerBlock::new);

    public SpawnerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpawnerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide()
                ? null
                : createTickerHelper(blockEntityType, DungeonDefendersMod.SPAWNER_BE.get(), SpawnerBlockEntity::serverTick);
    }

    /** Bascule Construction/Combat au clic droit. Harnais de test, en attendant un vrai déclencheur. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        GamePhase current = GamePhase.values()[level.getData(ModAttachments.GAME_PHASE)];
        GamePhase next = current == GamePhase.BUILD ? GamePhase.COMBAT : GamePhase.BUILD;

        level.setData(ModAttachments.GAME_PHASE, next.ordinal());
        level.syncData(ModAttachments.GAME_PHASE);

        player.sendSystemMessage(Component.translatable(
                "dungeon_defenders.spawner.phase_toggled", Component.translatable(next.translationKey())));

        return InteractionResult.SUCCESS;
    }
}
