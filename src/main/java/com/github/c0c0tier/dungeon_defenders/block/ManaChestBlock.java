package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.block.entity.ManaChestBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.menu.ManaChestConfigMenuProvider;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// "Coffre de mana" (feuille "Idées" du plan Excel du joueur — sert aussi à distribuer des
// armes plus tard, hors scope pour l'instant, voir doc/05-etat-et-problemes-connus.md). Meuble
// de map, comme le Cristal d'Eternia/le Spawner : posé par le créateur, pas par un joueur en
// jeu (aucun item ne le pose via la roue ni un clic droit sur un bloc, contrairement aux tours).
public class ManaChestBlock extends BaseEntityBlock {

    public static final MapCodec<ManaChestBlock> CODEC = simpleCodec(ManaChestBlock::new);

    public ManaChestBlock(Properties properties) {
        super(properties);
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
        return RenderShape.MODEL;
    }

    /**
     * Créatif : ouvre l'écran de configuration (quantité de mana) — même logique que
     * SpawnerBlock, la config est censée être figée une fois la map construite, pas modifiable
     * en survie. Survie : tente de donner le mana au joueur, une fois par vague pendant la
     * Construction (voir ManaChestBlockEntity#tryOpen) — pas en Combat, où les joueurs sont
     * censés se battre plutôt qu'ouvrir des coffres.
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

        if (level.getData(ModAttachments.GAME_PHASE) != GamePhase.BUILD.ordinal()) {
            player.sendSystemMessage(Component.translatable("dungeon_defenders.mana_chest.build_phase_only"));
            return InteractionResult.SUCCESS;
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
