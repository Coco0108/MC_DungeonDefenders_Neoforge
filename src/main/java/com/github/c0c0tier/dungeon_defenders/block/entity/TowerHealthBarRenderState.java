package com.github.c0c0tier.dungeon_defenders.block.entity;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/**
 * Instantané des données nécessaires à la barre de vie d'une tour (voir
 * {@link TowerHealthBarRenderer}).
 */
public class TowerHealthBarRenderState extends BlockEntityRenderState {
    /** false si la barre ne doit pas être dessinée ce frame (PV pleins, ou trop loin). */
    public boolean visible;
    public float healthPercent = 1.0F;
}
