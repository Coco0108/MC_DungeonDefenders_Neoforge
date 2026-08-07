package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Overlay volontairement minimal (texte + jauge unie) : à remplacer par une vraie
// texture quand le reste du HUD sera dessiné, voir 05-etat-et-problemes-connus.md.
public class ManaOverlay implements GuiLayer {
    private static final int MARGIN = 4;
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 6;
    private static final int FILLED_COLOR = 0xFF3B82F6;
    private static final int EMPTY_COLOR = 0xFF2B2B2B;
    private static final int TEXT_COLOR = 0xFFFFFF;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }

        int currentMana = player.getData(ModAttachments.MANA);
        int maxMana = ModAttachments.MAX_MANA;

        int barY = MARGIN;
        int filledWidth = maxMana <= 0 ? 0 : (int) ((long) BAR_WIDTH * currentMana / maxMana);

        guiGraphics.fill(MARGIN, barY, MARGIN + BAR_WIDTH, barY + BAR_HEIGHT, EMPTY_COLOR);
        if (filledWidth > 0) {
            guiGraphics.fill(MARGIN, barY, MARGIN + filledWidth, barY + BAR_HEIGHT, FILLED_COLOR);
        }

        // Le texte se place à côté de la jauge (et non dessus), pour lire la valeur exacte.
        int textX = MARGIN + BAR_WIDTH + 6;
        int textY = barY + BAR_HEIGHT / 2 - minecraft.font.lineHeight / 2;
        guiGraphics.text(minecraft.font,
                Component.translatable("dungeon_defenders.hud.mana", currentMana, maxMana),
                textX, textY, TEXT_COLOR);
    }
}
