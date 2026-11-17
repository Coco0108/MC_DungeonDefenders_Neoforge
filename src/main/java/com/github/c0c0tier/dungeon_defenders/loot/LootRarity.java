package com.github.c0c0tier.dungeon_defenders.loot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

// Rareté d'un objet de butin. Elle fixe trois choses : sa chance de sortir (poids), la force de
// ses bonus (multiplicateur) et le nombre de bonus secondaires en plus des dégâts.
//
// Persistée par NOM dans les objets (LootWeapon) : l'ordre peut changer sans casser une
// sauvegarde. Les poids sont des constantes : cinq nombres qui n'ont de sens qu'ensemble, les
// exposer un par un dans un JSON n'aiderait personne ; la FRÉQUENCE du butin, elle, est
// réglable (balance/loot.json).
public enum LootRarity {
    COMMON("common", 60, 1.0D, 0, ChatFormatting.WHITE, 0xFFFFFFFF),
    UNCOMMON("uncommon", 25, 1.5D, 1, ChatFormatting.GREEN, 0xFF55FF55),
    RARE("rare", 10, 2.2D, 1, ChatFormatting.AQUA, 0xFF55FFFF),
    EPIC("epic", 4, 3.0D, 2, ChatFormatting.LIGHT_PURPLE, 0xFFFF55FF),
    LEGENDARY("legendary", 1, 4.0D, 3, ChatFormatting.GOLD, 0xFFFFAA00);

    private final String id;
    private final int weight;
    private final double multiplier;
    private final int secondaryBonuses;
    private final ChatFormatting textColor;
    private final int argb;

    LootRarity(String id, int weight, double multiplier, int secondaryBonuses, ChatFormatting textColor, int argb) {
        this.id = id;
        this.weight = weight;
        this.multiplier = multiplier;
        this.secondaryBonuses = secondaryBonuses;
        this.textColor = textColor;
        this.argb = argb;
    }

    public int weight() {
        return this.weight;
    }

    public double multiplier() {
        return this.multiplier;
    }

    public int secondaryBonuses() {
        return this.secondaryBonuses;
    }

    public ChatFormatting textColor() {
        return this.textColor;
    }

    /** Couleur ARGB pour les cadres et textes dessinés à la main (écran de la Forge, HUD). */
    public int argb() {
        return this.argb;
    }

    public Component displayName() {
        return Component.translatable("dungeon_defenders.rarity." + this.id);
    }

    /** Lecture tolérante : un nom inconnu (sauvegarde d'une autre version) redevient Commun. */
    public static LootRarity byName(String name) {
        for (LootRarity rarity : values()) {
            if (rarity.name().equals(name)) {
                return rarity;
            }
        }
        return COMMON;
    }
}
