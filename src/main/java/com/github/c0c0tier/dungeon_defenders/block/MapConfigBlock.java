package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.block.entity.MapConfigBlockEntity;
import com.github.c0c0tier.dungeon_defenders.menu.MapConfigMenuProvider;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Bloc de configuration d'une map : nom affiché, ordre dans son pack, nombre de vagues,
// multiplicateur de score. Posé par le créateur DANS la map qu'il construit ; ses réglages
// partent dans le `.nbt` au moment de la sauvegarde avec un bloc de structure, et sont relus
// sans avoir à poser la map (voir MapConfigBlockEntity et MapRegistry).
//
// C'est ce qui permet de créer une map **entièrement en jeu** : aucun fichier de configuration à
// écrire à côté, aucun identifiant à enregistrer dans le code. Un pack tiers n'a qu'à embarquer
// son `.nbt` pour que ses maps apparaissent.
//
// Invisible, traversable, ciblable en créatif seulement : marqueur d'édition, même traitement
// que SpawnerBlock, PlayerSpawnBlock, TrainingDummyBlock et NoBuildZoneBlock.
public class MapConfigBlock extends BaseEntityBlock {

    public static final MapCodec<MapConfigBlock> CODEC = simpleCodec(MapConfigBlock::new);

    public MapConfigBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MapConfigBlockEntity(pos, state);
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

    // Réservé au créatif, comme la configuration d'un spawner : une map est censée être figée
    // une fois construite. Le verrou du ciblage (getShape ci-dessus) rend déjà le bloc
    // introuvable en survie ; ce test reste là par sécurité, au cas où un autre chemin
    // d'interaction existerait un jour.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!player.isCreative()) {
            player.sendSystemMessage(Component.translatable("dungeon_defenders.map_config.creative_only"));
            return InteractionResult.SUCCESS;
        }

        player.openMenu(new MapConfigMenuProvider(pos));
        return InteractionResult.CONSUME;
    }
}
