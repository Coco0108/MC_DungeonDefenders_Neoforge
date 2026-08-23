package com.github.c0c0tier.dungeon_defenders.client;

import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

// État client transitoire (pas persistant, pas synchronisé) du mode pose déclenché par
// TowerWheelScreen. Une seule étape : l'hologramme suit le rayon de la caméra ET peut être
// tourné (touche ROTATE_TOWER) en même temps — voir TowerPlacementClientEvents — jusqu'au clic
// droit qui pose la tour avec la position et la rotation courantes. Voir doc/02-gameplay.md
// pour le détail du flux.
public final class TowerPlacementState {

    private static @Nullable TowerDefinition selected;
    private static @Nullable BlockPos targetPos;
    private static boolean targetValid;
    private static Direction rotation = Direction.NORTH;

    private TowerPlacementState() {
    }

    /** Démarre le mode pose pour la tour choisie dans la roue. */
    public static void start(TowerDefinition tower) {
        selected = tower;
        targetPos = null;
        targetValid = false;
        rotation = Direction.NORTH;
    }

    /** Quitte le mode pose entièrement (annulation ou confirmation envoyée au serveur). */
    public static void cancel() {
        selected = null;
        targetPos = null;
        targetValid = false;
    }

    public static boolean isActive() {
        return selected != null;
    }

    public static @Nullable TowerDefinition selected() {
        return selected;
    }

    /** Appelé chaque tick avec le résultat du rayon lancé depuis la caméra. */
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

    /** Fait pivoter l'hologramme de 90°. */
    public static void rotate() {
        rotation = rotation.getClockWise();
    }

    public static Direction rotation() {
        return rotation;
    }
}
