package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Overlay volontairement minimal (jauge + texte), même principe que ManaOverlay /
// HealthOverlay : à remplacer par une vraie texture quand le reste du HUD sera dessiné, voir
// 05-etat-et-problemes-connus.md. C'est une expérience custom, pas l'XP vanilla : rien ne la
// fait encore varier (voir ModAttachments.EXPERIENCE), elle démarre donc vide.
//
// Barre horizontale ancrée en bas à gauche de l'écran, sous les colonnes mana/vie
// (ManaOverlay/HealthOverlay s'appuient sur barTop(...) ci-dessous pour se placer juste
// au-dessus).
public class ExperienceOverlay implements GuiLayer {
    private static final int BAR_WIDTH = 100;
    private static final int FILLED_COLOR = 0xFF22C55E;
    private static final int EMPTY_COLOR = 0xFF2B2B2B;
    private static final int TEXT_COLOR = 0xFFFFFF;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }

        int currentExperience = player.getData(ModAttachments.EXPERIENCE);
        int maxExperience = ModAttachments.MAX_EXPERIENCE;

        int barY = barTop(guiGraphics);
        int filledWidth = maxExperience <= 0 ? 0 : (int) ((long) BAR_WIDTH * currentExperience / maxExperience);

        guiGraphics.fill(HudLayout.MARGIN, barY, HudLayout.MARGIN + BAR_WIDTH, barY + HudLayout.EXPERIENCE_BAR_HEIGHT, EMPTY_COLOR);
        if (filledWidth > 0) {
            guiGraphics.fill(HudLayout.MARGIN, barY, HudLayout.MARGIN + filledWidth, barY + HudLayout.EXPERIENCE_BAR_HEIGHT, FILLED_COLOR);
        }

        int textX = HudLayout.MARGIN + BAR_WIDTH + 6;
        int textY = barY + HudLayout.EXPERIENCE_BAR_HEIGHT / 2 - minecraft.font.lineHeight / 2;
        guiGraphics.text(minecraft.font,
                Component.translatable("dungeon_defenders.hud.experience", currentExperience, maxExperience),
                textX, textY, TEXT_COLOR);
    }

    // Position Y du haut de cette barre, ancrée en bas de l'écran.
    static int barTop(GuiGraphicsExtractor guiGraphics) {
        return guiGraphics.guiHeight() - HudLayout.MARGIN - HudLayout.EXPERIENCE_BAR_HEIGHT;
    }
}
