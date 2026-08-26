package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.block.entity.ManaChestBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.menu.ManaChestConfigMenuProvider;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;

// "Coffre de mana" (feuille "Idées" du plan Excel du joueur — sert aussi à distribuer des
// armes plus tard, hors scope pour l'instant, voir doc/05-etat-et-problemes-connus.md). Meuble
// de map, comme le Cristal d'Eternia/le Spawner : posé par le créateur, pas par un joueur en
// jeu (aucun item ne le pose via la roue ni un clic droit sur un bloc, contrairement aux tours).
public class ManaChestBlock extends BaseEntityBlock {

    public static final MapCodec<ManaChestBlock> CODEC = simpleCodec(ManaChestBlock::new);

    // true une fois ouvert pour la vague en cours : le coffre devient invisible et traversable
    // (voir getRenderShape/getShape/getCollisionShape) plutôt que rester visible mais inerte —
    // comme dans le jeu de référence, décidé avec le joueur. Redevient false (bloc plein,
    // visible) à chaque nouvelle Construction, voir respawnAll ci-dessous.
    public static final BooleanProperty OPENED = BooleanProperty.create("opened");

    public ManaChestBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(OPENED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPENED);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaChestBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(OPENED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPENED) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPENED) ? Shapes.empty() : Shapes.block();
    }

    /**
     * Remet tous les coffres ouverts de {@code level} à leur état "plein" (visible, solide) —
     * appelé à chaque nouvelle Construction (voir PhaseTransitions#enterBuild). Copie
     * défensive d'ACTIVE_MANA_CHESTS : le registre peut changer (dé/rechargement de chunk)
     * pendant qu'on le parcourt, même raisonnement que PhaseTransitions#recomputeWaveEnemiesTotal.
     */
    public static void respawnAll(Level level) {
        for (BlockPos pos : new ArrayList<>(level.getData(ModAttachments.ACTIVE_MANA_CHESTS))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ManaChestBlock && state.getValue(OPENED)) {
                level.setBlock(pos, state.setValue(OPENED, false), Block.UPDATE_ALL);
            }
        }
    }

    /**
     * Créatif : ouvre l'écran de configuration (quantité de mana) — même logique que
     * SpawnerBlock, la config est censée être figée une fois la map construite, pas modifiable
     * en survie. Survie : tente de donner le mana au joueur, une fois par vague (voir
     * ManaChestBlockEntity#tryOpen) — décidé avec le joueur (2026-08-26) : ouvrable **quelle
     * que soit la phase**, plus seulement en Construction, un joueur peut vouloir aller
     * chercher du mana en pleine Combat.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player.isCreative()) {
            player.openMenu(new ManaChestConfigMenuProvider(pos));
            return InteractionResult.CONSUME;
        }

        if (!(level.getBlockEntity(pos) instanceof ManaChestBlockEntity chest)) {
            return InteractionResult.PASS;
        }

        int currentWave = level.getData(ModAttachments.CURRENT_WAVE);
        if (!chest.tryOpen(player, currentWave)) {
            player.sendSystemMessage(Component.translatable("dungeon_defenders.mana_chest.already_opened"));
            return InteractionResult.SUCCESS;
        }

        player.sendSystemMessage(Component.translatable(
                "dungeon_defenders.mana_chest.opened",
                chest.getManaAmount(), player.getData(ModAttachments.MANA), ModAttachments.MAX_MANA));
        return InteractionResult.SUCCESS;
    }
}
