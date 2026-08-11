package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Overlay minimal (jauge + texte), même principe que ManaOverlay/HealthOverlay/
// ExperienceOverlay, mais en miroir et sur la même rangée que WaveOverlay plutôt qu'en
// dessous : "Ennemis : X/Y" puis la jauge, juste à gauche du texte "Vague X/Y", pour que les
// deux infos se lisent ensemble d'un coup d'œil. "Tués/Total" : la jauge se remplit au fil
// des ennemis abattus. État de la Level (comme current_wave), voir ModAttachments et
// 05-etat-et-problemes-connus.md — rien ne fait encore varier ces valeurs.
public class WaveEnemiesOverlay implements GuiLayer {
    private static final int MARGIN = 4;
    private static final int BAR_WIDTH = 60;
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

        // Le texte "Vague X/Y" sert de point d'ancrage : cette jauge se place juste à sa
        // gauche, sur la même rangée (WaveOverlay.ROW_Y).
        int waveTextLeft = guiGraphics.guiWidth() - MARGIN - minecraft.font.width(WaveOverlay.waveText(level));

        int killed = level.getData(ModAttachments.WAVE_ENEMIES_KILLED);
        int total = level.getData(ModAttachments.WAVE_ENEMIES_TOTAL);

        int barRight = waveTextLeft - GAP;
        int barLeft = barRight - BAR_WIDTH;
        int barY = WaveOverlay.ROW_Y + (minecraft.font.lineHeight - BAR_HEIGHT) / 2;
        int filledWidth = total <= 0 ? 0 : (int) ((long) BAR_WIDTH * killed / total);

        guiGraphics.fill(barLeft, barY, barRight, barY + BAR_HEIGHT, EMPTY_COLOR);
        if (filledWidth > 0) {
            guiGraphics.fill(barLeft, barY, barLeft + filledWidth, barY + BAR_HEIGHT, FILLED_COLOR);
        }

        Component text = Component.translatable("dungeon_defenders.hud.wave_enemies", killed, total);
        int textX = barLeft - GAP - minecraft.font.width(text);
        guiGraphics.text(minecraft.font, text, textX, WaveOverlay.ROW_Y, TEXT_COLOR);
    }
}
