package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.client.ClientDisplayConfig;
import com.github.c0c0tier.dungeon_defenders.init.BalanceValue;
import com.github.c0c0tier.dungeon_defenders.init.GameBalance;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;

import java.util.UUID;

// Les deux retours visuels de la progression du personnage :
//  - un "+X XP" qui monte et s'efface à droite de la barre d'expérience à chaque gain ;
//  - une bannière "Niveau X !" au centre-haut de l'écran à chaque passage de niveau.
//
// Aucun paquet dédié : EXPERIENCE et LEVEL sont déjà synchronisés par attachment, il suffit de
// comparer leur valeur d'une image à l'autre. Le gain se reconstitue sans ambiguïté parce que
// l'XP ne fait que monter (passages de niveau × XP par niveau + différence d'XP) ; une valeur
// qui baisse (nouveau personnage, remise à zéro) n'affiche rien et sert juste de nouveau repère.
public class ProgressionFeedbackOverlay implements GuiLayer {

    private static final long POPUP_DURATION_MS = 1200L;
    // Deux gains reçus à moins de ce délai (plusieurs monstres tués par la même salve) sont
    // cumulés dans le même popup plutôt qu'empilés illisiblement au même endroit.
    private static final long POPUP_MERGE_WINDOW_MS = 400L;
    private static final float POPUP_RISE_PIXELS = 10.0F;
    private static final int POPUP_GAP = 6;
    private static final int POPUP_RGB = 0x22C55E; // même vert que la barre d'expérience

    private static final long BANNER_DURATION_MS = 2500L;
    private static final long BANNER_FADE_MS = 600L;
    private static final float BANNER_SCALE = 2.0F;
    private static final int BANNER_RGB = 0xFFD24A; // même jaune que le compte à rebours du combat

    // Le joueur suivi : un changement de joueur (reconnexion, autre monde) remet le repère à
    // zéro sans rien afficher, sinon rejoindre une partie au niveau 5 déclencherait la bannière.
    private UUID trackedPlayer;
    private int lastExperience;
    private int lastLevel;

    private int popupAmount;
    private long popupSpawnMs = Long.MIN_VALUE / 2;
    private int bannerLevel;
    private long bannerSpawnMs = Long.MIN_VALUE / 2;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            this.trackedPlayer = null;
            return;
        }

        // Le suivi tourne même interface masquée ou options décochées, pour ne pas rejouer d'un
        // coup tous les gains accumulés au moment où l'affichage revient.
        track(player);
        if (minecraft.options.hideGui) {
            return;
        }

        long now = Util.getMillis();
        if (ClientDisplayConfig.SHOW_EXPERIENCE_GAIN_POPUP.get()) {
            drawPopup(guiGraphics, minecraft, player, now);
        }
        if (ClientDisplayConfig.SHOW_LEVEL_UP_BANNER.get()) {
            drawBanner(guiGraphics, minecraft, now);
        }
    }

    private void track(Player player) {
        int experience = player.getData(ModAttachments.EXPERIENCE);
        int level = player.getData(ModAttachments.LEVEL);

        if (!player.getUUID().equals(this.trackedPlayer)) {
            this.trackedPlayer = player.getUUID();
            this.lastExperience = experience;
            this.lastLevel = level;
            return;
        }
        if (experience == this.lastExperience && level == this.lastLevel) {
            return;
        }

        long now = Util.getMillis();
        int xpPerLevel = Math.max(1, GameBalance.getInt(BalanceValue.XP_PER_LEVEL, true));
        int gained = (level - this.lastLevel) * xpPerLevel + (experience - this.lastExperience);
        if (gained > 0) {
            this.popupAmount = now - this.popupSpawnMs < POPUP_MERGE_WINDOW_MS ? this.popupAmount + gained : gained;
            this.popupSpawnMs = now;
        }
        if (level > this.lastLevel) {
            this.bannerLevel = level;
            this.bannerSpawnMs = now;
        }

        this.lastExperience = experience;
        this.lastLevel = level;
    }

    private void drawPopup(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, Player player, long now) {
        long age = now - this.popupSpawnMs;
        if (age < 0 || age >= POPUP_DURATION_MS) {
            return;
        }
        float progress = age / (float) POPUP_DURATION_MS;
        int alpha = (int) (255 * (1.0F - progress));
        // On s'arrête avant l'alpha nul : selon les versions, le moteur de texte a traité un
        // alpha quasi nul comme "non précisé" et dessiné le texte opaque. Pas revérifié ici,
        // quelques images presque invisibles en moins ne coûtent rien.
        if (alpha < 8) {
            return;
        }

        // À droite du libellé "Expérience : x/y" de la barre, sur la même ligne, pour ne
        // recouvrir ni les losanges ni les emplacements de compétences placés au-dessus.
        int x = ExperienceOverlay.labelRight(minecraft, player) + POPUP_GAP;
        int y = ExperienceOverlay.labelTop(guiGraphics, minecraft) - (int) (progress * POPUP_RISE_PIXELS);
        guiGraphics.text(minecraft.font,
                Component.translatable("dungeon_defenders.hud.experience_gain", this.popupAmount),
                x, y, (alpha << 24) | POPUP_RGB);
    }

    private void drawBanner(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, long now) {
        long age = now - this.bannerSpawnMs;
        if (age < 0 || age >= BANNER_DURATION_MS) {
            return;
        }
        // Pleine opacité, puis fondu sur la fin seulement.
        float fade = Mth.clamp((BANNER_DURATION_MS - age) / (float) BANNER_FADE_MS, 0.0F, 1.0F);
        int alpha = (int) (255 * fade);
        if (alpha < 8) {
            return;
        }

        Component text = Component.translatable("dungeon_defenders.hud.level_up", this.bannerLevel);
        float centerX = guiGraphics.guiWidth() / 2.0F;
        float y = guiGraphics.guiHeight() / 4.0F;

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(centerX, y);
        guiGraphics.pose().scale(BANNER_SCALE, BANNER_SCALE);
        guiGraphics.centeredText(minecraft.font, text, 0, 0, (alpha << 24) | BANNER_RGB);
        guiGraphics.pose().popMatrix();
    }
}
