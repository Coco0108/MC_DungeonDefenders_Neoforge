package com.github.c0c0tier.dungeon_defenders.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.entity.HealthBarRendering;
import com.github.c0c0tier.dungeon_defenders.block.entity.HealthLerp;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

import java.util.HashMap;
import java.util.Map;

// Barre de vie en billboard au-dessus des monstres (zombie/squelette pour l'instant — tout
// LivingEntity dont le renderer reçoit cette couche fonctionnerait, voir
// DungeonDefendersModClient#onAddLayers). Mêmes conditions d'affichage et même mécanisme que
// TowerHealthBarRenderer (endommagé + à portée de 16 blocs, animation temps réel 300 ms via
// HealthLerp, dessin via HealthBarRendering) — voir doc/05-etat-et-problemes-connus.md.
//
// La vie n'existe pas nativement sur un EntityRenderState vanilla (vérifié dans le code
// source : ni LivingEntityRenderState ni EntityRenderState ne portent de champ santé) : un
// RenderLayer ne voit que le render state, jamais l'entité vivante elle-même, exactement comme
// un BlockEntityRenderer ne voit plus le block entity au moment du rendu. HEALTH/MAX_HEALTH/
// ENTITY_ID sont donc ajoutés au render state via RegisterRenderStateModifiersEvent (NeoForge,
// voir DungeonDefendersModClient#onRegisterRenderStateModifiers) et lus ici via ContextKey.
public class MobHealthBarLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends RenderLayer<S, M> {

    public static final ContextKey<Float> HEALTH =
            new ContextKey<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "mob_health"));
    public static final ContextKey<Float> MAX_HEALTH =
            new ContextKey<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "mob_max_health"));
    public static final ContextKey<Integer> ENTITY_ID =
            new ContextKey<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "mob_entity_id"));

    private static final float BAR_WIDTH = 0.7F;
    private static final float BAR_HEIGHT = 0.08F;
    // Au-dessus de la tête, pas seulement du haut de la hitbox — même marge d'esprit que le
    // nametag vanilla.
    private static final float ABOVE_HEAD_MARGIN = 0.5F;
    private static final long LERP_MILLISECONDS = 300L;
    private static final double MAX_DISTANCE_SQ = 16.0 * 16.0;

    // Même raisonnement que TowerHealthBarRenderer : l'animation ne peut pas vivre sur le
    // render state (recréé à chaque frame), donc sur la couche elle-même. Pas de BlockPos ici
    // (un monstre bouge) : indexée par l'ID d'entité, ajouté au render state pour la même
    // raison que la vie (voir ENTITY_ID ci-dessus). Jamais nettoyée mais bornée par le nombre
    // de monstres vus dans la session, négligeable — même choix que les deux autres renderers.
    private final Map<Integer, HealthLerp> lerpByEntityId = new HashMap<>();

    public MobHealthBarLayer(RenderLayerParent<S, M> parent) {
        super(parent);
    }

    @Override
    public void submit(
            PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        Float health = state.getRenderData(HEALTH);
        Float maxHealth = state.getRenderData(MAX_HEALTH);
        Integer entityId = state.getRenderData(ENTITY_ID);
        if (health == null || maxHealth == null || entityId == null || health >= maxHealth) {
            return;
        }
        if (state.distanceToCameraSq > MAX_DISTANCE_SQ) {
            return;
        }

        float target = health / maxHealth;
        HealthLerp lerp = this.lerpByEntityId.computeIfAbsent(entityId, id -> new HealthLerp(target, LERP_MILLISECONDS));
        lerp.setTarget(target);

        poseStack.pushPose();
        poseStack.translate(0.0F, state.boundingBoxHeight + ABOVE_HEAD_MARGIN, 0.0F);
        // RenderLayer#submit ne reçoit pas de CameraRenderState (contrairement à
        // BlockEntityRenderer/EntityRenderer#submit) : la caméra du frame en cours est un état
        // global, pas une donnée par entité, donc la lire en direct ici ne contourne pas la
        // séparation extraction/rendu de la même façon que toucherait l'entité vivante.
        poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        poseStack.scale(1.0F, -1.0F, 1.0F);

        HealthBarRendering.render(poseStack, submitNodeCollector, BAR_WIDTH, BAR_HEIGHT, lerp.currentPercent());

        poseStack.popPose();
    }
}
