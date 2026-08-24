package com.github.c0c0tier.dungeon_defenders.block.entity;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;

// Anime le passage d'un ratio de PV (0-1) à l'autre sur une durée fixe, en temps réel plutôt
// que sur partialTicks — même principe que LerpingBossEvent vanilla (barres de boss). Un
// BlockEntityRenderState est recréé à chaque frame (voir
// BlockEntityRenderDispatcher#tryExtractRenderState) : rien ne peut être retenu dessus d'une
// frame à l'autre, cette petite classe vit donc sur le renderer lui-même (voir
// EterniaCrystalBlockEntityRenderer/TowerHealthBarRenderer, indexée par BlockPos).
public final class HealthLerp {
    private final long durationMillis;
    private float from;
    private float to;
    private long startTimeMs = Util.getMillis();

    public HealthLerp(float initial, long durationMillis) {
        this.from = initial;
        this.to = initial;
        this.durationMillis = durationMillis;
    }

    /** Redirige l'animation vers {@code target}, en repartant de la valeur actuellement affichée. */
    public void setTarget(float target) {
        if (target == this.to) {
            return;
        }
        this.from = this.currentPercent();
        this.to = target;
        this.startTimeMs = Util.getMillis();
    }

    public float currentPercent() {
        long elapsed = Util.getMillis() - this.startTimeMs;
        float lerpAmount = Mth.clamp(elapsed / (float) this.durationMillis, 0.0F, 1.0F);
        return Mth.lerp(lerpAmount, this.from, this.to);
    }
}
