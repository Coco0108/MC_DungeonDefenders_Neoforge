package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.entity.TrainingDummyBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Le "spawner de mannequin" : ce bloc n'a aucun comportement propre, il se contente de garantir
// qu'un TrainingDummyEntity existe à sa position (voir TrainingDummyBlockEntity).
//
// Pourquoi un bloc plutôt qu'un mannequin posé directement dans le `.nbt` de la taverne
// (décidé avec le joueur, 2026-08-31) : le bloc, lui, fait partie de la structure et se repose
// donc proprement à chaque chargement du monde, alors qu'une entité dépend du nettoyage
// d'entités de la zone (`TavernSpawn#clearZone`) pour ne pas se dupliquer. Le bloc vérifie
// "est-ce que mon mannequin est encore là ?" avant d'en invoquer un : deux exemplaires ne
// peuvent pas s'accumuler, même si le nettoyage ratait l'ancien. Il permet aussi de déplacer le
// mannequin en créatif sans retoucher au fichier de structure.
//
// Traversable et ciblable en créatif seulement, comme SpawnerBlock et PlayerSpawnBlock (voir ces
// classes pour le raisonnement). Depuis le 2026-11-27, il n'est plus invisible : c'est lui qui
// dessine le mannequin (modèle dd_dummy de l'Atelier, orienté), à la place du zombie que
// montrait l'entité.
public class TrainingDummyBlock extends BaseEntityBlock {

    public static final MapCodec<TrainingDummyBlock> CODEC = simpleCodec(TrainingDummyBlock::new);

    public TrainingDummyBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    // Orientée comme le bloc de décor dont elle prend le modèle (dd_dummy, dessiné devant au nord) : posée, elle
    // regarde le joueur. Par défaut (structure sans la propriété, comme la taverne déployée avant qu'elle existe),
    // elle regarde le sud.
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final FormeAtelier FORME = new FormeAtelier("dd_dummy");

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
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrainingDummyBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // Depuis le 2026-11-27, c'est le bloc qui montre le mannequin (modèle dd_dummy de l'Atelier) ; l'entité, elle,
        // n'est plus dessinée (NoopRenderer) : elle reste la cible des tours, invisible dans le modèle.
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof Player player
                && player.isCreative()) {
            return FORME.get(state.getValue(FACING));
        }
        return Shapes.empty();
    }

    // Retirer le bloc emporte son mannequin : sans ça, casser le support en créatif laisserait
    // une entité orpheline que plus rien ne gère (le nettoyage de zone de la taverne ne passe
    // qu'au chargement du monde, et une map n'en a pas du tout).
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        TrainingDummyBlockEntity.discardDummy(level, pos);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide()
                ? null
                : createTickerHelper(blockEntityType, DungeonDefendersMod.TRAINING_DUMMY_BE.get(),
                        TrainingDummyBlockEntity::serverTick);
    }
}
