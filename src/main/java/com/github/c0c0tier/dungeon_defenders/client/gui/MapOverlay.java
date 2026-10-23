package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.MapInstance;
import com.github.c0c0tier.dungeon_defenders.TavernSpawn;
import com.github.c0c0tier.dungeon_defenders.client.MapOverlayState;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Plan plein écran de la zone actuelle (taverne ou map), avec un point par joueur — voir
// MapOverlayClientEvents pour la touche (maintenue) et le scan du cristal. Première tentative,
// volontairement simple : un rectangle schématique aux vraies proportions (pas un rendu de
// terrain façon carte vanilla), pas d'interpolation de mouvement (positions tick-à-tick brutes,
// largement assez fluide pour un point de quelques pixels).
//
// Décidé avec le joueur (2026-09-07) : approche expérimentale, "on essaie comme ça et on verra
// sinon on fera autrement" — pas peaufiné avant d'avoir vu le résultat en jeu.
public class MapOverlay implements GuiLayer {

    private static final int BACKDROP_COLOR = 0xD0101010;
    private static final int RECT_FILL_COLOR = 0x992B2B2B;
    private static final int RECT_BORDER_COLOR = 0xFFFFFFFF;
    private static final float RECT_SCREEN_RATIO = 0.7F;

    private static final int DOT_RADIUS = 4;
    private static final int DOT_BORDER_COLOR = 0xFF000000;
    private static final int LOCAL_PLAYER_COLOR = 0xFF33DD55;
    // Palette pour les autres joueurs, choisie par le hash de leur UUID (voir colorFor) — pas
    // de vrai système de couleur/équipe par joueur dans le mod, une palette fixe suffit pour
    // les distinguer d'un coup d'œil.
    private static final int[] TEAMMATE_COLORS = {0xFF3388FF, 0xFFFF8800, 0xFFCC44FF, 0xFFFFDD22};

    private static final int CRYSTAL_MARKER_SIZE = 6;
    private static final int CRYSTAL_MARKER_COLOR = 0xFFFFD700;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        if (!MapOverlayState.visible()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        Player localPlayer = minecraft.player;
        if (level == null || localPlayer == null || minecraft.options.hideGui) {
            return;
        }

        int sizeX = level.getData(ModAttachments.PLAYFIELD_SIZE_X);
        int sizeZ = level.getData(ModAttachments.PLAYFIELD_SIZE_Z);
        if (sizeX <= 0 || sizeZ <= 0) {
            return;
        }

        BlockPos anchor = GamePhase.of(level) == GamePhase.TAVERN ? TavernSpawn.SPAWN_POS : MapInstance.MAP_POS;
        double originX = anchor.getX() - sizeX / 2.0D;
        double originZ = anchor.getZ() - sizeZ / 2.0D;

        int screenW = guiGraphics.guiWidth();
        int screenH = guiGraphics.guiHeight();
        guiGraphics.fill(0, 0, screenW, screenH, BACKDROP_COLOR);

        double scale = Math.min(
                screenW * RECT_SCREEN_RATIO / sizeX,
                screenH * RECT_SCREEN_RATIO / sizeZ);
        int rectW = Math.max(1, (int) Math.round(sizeX * scale));
        int rectH = Math.max(1, (int) Math.round(sizeZ * scale));
        int rectLeft = (screenW - rectW) / 2;
        int rectTop = (screenH - rectH) / 2;

        guiGraphics.fill(rectLeft, rectTop, rectLeft + rectW, rectTop + rectH, RECT_FILL_COLOR);
        renderRectBorder(guiGraphics, rectLeft, rectTop, rectW, rectH);

        BlockPos crystalPos = MapOverlayState.crystalPos();
        if (crystalPos != null) {
            int cx = toScreenX(crystalPos.getX() + 0.5D, originX, sizeX, rectLeft, rectW);
            int cy = toScreenZ(crystalPos.getZ() + 0.5D, originZ, sizeZ, rectTop, rectH);
            guiGraphics.fill(
                    cx - CRYSTAL_MARKER_SIZE / 2, cy - CRYSTAL_MARKER_SIZE / 2,
                    cx + CRYSTAL_MARKER_SIZE / 2, cy + CRYSTAL_MARKER_SIZE / 2,
                    CRYSTAL_MARKER_COLOR);
        }

        for (Player player : level.players()) {
            boolean isLocal = player == localPlayer;
            int color = isLocal ? LOCAL_PLAYER_COLOR : colorFor(player);
            int px = toScreenX(player.getX(), originX, sizeX, rectLeft, rectW);
            int py = toScreenZ(player.getZ(), originZ, sizeZ, rectTop, rectH);
            CircleSlot.render(guiGraphics, px, py, DOT_RADIUS, color, DOT_BORDER_COLOR);
        }
    }

    private static int toScreenX(double worldX, double originX, int sizeX, int rectLeft, int rectW) {
        double normalized = Mth.clamp((worldX - originX) / sizeX, 0.0D, 1.0D);
        return rectLeft + (int) Math.round(normalized * rectW);
    }

    private static int toScreenZ(double worldZ, double originZ, int sizeZ, int rectTop, int rectH) {
        double normalized = Mth.clamp((worldZ - originZ) / sizeZ, 0.0D, 1.0D);
        return rectTop + (int) Math.round(normalized * rectH);
    }

    private static void renderRectBorder(GuiGraphicsExtractor guiGraphics, int left, int top, int width, int height) {
        int right = left + width;
        int bottom = top + height;
        guiGraphics.fill(left, top, right, top + 1, RECT_BORDER_COLOR);
        guiGraphics.fill(left, bottom - 1, right, bottom, RECT_BORDER_COLOR);
        guiGraphics.fill(left, top, left + 1, bottom, RECT_BORDER_COLOR);
        guiGraphics.fill(right - 1, top, right, bottom, RECT_BORDER_COLOR);
    }

    // Couleur déterministe par joueur (même joueur = même couleur toute la session), pas
    // d'affectation par ordre de connexion pour éviter qu'elle change si quelqu'un se
    // reconnecte entre deux ouvertures de l'overlay.
    private static int colorFor(Player player) {
        int index = Math.floorMod(player.getUUID().hashCode(), TEAMMATE_COLORS.length);
        return TEAMMATE_COLORS[index];
    }
}
