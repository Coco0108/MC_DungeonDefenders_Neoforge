package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// "Spike Blockade" (nom repris du plan Excel du joueur, feuille Tours — Squire) : premier
// membre concret de la catégorie "Blockade" (voir AbstractBlockadeBlockEntity). Se contente de
// fixer ses propres stats : 30 PV, pas de coût en mana pour l'instant, et dégâts de contact
// activés (2 PV toutes les 20 ticks).
public class SpikeBlockadeBlockEntity extends AbstractBlockadeBlockEntity {

    // Chiffres (PV, coût, dégâts, cadence, rayon) : data-driven, voir
    // data/dungeon_defenders/dungeon_defenders/tower/spike_blockade.json et
    // TowerDefinition.SPIKE_BLOCKADE pour les valeurs par défaut.
    //
    // contact_range : distance sur laquelle le blockade "gonfle" sa propre boîte pour détecter un
    // contact — approximation grossière de la portée de mêlée, pas une vraie détection de
    // collision.
    public SpikeBlockadeBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.SPIKE_BLOCKADE_BE.get(), pos, state, TowerDefinition.SPIKE_BLOCKADE, true);
    }
}
