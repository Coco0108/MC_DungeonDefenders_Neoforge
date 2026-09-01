package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Overlay minimal : juste du texte (pas de jauge, contrairement au mana/vie/expérience), en
// haut à droite. Le déroulement des vagues (déclenchement, victoire/défaite) n'existe pas
// encore, voir 05-etat-et-problemes-connus.md. C'est un état du monde (Level), pas du
// joueur : lu depuis Minecraft.level, pas Minecraft.player.
public class WaveOverlay implements GuiLayer {
    private static final int MARGIN = 4;
    // PhaseOverlay se place dans la rangée suivante via ROW_Y + ROW_HEIGHT.
    static final int ROW_Y = MARGIN;
    static final int ROW_HEIGHT = 14;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || minecraft.options.hideGui) {
            return;
        }

        // Rien à afficher à la Taverne : il n'y a pas de partie en cours, donc pas de vague.
        if (!GamePhase.of(level).isInGame()) {
            return;
        }

        int currentWave = level.getData(ModAttachments.CURRENT_WAVE);
        int maxWave = ModAttachments.MAX_WAVE;

        Component text = Component.translatable("dungeon_defenders.hud.wave", currentWave, maxWave);
        int x = guiGraphics.guiWidth() - MARGIN - minecraft.font.width(text);
        guiGraphics.text(minecraft.font, text, x, ROW_Y, TEXT_COLOR);
    }
}
