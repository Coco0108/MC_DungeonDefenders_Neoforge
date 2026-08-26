package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Overlay minimal : juste du texte, centré tout en bas de l'écran (à droite de la barre
// d'expérience, qui elle est en bas à gauche). "Score" = ModAttachments.SCORE, censé
// correspondre à l'expérience gagnée sur cette carte, mais rien ne l'alimente encore, voir
// 05-etat-et-problemes-connus.md. État de la Level (comme current_wave), pas du joueur.
public class ScoreOverlay implements GuiLayer {
    private static final int MARGIN = 4;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || minecraft.options.hideGui) {
            return;
        }

        int score = level.getData(ModAttachments.SCORE);
        Component text = Component.translatable("dungeon_defenders.hud.score", score);
        int centerX = guiGraphics.guiWidth() / 2;
        guiGraphics.centeredText(minecraft.font, text, centerX, rowY(guiGraphics), TEXT_COLOR);
    }

    // Position Y de cette ligne, ancrée en bas de l'écran. CharacterOverlay s'appuie dessus
    // pour se placer juste au-dessus.
    static int rowY(GuiGraphicsExtractor guiGraphics) {
        return guiGraphics.guiHeight() - MARGIN - Minecraft.getInstance().font.lineHeight;
    }
}
