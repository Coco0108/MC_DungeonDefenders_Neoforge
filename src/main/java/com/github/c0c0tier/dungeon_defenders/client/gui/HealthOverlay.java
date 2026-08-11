package com.github.c0c0tier.dungeon_defenders.client.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Overlay volontairement minimal (jauge + texte), même principe que ManaOverlay : à
// remplacer par une vraie texture quand le reste du HUD sera dessiné, voir
// 05-etat-et-problemes-connus.md. Contrairement au mana, pas d'attachment ici : la vie est
// déjà un état vanilla synchronisé (getHealth/getMaxHealth), le maximum à 100 est fixé
// dans ModEvents (attribut MAX_HEALTH du joueur).
public class HealthOverlay implements GuiLayer {
    private static final int MARGIN = 4;
    private static final int ROW_Y = ManaOverlay.ROW_Y + ManaOverlay.ROW_HEIGHT;
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 6;
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

        int filledWidth = maxHealth <= 0 ? 0 : (int) ((long) BAR_WIDTH * currentHealth / maxHealth);

        guiGraphics.fill(MARGIN, ROW_Y, MARGIN + BAR_WIDTH, ROW_Y + BAR_HEIGHT, EMPTY_COLOR);
        if (filledWidth > 0) {
            guiGraphics.fill(MARGIN, ROW_Y, MARGIN + filledWidth, ROW_Y + BAR_HEIGHT, FILLED_COLOR);
        }

        int textX = MARGIN + BAR_WIDTH + 6;
        int textY = ROW_Y + BAR_HEIGHT / 2 - minecraft.font.lineHeight / 2;
        guiGraphics.text(minecraft.font,
                Component.translatable("dungeon_defenders.hud.health", currentHealth, maxHealth),
                textX, textY, TEXT_COLOR);
    }
}
