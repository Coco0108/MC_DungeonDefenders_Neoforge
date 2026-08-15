package com.github.c0c0tier.dungeon_defenders.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Instantané des données nécessaires au rendu de l'hologramme de pose (voir
 * TowerPlacementClientEvents). Rempli dans {@code ExtractLevelRenderStateEvent}, lu dans
 * {@code SubmitCustomGeometryEvent} — même principe que les autres render states du mod
 * (EterniaCrystalRenderState, SpawnerRenderState), mais posé sur le {@code LevelRenderState}
 * global via un {@code ContextKey} plutôt que par block entity, puisque l'hologramme n'est
 * attaché à aucun bloc existant.
 * <p>
 * Classe purement client, jamais chargée sur un serveur dédié.
 */
public class TowerPlacementRenderState {
    public BlockPos pos = BlockPos.ZERO;
    public boolean valid;
    public TowerPlacementState.Step step = TowerPlacementState.Step.AIMING;
    public Direction rotation = Direction.NORTH;
    public double range;
}
