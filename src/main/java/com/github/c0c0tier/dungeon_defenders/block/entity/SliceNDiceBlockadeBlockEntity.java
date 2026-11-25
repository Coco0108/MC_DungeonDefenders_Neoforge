package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// "Slice N Dice Blockade" (nom repris du plan Excel du joueur, feuille Tours — Squire) :
// troisième membre concret de la catégorie "Blockade". Confirmé avec le joueur (2026-08-29) :
// aucun nouveau comportement nécessaire — AbstractBlockadeBlockEntity#serverTick inflige déjà
// des dégâts à TOUS les monstres présents dans contactRange (pas seulement le premier), ce que
// Spike Blockade n'exploite pas vraiment avec son rayon d'1 bloc. Se différencie de Spike
// Blockade par des dégâts plus faibles à cadence bien plus rapide (lames tournantes, DPS
// continu plutôt que coups espacés) et un rayon légèrement plus large.
public class SliceNDiceBlockadeBlockEntity extends AbstractBlockadeBlockEntity {

    // Chiffres (PV, coût, dégâts, cadence, rayon) : data-driven, voir
    // data/dungeon_defenders/dungeon_defenders/tower/slice_n_dice_blockade.json et
    // TowerDefinition.SLICE_N_DICE_BLOCKADE pour les valeurs par défaut.
    public SliceNDiceBlockadeBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.SLICE_N_DICE_BLOCKADE_BE.get(), pos, state, TowerDefinition.SLICE_N_DICE_BLOCKADE, true);
    }
}
