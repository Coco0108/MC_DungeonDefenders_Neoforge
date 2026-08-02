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
                    // Fond gris sur toute la largeur, puis la jauge par-dessus.
                    addBar(pose, buffer, BAR_WIDTH, 0.3F, 0.3F, 0.3F);
                    addBar(pose, buffer, BAR_WIDTH * healthPercent,
                            red(healthPercent), green(healthPercent), 0.0F);
                });

        poseStack.popPose();
    }

    /** Ajoute un quad centré horizontalement, de largeur {@code width}. */
    private static void addBar(PoseStack.Pose pose, VertexConsumer buffer, float width, float r, float g, float b) {
        float left = -BAR_WIDTH / 2.0F;
        float right = left + width;

        buffer.addVertex(pose, left, 0.0F, 0.0F).setColor(r, g, b, 1.0F);
        buffer.addVertex(pose, right, 0.0F, 0.0F).setColor(r, g, b, 1.0F);
        buffer.addVertex(pose, right, BAR_HEIGHT, 0.0F).setColor(r, g, b, 1.0F);
        buffer.addVertex(pose, left, BAR_HEIGHT, 0.0F).setColor(r, g, b, 1.0F);
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
