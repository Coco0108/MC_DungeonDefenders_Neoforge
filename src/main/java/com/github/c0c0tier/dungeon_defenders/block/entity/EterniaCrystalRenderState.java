package com.github.c0c0tier.dungeon_defenders.block.entity;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Instantané des données nécessaires au rendu de la barre de vie.
 * <p>
 * Depuis 26.1, un {@code BlockEntityRenderer} ne voit plus le block entity au moment
 * du rendu : les données sont extraites en amont dans un état comme celui-ci.
 */
@OnlyIn(Dist.CLIENT)
public class EterniaCrystalRenderState extends BlockEntityRenderState {
    /** Ratio de PV restants, entre 0 et 1. */
    public float healthPercent = 1.0F;
}
