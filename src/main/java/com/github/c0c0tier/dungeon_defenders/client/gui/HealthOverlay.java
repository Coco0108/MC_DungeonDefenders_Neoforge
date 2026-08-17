package com.github.c0c0tier.dungeon_defenders.client.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Overlay volontairement minimal (losange + texte, via DiamondGauge), même principe que
// ManaOverlay : un premier pas vers la forme du HUD de référence, en couleurs plates — les
// vraies textures viendront dans un second temps, voir 05-etat-et-problemes-connus.md.
// Contrairement au mana, pas d'attachment ici : la vie est déjà un état vanilla synchronisé
// (getHealth/getMaxHealth), le maximum à 100 est fixé dans ModEvents (attribut MAX_HEALTH du
// joueur).
//
// Losange de droite du groupe bas-gauche (mana | vie), juste à droite de ManaOverlay, même
// hauteur, au-dessus de la barre d'expérience. Voir HudLayout pour les constantes partagées.
public class HealthOverlay implements GuiLayer {
    private static final int FILLED_COLOR = 0xFFEF4444;
    private static final int EMPTY_COLOR = 0xFF2B2B2B;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }

        int currentHealth = Math.round(player.getHealth());
        int maxHealth = Math.round(player.getMaxHealth());

        int centerX = HudLayout.MARGIN + HudLayout.DIAMOND_RADIUS * 3 + HudLayout.DIAMOND_GAP;
        int bottomY = ExperienceOverlay.barTop(guiGraphics) - HudLayout.ROW_GAP;
        double fillRatio = maxHealth <= 0 ? 0 : (double) currentHealth / maxHealth;

        DiamondGauge.render(guiGraphics, centerX, bottomY, HudLayout.DIAMOND_RADIUS, fillRatio, FILLED_COLOR, EMPTY_COLOR);

        Component text = Component.translatable("dungeon_defenders.hud.health", currentHealth, maxHealth);
        int textY = bottomY - HudLayout.DIAMOND_RADIUS * 2 - minecraft.font.lineHeight - 2;
        guiGraphics.centeredText(minecraft.font, text, centerX, textY, TEXT_COLOR);
    }
}
