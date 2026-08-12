package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.PhaseTransitions;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class EterniaCrystalBlock extends BaseEntityBlock {

    // La bonne syntaxe pour déclarer le codec d'un bloc simple
    public static final MapCodec<EterniaCrystalBlock> CODEC = simpleCodec(EterniaCrystalBlock::new);

    public EterniaCrystalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EterniaCrystalBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Crée une boîte qui fait 1 bloc de large (de 0 à 16) mais 3 blocs de haut (de 0 à 48)
        return Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 3.0D, 1.0D);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // La hitbox de sélection (le contour noir quand on le regarde) s'aligne aussi sur les 3 blocs de haut
        return Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 3.0D, 1.0D);
    }

    /** Dégâts infligés au clic droit à main nue en Combat. Harnais de test. */
    private static final int DEBUG_DAMAGE_ON_USE = 10;

    /**
     * Clic droit sur le cristal : en Construction, bascule "prêt" (déclenche le Combat une
     * fois tous les joueurs présents prêts, voir toggleReady) ; en Combat, garde l'ancien
     * harnais de test qui inflige des dégâts (utile pour tester la destruction du cristal
     * sans attendre une vraie vague).
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // Le client se contente de prédire le succès ; la logique tourne côté serveur.
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (level.getData(ModAttachments.GAME_PHASE) == GamePhase.BUILD.ordinal()) {
            return toggleReady(level, player);
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof EterniaCrystalBlockEntity crystal)) {
            return InteractionResult.PASS;
        }

        crystal.damage(DEBUG_DAMAGE_ON_USE);
        player.sendSystemMessage(Component.translatable(
                "dungeon_defenders.eternia_crystal.damaged", DEBUG_DAMAGE_ON_USE, crystal.getCrystalHealth()));

        return InteractionResult.SUCCESS;
    }

    // Déclencheur du Combat : il faut que tous les joueurs actuellement dans cette Level
    // (pas tout le serveur — une future map/dimension différente aura ses propres joueurs,
    // voir 05-etat-et-problemes-connus.md) aient cliqué sur le cristal. "Prêt" se remet à
    // faux pour tout le monde une fois le Combat lancé (voir PhaseTransitions#enterCombat).
    private static InteractionResult toggleReady(Level level, Player player) {
        boolean ready = !player.getData(ModAttachments.READY);
        player.setData(ModAttachments.READY, ready);
        player.syncData(ModAttachments.READY);

        List<? extends Player> players = level.players();
        long readyCount = players.stream().filter(p -> p.getData(ModAttachments.READY)).count();

        for (Player p : players) {
            p.sendSystemMessage(Component.translatable(
                    "dungeon_defenders.eternia_crystal.ready_progress", readyCount, players.size()));
        }

        if (readyCount == players.size()) {
            PhaseTransitions.enterCombat(level);
            for (Player p : players) {
                p.sendSystemMessage(Component.translatable(
                        "dungeon_defenders.spawner.phase_toggled", Component.translatable(GamePhase.COMBAT.translationKey())));
            }
        }

        return InteractionResult.SUCCESS;
    }
}