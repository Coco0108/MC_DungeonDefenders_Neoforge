package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Overlay volontairement minimal (losange + texte, via DiamondGauge) : un premier pas vers
// la forme du HUD de référence (cristaux de mana en losange/triangle), en couleurs plates —
// les vraies textures viendront dans un second temps, voir 05-etat-et-problemes-connus.md.
//
// Losange de gauche du groupe bas-gauche (mana | vie), au-dessus de la barre d'expérience.
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

        int centerX = HudLayout.MARGIN + HudLayout.DIAMOND_RADIUS;
        int bottomY = ExperienceOverlay.barTop(guiGraphics) - HudLayout.ROW_GAP;
        double fillRatio = maxMana <= 0 ? 0 : (double) currentMana / maxMana;

        DiamondGauge.render(guiGraphics, centerX, bottomY, HudLayout.DIAMOND_RADIUS, fillRatio, FILLED_COLOR, EMPTY_COLOR);

        // Le texte se place au-dessus du losange (et non à côté), pour lire la valeur
        // exacte sans empiéter sur le losange voisin (vie).
        Component text = Component.translatable("dungeon_defenders.hud.mana", currentMana, maxMana);
        int textY = bottomY - HudLayout.DIAMOND_RADIUS * 2 - minecraft.font.lineHeight - 2;
        guiGraphics.centeredText(minecraft.font, text, centerX, textY, TEXT_COLOR);
    }
}
