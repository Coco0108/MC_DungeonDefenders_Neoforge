package com.github.c0c0tier.dungeon_defenders.client;

import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

// État client transitoire (pas persistant, pas synchronisé) du mode pose déclenché par
// TowerWheelScreen. Deux étapes : AIMING (l'hologramme suit le rayon de la caméra, voir
// TowerPlacementClientEvents) puis ORIENTING (position verrouillée, seule la rotation change) —
// voir doc/02-gameplay.md pour le détail du flux.
public final class TowerPlacementState {

    public enum Step { AIMING, ORIENTING }

    private static @Nullable TowerDefinition selected;
    private static Step step = Step.AIMING;
    private static @Nullable BlockPos targetPos;
    private static boolean targetValid;
    private static Direction rotation = Direction.NORTH;

    private TowerPlacementState() {
    }

    /** Démarre le mode pose pour la tour choisie dans la roue — repart toujours en AIMING. */
    public static void start(TowerDefinition tower) {
        selected = tower;
        step = Step.AIMING;
        targetPos = null;
        targetValid = false;
        rotation = Direction.NORTH;
    }

    /** Quitte le mode pose entièrement (annulation ou confirmation envoyée au serveur). */
    public static void cancel() {
        selected = null;
        step = Step.AIMING;
        targetPos = null;
        targetValid = false;
    }

    public static boolean isActive() {
        return selected != null;
    }

    public static @Nullable TowerDefinition selected() {
        return selected;
    }

    public static Step step() {
        return step;
    }

    /** Appelé chaque tick en AIMING avec le résultat du rayon lancé depuis la caméra. */
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

    /** Fige la position courante et passe à l'étape orientation. */
    public static void lockPosition() {
        step = Step.ORIENTING;
    }

    /** Fait pivoter l'hologramme de 90° (appelé en ORIENTING uniquement). */
    public static void rotate() {
        rotation = rotation.getClockWise();
    }

    public static Direction rotation() {
        return rotation;
    }
}
