package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
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

    public static final int MAX_HEALTH = 35;
    public static final int MANA_COST = 40;
    private static final float CONTACT_DAMAGE = 1.0F;
    private static final long CONTACT_DAMAGE_INTERVAL_TICKS = 5L;
    private static final double CONTACT_RANGE = 1.5D;

    public SliceNDiceBlockadeBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.SLICE_N_DICE_BLOCKADE_BE.get(), pos, state,
                MAX_HEALTH, MANA_COST, true, CONTACT_DAMAGE, CONTACT_DAMAGE_INTERVAL_TICKS, CONTACT_RANGE, 0.0F);
    }
}
