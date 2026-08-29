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
public class BouncerBlockadeBlockEntity extends AbstractBlockadeBlockEntity {

    public static final int MAX_HEALTH = 25;
    // Valeur de test, pas encore équilibrée, comme le reste des coûts de pose du mod — moins
    // cher que Spike Blockade (30) puisque l'essentiel de son intérêt n'est pas les dégâts.
    public static final int MANA_COST = 25;
    private static final float CONTACT_DAMAGE = 1.0F;
    private static final long CONTACT_DAMAGE_INTERVAL_TICKS = 10L;
    private static final double CONTACT_RANGE = 1.0D;
    // Valeur de test : à ajuster en jeu pour un "coup de pouce" satisfaisant, pas un chiffre
    // qu'on peut deviner sans essayer (voir LivingEntity#knockback pour l'échelle de la force).
    private static final float KNOCKBACK_STRENGTH = 0.8F;

    public BouncerBlockadeBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.BOUNCER_BLOCKADE_BE.get(), pos, state,
                MAX_HEALTH, MANA_COST, true, CONTACT_DAMAGE, CONTACT_DAMAGE_INTERVAL_TICKS, CONTACT_RANGE,
                KNOCKBACK_STRENGTH);
    }
}
