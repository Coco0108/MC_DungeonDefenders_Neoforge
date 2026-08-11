package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Overlay volontairement minimal (colonne verticale + texte) : à remplacer par une vraie
// texture quand le reste du HUD sera dessiné, voir 05-etat-et-problemes-connus.md.
//
// Colonne de gauche du groupe bas-gauche (mana | vie), au-dessus de la barre d'expérience.
// Se remplit du bas vers le haut. Voir HudLayout pour les constantes partagées avec
// HealthOverlay/ExperienceOverlay.
public class ManaOverlay implements GuiLayer {
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

        int columnLeft = HudLayout.MARGIN;
        int columnBottom = ExperienceOverlay.barTop(guiGraphics) - HudLayout.ROW_GAP;
        int columnTop = columnBottom - HudLayout.COLUMN_HEIGHT;
        int filledHeight = maxMana <= 0 ? 0 : (int) ((long) HudLayout.COLUMN_HEIGHT * currentMana / maxMana);
        int filledTop = columnBottom - filledHeight;

        guiGraphics.fill(columnLeft, columnTop, columnLeft + HudLayout.COLUMN_WIDTH, columnBottom, EMPTY_COLOR);
        if (filledHeight > 0) {
            guiGraphics.fill(columnLeft, filledTop, columnLeft + HudLayout.COLUMN_WIDTH, columnBottom, FILLED_COLOR);
        }

        // Le texte se place au-dessus de la colonne (et non à côté), pour lire la valeur
        // exacte sans empiéter sur la colonne voisine (vie).
        Component text = Component.translatable("dungeon_defenders.hud.mana", currentMana, maxMana);
        int centerX = columnLeft + HudLayout.COLUMN_WIDTH / 2;
        int textY = columnTop - minecraft.font.lineHeight - 2;
        guiGraphics.centeredText(minecraft.font, text, centerX, textY, TEXT_COLOR);
    }
}
