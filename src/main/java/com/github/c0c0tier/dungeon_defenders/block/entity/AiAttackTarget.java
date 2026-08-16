package com.github.c0c0tier.dungeon_defenders.block.entity;

// Contrat commun à tout ce qu'un monstre de mêlée peut attaquer via
// entity/ai/AttackPriorityTargetGoal.java — implémenté par AbstractTowerBlockEntity (donc
// Blockade et Turret) ET par EterniaCrystalBlockEntity, qui n'a et n'aura pas de lien de code
// avec les tours. Décidé avec le joueur (voir doc/05-etat-et-problemes-connus.md, section
// Système de priorité IA) :
//
// 1. Block (mur pur, pas de dégâts actifs) — priorité la plus haute.
// 2. Corps à corps (dégâts périodiques dans un petit rayon, comme Spike Blockade).
// 3. Cristal d'Eternia.
// 4. Tourelle — priorité la plus basse, un monstre ne s'en occupe qu'en dernier recours.
//
// Indices espacés (10/20/30/40, pas 1/2/3/4) pour laisser de la place à un futur mécanisme de
// provocation ou un nouveau type de tour, sans avoir à décaler les valeurs existantes.
public interface AiAttackTarget {

    int PRIORITY_BLOCK = 10;
    int PRIORITY_MELEE_TOWER = 20;
    int PRIORITY_CRYSTAL = 30;
    int PRIORITY_RANGED_TOWER = 40;

    int getAiPriority();

    void damage(int amount);
}
