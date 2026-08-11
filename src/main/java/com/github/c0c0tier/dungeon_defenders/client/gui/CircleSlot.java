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
