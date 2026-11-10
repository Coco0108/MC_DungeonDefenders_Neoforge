package com.github.c0c0tier.dungeon_defenders.client;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

// La tour que le joueur regarde en ce moment (voir TowerInspectClientEvents), ou null. État de
// rendu pur, même famille que TowerPlacementState/TowerRemovalState : ni synchronisé, ni
// persisté. Lu par TowerHealthBarRenderer (barre de vie affichée même à PV pleins) et par
// TowerInspectOverlay (nom + PV sous le viseur).
public final class TowerInspectState {

    private static @Nullable BlockPos target;

    private TowerInspectState() {
    }

    public static @Nullable BlockPos target() {
        return target;
    }

    public static void setTarget(@Nullable BlockPos pos) {
        target = pos;
    }

    public static boolean isTarget(BlockPos pos) {
        return pos.equals(target);
    }
}
