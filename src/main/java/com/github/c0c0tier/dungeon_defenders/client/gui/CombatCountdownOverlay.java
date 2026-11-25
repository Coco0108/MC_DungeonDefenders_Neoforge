package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.gui.GuiLayer;

// "Combat dans 3..." au-dessus du viseur pendant le compte à rebours qui suit le vote "prêt"
// unanime (voir CombatCountdown). Le serveur synchronise le temps restant une fois par seconde.
public class CombatCountdownOverlay implements GuiLayer {

    private static final int TICKS_PER_SECOND = 20;
    private static final int OFFSET_ABOVE_CROSSHAIR = 40;
    private static final int TEXT_COLOR = 0xFFFFD24A;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || minecraft.options.hideGui) {
            return;
        }

        int remainingTicks = level.getData(ModAttachments.COMBAT_COUNTDOWN);
        if (remainingTicks <= 0) {
            return;
        }

        int seconds = (remainingTicks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
        guiGraphics.centeredText(minecraft.font,
                Component.translatable("dungeon_defenders.hud.combat_countdown", seconds),
                guiGraphics.guiWidth() / 2, guiGraphics.guiHeight() / 2 - OFFSET_ABOVE_CROSSHAIR, TEXT_COLOR);
    }
}
