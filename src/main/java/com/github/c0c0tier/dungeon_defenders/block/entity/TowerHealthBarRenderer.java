package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

// Barre de vie en billboard au-dessus de n'importe quelle tour (Blockade, Turret, et les
// suivantes — un seul renderer générique sur AbstractTowerBlockEntity, enregistré une fois par
// BlockEntityType concret dans DungeonDefendersModClient). Même mécanisme que
// EterniaCrystalBlockEntityRenderer (HealthLerp, HealthBarRendering), mais PAS affichée en
// permanence : décidé avec le joueur (2026-08-24) qu'avec potentiellement des dizaines de
// tours posées, une barre visible sur toutes en permanence deviendrait illisible. Cachée à PV
// pleins, et au-delà d'une certaine portée de la caméra — même principe que l'aperçu du
// spawner (SpawnerBlockEntityRenderer.MAX_DISTANCE_SQ).
public class TowerHealthBarRenderer<T extends AbstractTowerBlockEntity>
        implements BlockEntityRenderer<T, TowerHealthBarRenderState> {

    // Hauteur de la barre : juste au-dessus de la hitbox de 1,5 bloc des tours (voir
    // SpikeBlockadeBlock/HarpoonTurretBlock), plus petite que celle du cristal (3 blocs de
    // haut, groupe géré à part par EterniaCrystalBlockEntityRenderer).
    private static final float BAR_Y = 1.8F;
    private static final float BAR_WIDTH = 1.0F;
    private static final float BAR_HEIGHT = 0.12F;

    private static final long LERP_MILLISECONDS = 300L;
    private static final double MAX_DISTANCE_SQ = 16.0 * 16.0;

    // Voir EterniaCrystalBlockEntityRenderer : extractRenderState reçoit un état neuf à
    // chaque frame, l'animation vit donc sur le renderer, indexée par position. Ici, avec
    // potentiellement plusieurs dizaines de tours, une entrée par position de tour posée dans
    // la session — reste borné, jamais nettoyé mais négligeable.
    private final Map<BlockPos, HealthLerp> lerpByPosition = new HashMap<>();

    public TowerHealthBarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public TowerHealthBarRenderState createRenderState() {
        return new TowerHealthBarRenderState();
    }

    @Override
    public void extractRenderState(
            T blockEntity,
            TowerHealthBarRenderState state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        state.visible = false;

        // À PV pleins : rien à signaler, la barre reste cachée (voir le commentaire de
        // classe) — pas la peine de calculer/mettre à jour l'animation pour rien.
        if (blockEntity.getHealth() >= blockEntity.getMaxHealth()) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        double distanceSq = cameraPosition.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (distanceSq > MAX_DISTANCE_SQ) {
            return;
        }

        float target = (float) blockEntity.getHealth() / blockEntity.getMaxHealth();
        HealthLerp lerp = this.lerpByPosition.computeIfAbsent(pos, p -> new HealthLerp(target, LERP_MILLISECONDS));
        lerp.setTarget(target);

        state.visible = true;
        state.healthPercent = lerp.currentPercent();
    }

    @Override
    public void submit(
            TowerHealthBarRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        if (!state.visible) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, BAR_Y, 0.5F);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(1.0F, -1.0F, 1.0F);

        HealthBarRendering.render(poseStack, collector, BAR_WIDTH, BAR_HEIGHT, state.healthPercent);

        poseStack.popPose();
    }
}
