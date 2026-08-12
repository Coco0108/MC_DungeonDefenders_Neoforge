package com.github.c0c0tier.dungeon_defenders.block.entity;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Instantané des données nécessaires à l'aperçu de composition du spawner, affiché en phase
 * Construction (voir {@link SpawnerBlockEntityRenderer}).
 */
public class SpawnerRenderState extends BlockEntityRenderState {
    /** false si rien ne doit être dessiné ce frame (hors phase Construction, ou trop loin). */
    public boolean visible;
    /** Le total en premier, puis une ligne par ennemi de la composition. */
    public final List<Component> lines = new ArrayList<>();
}
