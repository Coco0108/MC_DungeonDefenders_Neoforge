package com.github.c0c0tier.dungeon_defenders.client.atelier;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.github.c0c0tier.dungeon_defenders.atelier.FicheBloc;
import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Le cristal d'Eternia quand l'Atelier a déployé le modèle animé de {@code dd_cristal_v2} : ce modèle (idle, puis death à
 * sa destruction), plus la même barre de vie que {@link EterniaCrystalBlockEntityRenderer}. Un bloc n'a qu'un renderer :
 * celui-ci fait les deux.
 */
public class EterniaCrystalGeoRenderer<R extends BlockEntityRenderState & GeoRenderState>
        extends BlocAnimeRenderer<EterniaCrystalBlockEntity, R> {
    private static final DataTicket<Float> HEALTH = DataTicket.create("dungeon_defenders_crystal_health", Float.class);

    public EterniaCrystalGeoRenderer(BlockEntityRendererProvider.Context context, FicheBloc.Anime anime) {
        super(context, anime);
    }

    @Override
    public void extractRenderState(EterniaCrystalBlockEntity crystal, R state, float partialTick, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(crystal, state, partialTick, cameraPosition, breakProgress);
        state.addGeckolibData(HEALTH, EterniaCrystalBlockEntityRenderer.healthPercent(crystal));
    }

    @Override
    public void submit(R state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        EterniaCrystalBlockEntityRenderer.submitBar(state.getOrDefaultGeckolibData(HEALTH, 1.0F), poseStack, collector, camera);
    }
}
