package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.entity.TrainingDummyBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
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
// qu'un TrainingDummyEntity existe juste au-dessus de lui (voir TrainingDummyBlockEntity).
//
// Pourquoi un bloc plutôt qu'un mannequin posé directement dans le `.nbt` de la taverne
// (décidé avec le joueur, 2026-08-31) : le bloc, lui, fait partie de la structure et se repose
// donc proprement à chaque chargement du monde, alors qu'une entité dépend du nettoyage
// d'entités de la zone (`TavernSpawn#clearZone`) pour ne pas se dupliquer. Le bloc vérifie
// "est-ce que mon mannequin est encore là ?" avant d'en invoquer un : deux exemplaires ne
// peuvent pas s'accumuler, même si le nettoyage ratait l'ancien. Il permet aussi de déplacer le
// mannequin en créatif sans retoucher au fichier de structure.
//
// Invisible, traversable, ciblable en créatif seulement : c'est un marqueur d'édition, même
// traitement que SpawnerBlock et PlayerSpawnBlock (voir ces classes pour le détail du
// raisonnement, identique ici).
public class TrainingDummyBlock extends BaseEntityBlock {

    public static final MapCodec<TrainingDummyBlock> CODEC = simpleCodec(TrainingDummyBlock::new);

    public TrainingDummyBlock(Properties properties) {
        super(properties);
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
        return RenderShape.INVISIBLE;
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
            return Shapes.block();
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
