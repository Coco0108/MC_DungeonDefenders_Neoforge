package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Overlay minimal : juste du texte (pas de jauge, contrairement au mana/vie/expérience), en
// haut à droite. Le déroulement des vagues (déclenchement, victoire/défaite) n'existe pas
// encore, voir 05-etat-et-problemes-connus.md. C'est un état du monde (Level), pas du
// joueur : lu depuis Minecraft.level, pas Minecraft.player.
public class WaveOverlay implements GuiLayer {
    private static final int MARGIN = 4;
    // Rangée partagée avec WaveEnemiesOverlay, qui se place juste à gauche de ce texte (via
    // waveText(...) ci-dessous) plutôt qu'en dessous, pour rester sur la même ligne.
    // PhaseOverlay, lui, se place dans la rangée suivante via ROW_Y + ROW_HEIGHT.
    static final int ROW_Y = MARGIN;
    static final int ROW_HEIGHT = 14;
    private static final int TEXT_COLOR = 0xFFFFFF;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || minecraft.options.hideGui) {
            return;
        }

        Component text = waveText(level);
        int x = guiGraphics.guiWidth() - MARGIN - minecraft.font.width(text);
        guiGraphics.text(minecraft.font, text, x, ROW_Y, TEXT_COLOR);
    }

    static Component waveText(Level level) {
        int currentWave = level.getData(ModAttachments.CURRENT_WAVE);
        int maxWave = ModAttachments.MAX_WAVE;
        return Component.translatable("dungeon_defenders.hud.wave", currentWave, maxWave);
    }
}
