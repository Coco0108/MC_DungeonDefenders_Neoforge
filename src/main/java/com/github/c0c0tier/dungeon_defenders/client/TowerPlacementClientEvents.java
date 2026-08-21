package com.github.c0c0tier.dungeon_defenders.client;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.client.gui.screen.TowerWheelScreen;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.network.PlaceTowerPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.InteractionHand;
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

// Fait vivre le mode pose (voir TowerPlacementState) : ouverture de la roue, mise à jour de la
// cible visée, rotation, confirmation/annulation, et rendu de l'hologramme + du cercle de
// portée. Regroupé ici plutôt qu'éclaté, car toute cette logique partage le même état
// transitoire (TowerPlacementState) — voir doc/02-gameplay.md pour le détail du flux.
@EventBusSubscriber(modid = DungeonDefendersMod.MODID, value = Dist.CLIENT)
public class TowerPlacementClientEvents {

    // Portée du rayon de visée, en blocs — assez pour poser à bonne distance sans viser à
    // travers toute la carte.
    private static final double MAX_REACH = 20.0D;
    private static final int CIRCLE_SEGMENTS = 32;
    private static final float LINE_WIDTH = 2.0F;
    private static final int COLOR_VALID = ARGB.color(220, 0, 255, 0);
    private static final int COLOR_INVALID = ARGB.color(220, 255, 0, 0);
    private static final int COLOR_RANGE = ARGB.color(180, 255, 255, 0);

    private static final ContextKey<TowerPlacementRenderState> RENDER_KEY =
            new ContextKey<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "tower_placement"));

    private TowerPlacementClientEvents() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (ModKeyMappings.TOWER_WHEEL.consumeClick() && minecraft.screen == null) {
            // Autant prévenir tout de suite plutôt que de laisser le joueur faire tout le
            // mode pose pour se faire refuser à la toute fin (voir ModEvents.onBlockadePlace,
            // seule autorité réelle côté serveur) : la roue elle-même ne s'ouvre qu'en phase
            // Construction.
            if (minecraft.level != null && minecraft.player != null
                    && minecraft.level.getData(ModAttachments.GAME_PHASE) == GamePhase.BUILD.ordinal()) {
                minecraft.setScreen(new TowerWheelScreen());
            } else if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.translatable("dungeon_defenders.tower.build_phase_only"));
            }
        }

        if (!TowerPlacementState.isActive()) {
            return;
        }

        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            TowerPlacementState.cancel();
            return;
        }

        if (TowerPlacementState.step() == TowerPlacementState.Step.AIMING) {
            // Draine une éventuelle rotation en attente : elle n'a pas d'effet tant que la
            // position n'est pas verrouillée, et ne doit pas s'appliquer en rafale une fois
            // l'étape orientation atteinte.
            ModKeyMappings.ROTATE_TOWER.consumeClick();
            updateTargetFromRaycast(player, level);
        } else if (ModKeyMappings.ROTATE_TOWER.consumeClick()) {
            TowerPlacementState.rotate();
        }
    }

    private static void updateTargetFromRaycast(LocalPlayer player, Level level) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(MAX_REACH));

        BlockHitResult hit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        if (hit.getType() != HitResult.Type.BLOCK) {
            TowerPlacementState.updateTarget(null, false);
            return;
        }

        BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
        boolean valid = level.getBlockState(pos).canBeReplaced();
        TowerPlacementState.updateTarget(pos, valid);
    }

    @SubscribeEvent
    static void onInteractionKeyTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!TowerPlacementState.isActive()) {
            return;
        }

        if (event.isAttack()) {
            event.setCanceled(true);
            TowerPlacementState.cancel();
            return;
        }

        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        event.setCanceled(true);

        if (TowerPlacementState.step() == TowerPlacementState.Step.AIMING) {
            if (TowerPlacementState.isTargetValid() && TowerPlacementState.targetPos() != null) {
                TowerPlacementState.lockPosition();
            }
            return;
        }

        confirmPlacement();
    }

    private static void confirmPlacement() {
        TowerDefinition tower = TowerPlacementState.selected();
        BlockPos pos = TowerPlacementState.targetPos();
        if (tower == null || pos == null) {
            TowerPlacementState.cancel();
            return;
        }

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new PlaceTowerPayload(
                    tower.ordinal(), pos, TowerPlacementState.rotation().ordinal()).toVanillaServerbound());
        }
        TowerPlacementState.cancel();
    }

    @SubscribeEvent
    static void onExtractRenderState(ExtractLevelRenderStateEvent event) {
        TowerDefinition tower = TowerPlacementState.selected();
        BlockPos pos = TowerPlacementState.targetPos();

        if (tower == null || pos == null) {
            event.getRenderState().setRenderData(RENDER_KEY, null);
            return;
        }

        TowerPlacementRenderState state = new TowerPlacementRenderState();
        state.pos = pos;
        // En ORIENTING, la position est déjà verrouillée comme valide (voir
        // onInteractionKeyTriggered) : toujours vert à cette étape.
        state.valid = TowerPlacementState.step() == TowerPlacementState.Step.ORIENTING
                || TowerPlacementState.isTargetValid();
        state.step = TowerPlacementState.step();
        state.rotation = TowerPlacementState.rotation();
        state.range = tower.range();
        state.coneAngleDegrees = tower.coneAngleDegrees();
        event.getRenderState().setRenderData(RENDER_KEY, state);
    }

    @SubscribeEvent
    static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        TowerPlacementRenderState state = event.getLevelRenderState().getRenderData(RENDER_KEY);
        if (state == null) {
            return;
        }

        Vec3 camPos = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        int color = state.valid ? COLOR_VALID : COLOR_INVALID;

        poseStack.pushPose();
        poseStack.translate(state.pos.getX() - camPos.x, state.pos.getY() - camPos.y, state.pos.getZ() - camPos.z);
        if (state.step == TowerPlacementState.Step.ORIENTING) {
            // Symbolique pour l'instant : Spike Blockade est un cube symétrique, cette
            // rotation n'a donc aucun effet visuel sur lui — prête pour une future tour
            // asymétrique (voir doc/02-gameplay.md).
            poseStack.translate(0.5D, 0.5D, 0.5D);
            poseStack.mulPose(Axis.YP.rotationDegrees(facingYRot(state.rotation)));
            poseStack.translate(-0.5D, -0.5D, -0.5D);
        }
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.lines(),
                (pose, buffer) -> renderBoxOutline(pose, buffer, Shapes.block(), color, LINE_WIDTH));
        poseStack.popPose();

        if (state.range > 0.0D) {
            poseStack.pushPose();
            poseStack.translate(
                    state.pos.getX() - camPos.x + 0.5D,
                    state.pos.getY() - camPos.y,
                    state.pos.getZ() - camPos.z + 0.5D);
            // Toujours appliquée (pas seulement en ORIENTING comme pour le contour du bloc,
            // qui ne montre de toute façon jamais sa rotation visuellement pour un cube
            // parfait) : le cône doit refléter state.rotation dès l'étape "visée" (NORTH par
            // défaut), pas seulement une fois la position verrouillée.
            poseStack.mulPose(Axis.YP.rotationDegrees(facingYRot(state.rotation)));
            double coneAngle = state.coneAngleDegrees;
            event.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.lines(),
                    (pose, buffer) -> renderRangeArea(pose, buffer, state.range, coneAngle, COLOR_RANGE, LINE_WIDTH));
            poseStack.popPose();
        }
    }

    // Même principe que ShapeRenderer.renderShape (vanilla), adapté pour prendre un
    // PoseStack.Pose plutôt qu'un PoseStack complet : submitCustomGeometry ne fournit que le
    // Pose au moment différé où ce rendu est réellement exécuté.
    private static void renderBoxOutline(
            PoseStack.Pose pose, VertexConsumer buffer, VoxelShape shape, int color, float width) {
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            Vector3f normal = new Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
            buffer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(color).setNormal(pose, normal).setLineWidth(width);
            buffer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(color).setNormal(pose, normal).setLineWidth(width);
        });
    }

    // Rotation Y (degrés) à appliquer au gabarit du cône/contour pour qu'il pointe dans la
    // direction donnée. Volontairement PAS Direction.toYRot() : cette méthode a une convention
    // différente (SOUTH=0°, WEST=90°, NORTH=180°, EAST=270°, utilisée pour le yaw des entités),
    // qui avait fait pointer le cône de prévisualisation à 180° de la vraie direction de tir
    // (bug constaté en jeu). Cette table suit au contraire la convention du blockstate posé
    // (voir harpoon_turret.json : facing=north → y:0, facing=east → y:90, etc.), pour que
    // l'aperçu corresponde exactement à l'orientation réellement appliquée au bloc.
    private static float facingYRot(Direction direction) {
        return switch (direction) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F; // NORTH (et UP/DOWN, qui n'arrivent jamais ici)
        };
    }

    // Cercle complet si coneAngleDegrees >= 360 (omnidirectionnel, ex. Spike Blockade si un
    // jour range() > 0), sinon un secteur/cône borné : l'arc PLUS deux segments droits vers
    // l'origine, pour lire visuellement un cône et pas un arc flottant dans le vide.
    //
    // Convention du gabarit local (avant rotation par l'appelant) : angle -90° = (0,0,-radius),
    // soit la direction NORTH — cohérent avec facingYRot(NORTH) == 0 (rotation identité),
    // vérifiée pour les 4 directions (voir facingYRot ci-dessus ; ce gabarit utilisait
    // auparavant Direction.toYRot(), qui a une convention différente et faisait pointer le
    // cône à 180° de la vraie direction de tir — corrigé).
    private static void renderRangeArea(
            PoseStack.Pose pose, VertexConsumer buffer, double radius, double coneAngleDegrees, int color, float width) {
        Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F);
        boolean omnidirectional = coneAngleDegrees >= 360.0D;
        double centerDeg = -90.0D;
        double startDeg = omnidirectional ? 0.0D : centerDeg - coneAngleDegrees / 2.0D;
        double endDeg = omnidirectional ? 360.0D : centerDeg + coneAngleDegrees / 2.0D;
        int segments = omnidirectional
                ? CIRCLE_SEGMENTS
                : Math.max(1, (int) Math.round(CIRCLE_SEGMENTS * (coneAngleDegrees / 360.0D)));

        float prevX = (float) (Math.cos(Math.toRadians(startDeg)) * radius);
        float prevZ = (float) (Math.sin(Math.toRadians(startDeg)) * radius);

        if (!omnidirectional) {
            buffer.addVertex(pose, 0.0F, 0.0F, 0.0F).setColor(color).setNormal(pose, normal).setLineWidth(width);
            buffer.addVertex(pose, prevX, 0.0F, prevZ).setColor(color).setNormal(pose, normal).setLineWidth(width);
        }

        for (int i = 1; i <= segments; i++) {
            double deg = startDeg + (endDeg - startDeg) * i / segments;
            float x = (float) (Math.cos(Math.toRadians(deg)) * radius);
            float z = (float) (Math.sin(Math.toRadians(deg)) * radius);
            buffer.addVertex(pose, prevX, 0.0F, prevZ).setColor(color).setNormal(pose, normal).setLineWidth(width);
            buffer.addVertex(pose, x, 0.0F, z).setColor(color).setNormal(pose, normal).setLineWidth(width);
            prevX = x;
            prevZ = z;
        }

        if (!omnidirectional) {
            buffer.addVertex(pose, prevX, 0.0F, prevZ).setColor(color).setNormal(pose, normal).setLineWidth(width);
            buffer.addVertex(pose, 0.0F, 0.0F, 0.0F).setColor(color).setNormal(pose, normal).setLineWidth(width);
        }
    }
}
