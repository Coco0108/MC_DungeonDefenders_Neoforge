package com.github.c0c0tier.dungeon_defenders.client;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import com.github.c0c0tier.dungeon_defenders.init.TowerStats;
import com.github.c0c0tier.dungeon_defenders.network.UpgradeTowerPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

// "Inspecter" une tour déjà posée simplement en la regardant (idées du joueur, issues #13 et
// #16, faites ensemble le 2026-11-10) : sa portée se dessine au sol comme pendant la pose, sa
// barre de vie s'affiche même à PV pleins, et son nom + ses PV apparaissent sous le viseur.
//
// Un seul déclencheur, le regard, plutôt qu'un clic ou une touche : rien à apprendre, rien à
// refermer, et aucun conflit avec le clic droit/gauche déjà pris par la pose et la suppression.
// Désactivé pendant ces deux modes, qui ont leur propre affichage au même endroit.
//
// Même raycast OUTLINE et même portée que la suppression de tour (TowerRemovalClientEvents) ; le
// dessin de la portée réutilise tel quel celui de la pose (TowerPlacementClientEvents).
@EventBusSubscriber(modid = DungeonDefendersMod.MODID, value = Dist.CLIENT)
public final class TowerInspectClientEvents {

    private static final double MAX_REACH = TowerDefinition.REACH;

    /** Ce qu'il faut pour dessiner la portée de la tour regardée. */
    private record RangeRenderState(BlockPos pos, Direction facing, double range, double coneAngleDegrees) {
    }

    private static final ContextKey<RangeRenderState> RENDER_KEY =
            new ContextKey<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "tower_inspect_range"));

    private TowerInspectClientEvents() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;

        if (player == null || level == null || minecraft.screen != null
                || TowerPlacementState.isActive() || TowerRemovalState.isActive()) {
            TowerInspectState.setTarget(null);
            return;
        }

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(MAX_REACH));
        BlockHitResult hit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        boolean onTower = hit.getType() == HitResult.Type.BLOCK
                && level.getBlockEntity(hit.getBlockPos()) instanceof AbstractTowerBlockEntity;
        TowerInspectState.setTarget(onTower ? hit.getBlockPos().immutable() : null);

        // Touche d'amélioration : n'a d'effet qu'en regardant une tour. Le serveur revérifie tout
        // (phase, niveau maximum, mana) et répond par un message ; consumeClick est vidé même
        // sans cible, pour qu'un appui dans le vide ne se déclenche pas plus tard.
        while (ModKeyMappings.UPGRADE_TOWER.consumeClick()) {
            ClientPacketListener connection = minecraft.getConnection();
            if (onTower && connection != null) {
                connection.send(new UpgradeTowerPayload(hit.getBlockPos().immutable()).toVanillaServerbound());
            }
        }
    }

    @SubscribeEvent
    static void onExtractRenderState(ExtractLevelRenderStateEvent event) {
        event.getRenderState().setRenderData(RENDER_KEY, currentRange());
    }

    // Recalculé à chaque frame à partir de la tour regardée : sa portée peut changer d'un
    // /reload à l'autre (stats data-driven), et la tour peut avoir été détruite entre deux ticks.
    private static RangeRenderState currentRange() {
        BlockPos pos = TowerInspectState.target();
        Level level = Minecraft.getInstance().level;
        if (pos == null || level == null || !(level.getBlockEntity(pos) instanceof AbstractTowerBlockEntity tower)) {
            return null;
        }

        TowerStats stats = tower.definition().stats(true);
        double range = stats.getOr(TowerStat.RANGE, 0.0D);
        if (range <= 0.0D) {
            return null; // une Blockade n'a pas de portée à montrer
        }

        BlockState state = tower.getBlockState();
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
        return new RangeRenderState(pos, facing, range,
                stats.getOr(TowerStat.CONE_ANGLE_DEGREES, TowerDefinition.FULL_CIRCLE_DEGREES));
    }

    @SubscribeEvent
    static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        RangeRenderState state = event.getLevelRenderState().getRenderData(RENDER_KEY);
        if (state == null) {
            return;
        }

        Vec3 camPos = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(
                state.pos().getX() - camPos.x + 0.5D,
                state.pos().getY() - camPos.y,
                state.pos().getZ() - camPos.z + 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(TowerPlacementClientEvents.facingYRot(state.facing())));
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.lines(),
                (pose, buffer) -> TowerPlacementClientEvents.renderRangeArea(pose, buffer, state.range(),
                        state.coneAngleDegrees(), TowerPlacementClientEvents.COLOR_RANGE, TowerPlacementClientEvents.LINE_WIDTH));
        poseStack.popPose();
    }
}
