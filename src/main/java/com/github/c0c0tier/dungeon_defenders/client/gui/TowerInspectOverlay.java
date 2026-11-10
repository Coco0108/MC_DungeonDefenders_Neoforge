package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.client.ModKeyMappings;
import com.github.c0c0tier.dungeon_defenders.client.TowerInspectState;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
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
    private static final int UPGRADE_COLOR = 0xFF7FD4FF;

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
        guiGraphics.centeredText(minecraft.font,
                Component.translatable("dungeon_defenders.tower.inspect_name",
                        tower.definition().displayName(), tower.getTowerLevel(), tower.getMaxTowerLevel()),
                centerX, y, NAME_COLOR);
        guiGraphics.centeredText(minecraft.font,
                Component.translatable("dungeon_defenders.tower.inspect_health", tower.getHealth(), tower.getMaxHealth()),
                centerX, y + LINE_HEIGHT, HEALTH_COLOR);

        // Rappel de la touche d'amélioration et de son prix, seulement quand c'est possible :
        // tour pas au maximum, et phase où l'on construit (gratuit à la Taverne).
        GamePhase phase = GamePhase.of(level);
        if (tower.canUpgrade() && phase.allowsTowerBuilding()) {
            int cost = phase == GamePhase.TAVERN ? 0 : tower.getUpgradeCost();
            guiGraphics.centeredText(minecraft.font,
                    Component.translatable("dungeon_defenders.tower.inspect_upgrade",
                            ModKeyMappings.UPGRADE_TOWER.getTranslatedKeyMessage(), cost),
                    centerX, y + 2 * LINE_HEIGHT, UPGRADE_COLOR);
        }
    }
}
