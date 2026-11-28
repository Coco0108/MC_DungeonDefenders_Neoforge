package com.github.c0c0tier.dungeon_defenders.entity.ai;

import net.minecraft.world.entity.Mob;

// Un monstre dont le coup suit une animation (les monstres de l'Atelier, voir atelier/AtelierMonster) :
// le coup part au début de l'animation « attack », mais ses DÉGÂTS tombent à son marqueur d'impact.
// Les goals d'attaque de Dungeon Defenders (AttackPriorityTargetGoal, RangedAttackEterniaCrystalGoal)
// passent par strike(...) au lieu d'appliquer les dégâts directement : un zombie ou un squelette
// vanilla frappe tout de suite, comme avant ; un monstre animé attend son impact.
public interface AnimatedAttacker {

    /**
     * Commence un coup. {@code hit} est appelé à l'impact de l'animation (côté serveur), s'il est
     * encore vivant. Renvoie faux si le monstre est occupé (animation précédente pas finie, ou
     * recharge de sa fiche) : le coup n'est pas parti, le goal réessaiera.
     */
    boolean beginAttack(Runnable hit);

    /** Le coup d'un goal : animé pour un AnimatedAttacker, immédiat pour tout autre monstre. */
    static boolean strike(Mob mob, Runnable hit) {
        if (mob instanceof AnimatedAttacker animated) {
            return animated.beginAttack(hit);
        }
        hit.run();
        return true;
    }
}
