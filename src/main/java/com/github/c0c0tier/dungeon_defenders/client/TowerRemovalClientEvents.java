package com.github.c0c0tier.dungeon_defenders.client;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.network.RemoveTowerPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Vector3f;

// Fait vivre le mode suppression de tour (voir TowerRemovalState) : bascule au clavier, mise à
// jour de la cible visée, envoi de la confirmation, et rendu du contour de surbrillance. Même
// principe que TowerPlacementClientEvents (raycast OUTLINE + rendu par ContextKey), mais sans
// hologramme ni étape d'orientation — juste "vise une tour, clique".
@EventBusSubscriber(modid = DungeonDefendersMod.MODID, value = Dist.CLIENT)
public class TowerRemovalClientEvents {

    private static final double MAX_REACH = 20.0D;
    private static final float LINE_WIDTH = 2.0F;
    // Distinct des couleurs vert/rouge de validité de pose (TowerPlacementClientEvents) pour
    // ne pas laisser croire aux deux modes qu'ils partagent une sémantique - orange = "cible
    // prête à être supprimée".
    private static final int COLOR_TARGET = ARGB.color(220, 255, 140, 0);

    private static final ContextKey<BlockPos> RENDER_KEY =
            new ContextKey<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "tower_removal_target"));

    private TowerRemovalClientEvents() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (ModKeyMappings.REMOVE_TOWER_MODE.consumeClick() && minecraft.screen == null
                && !TowerPlacementState.isActive()) {
            TowerRemovalState.toggle();
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.translatable(TowerRemovalState.isActive()
                        ? "dungeon_defenders.tower.removal_mode_on"
                        : "dungeon_defenders.tower.removal_mode_off"));
            }
        }

        if (!TowerRemovalState.isActive()) {
            return;
        }

        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            TowerRemovalState.cancel();
            return;
        }

        // Même raisonnement que pour l'ouverture de la roue (TowerPlacementClientEvents) :
        // les tours ne se retirent qu'en phase Construction, autant sortir du mode dès que la
        // phase change plutôt que laisser le joueur cliquer pour rien jusqu'au refus serveur.
        if (!GamePhase.of(level).allowsTowerBuilding()) {
            TowerRemovalState.cancel();
            player.sendSystemMessage(Component.translatable("dungeon_defenders.tower.build_phase_only"));
            return;
        }

        updateTargetFromRaycast(player, level);
    }

    private static void updateTargetFromRaycast(LocalPlayer player, Level level) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(MAX_REACH));

        BlockHitResult hit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        if (hit.getType() != HitResult.Type.BLOCK) {
            TowerRemovalState.updateTarget(null, false);
            return;
        }

        BlockPos pos = hit.getBlockPos();
        boolean valid = level.getBlockEntity(pos) instanceof AbstractTowerBlockEntity;
        TowerRemovalState.updateTarget(pos, valid);
    }

    @SubscribeEvent
    static void onInteractionKeyTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!TowerRemovalState.isActive() || !event.isAttack()) {
            return;
        }
        // Toujours annulé pendant le mode : on ne veut jamais qu'un clic gauche casse un bloc
        // ou frappe un monstre pendant qu'on vise une tour à retirer.
        event.setCanceled(true);

        BlockPos pos = TowerRemovalState.targetPos();
        if (!TowerRemovalState.isTargetValid() || pos == null) {
            return;
        }

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new RemoveTowerPayload(pos).toVanillaServerbound());
        }

        // Désactivé après chaque suppression plutôt que laissé actif pour en enchaîner
        // plusieurs (comportement d'origine) : changé sur retour du joueur (2026-08-26), qui
        // supprime généralement une seule tour à la fois — redevenir actif à chaque suppression
        // était plus gênant que pratique en usage réel. Reste un simple appui sur `X` pour
        // repartir sur la suivante.
        TowerRemovalState.cancel();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(Component.translatable("dungeon_defenders.tower.removal_mode_off"));
        }
    }

    @SubscribeEvent
    static void onExtractRenderState(ExtractLevelRenderStateEvent event) {
        BlockPos pos = TowerRemovalState.isActive() && TowerRemovalState.isTargetValid()
                ? TowerRemovalState.targetPos()
                : null;
        event.getRenderState().setRenderData(RENDER_KEY, pos);
    }

    @SubscribeEvent
    static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        BlockPos pos = event.getLevelRenderState().getRenderData(RENDER_KEY);
        if (pos == null) {
            return;
        }

        Vec3 camPos = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.lines(),
                (pose, buffer) -> renderBoxOutline(pose, buffer, Shapes.block(), COLOR_TARGET, LINE_WIDTH));
        poseStack.popPose();
    }

    // Copié de TowerPlacementClientEvents (même principe, pas assez de logique partagée pour
    // justifier une extraction commune vu la taille des deux classes).
    private static void renderBoxOutline(
            PoseStack.Pose pose, VertexConsumer buffer, VoxelShape shape, int color, float width) {
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            Vector3f normal = new Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
            buffer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(color).setNormal(pose, normal).setLineWidth(width);
            buffer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(color).setNormal(pose, normal).setLineWidth(width);
        });
    }
}
