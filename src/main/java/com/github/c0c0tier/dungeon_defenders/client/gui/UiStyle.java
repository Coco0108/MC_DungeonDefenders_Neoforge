package com.github.c0c0tier.dungeon_defenders.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

// Style commun des écrans et encadrés du mod (roue des tours, Forge, caractéristiques, choix de
// héros, fiche de tour, butin au sol). Tout est dessiné en aplats (fill), sans texture : un
// panneau sombre aux bords biseautés, liseré doré pour les titres, cases creusées comme les
// emplacements d'inventaire vanilla. Centraliser les couleurs ici garde les écrans cohérents
// entre eux ; changer l'ambiance du mod, c'est changer ces constantes.
public final class UiStyle {

    // Panneau : fond presque opaque, bord extérieur noir, biseau clair en haut à gauche et
    // sombre en bas à droite.
    public static final int PANEL_FILL = 0xF0201A14;
    public static final int PANEL_OUTER = 0xFF000000;
    public static final int PANEL_LIGHT = 0xFF6E5B3E;
    public static final int PANEL_DARK = 0xFF120E0A;
    // Bandeau de titre.
    public static final int HEADER_FILL = 0xFF3A2D1C;
    public static final int GOLD = 0xFFE8C060;
    // Cases creusées (emplacements, jauges vides, encarts).
    public static final int SLOT_FILL = 0xFF14100C;
    public static final int SLOT_DARK = 0xFF080604;
    public static final int SLOT_LIGHT = 0xFF4A3D2A;
    public static final int HOVER_FILL = 0x40FFFFFF;

    public static final int TEXT = 0xFFEDE6D6;
    public static final int DIM = 0xFF9C9284;
    public static final int MANA = 0xFF6FB8FF;
    public static final int POSITIVE = 0xFF6BD26B;
    public static final int NEGATIVE = 0xFFE06060;
    public static final int HEALTH = 0xFF4FC24F;
    public static final int HEALTH_LOW = 0xFFD84A3A;

    public static final int HEADER_HEIGHT = 16;

    private UiStyle() {
    }

    /** Panneau biseauté, sans titre. */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, PANEL_OUTER);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, PANEL_FILL);
        g.fill(x + 1, y + 1, x + width - 1, y + 2, PANEL_LIGHT);
        g.fill(x + 1, y + 1, x + 2, y + height - 1, PANEL_LIGHT);
        g.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, PANEL_DARK);
        g.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, PANEL_DARK);
    }

    /** Panneau avec un bandeau de titre doré en haut ; le contenu commence à y + HEADER_HEIGHT + 4. */
    public static void titledPanel(GuiGraphicsExtractor g, Font font, Component title, int x, int y, int width, int height) {
        panel(g, x, y, width, height);
        g.fill(x + 2, y + 2, x + width - 2, y + HEADER_HEIGHT, HEADER_FILL);
        g.fill(x + 2, y + HEADER_HEIGHT, x + width - 2, y + HEADER_HEIGHT + 1, GOLD);
        g.centeredText(font, title, x + width / 2, y + 5, GOLD);
    }

    /** Case creusée : sombre en haut à gauche, claire en bas à droite. */
    public static void inset(GuiGraphicsExtractor g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, SLOT_FILL);
        g.fill(x, y, x + width, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + height, SLOT_DARK);
        g.fill(x, y + height - 1, x + width, y + height, SLOT_LIGHT);
        g.fill(x + width - 1, y, x + width, y + height, SLOT_LIGHT);
    }

    /** Emplacement d'objet 18x18 avec son icône (16x16) au centre ; {@code frame} 0 = sans cadre. */
    public static void itemSlot(GuiGraphicsExtractor g, ItemStack stack, int x, int y, int frame, boolean hovered) {
        inset(g, x, y, 18, 18);
        if (hovered) {
            g.fill(x + 1, y + 1, x + 17, y + 17, HOVER_FILL);
        }
        if (!stack.isEmpty()) {
            g.item(stack, x + 1, y + 1);
        }
        if (frame != 0) {
            g.outline(x, y, 18, 18, frame);
        }
    }

    /** Jauge horizontale creusée, remplie de {@code fraction} (0 à 1). */
    public static void bar(GuiGraphicsExtractor g, int x, int y, int width, int height, double fraction, int color) {
        inset(g, x, y, width, height);
        int filled = (int) Math.round((width - 2) * Math.clamp(fraction, 0.0D, 1.0D));
        if (filled > 0) {
            g.fill(x + 1, y + 1, x + 1 + filled, y + height - 1, color);
            // Reflet d'un pixel en haut de la partie remplie.
            g.fill(x + 1, y + 1, x + 1 + filled, y + 2, 0x40FFFFFF);
        }
    }

    /**
     * Rangée de {@code max} petits carrés dont les {@code filled} premiers sont allumés : niveau
     * d'une tour, points d'une caractéristique. Renvoie la largeur occupée.
     */
    public static int pips(GuiGraphicsExtractor g, int x, int y, int filled, int max, int size, int color) {
        int step = size + 2;
        for (int i = 0; i < max; i++) {
            int px = x + i * step;
            inset(g, px, y, size, size);
            if (i < filled) {
                g.fill(px + 1, y + 1, px + size - 1, y + size - 1, color);
            }
        }
        return max * step - 2;
    }

    /** Filet horizontal discret, pour séparer deux parties d'un panneau. */
    public static void divider(GuiGraphicsExtractor g, int x, int y, int width) {
        g.fill(x, y, x + width, y + 1, PANEL_DARK);
        g.fill(x, y + 1, x + width, y + 2, PANEL_LIGHT & 0x80FFFFFF);
    }

    /** Touche dessinée comme une petite pastille ([E]) ; renvoie la largeur occupée. */
    public static int keyCap(GuiGraphicsExtractor g, Font font, Component key, int x, int y) {
        int width = font.width(key) + 6;
        g.fill(x, y - 2, x + width, y + font.lineHeight, SLOT_LIGHT);
        g.fill(x + 1, y - 1, x + width - 1, y + font.lineHeight - 1, SLOT_FILL);
        g.text(font, key, x + 3, y, GOLD, false);
        return width;
    }

    /** Couleur de jauge de vie : verte, puis rouge sous le tiers. */
    public static int healthColor(double fraction) {
        return fraction < 1.0D / 3.0D ? HEALTH_LOW : HEALTH;
    }

    public static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
