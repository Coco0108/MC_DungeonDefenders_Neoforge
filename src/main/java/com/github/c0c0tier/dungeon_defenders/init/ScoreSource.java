package com.github.c0c0tier.dungeon_defenders.init;

import java.util.Locale;

// Source d'un gain de score (voir ModEvents.grantScore et network/ScoreGainPayload.java) : sert
// à afficher un popup détaillé ("+10 Ennemi tué") plutôt qu'un simple total qui monte
// (ScoreGainOverlay). Un seul membre pour l'instant, même principe extensible que
// ManaCrystalType/SpawnableEnemy : les futures sources de score (fin de vague, fin de map,
// multiplicateurs — voir doc/02-gameplay.md) ajouteront leur propre membre le moment venu,
// transmis par ordinal comme le reste des enums réseau du mod (GamePhase, GameDifficulty...).
public enum ScoreSource {
    MONSTER_KILLED;

    public String translationKey() {
        return "dungeon_defenders.score_source." + name().toLowerCase(Locale.ROOT);
    }
}
