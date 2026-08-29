package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
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

    public static final int MAX_HEALTH = 25;
    // Valeur de test, pas encore équilibrée, comme le reste des coûts de pose du mod — moins
    // cher que Spike Blockade (30) puisque l'essentiel de son intérêt n'est pas les dégâts.
    public static final int MANA_COST = 25;
    private static final float CONTACT_DAMAGE = 1.0F;
    // La "vitesse d'attaque" du Bouncer : un monstre au contact ne peut être endommagé/repoussé
    // qu'une fois par intervalle, pas en continu — voir le commentaire de classe ci-dessus.
    private static final long CONTACT_DAMAGE_INTERVAL_TICKS = 10L;
    private static final double CONTACT_RANGE = 1.0D;
    // 0.8F testé en jeu (2026-08-29) : quasi imperceptible — mais le sens du vecteur était
    // inversé à ce moment-là (les monstres étaient attirés, pas repoussés, voir
    // AbstractBlockadeBlockEntity), donc cette impression n'était pas fiable. Remonté à 1.6F
    // dans la foulée, sens corrigé ensuite : une fois la repousse effectivement fonctionnelle
    // dans le bon sens, 1.6F s'est avéré trop fort — remis à 0.8F, sa valeur d'origine.
    private static final float KNOCKBACK_STRENGTH = 0.8F;

    public BouncerBlockadeBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.BOUNCER_BLOCKADE_BE.get(), pos, state,
                MAX_HEALTH, MANA_COST, true, CONTACT_DAMAGE, CONTACT_DAMAGE_INTERVAL_TICKS, CONTACT_RANGE,
                KNOCKBACK_STRENGTH);
    }
}
