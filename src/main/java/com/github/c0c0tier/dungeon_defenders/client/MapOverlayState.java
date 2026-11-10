package com.github.c0c0tier.dungeon_defenders.client;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

// État client transitoire (ni synced, ni persisté) du plan plein écran de la zone actuelle —
// voir MapOverlayClientEvents (tick, remplit cet état) et client.gui.MapOverlay (rendu, le lit).
// Même famille que TowerPlacementState/TowerRemovalState : une petite classe d'état statique
// dédiée par fonctionnalité client plutôt qu'un état partagé fourre-tout.
public final class MapOverlayState {

    private static boolean visible;
    private static @Nullable BlockPos crystalPos;

    private MapOverlayState() {
    }

    public static boolean visible() {
        return visible;
    }

    public static void setVisible(boolean value) {
        visible = value;
    }

    /** Position du Cristal d'Eternia (en map) ou du cristal de la taverne — {@code null} si pas encore trouvé. */
    public static @Nullable BlockPos crystalPos() {
        return crystalPos;
    }

    public static void setCrystalPos(@Nullable BlockPos pos) {
        crystalPos = pos;
    }
}
