package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// "Spike Blockade" (nom repris du plan Excel du joueur, feuille Tours — Squire) : premier
// membre concret de la catégorie "Blockade" (voir AbstractBlockadeBlockEntity). Se contente de
// fixer ses propres stats : 30 PV, pas de coût en mana pour l'instant, et dégâts de contact
// activés (2 PV toutes les 20 ticks).
public class SpikeBlockadeBlockEntity extends AbstractBlockadeBlockEntity {

    public static final int MAX_HEALTH = 30;
    // Valeur de test demandée par le joueur pour vérifier que la dépense de mana à la pose
    // fonctionne (voir ModEvents.onBlockadePlace) — pas encore équilibrée pour de vrai.
    public static final int MANA_COST = 30;
    private static final float CONTACT_DAMAGE = 2.0F;
    private static final long CONTACT_DAMAGE_INTERVAL_TICKS = 20L;
    // Distance sur laquelle le blockade "gonfle" sa propre boîte pour détecter un contact —
    // approximation grossière de la portée de mêlée, pas une vraie détection de collision.
    private static final double CONTACT_RANGE = 1.0D;

    public SpikeBlockadeBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.SPIKE_BLOCKADE_BE.get(), pos, state,
                MAX_HEALTH, MANA_COST, true, CONTACT_DAMAGE, CONTACT_DAMAGE_INTERVAL_TICKS, CONTACT_RANGE);
    }
}
