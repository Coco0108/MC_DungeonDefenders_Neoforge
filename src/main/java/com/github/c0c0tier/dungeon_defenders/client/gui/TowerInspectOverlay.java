package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.client.TowerInspectState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Nom et PV de la tour regardée (voir TowerInspectClientEvents), sous le viseur. Les PV sont lus
// sur la copie cliente du block entity, déjà synchronisée pour la barre de vie.
public class TowerInspectOverlay implements GuiLayer {

    private static final int OFFSET_BELOW_CROSSHAIR = 14;
    private static final int LINE_HEIGHT = 10;
    private static final int NAME_COLOR = 0xFFFFFFFF;
    private static final int HEALTH_COLOR = 0xFFFFE066;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        BlockPos pos = TowerInspectState.target();
        if (level == null || pos == null || minecraft.options.hideGui
                || !(level.getBlockEntity(pos) instanceof AbstractTowerBlockEntity tower)) {
            return;
        }

        int centerX = guiGraphics.guiWidth() / 2;
        int y = guiGraphics.guiHeight() / 2 + OFFSET_BELOW_CROSSHAIR;
        guiGraphics.centeredText(minecraft.font, tower.definition().displayName(), centerX, y, NAME_COLOR);
        guiGraphics.centeredText(minecraft.font,
                Component.translatable("dungeon_defenders.tower.inspect_health", tower.getHealth(), tower.getMaxHealth()),
                centerX, y + LINE_HEIGHT, HEALTH_COLOR);
    }
}
