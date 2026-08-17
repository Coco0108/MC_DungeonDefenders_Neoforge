package com.github.c0c0tier.dungeon_defenders.init;

// Paliers de cristaux de mana (drop des monstres, voir entity/ManaCrystalEntity.java). Un seul
// membre pour l'instant (valeur de test, comme les coûts de pose des tours) — le joueur en
// prévoit au moins 6 à terme (couleurs/valeurs différentes, comme le vrai Dungeon Defenders),
// pas de logique de sélection pondérée tant qu'il n'y a qu'un seul palier à choisir.
public enum ManaCrystalType {

    SMALL(5);

    private final int value;

    ManaCrystalType(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }
}
