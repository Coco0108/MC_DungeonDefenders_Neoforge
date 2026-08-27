package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.ScoreSource;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.gui.GuiLayer;

import java.util.ArrayList;
import java.util.List;

// Petit "+X <source>" flottant en bas à droite de l'écran à chaque gain de score. Alimenté par
// ScoreGainPayload (network/), pas par une lecture de ModAttachments.SCORE : le total seul ne
// dit pas d'où vient le gain (kill ? fin de vague ? multiplicateur ?), voir ce paquet et
// ModEvents.grantScore pour le pourquoi de ce canal séparé.
//
// Instance unique exposée en statique plutôt qu'enregistrée par valeur : le handler client du
// paquet (DungeonDefendersModClient) doit pouvoir pousser un popup dans la même instance que
// celle enregistrée pour le rendu (RegisterGuiLayersEvent) — les deux se rejoignent ici.
public class ScoreGainOverlay implements GuiLayer {
    public static final ScoreGainOverlay INSTANCE = new ScoreGainOverlay();

    private static final long DURATION_MS = 1500L;
    private static final float RISE_PIXELS = 20.0F;
    private static final int MARGIN = 4;
    private static final int RGB = 0x22C55E; // même vert que ExperienceOverlay

    private final List<Popup> popups = new ArrayList<>();

    private record Popup(int amount, ScoreSource source, long spawnTimeMs) {
    }

    private ScoreGainOverlay() {
    }

    /** Appelé par le handler client de ScoreGainPayload à chaque gain de score reçu du serveur. */
    public void addPopup(int amount, ScoreSource source) {
        this.popups.add(new Popup(amount, source, Util.getMillis()));
    }

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.options.hideGui) {
            return;
        }

        purgeExpired();
        drawPopups(guiGraphics, minecraft);
    }

    private void purgeExpired() {
        long now = Util.getMillis();
        this.popups.removeIf(popup -> now - popup.spawnTimeMs() >= DURATION_MS);
    }

    private void drawPopups(GuiGraphicsExtractor guiGraphics, Minecraft minecraft) {
        long now = Util.getMillis();
        int rightX = guiGraphics.guiWidth() - MARGIN;
        int baseY = guiGraphics.guiHeight() - MARGIN - minecraft.font.lineHeight;

        for (Popup popup : this.popups) {
            float progress = Mth.clamp((now - popup.spawnTimeMs()) / (float) DURATION_MS, 0.0F, 1.0F);
            int alpha = (int) (255 * (1.0F - progress));
            if (alpha <= 0) {
                continue;
            }
            int y = baseY - (int) (progress * RISE_PIXELS);
            int color = (alpha << 24) | RGB;

            Component text = Component.translatable("dungeon_defenders.hud.score_gain",
                    popup.amount(), Component.translatable(popup.source().translationKey()));
            int width = minecraft.font.width(text);
            guiGraphics.text(minecraft.font, text, rightX - width, y, color);
        }
    }
}
