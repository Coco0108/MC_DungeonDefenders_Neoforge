package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// "Bouncer Blockade" (nom repris du plan Excel du joueur, feuille Tours — Squire) : deuxième
// membre concret de la catégorie "Blockade". Contrairement à Spike Blockade (dégâts seuls),
// repousse aussi les monstres au contact — décidé avec le joueur (2026-08-29) : dégâts ET
// repousse, pas l'un à la place de l'autre, uniquement pour les monstres déjà dans sa portée de
// contact (même AABB que les dégâts, voir AbstractBlockadeBlockEntity). Dégâts plus faibles et
// cadence plus rapide que Spike Blockade — l'intérêt est le contrôle de foule, pas les dégâts.
//
// "Vitesse d'attaque" (question du joueur, 2026-08-29) : oui, CONTACT_DAMAGE_INTERVAL_TICKS
// EST la cadence, pour les dégâts et la repousse à la fois — un seul cooldown par monstre
// (voir AbstractBlockadeBlockEntity#serverTick, lastContactDamageTick), pas de repousse à
// chaque tick tant qu'un monstre reste au contact. C'est ce qui empêche déjà la repousse
// d'être "trop forte" en la déclenchant en continu ; KNOCKBACK_STRENGTH ne règle que
// l'intensité d'une seule poussée, pas sa fréquence.
public class BouncerBlockadeBlockEntity extends AbstractBlockadeBlockEntity {

    // Chiffres (PV, coût, dégâts, cadence, rayon) : data-driven, voir
    // data/dungeon_defenders/dungeon_defenders/tower/bouncer_blockade.json et
    // TowerDefinition.BOUNCER_BLOCKADE pour les valeurs par défaut.
    //
    // contact_interval_ticks (10) est la "vitesse d'attaque" du Bouncer : un monstre au contact ne
    // peut être endommagé/repoussé qu'une fois par intervalle, pas en continu.
    //
    // knockback_strength (0.8) : testé en jeu le 2026-08-29. Jugé quasi imperceptible alors que
    // le sens du vecteur était inversé (les monstres étaient attirés, voir
    // AbstractBlockadeBlockEntity) ; remonté à 1.6 puis, le sens corrigé, 1.6 s'est avéré trop
    // fort — remis à 0.8, sa valeur d'origine.
    public BouncerBlockadeBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.BOUNCER_BLOCKADE_BE.get(), pos, state, TowerDefinition.BOUNCER_BLOCKADE, true);
    }
}
