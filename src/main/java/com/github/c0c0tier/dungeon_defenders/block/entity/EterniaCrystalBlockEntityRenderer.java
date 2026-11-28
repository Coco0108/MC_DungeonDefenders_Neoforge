package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.Config;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Dessine une barre de vie en billboard au-dessus du Cristal d'Eternia — toujours affichée
 * (contrairement à {@link TowerHealthBarRenderer}), il n'y en a jamais qu'un à l'écran.
 */
public class EterniaCrystalBlockEntityRenderer
        implements BlockEntityRenderer<EterniaCrystalBlockEntity, EterniaCrystalRenderState> {

    /** Hauteur de la barre, juste au-dessus de la hitbox de 3 blocs. */
    private static final float BAR_Y = 3.2F;
    private static final float BAR_WIDTH = 2.0F;
    private static final float BAR_HEIGHT = 0.2F;

    // Durée de l'animation d'un palier de PV à l'autre, même principe que le lerp du temps
    // réel des barres de boss vanilla (LerpingBossEvent, 100 ms) — un peu plus lent ici, cette
    // barre n'a pas besoin d'être lue aussi vite qu'un boss en plein combat.
    private static final long LERP_MILLISECONDS = 300L;

    // extractRenderState reçoit un EterniaCrystalRenderState flambant neuf à CHAQUE frame
    // (voir BlockEntityRenderDispatcher#tryExtractRenderState) : impossible d'y stocker quoi
    // que ce soit d'une frame à l'autre. L'animation vit donc ici, sur le renderer lui-même
    // (une seule instance, réutilisée pour tous les cristaux), indexée par position — en
    // pratique une seule entrée à la fois (une seule map active), jamais nettoyée mais bornée
    // par le nombre de positions de cristal distinctes vues dans la session, négligeable.
    // Statique depuis le 2026-11-28 : partagée avec le renderer GeckoLib du cristal animé (EterniaCrystalGeoRenderer),
    // qui dessine la même barre.
    private static final Map<BlockPos, HealthLerp> lerpByPosition = new HashMap<>();

    public EterniaCrystalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public EterniaCrystalRenderState createRenderState() {
        return new EterniaCrystalRenderState();
    }

    @Override
    public void extractRenderState(
            EterniaCrystalBlockEntity blockEntity,
            EterniaCrystalRenderState state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        state.healthPercent = healthPercent(blockEntity);
    }

    /** La part de PV à afficher pour ce cristal, animée d'un palier à l'autre. */
    public static float healthPercent(EterniaCrystalBlockEntity blockEntity) {
        float target = Mth.clamp(
                blockEntity.getCrystalHealth() / (float) Config.DEFAULT_HEALTH.get(), 0.0F, 1.0F);
        HealthLerp lerp = lerpByPosition.computeIfAbsent(
                blockEntity.getBlockPos(), pos -> new HealthLerp(target, LERP_MILLISECONDS));
        lerp.setTarget(target);
        return lerp.currentPercent();
    }

    @Override
    public void submit(
            EterniaCrystalRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        submitBar(state.healthPercent, poseStack, collector, camera);
    }

    /** La barre de vie en billboard au-dessus du cristal, la pose étant à l'origine du bloc. */
    public static void submitBar(float healthPercent, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, BAR_Y, 0.5F);
        // L'orientation de la caméra suffit à obtenir un billboard ; après cette
        // rotation, +X va vers la droite et +Y vers le BAS (comme pour les name tags),
        // d'où l'inversion de Y pour raisonner en coordonnées naturelles.
        poseStack.mulPose(camera.orientation);
        poseStack.scale(1.0F, -1.0F, 1.0F);

        HealthBarRendering.render(poseStack, collector, BAR_WIDTH, BAR_HEIGHT, healthPercent);

        poseStack.popPose();
    }
}
