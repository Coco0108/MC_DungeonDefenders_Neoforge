package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Overlay minimal (jauge + texte), même principe que ManaOverlay/HealthOverlay/
// ExperienceOverlay, mais en miroir : le groupe (texte puis jauge) est aligné sur le bord
// droit de l'écran, sous WaveOverlay. "Tués/Total" : la jauge se remplit au fil des
// ennemis abattus. État de la Level (comme current_wave), voir ModAttachments et
// 05-etat-et-problemes-connus.md — rien ne fait encore varier ces valeurs.
public class WaveEnemiesOverlay implements GuiLayer {
    private static final int MARGIN = 4;
    private static final int ROW_Y = WaveOverlay.ROW_Y + WaveOverlay.ROW_HEIGHT;
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 6;
    private static final int GAP = 6;
    private static final int FILLED_COLOR = 0xFFF97316;
    private static final int EMPTY_COLOR = 0xFF2B2B2B;
    private static final int TEXT_COLOR = 0xFFFFFF;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || minecraft.options.hideGui) {
            return;
        }

        int killed = level.getData(ModAttachments.WAVE_ENEMIES_KILLED);
        int total = level.getData(ModAttachments.WAVE_ENEMIES_TOTAL);

        int barRight = guiGraphics.guiWidth() - MARGIN;
        int barLeft = barRight - BAR_WIDTH;
        int filledWidth = total <= 0 ? 0 : (int) ((long) BAR_WIDTH * killed / total);

        guiGraphics.fill(barLeft, ROW_Y, barRight, ROW_Y + BAR_HEIGHT, EMPTY_COLOR);
        if (filledWidth > 0) {
            guiGraphics.fill(barLeft, ROW_Y, barLeft + filledWidth, ROW_Y + BAR_HEIGHT, FILLED_COLOR);
        }

        Component text = Component.translatable("dungeon_defenders.hud.wave_enemies", killed, total);
        int textY = ROW_Y + BAR_HEIGHT / 2 - minecraft.font.lineHeight / 2;
        int textX = barLeft - GAP - minecraft.font.width(text);
        guiGraphics.text(minecraft.font, text, textX, textY, TEXT_COLOR);
    }
}
