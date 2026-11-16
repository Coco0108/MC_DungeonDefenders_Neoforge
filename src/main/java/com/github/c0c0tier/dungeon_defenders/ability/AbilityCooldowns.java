package com.github.c0c0tier.dungeon_defenders.ability;

import com.github.c0c0tier.dungeon_defenders.init.AbilitySlot;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Dernier instant d'utilisation d'une compétence en salve (BurstAbility), par joueur et par
// emplacement — pour Circular Slice aujourd'hui, pour toute future compétence en salve d'un
// autre héros demain, sans redesign. Même limite acceptée que PlayerAbilityChannels : pas de
// nettoyage à la déconnexion, négligeable.
public final class AbilityCooldowns {

    private static final Map<UUID, Map<AbilitySlot, Long>> LAST_USE = new HashMap<>();

    private AbilityCooldowns() {
    }

    /** @return vrai si {@code cooldownTicks} se sont écoulés depuis le dernier usage (ou s'il n'y en a jamais eu). */
    public static boolean isReady(UUID playerId, AbilitySlot slot, long currentGameTime, int cooldownTicks) {
        Long lastUse = LAST_USE.getOrDefault(playerId, Map.of()).get(slot);
        return lastUse == null || currentGameTime - lastUse >= cooldownTicks;
    }

    public static void markUsed(UUID playerId, AbilitySlot slot, long currentGameTime) {
        LAST_USE.computeIfAbsent(playerId, id -> new EnumMap<>(AbilitySlot.class)).put(slot, currentGameTime);
    }
}
