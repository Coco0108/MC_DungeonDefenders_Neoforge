package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Overlay minimal : juste du texte "Nom - niv X" (clé dungeon_defenders.hud.character),
// centré juste au-dessus de ScoreOverlay. "Nom" est pour l'instant le pseudo Minecraft du
// joueur (player.getName()) : aucun système de nom de personnage custom n'existe. "niv X" =
// ModAttachments.LEVEL, qui démarre à 1 et que rien ne fait encore monter, voir
// 05-etat-et-problemes-connus.md.
public class CharacterOverlay implements GuiLayer {
    private static final int GAP = 2;
    private static final int TEXT_COLOR = 0xFFFFFF;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }

        int level = player.getData(ModAttachments.LEVEL);
        Component text = Component.translatable("dungeon_defenders.hud.character", player.getName(), level);
        int centerX = guiGraphics.guiWidth() / 2;
        int y = ScoreOverlay.rowY(guiGraphics) - minecraft.font.lineHeight - GAP;
        guiGraphics.centeredText(minecraft.font, text, centerX, y, TEXT_COLOR);
    }
}
