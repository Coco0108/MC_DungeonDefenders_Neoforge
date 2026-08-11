package com.github.c0c0tier.dungeon_defenders.init;

import java.util.Locale;

// Phase de la partie en cours. Aucune mécanique n'y est encore attachée (voir
// 05-etat-et-problemes-connus.md) : c'est pour l'instant un simple libellé affiché en HUD
// via ModAttachments.GAME_PHASE + PhaseOverlay.
public enum GamePhase {
    BUILD,
    COMBAT;

    // Une clé par valeur : "dungeon_defenders.phase.build", "dungeon_defenders.phase.combat".
    public String translationKey() {
        return "dungeon_defenders.phase." + name().toLowerCase(Locale.ROOT);
    }
}
