package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Overlay minimal : juste du texte (comme WaveOverlay), en haut à droite, juste en dessous
// de la rangée vague/ennemis. Aucune mécanique de phase n'existe encore (transition
// construction -> combat, etc.), voir 05-etat-et-problemes-connus.md.
public class PhaseOverlay implements GuiLayer {
    private static final int MARGIN = 4;
    private static final int ROW_Y = WaveOverlay.ROW_Y + WaveOverlay.ROW_HEIGHT;
    private static final int TEXT_COLOR = 0xFFFFFF;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || minecraft.options.hideGui) {
            return;
        }

        GamePhase phase = GamePhase.values()[level.getData(ModAttachments.GAME_PHASE)];
        Component text = Component.translatable("dungeon_defenders.hud.phase", Component.translatable(phase.translationKey()));
        int x = guiGraphics.guiWidth() - MARGIN - minecraft.font.width(text);
        guiGraphics.text(minecraft.font, text, x, ROW_Y, TEXT_COLOR);
    }
}
