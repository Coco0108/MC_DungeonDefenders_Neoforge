package com.github.c0c0tier.dungeon_defenders.client;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// Repérage en créatif des cinq blocs marqueurs invisibles (RenderShape.INVISIBLE) que le mod
// utilise pour construire une map : spawn joueur, zone interdite, configuration de map, support
// de mannequin, spawner. Sans ça, un mappeur ne peut les retrouver qu'en visant très
// précisément au jugé (leur seul repère restant, voir getShape sur chacun de ces blocs) —
// demandé en jeu (2026-09-06), pendant le premier test de la pile maps/taverne.
//
// Principe : un scan périodique d'un cube autour du joueur créatif, puis pour chaque marqueur
// trouvé, un contour coloré (même technique que TowerPlacementClientEvents/
// TowerRemovalClientEvents, RenderTypes.lines() via LineBoxRenderer — masqué par les murs,
// utile une fois à portée de vue) PLUS une étiquette de nom façon "nom au-dessus d'un mob", à
// travers les murs (submitNameTag(..., seeThrough=true)) pour rester repérable même sans ligne
// de vue directe. Rien de tout ceci n'existe en survie : c'est un outil d'édition de map, pas
// un élément de jeu.
@EventBusSubscriber(modid = DungeonDefendersMod.MODID, value = Dist.CLIENT)
public final class MarkerOverlayClientEvents {

    // Rayon du scan, en blocs, et intervalle entre deux scans — valeurs de test, jamais vues en
    // jeu. À agrandir si des maps dépassent un cube de ce rayon autour du joueur ; à réduire ou
    // espacer si le scan coûte trop cher (jamais profilé, mais un cube de rayon 16 ne fait que
    // 33^3 positions, negligeable pour un scan une fois par seconde).
    private static final int SCAN_RADIUS = 16;
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final float LINE_WIDTH = 2.0F;
    // Hauteur de l'étiquette au-dessus du bloc, en blocs.
    private static final float LABEL_Y = 1.2F;
    // Lightmap "pleine luminosité" (0xF000F0) : même constante que SpawnerBlockEntityRenderer,
    // pour que l'étiquette reste lisible quel que soit l'éclairage ambiant.
    private static final int FULL_BRIGHT_LIGHT = 15728880;

    private static final ContextKey<List<FoundMarker>> RENDER_KEY =
            new ContextKey<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "marker_overlay"));

    private static List<FoundMarker> markers = List.of();
    private static int tickCounter = 0;

    private MarkerOverlayClientEvents() {
    }

    // Une couleur par type, purement pour distinguer les marqueurs entre eux d'un coup d'œil —
    // valeurs de test, à ajuster une fois vues en jeu. Le nom affiché réutilise directement
    // Block#getName() (clé de traduction block.dungeon_defenders.<id>, déjà présente pour
    // chacun), pas de nouvelle clé de langue nécessaire.
    private enum MarkerType {
        PLAYER_SPAWN(() -> ModBlocks.PLAYER_SPAWN.get(), ARGB.color(255, 0, 220, 255)),
        NO_BUILD_ZONE(() -> ModBlocks.NO_BUILD_ZONE.get(), ARGB.color(255, 255, 60, 60)),
        MAP_CONFIG(() -> ModBlocks.MAP_CONFIG.get(), ARGB.color(255, 255, 220, 0)),
        TRAINING_DUMMY(() -> ModBlocks.TRAINING_DUMMY.get(), ARGB.color(255, 255, 140, 0)),
        SPAWNER(() -> ModBlocks.SPAWNER.get(), ARGB.color(255, 200, 0, 255));

        private final Supplier<Block> block;
        private final int color;

        MarkerType(Supplier<Block> block, int color) {
            this.block = block;
            this.color = color;
        }

        Component displayName() {
            return this.block.get().getName();
        }
    }

    private record FoundMarker(BlockPos pos, MarkerType type) {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;

        if (player == null || level == null || !player.isCreative()) {
            markers = List.of();
            return;
        }

        // Rescanné une fois par intervalle, pas à chaque tick : ces marqueurs ne bougent
        // quasiment jamais une fois posés, pas besoin de suivre le joueur en temps réel.
        if (tickCounter++ % SCAN_INTERVAL_TICKS != 0) {
            return;
        }
        markers = scan(level, player.blockPosition());
    }

    private static List<FoundMarker> scan(ClientLevel level, BlockPos center) {
        List<FoundMarker> found = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -SCAN_RADIUS; y <= SCAN_RADIUS; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!level.isLoaded(pos)) {
                        continue;
                    }
                    Block block = level.getBlockState(pos).getBlock();
                    for (MarkerType type : MarkerType.values()) {
                        if (block == type.block.get()) {
                            found.add(new FoundMarker(pos.immutable(), type));
                            break;
                        }
                    }
                }
            }
        }
        return found;
    }

    @SubscribeEvent
    static void onExtractRenderState(ExtractLevelRenderStateEvent event) {
        event.getRenderState().setRenderData(RENDER_KEY, markers.isEmpty() ? null : markers);
    }

    @SubscribeEvent
    static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        List<FoundMarker> currentMarkers = event.getLevelRenderState().getRenderData(RENDER_KEY);
        if (currentMarkers == null) {
            return;
        }

        Vec3 camPos = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();

        for (FoundMarker marker : currentMarkers) {
            BlockPos pos = marker.pos();
            int color = marker.type().color;

            poseStack.pushPose();
            poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
            event.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.lines(),
                    (pose, buffer) -> LineBoxRenderer.renderBoxOutline(pose, buffer, Shapes.block(), color, LINE_WIDTH));
            poseStack.popPose();

            double labelX = pos.getX() + 0.5D;
            double labelY = pos.getY() + LABEL_Y;
            double labelZ = pos.getZ() + 0.5D;
            poseStack.pushPose();
            poseStack.translate(labelX - camPos.x, labelY - camPos.y, labelZ - camPos.z);
            // seeThrough = true : c'est ce qui rend l'étiquette lisible à travers les murs,
            // exactement comme le nom d'un joueur/mob brillant (Glowing) au-delà d'un obstacle.
            event.getSubmitNodeCollector().submitNameTag(
                    poseStack, null, 0, marker.type().displayName(), true, FULL_BRIGHT_LIGHT,
                    camPos.distanceToSqr(labelX, labelY, labelZ), event.getLevelRenderState().cameraRenderState);
            poseStack.popPose();
        }
    }
}
