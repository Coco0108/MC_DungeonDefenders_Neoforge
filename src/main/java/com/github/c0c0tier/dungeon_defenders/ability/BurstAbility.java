package com.github.c0c0tier.dungeon_defenders.ability;

import net.minecraft.server.level.ServerPlayer;

// Compétence à déclenchement instantané, payée d'un coup, puis en recharge — comme Circular
// Slice dans le jeu de référence (60 mana, 3 secondes de recharge). Toute la validation
// (mana disponible, recharge écoulée) est faite par l'appelant (ModNetworking) avant
// d'appeler activate() : cette méthode ne fait que produire l'effet, elle ne débite rien et
// ne vérifie rien elle-même — même séparation que TowerDefinition (les stats) et le block
// entity (le comportement).
public interface BurstAbility extends HeroAbility {

    int manaCost();

    int cooldownTicks();

    /** Produit l'effet. Appelé une fois la validation faite ; ne débite pas le mana lui-même. */
    void activate(ServerPlayer player);
}
