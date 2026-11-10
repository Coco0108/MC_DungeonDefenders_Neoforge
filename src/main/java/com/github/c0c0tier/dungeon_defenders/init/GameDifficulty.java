package com.github.c0c0tier.dungeon_defenders.init;

import java.util.Locale;

// Difficulté de la partie, censée être choisie au lancement de la map (aucun écran de
// lancement n'existe encore, voir 05-etat-et-problemes-connus.md). Consultée par
// DifficultyScaling pour calculer le multiplicateur appliqué aux compositions de spawner.
public enum GameDifficulty {
    EASY(BalanceValue.DIFFICULTY_EASY_MULTIPLIER),
    NORMAL(BalanceValue.DIFFICULTY_NORMAL_MULTIPLIER),
    HARD(BalanceValue.DIFFICULTY_HARD_MULTIPLIER);

    // Le multiplicateur lui-même est data-driven depuis le 2026-11-10 (balance/difficulty.json,
    // voir GameBalance) : ×0.75 / ×1.0 / ×1.5 par défaut, comme avant.
    private final BalanceValue multiplier;

    GameDifficulty(BalanceValue multiplier) {
        this.multiplier = multiplier;
    }

    /** @param clientSide quelle copie des réglages lire (voir GameBalance). */
    public double multiplier(boolean clientSide) {
        return GameBalance.get(this.multiplier, clientSide);
    }

    public String translationKey() {
        return "dungeon_defenders.difficulty." + name().toLowerCase(Locale.ROOT);
    }
}
