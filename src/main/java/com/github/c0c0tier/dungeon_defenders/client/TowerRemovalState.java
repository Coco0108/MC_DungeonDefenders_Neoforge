package com.github.c0c0tier.dungeon_defenders.client;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

// État client transitoire (pas persistant, pas synchronisé) du mode suppression de tour,
// déclenché par ModKeyMappings.REMOVE_TOWER_MODE (voir TowerRemovalClientEvents). Contrairement
// à TowerPlacementState (une seule pose puis retour à l'état inactif), reste actif après une
// suppression pour permettre d'en enchaîner plusieurs — comme dans le jeu de référence
// (touche dédiée, puis un clic par tour à retirer) — jusqu'à un nouvel appui sur la touche ou
// une sortie automatique (changement de phase, etc., voir TowerRemovalClientEvents).
public final class TowerRemovalState {

    private static boolean active;
    private static @Nullable BlockPos targetPos;
    private static boolean targetValid;

    private TowerRemovalState() {
    }

    public static void toggle() {
        active = !active;
        if (!active) {
            targetPos = null;
            targetValid = false;
        }
    }

    public static void cancel() {
        active = false;
        targetPos = null;
        targetValid = false;
    }

    public static boolean isActive() {
        return active;
    }

    /** Appelé chaque tick pendant que le mode est actif, avec le résultat du rayon de visée. */
    public static void updateTarget(@Nullable BlockPos pos, boolean valid) {
        targetPos = pos;
        targetValid = valid;
    }

    public static @Nullable BlockPos targetPos() {
        return targetPos;
    }

    public static boolean isTargetValid() {
        return targetValid;
    }
}
