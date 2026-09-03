package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Overlay minimal : juste du texte "Nom - niv X" (clé dungeon_defenders.hud.character),
// centré juste au-dessus de ScoreOverlay. "Nom" = ModAttachments.CHARACTER_NAME, un nom de
// personnage distinct du pseudo Minecraft (même s'il en reprend la valeur par défaut, voir
// ModAttachments). "niv X" = ModAttachments.LEVEL, qui démarre à 1 et que rien ne fait
// encore monter, voir 05-etat-et-problemes-connus.md.
public class CharacterOverlay implements GuiLayer {
    private static final int GAP = 2;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }

        String characterName = player.getData(ModAttachments.CHARACTER_NAME);
        int level = player.getData(ModAttachments.LEVEL);
        // Le héros s'ajoute au nom et au niveau : c'est l'information qui dit au joueur quelles
        // tours il peut poser, elle a plus de valeur affichée en permanence qu'un pseudo.
        Component text = Component.translatable("dungeon_defenders.hud.character",
                Component.literal(characterName), HeroDefinition.of(player).displayName(), level);
        int centerX = guiGraphics.guiWidth() / 2;
        int y = ScoreOverlay.rowY(guiGraphics) - minecraft.font.lineHeight - GAP;
        guiGraphics.centeredText(minecraft.font, text, centerX, y, TEXT_COLOR);
    }
}
