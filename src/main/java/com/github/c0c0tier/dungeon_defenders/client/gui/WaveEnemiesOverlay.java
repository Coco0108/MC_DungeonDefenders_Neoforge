package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Overlay minimal (jauge + texte), même principe que ManaOverlay/HealthOverlay/
// ExperienceOverlay, mais volontairement plus imposant et centré tout en haut de l'écran —
// c'est l'info la plus visible du jeu original (une grande barre en haut-centre), donc elle
// mérite plus de place que le reste du HUD. Le texte est superposé au centre de la jauge,
// pas à côté, pour rester compact malgré la largeur. "Tués/Total" : la jauge se remplit au
// fil des ennemis abattus. État de la Level (comme current_wave), voir ModAttachments et
// 05-etat-et-problemes-connus.md — rien ne fait encore varier ces valeurs.
public class WaveEnemiesOverlay implements GuiLayer {
    private static final int MARGIN = 4;
    private static final int BAR_WIDTH = 240;
    private static final int BAR_HEIGHT = 10;
    private static final int FILLED_COLOR = 0xFFF97316;
    private static final int EMPTY_COLOR = 0xFF2B2B2B;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || minecraft.options.hideGui) {
            return;
        }

        // Rien à afficher à la Taverne : il n'y a pas de partie en cours, donc pas de vague.
        if (!GamePhase.of(level).isInGame()) {
            return;
        }

        int killed = level.getData(ModAttachments.WAVE_ENEMIES_KILLED);
        int total = level.getData(ModAttachments.WAVE_ENEMIES_TOTAL);

        int centerX = guiGraphics.guiWidth() / 2;
        int barLeft = centerX - BAR_WIDTH / 2;
        int barRight = barLeft + BAR_WIDTH;
        int filledWidth = total <= 0 ? 0 : (int) ((long) BAR_WIDTH * killed / total);

        guiGraphics.fill(barLeft, MARGIN, barRight, MARGIN + BAR_HEIGHT, EMPTY_COLOR);
        if (filledWidth > 0) {
            guiGraphics.fill(barLeft, MARGIN, barLeft + filledWidth, MARGIN + BAR_HEIGHT, FILLED_COLOR);
        }

        Component text = Component.translatable("dungeon_defenders.hud.wave_enemies", killed, total);
        int textY = MARGIN + BAR_HEIGHT / 2 - minecraft.font.lineHeight / 2;
        guiGraphics.centeredText(minecraft.font, text, centerX, textY, TEXT_COLOR);
    }
}
