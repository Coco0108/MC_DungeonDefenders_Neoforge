package com.github.c0c0tier.dungeon_defenders.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.entity.HealthBarRendering;
import com.github.c0c0tier.dungeon_defenders.block.entity.HealthLerp;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import java.util.HashMap;
import java.util.Map;

// Barre de vie en billboard au-dessus des monstres (zombie/squelette pour l'instant). Mêmes
// conditions d'affichage et même mécanisme que TowerHealthBarRenderer (endommagé + à portée de
// 16 blocs, animation temps réel 300 ms via HealthLerp, dessin via HealthBarRendering) — voir
// doc/05-etat-et-problemes-connus.md.
//
// PAS un RenderLayer (contrairement à la première version de cette classe) : un RenderLayer
// s'exécute DANS le repère local du modèle de LivingEntityRenderer#submit — poseStack déjà
// mis à l'échelle (-1,-1,1) et tourné selon state.bodyRot, entre le push et le pop de ce
// repère (voir la boucle `for (RenderLayer<S,M> layer : this.layers)`, avant
// `poseStack.popPose()`). Y appliquer soi-même une rotation caméra (comme pour un billboard
// world-space classique) compose deux transformations incompatibles et déplace/orient la
// géométrie n'importe où — c'était le vrai bug derrière "la barre ne s'affiche jamais" :
// probablement soumise, juste invisible/mal placée. Le nametag vanilla évite exactement ce
// piège en rendant APRÈS ce pop, via EntityRenderer#submitNameDisplay, appelé par
// LivingEntityRenderer#submit → super.submit(...) une fois le repère du modèle refermé.
// RenderLivingEvent.Post se déclenche juste après ce super.submit() : même repère
// caméra-relatif, sans transformation du modèle — exactement ce qu'il faut pour un billboard.
//
// La vie n'existe pas nativement sur un EntityRenderState vanilla (vérifié dans le code
// source : ni LivingEntityRenderState ni EntityRenderState ne portent de champ santé) :
// HEALTH/MAX_HEALTH/ENTITY_ID sont ajoutés au render state via RegisterRenderStateModifiersEvent
// (NeoForge, voir DungeonDefendersModClient#onRegisterRenderStateModifiers) et lus ici via
// ContextKey — ce mécanisme-là n'a jamais été le problème, seul l'endroit où le rendu du
// billboard avait lieu l'était.
@EventBusSubscriber(modid = DungeonDefendersMod.MODID, value = Dist.CLIENT)
public final class MobHealthBarRenderer {

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
    // render state (recréé à chaque frame), donc ici, dans une classe statique. Pas de BlockPos
    // (un monstre bouge) : indexée par l'ID d'entité, ajouté au render state pour la même
    // raison que la vie (voir ENTITY_ID ci-dessus). Jamais nettoyée mais bornée par le nombre
    // de monstres vus dans la session, négligeable — même choix que les deux autres renderers.
    private static final Map<Integer, HealthLerp> LERP_BY_ENTITY_ID = new HashMap<>();

    private MobHealthBarRenderer() {
    }

    // Se déclenche pour TOUTE LivingEntity (RenderLivingEvent n'est pas filtré par type) :
    // limité ici au zombie/squelette, seuls monstres du mod pour l'instant — pas de tag/liste
    // partagée avec SpawnableEnemy, ça resterait à généraliser le jour où ce filtre grandit.
    @SubscribeEvent
    static void onRenderLiving(RenderLivingEvent.Post<?, ?, ?> event) {
        LivingEntityRenderState state = event.getRenderState();
        if (state.entityType != EntityType.ZOMBIE && state.entityType != EntityType.SKELETON) {
            return;
        }

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
        HealthLerp lerp = LERP_BY_ENTITY_ID.computeIfAbsent(entityId, id -> new HealthLerp(target, LERP_MILLISECONDS));
        lerp.setTarget(target);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0F, state.boundingBoxHeight + ABOVE_HEAD_MARGIN, 0.0F);
        poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        poseStack.scale(1.0F, -1.0F, 1.0F);

        HealthBarRendering.render(poseStack, event.getSubmitNodeCollector(), BAR_WIDTH, BAR_HEIGHT, lerp.currentPercent());

        poseStack.popPose();
    }
}
