package com.github.c0c0tier.dungeon_defenders.init;

import net.minecraft.world.level.Level;

import java.util.Locale;

// Phase courante, portée par ModAttachments.GAME_PHASE (un attachment de Level, donc global au
// monde) et affichée par PhaseOverlay.
//
// Le fait qu'elle soit globale et non par joueur n'est pas une approximation : startGame et
// returnToTavern (MapInstance) téléportent TOUS les joueurs ensemble — le mod est bâti autour
// d'une seule session partagée (voir doc/05-etat-et-problemes-connus.md, "Système de
// maps/structures"). "Tout le monde est à la taverne" est donc un vrai état du monde.
public enum GamePhase {
    BUILD,
    COMBAT,
    // Ajoutée à la FIN volontairement (2026-09-01) : la phase est persistée par nom, mais la
    // valeur synchronisée vers le client est un ordinal — insérer TAVERN avant les deux autres
    // les aurait décalées.
    TAVERN;

    // Une clé par valeur : "dungeon_defenders.phase.build", "...combat", "...tavern".
    public String translationKey() {
        return "dungeon_defenders.phase." + name().toLowerCase(Locale.ROOT);
    }

    /** La phase courante de cette {@code Level}, lue depuis l'attachment. */
    public static GamePhase of(Level level) {
        return values()[level.getData(ModAttachments.GAME_PHASE)];
    }

    /**
     * Les tours se posent et se retirent en Construction (pendant une partie) <b>et</b> à la
     * Taverne (zone d'essai libre, voir doc/02-gameplay.md) — jamais en Combat.
     */
    public boolean allowsTowerBuilding() {
        return this == BUILD || this == TAVERN;
    }

    /**
     * Vrai pendant une partie sur une map (Construction ou Combat), faux à la Taverne. Sert aux
     * éléments de HUD qui n'ont aucun sens dans le hub (vague en cours, ennemis restants).
     */
    public boolean isInGame() {
        return this != TAVERN;
    }
}
