package com.github.c0c0tier.dungeon_defenders.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

// Dessine un rond plein avec une fine bordure, en couleurs plates — même philosophie que
// DiamondGauge : un empilement de guiGraphics.fill() (une bande de 1px par ligne, largeur
// donnée par le théorème de Pythagore), pas de texture ni de géométrie custom bas niveau.
// Sert de fond aux emplacements de compétences (voir AbilitySlotsOverlay), en attendant de
// vraies icônes.
final class CircleSlot {

    private CircleSlot() {
    }

    static void render(GuiGraphicsExtractor guiGraphics, int centerX, int centerY, int radius, int fillColor, int borderColor) {
        renderDisc(guiGraphics, centerX, centerY, radius, borderColor);
        renderDisc(guiGraphics, centerX, centerY, radius - 2, fillColor);
    }

    /**
     * Voile posé par-dessus un emplacement : couvre la part basse {@code fraction} (0 à 1) du
     * disque intérieur. Sert à la recharge (le voile descend à mesure qu'elle s'écoule) et, à
     * fraction 1, au grisé d'une compétence inutilisable.
     */
    static void renderVeil(GuiGraphicsExtractor guiGraphics, int centerX, int centerY, int radius, float fraction, int color) {
        int inner = radius - 2;
        int firstRow = inner - Math.round(Math.clamp(fraction, 0.0F, 1.0F) * (inner * 2 + 1)) + 1;
        for (int dy = Math.max(firstRow, -inner); dy <= inner; dy++) {
            int halfWidth = (int) Math.round(Math.sqrt((double) (inner * inner - dy * dy)));
            if (halfWidth <= 0) {
                continue;
            }
            int y = centerY + dy;
            guiGraphics.fill(centerX - halfWidth, y, centerX + halfWidth, y + 1, color);
        }
    }

    private static void renderDisc(GuiGraphicsExtractor guiGraphics, int centerX, int centerY, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int halfWidth = (int) Math.round(Math.sqrt((double) (radius * radius - dy * dy)));
            if (halfWidth <= 0) {
                continue;
            }
            int y = centerY + dy;
            guiGraphics.fill(centerX - halfWidth, y, centerX + halfWidth, y + 1, color);
        }
    }
}
