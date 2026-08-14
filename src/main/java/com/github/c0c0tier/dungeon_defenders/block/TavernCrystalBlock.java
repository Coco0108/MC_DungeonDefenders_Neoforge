package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.client.gui.screen.MapSelectionScreen;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// Le cristal de la taverne : contrairement à EterniaCrystalBlock (celui des maps, avec des PV,
// visé par les ennemis), celui-ci n'a aucune mécanique de combat — un simple bloc dont le clic
// droit ouvre l'écran de choix de map (voir MapSelectionScreen). Pas de PV, pas de block
// entity, rien à synchroniser : contrairement au spawner, cet écran n'a besoin d'aucune donnée
// propre à CE bloc (la liste des maps est statique, la difficulté vient d'un attachment de
// Level déjà synchronisé) — inutile de passer par le système de Menu/MenuProvider, l'écran
// s'ouvre directement côté client.
public class TavernCrystalBlock extends Block {

    public static final MapCodec<TavernCrystalBlock> CODEC = simpleCodec(TavernCrystalBlock::new);

    public TavernCrystalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            Minecraft.getInstance().setScreen(new MapSelectionScreen());
        }
        return InteractionResult.SUCCESS;
    }
}
