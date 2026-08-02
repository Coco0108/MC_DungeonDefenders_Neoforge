package com.github.c0c0tier.dungeon_defenders.block.entity;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/**
 * Instantané des données nécessaires au rendu de la barre de vie.
 * <p>
 * Depuis 26.1, un {@code BlockEntityRenderer} ne voit plus le block entity au moment
 * du rendu : les données sont extraites en amont dans un état comme celui-ci.
 * <p>
 * Classe purement client : elle n'est référencée que depuis le renderer et
 * {@code DungeonDefendersModClient}, qui ne se chargent pas sur un serveur dédié.
 * Pas d'annotation {@code @OnlyIn} ici — elle est réservée à Minecraft et NeoForge,
 * son effet n'existe plus au runtime et NeoForge avertit les mods qui l'utilisent.
 */
public class EterniaCrystalRenderState extends BlockEntityRenderState {
    /** Ratio de PV restants, entre 0 et 1. */
    public float healthPercent = 1.0F;
}
