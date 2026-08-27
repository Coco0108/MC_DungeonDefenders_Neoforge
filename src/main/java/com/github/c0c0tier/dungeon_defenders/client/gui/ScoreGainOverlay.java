package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.gui.GuiLayer;

import java.util.ArrayList;
import java.util.List;

// Petit "+X" flottant en bas à droite de l'écran à chaque gain de score (aujourd'hui : chaque
// kill, voir ModEvents.awardExperienceAndScore ; plus tard fin de vague/de map/multiplicateurs,
// voir doc/02-gameplay.md). Détecte le gain en comparant ModAttachments.SCORE d'une frame à
// l'autre plutôt que d'ajouter un paquet réseau dédié : le score arrive déjà par la sync
// d'attachment existante (level.syncData), pas besoin d'un mécanisme séparé.
//
// Limite assumée : si le serveur incrémente SCORE plusieurs fois dans le même tick (plusieurs
// morts simultanées), la synchronisation d'attachment ne garantit pas un paquet par
// incrément — le client peut alors observer un seul saut combiné plutôt que deux popups
// distincts. Sans effet dans le cas courant (kills espacés dans le temps).
public class ScoreGainOverlay implements GuiLayer {
    private static final long DURATION_MS = 1500L;
    private static final float RISE_PIXELS = 20.0F;
    private static final int MARGIN = 4;
    private static final int RGB = 0x22C55E; // même vert que ExperienceOverlay

    private final List<Popup> popups = new ArrayList<>();
    private boolean initialized = false;
    private int lastKnownScore = 0;

    private record Popup(int amount, long spawnTimeMs) {
    }

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || minecraft.options.hideGui) {
            return;
        }

        trackScoreChange(level);
        purgeExpired();
        drawPopups(guiGraphics, minecraft);
    }

    private void trackScoreChange(Level level) {
        int score = level.getData(ModAttachments.SCORE);
        if (!this.initialized) {
            // Premier tick observé (connexion/rejoin en cours de partie) : initialise sans
            // popup, pour ne pas afficher d'un coup tout le score déjà accumulé.
            this.initialized = true;
            this.lastKnownScore = score;
            return;
        }
        if (score > this.lastKnownScore) {
            this.popups.add(new Popup(score - this.lastKnownScore, Util.getMillis()));
        }
        // score < lastKnownScore : remise à zéro en début de partie
        // (PhaseTransitions.resetGameState), pas un gain — resynchronisé sans popup.
        this.lastKnownScore = score;
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

            Component text = Component.translatable("dungeon_defenders.hud.score_gain", popup.amount());
            int width = minecraft.font.width(text);
            guiGraphics.text(minecraft.font, text, rightX - width, y, color);
        }
    }
}
