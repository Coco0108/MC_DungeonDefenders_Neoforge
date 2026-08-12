package com.github.c0c0tier.dungeon_defenders.init;

import java.util.Locale;

// Difficulté de la partie, censée être choisie au lancement de la map (aucun écran de
// lancement n'existe encore, voir 05-etat-et-problemes-connus.md). Consultée par
// DifficultyScaling pour calculer le multiplicateur appliqué aux compositions de spawner.
public enum GameDifficulty {
    EASY(0.75D),
    NORMAL(1.0D),
    HARD(1.5D);

    private final double multiplier;

    GameDifficulty(double multiplier) {
        this.multiplier = multiplier;
    }

    public double multiplier() {
        return this.multiplier;
    }

    public String translationKey() {
        return "dungeon_defenders.difficulty." + name().toLowerCase(Locale.ROOT);
    }
}
