package com.github.c0c0tier.dungeon_defenders.client.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Overlay volontairement minimal (colonne verticale + texte), même principe que
// ManaOverlay : à remplacer par une vraie texture quand le reste du HUD sera dessiné, voir
// 05-etat-et-problemes-connus.md. Contrairement au mana, pas d'attachment ici : la vie est
// déjà un état vanilla synchronisé (getHealth/getMaxHealth), le maximum à 100 est fixé
// dans ModEvents (attribut MAX_HEALTH du joueur).
//
// Colonne de droite du groupe bas-gauche (mana | vie), juste à droite de ManaOverlay, même
// hauteur, au-dessus de la barre d'expérience. Voir HudLayout pour les constantes partagées.
public class HealthOverlay implements GuiLayer {
    private static final int FILLED_COLOR = 0xFFEF4444;
    private static final int EMPTY_COLOR = 0xFF2B2B2B;
    private static final int TEXT_COLOR = 0xFFFFFF;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }

        int currentHealth = Math.round(player.getHealth());
        int maxHealth = Math.round(player.getMaxHealth());

        int columnLeft = HudLayout.MARGIN + HudLayout.COLUMN_WIDTH + HudLayout.COLUMN_GAP;
        int columnBottom = ExperienceOverlay.barTop(guiGraphics) - HudLayout.ROW_GAP;
        int columnTop = columnBottom - HudLayout.COLUMN_HEIGHT;
        int filledHeight = maxHealth <= 0 ? 0 : (int) ((long) HudLayout.COLUMN_HEIGHT * currentHealth / maxHealth);
        int filledTop = columnBottom - filledHeight;

        guiGraphics.fill(columnLeft, columnTop, columnLeft + HudLayout.COLUMN_WIDTH, columnBottom, EMPTY_COLOR);
        if (filledHeight > 0) {
            guiGraphics.fill(columnLeft, filledTop, columnLeft + HudLayout.COLUMN_WIDTH, columnBottom, FILLED_COLOR);
        }

        Component text = Component.translatable("dungeon_defenders.hud.health", currentHealth, maxHealth);
        int centerX = columnLeft + HudLayout.COLUMN_WIDTH / 2;
        int textY = columnTop - minecraft.font.lineHeight - 2;
        guiGraphics.centeredText(minecraft.font, text, centerX, textY, TEXT_COLOR);
    }
}
