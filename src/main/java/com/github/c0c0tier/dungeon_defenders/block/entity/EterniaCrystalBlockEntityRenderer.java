package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.Config;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Dessine une barre de vie en billboard au-dessus du Cristal d'Eternia.
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
    private final Map<BlockPos, HealthLerp> lerpByPosition = new HashMap<>();

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

        float target = Mth.clamp(
                blockEntity.getCrystalHealth() / (float) Config.DEFAULT_HEALTH.get(), 0.0F, 1.0F);
        HealthLerp lerp = this.lerpByPosition.computeIfAbsent(blockEntity.getBlockPos(), pos -> new HealthLerp(target));

        if (target != lerp.to) {
            // Repart de la valeur actuellement affichée (pas de l'ancienne cible) : un coup
            // qui arrive pendant que la barre bouge encore ne doit pas la faire sauter en
            // arrière avant de repartir, juste rediriger l'animation en cours.
            lerp.from = lerp.currentPercent();
            lerp.to = target;
            lerp.startTimeMs = Util.getMillis();
        }

        state.healthPercent = lerp.currentPercent();
    }

    /** Anime le passage de {@link #from} à {@link #to} sur {@link #LERP_MILLISECONDS}, en temps réel. */
    private static final class HealthLerp {
        float from;
        float to;
        long startTimeMs = Util.getMillis();

        HealthLerp(float initial) {
            this.from = initial;
            this.to = initial;
        }

        float currentPercent() {
            long elapsed = Util.getMillis() - this.startTimeMs;
            float lerpAmount = Mth.clamp(elapsed / (float) LERP_MILLISECONDS, 0.0F, 1.0F);
            return Mth.lerp(lerpAmount, this.from, this.to);
        }
    }

    @Override
    public void submit(
            EterniaCrystalRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        float healthPercent = state.healthPercent;

        poseStack.pushPose();
        poseStack.translate(0.5F, BAR_Y, 0.5F);
        // L'orientation de la caméra suffit à obtenir un billboard ; après cette
        // rotation, +X va vers la droite et +Y vers le BAS (comme pour les name tags),
        // d'où l'inversion de Y pour raisonner en coordonnées naturelles.
        poseStack.mulPose(camera.orientation);
        poseStack.scale(1.0F, -1.0F, 1.0F);

        // debugQuads : quads non texturés, translucides et non cullés — exactement
        // ce qu'il faut pour une barre de vie, et visible sous tous les angles.
        collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(),
                (pose, buffer) -> {
                    float left = -BAR_WIDTH / 2.0F;
                    float right = left + BAR_WIDTH;
                    float split = left + BAR_WIDTH * healthPercent;

                    // Les deux segments sont juxtaposés, jamais superposés : ce render
                    // type trie les quads par distance à la caméra (sortOnUpload), et
                    // deux quads coplanaires donneraient un ordre instable — la barre
                    // apparaissait alors entièrement grise par intermittence.
                    if (healthPercent > 0.0F) {
                        addSegment(pose, buffer, left, split, red(healthPercent), green(healthPercent), 0.0F);
                    }
                    if (healthPercent < 1.0F) {
                        addSegment(pose, buffer, split, right, 0.3F, 0.3F, 0.3F);
                    }
                });

        poseStack.popPose();
    }

    /** Ajoute un quad allant de {@code x0} à {@code x1}. */
    private static void addSegment(
            PoseStack.Pose pose, VertexConsumer buffer, float x0, float x1, float r, float g, float b) {
        buffer.addVertex(pose, x0, 0.0F, 0.0F).setColor(r, g, b, 1.0F);
        buffer.addVertex(pose, x1, 0.0F, 0.0F).setColor(r, g, b, 1.0F);
        buffer.addVertex(pose, x1, BAR_HEIGHT, 0.0F).setColor(r, g, b, 1.0F);
        buffer.addVertex(pose, x0, BAR_HEIGHT, 0.0F).setColor(r, g, b, 1.0F);
    }

    // Dégradé vert -> jaune -> rouge : au-dessus de 50 % le rouge monte,
    // en dessous le vert descend.
    private static float red(float healthPercent) {
        return healthPercent > 0.5F ? (1.0F - healthPercent) * 2.0F : 1.0F;
    }

    private static float green(float healthPercent) {
        return healthPercent > 0.5F ? 1.0F : healthPercent * 2.0F;
    }
}
