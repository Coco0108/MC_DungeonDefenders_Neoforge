package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Le cristal de la taverne n'a toujours aucune mécanique (voir {@code TavernCrystalBlock}) : ce block entity n'existe
 * que pour son modèle animé ({@code dd_cristal_v2}, qui boucle {@code idle}). Il ne joue jamais {@code death} : ce
 * cristal ne peut pas être détruit ; c'est celui des maps qui la joue ({@link EterniaCrystalBlockEntity}).
 */
public class TavernCrystalBlockEntity extends AnimatedBlockEntity {
    public TavernCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.TAVERN_CRYSTAL_BE.get(), pos, state, "dd_cristal_v2");
    }
}
