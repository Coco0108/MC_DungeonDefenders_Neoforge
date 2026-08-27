package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.ScoreSource;
import com.github.c0c0tier.dungeon_defenders.init.SpawnableEnemy;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.GuiLayer;

import java.util.ArrayList;
import java.util.List;

// Petit "<œuf> +X <source>" flottant en bas à droite de l'écran à chaque gain de score.
// Alimenté par ScoreGainPayload (network/), pas par une lecture de ModAttachments.SCORE : le
// total seul ne dit pas d'où vient le gain (kill ? fin de vague ? multiplicateur ?), voir ce
// paquet et ModEvents.grantScore pour le pourquoi de ce canal séparé.
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

    // Taille fixe d'une icône d'item dessinée par GuiGraphicsExtractor#item (même taille que
    // dans la hotbar vanilla) — pas de mise à l'échelle, on se contente de la place qu'il faut.
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 2;

    private final List<Popup> popups = new ArrayList<>();

    // enemy peut être null : toute source de score sans ennemi précis (fin de vague/de map, pas
    // encore implémentées, voir doc/02-gameplay.md) n'a simplement pas d'icône à afficher.
    private record Popup(int amount, ScoreSource source, SpawnableEnemy enemy, long spawnTimeMs) {
    }

    private ScoreGainOverlay() {
    }

    /**
     * Appelé par le handler client de ScoreGainPayload à chaque gain de score reçu du serveur.
     *
     * @param enemy null si ce gain n'a pas d'ennemi associé (pas d'icône affichée pour ce popup)
     */
    public void addPopup(int amount, ScoreSource source, SpawnableEnemy enemy) {
        this.popups.add(new Popup(amount, source, enemy, Util.getMillis()));
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
            int textWidth = minecraft.font.width(text);
            int textX = rightX - textWidth;
            guiGraphics.text(minecraft.font, text, textX, y, color);

            // L'icône ne suit pas le fondu du texte (GuiGraphicsExtractor#item n'a pas de
            // paramètre de teinte/alpha) : elle reste pleinement opaque tant que le popup est
            // affiché, puis disparaît d'un coup avec lui — simplification assumée plutôt qu'une
            // vraie transition, voir doc/02-gameplay.md.
            if (popup.enemy() != null) {
                int iconX = textX - ICON_GAP - ICON_SIZE;
                int iconY = y - (ICON_SIZE - minecraft.font.lineHeight) / 2;
                guiGraphics.item(new ItemStack(popup.enemy().spawnEggItem()), iconX, iconY);
            }
        }
    }
}
