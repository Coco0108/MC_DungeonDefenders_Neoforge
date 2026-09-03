package com.github.c0c0tier.dungeon_defenders.init;

// Les quatre emplacements de compétence, dans l'ordre du jeu de référence — voir
// AbilitySlotsOverlay pour l'ordre d'affichage, identique. HEAL et REPAIR sont génériques
// (mêmes ClientDisplayConfig.HealAbility/RepairAbility pour tout héros) ; SPELL_1 et SPELL_2
// sont propres à chaque HeroDefinition.
//
// L'ordinal sert d'index dans ModKeyMappings.ABILITY_KEYS et d'identifiant réseau (les
// paquets de compétence portent un ordinal, comme PlaceTowerPayload/SetDifficultyPayload) —
// jamais persisté, donc pas besoin d'une sérialisation par nom comme GamePhase/HeroDefinition.
public enum AbilitySlot {
    HEAL,
    SPELL_1,
    SPELL_2,
    REPAIR
}
