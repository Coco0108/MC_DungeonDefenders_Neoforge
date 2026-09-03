package com.github.c0c0tier.dungeon_defenders.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

// Contrat minimal partagé par toute compétence — juste ce qu'il faut pour l'afficher (nom,
// icône du HUD). Deux familles concrètes : BurstAbility (déclenchement instantané + recharge)
// et ChannelAbility (maintenue, mana consommé en continu) — voir chacune pour le détail, et
// doc/02-gameplay.md pour pourquoi le jeu de référence mélange les deux plutôt que d'avoir un
// seul mécanisme pour les quatre emplacements.
public interface HeroAbility {

    Component displayName();

    /**
     * Icône affichée dans AbilitySlotsOverlay. Réutilise un item vanilla thématique — même
     * convention que les textures placeholder ailleurs dans le mod (lodestone pour le spawn
     * joueur, hay_block pour le mannequin...) — en attendant une vraie icône dédiée.
     */
    Item icon();
}
