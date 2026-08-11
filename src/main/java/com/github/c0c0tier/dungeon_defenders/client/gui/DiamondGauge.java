package com.github.c0c0tier.dungeon_defenders.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

// Dessine une jauge en forme de losange, remplie du bas vers le haut selon un pourcentage —
// première étape vers un HUD qui ressemble au jeu de référence (voir 05-etat-et-problemes-
// connus.md et 02-gameplay.md), en s'en tenant volontairement à guiGraphics.fill() (pas de
// texture, pas de géométrie custom bas niveau) : un empilement de bandes horizontales dont la
// largeur croît puis décroît, façon pixel art. Les vraies textures viendront dans un second
// temps, une fois disponibles.
final class DiamondGauge {

    private DiamondGauge() {
    }

    // centerX/bottomY : position du sommet bas du losange. radius : demi-largeur/demi-hauteur
    // (le losange occupe un carré de 2*radius de côté, pointes en haut/bas/gauche/droite).
    static void render(GuiGraphicsExtractor guiGraphics, int centerX, int bottomY, int radius,
            double fillRatio, int filledColor, int emptyColor) {
        int height = radius * 2;
        double clampedRatio = Math.max(0.0, Math.min(1.0, fillRatio));
        int filledRows = (int) Math.round(height * clampedRatio);
        double middleRow = (height - 1) / 2.0;

        // row = 0 : bande la plus basse (pointe du bas) ; row = height - 1 : bande la plus
        // haute (pointe du haut). La largeur est maximale (radius) au milieu, nulle aux deux
        // extrémités : c'est ce qui dessine le losange.
        for (int row = 0; row < height; row++) {
            int halfWidth = (int) Math.round(radius - Math.abs(row - middleRow));
            if (halfWidth <= 0) {
                continue;
            }
            int y = bottomY - 1 - row;
            int color = row < filledRows ? filledColor : emptyColor;
            guiGraphics.fill(centerX - halfWidth, y, centerX + halfWidth, y + 1, color);
        }
    }
}
