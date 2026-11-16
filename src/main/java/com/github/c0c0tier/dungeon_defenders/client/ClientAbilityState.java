package com.github.c0c0tier.dungeon_defenders.client;

import com.github.c0c0tier.dungeon_defenders.init.AbilitySlot;

// Ce que le HUD des compétences (AbilitySlotsOverlay) a besoin de savoir en plus du héros et du
// mana : la recharge en cours de chaque emplacement (annoncée par le serveur, voir
// AbilityCooldownPayload) et l'emplacement en cours de canalisation (connu du client lui-même,
// voir AbilityClientEvents). Purement de l'affichage : le serveur ne lit jamais rien d'ici.
//
// Les recharges sont comptées en ticks CLIENT (décrémentées par AbilityClientEvents à chaque
// tick), pas en heure du monde : l'horloge du monde peut être figée ou resynchronisée par le
// serveur, un simple compte à rebours local n'a pas ce problème.
public final class ClientAbilityState {

    private static final int SLOT_COUNT = AbilitySlot.values().length;
    private static final int[] REMAINING_TICKS = new int[SLOT_COUNT];
    private static final int[] TOTAL_TICKS = new int[SLOT_COUNT];
    private static int channelingSlot = -1;

    private ClientAbilityState() {
    }

    public static void startCooldown(int slotOrdinal, int cooldownTicks) {
        if (slotOrdinal < 0 || slotOrdinal >= SLOT_COUNT || cooldownTicks <= 0) {
            return;
        }
        REMAINING_TICKS[slotOrdinal] = cooldownTicks;
        TOTAL_TICKS[slotOrdinal] = cooldownTicks;
    }

    static void tick() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (REMAINING_TICKS[i] > 0) {
                REMAINING_TICKS[i]--;
            }
        }
    }

    /** Déconnexion ou changement de monde : rien ne doit survivre d'une partie à l'autre. */
    static void reset() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            REMAINING_TICKS[i] = 0;
            TOTAL_TICKS[i] = 0;
        }
        channelingSlot = -1;
    }

    static void setChanneling(AbilitySlot slot, boolean active) {
        if (active) {
            channelingSlot = slot.ordinal();
        } else if (channelingSlot == slot.ordinal()) {
            channelingSlot = -1;
        }
    }

    static void clearChanneling() {
        channelingSlot = -1;
    }

    public static int remainingTicks(AbilitySlot slot) {
        return REMAINING_TICKS[slot.ordinal()];
    }

    /** Part de la recharge encore à attendre, entre 0 (prête) et 1 (vient de partir). */
    public static float cooldownFraction(AbilitySlot slot) {
        int total = TOTAL_TICKS[slot.ordinal()];
        return total <= 0 ? 0.0F : (float) REMAINING_TICKS[slot.ordinal()] / total;
    }

    public static boolean isChanneling(AbilitySlot slot) {
        return channelingSlot == slot.ordinal();
    }
}
