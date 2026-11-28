package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * La Forge ne stocke toujours rien (l'inventaire appartient au joueur, voir {@code ForgeBlock}) : ce block entity tient
 * son modèle animé ({@code dd_forge_v2}). {@code open} quand un premier joueur ouvre son écran, {@code opened} tant
 * qu'il reste ouvert, {@code close} quand le dernier le ferme (voir {@code ForgeViewers}).
 */
public class ForgeBlockEntity extends AnimatedBlockEntity {
    public ForgeBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.FORGE_BE.get(), pos, state, "dd_forge_v2");
    }

    // Personne n'a l'écran ouvert au chargement : une forge enregistrée ouverte (partie arrêtée pendant qu'un joueur s'en
    // servait) redémarre fermée.
    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (!level.isClientSide()) {
            animations().fermerSansAnimation();
        }
    }
}
