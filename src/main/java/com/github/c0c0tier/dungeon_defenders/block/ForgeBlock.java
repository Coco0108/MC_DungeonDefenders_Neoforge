package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.network.OpenForgePayload;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// La Forge : meuble de la taverne (comme le cristal de la taverne), sans PV ni block entity. Le
// clic droit ouvre ForgeScreen, dont le premier onglet est l'inventaire du joueur — c'est le
// SEUL endroit où il le consulte, il n'a pas d'inventaire portatif (décidé avec le joueur le
// 2026-11-16). D'autres onglets viendront (« on verra les précisions plus tard »).
//
// Rien n'est stocké dans le bloc : l'inventaire appartient au joueur (attachment
// FORGE_INVENTORY), toutes les Forges montrent donc le même, et chaque joueur voit le sien.
public class ForgeBlock extends Block {

    public static final MapCodec<ForgeBlock> CODEC = simpleCodec(ForgeBlock::new);

    public ForgeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    // Orientée comme le bloc de décor dont elle prend le modèle (dd_forge_v2, dessiné devant au nord) : posée, elle
    // regarde le joueur. Par défaut (structure sans la propriété, comme la taverne déployée avant qu'elle existe),
    // elle regarde le sud.
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final FormeAtelier FORME = new FormeAtelier("dd_forge_v2");

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FORME.get(state.getValue(FACING));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new OpenForgePayload().toVanillaClientbound());
        }
        return InteractionResult.SUCCESS;
    }
}
