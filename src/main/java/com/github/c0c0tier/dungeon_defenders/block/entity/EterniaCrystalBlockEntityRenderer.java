package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class EterniaCrystalBlockEntityRenderer implements BlockEntityRenderer<EterniaCrystalBlockEntity, BlockEntityRenderState> {

    public EterniaCrystalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            EterniaCrystalBlockEntity blockEntity,
            BlockEntityRenderState renderState,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 3.2D, 0.5D);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-mc.player.getYRot()));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(mc.player.getXRot()));
        }

        int currentHealth = blockEntity.getCrystalHealth();
        float healthPercent = Math.max(0.0F, Math.min(1.0F, currentHealth / (float) EterniaCrystalBlockEntity.DEFAULT_HEALTH));

        VertexConsumer consumer = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.gui());

        renderBar(poseStack, consumer, -1.0F, 0.0F, 2.0F, 0.2F, 0.3F, 0.3F, 0.3F, 1.0F, packedLight);
        renderBar(poseStack, consumer, -1.0F, 0.0F, 2.0F * healthPercent, 0.2F, getRed(healthPercent), getGreen(healthPercent), getBlue(healthPercent), 1.0F, packedLight);

        poseStack.popPose();
    }

    private void renderBar(
            PoseStack poseStack,
            VertexConsumer consumer,
            float x,
            float y,
            float width,
            float height,
            float r,
            float g,
            float b,
            float a,
            int packedLight
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, 0.0F);

        consumer.vertex(poseStack.last().pose(), 0.0F, 0.0F, 0.0F)
                .color(r, g, b, a)
                .uv2(packedLight)
                .endVertex();

        consumer.vertex(poseStack.last().pose(), width, 0.0F, 0.0F)
                .color(r, g, b, a)
                .uv2(packedLight)
                .endVertex();

        consumer.vertex(poseStack.last().pose(), width, height, 0.0F)
                .color(r, g, b, a)
                .uv2(packedLight)
                .endVertex();

        consumer.vertex(poseStack.last().pose(), 0.0F, height, 0.0F)
                .color(r, g, b, a)
                .uv2(packedLight)
                .endVertex();

        poseStack.popPose();
    }

    private float getRed(float healthPercent) {
        if (healthPercent > 0.5F) {
            return (healthPercent - 0.5F) * 2.0F;
        }
        return 1.0F;
    }

    private float getGreen(float healthPercent) {
        if (healthPercent > 0.5F) {
            return 1.0F;
        }
        return healthPercent * 2.0F;
    }

    private float getBlue(float healthPercent) {
        return 0.0F;
    }
}