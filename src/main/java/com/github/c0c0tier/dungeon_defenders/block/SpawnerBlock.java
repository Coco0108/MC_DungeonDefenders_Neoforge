package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.entity.SpawnerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.PhaseTransitions;
import com.github.c0c0tier.dungeon_defenders.menu.SpawnerConfigMenuProvider;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Décidé avec le joueur (2026-08-25) : le spawner ne doit plus jamais être un obstacle
// physique, ni pour les monstres qui essaient de se déplacer/spawner, ni pour le joueur — plus
// proche du jeu de référence, où un point de spawn est une zone/un marqueur, pas un objet
// solide. Voir doc/02-gameplay.md pour le détail de la mécanique de forme/ciblage.
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

    // Jamais rendu : ni en Construction ni en Combat, pour personne — voir la classe. Le
    // repérage en jeu passe par l'aperçu de composition (SpawnerBlockEntityRenderer, texte à
    // travers les murs en Construction) et, en créatif, par le contour de visée (getShape).
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // Toujours vide : contrairement à getShape (ciblage/contour), lu par la résolution de
    // collision physique — un monstre ou un joueur qui essaie de se déplacer sur cette
    // position passe à travers, quelle que soit la phase ou le mode de jeu.
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    // Pleine (ciblable/cliquable) uniquement pour un joueur en mode créatif : c'est cette forme
    // (pas getCollisionShape) que le jeu utilise pour le rayon de visée du joueur — le contour
    // de sélection ET le clic droit (useWithoutItem). Vide pour tout le reste (survie, ou tout
    // appel sans entité précise, ex. génération de monde/pathfinding) : le bloc devient
    // strictement introuvable/inutilisable en dehors du créatif, sans vérification
    // supplémentaire dans useWithoutItem.
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof Player player
                && player.isCreative()) {
            return Shapes.block();
        }
        return Shapes.empty();
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide()
                ? null
                : createTickerHelper(blockEntityType, DungeonDefendersMod.SPAWNER_BE.get(), SpawnerBlockEntity::serverTick);
    }

    /**
     * Shift + clic droit : bascule Construction/Combat (harnais de test, en attendant un
     * vrai déclencheur). Clic droit seul : ouvre l'écran de configuration — créatif
     * uniquement (voir openConfigScreen), les spawners d'une vraie partie sont censés être
     * déjà configurés dans la structure de la map, pas modifiables en survie. Les deux ne
     * sont de toute façon plus jamais atteignables hors créatif depuis getShape ci-dessus
     * (un joueur en survie ne peut plus cibler ce bloc) : le check `isCreative()` ici reste
     * en place par prudence (double vérification), pas parce qu'il est encore nécessaire.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return togglePhase(level, player);
        }
        return openConfigScreen(level, pos, player);
    }

    private static InteractionResult togglePhase(Level level, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        GamePhase current = GamePhase.values()[level.getData(ModAttachments.GAME_PHASE)];
        GamePhase next = current == GamePhase.BUILD ? GamePhase.COMBAT : GamePhase.BUILD;

        if (next == GamePhase.COMBAT) {
            PhaseTransitions.enterCombat(level);
        } else {
            PhaseTransitions.enterBuild(level);
        }

        player.sendSystemMessage(Component.translatable(
                "dungeon_defenders.spawner.phase_toggled", Component.translatable(next.translationKey())));

        return InteractionResult.SUCCESS;
    }

    // Comme pour un bloc de structure vanilla : la configuration d'un spawner est censée être
    // figée une fois la map construite (voir doc/02-gameplay.md) — pas d'accès en survie.
    private static InteractionResult openConfigScreen(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!player.isCreative()) {
            player.sendSystemMessage(Component.translatable("dungeon_defenders.spawner.config_creative_only"));
            return InteractionResult.SUCCESS;
        }

        player.openMenu(new SpawnerConfigMenuProvider(pos));
        return InteractionResult.CONSUME;
    }
}
