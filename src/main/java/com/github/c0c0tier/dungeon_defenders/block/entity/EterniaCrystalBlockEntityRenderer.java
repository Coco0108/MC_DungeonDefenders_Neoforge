package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Dessine une barre de vie en billboard au-dessus du Cristal d'Eternia.
 */
public class EterniaCrystalBlockEntityRenderer
        implements BlockEntityRenderer<EterniaCrystalBlockEntity, EterniaCrystalRenderState> {

    /** Hauteur de la barre, juste au-dessus de la hitbox de 3 blocs. */
    private static final float BAR_Y = 3.2F;
    private static final float BAR_WIDTH = 2.0F;
    private static final float BAR_HEIGHT = 0.2F;

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
        state.healthPercent = Mth.clamp(
                blockEntity.getCrystalHealth() / (float) EterniaCrystalBlockEntity.DEFAULT_HEALTH, 0.0F, 1.0F);
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
