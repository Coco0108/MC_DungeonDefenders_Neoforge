package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.client.ModKeyMappings;
import com.github.c0c0tier.dungeon_defenders.client.TowerInspectState;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.gui.GuiLayer;

import java.util.List;

// Fiche de la tour regardée (voir TowerInspectClientEvents) : un petit panneau à DROITE du
// viseur, pour ne pas masquer ce qu'on vise. Nom, niveau en pastilles, jauge de PV, chiffres au
// niveau actuel (TowerStatSummary) et, quand c'est possible, la touche et le prix de
// l'amélioration.
//
// Tout est lu sur la copie cliente du block entity, déjà synchronisée pour la barre de vie. La
// maîtrise des tours du poseur (ownerBonus) n'est pas synchronisée : les dégâts affichés sont
// ceux du niveau seul.
public class TowerInspectOverlay implements GuiLayer {

    private static final int OFFSET_RIGHT_OF_CROSSHAIR = 14;
    private static final int WIDTH = 170;
    private static final int LINE_HEIGHT = 10;
    private static final int PAD = 5;
    private static final int PIP_SIZE = 6;

    @Override
    public void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        BlockPos pos = TowerInspectState.target();
        if (level == null || pos == null || minecraft.options.hideGui
                || !(level.getBlockEntity(pos) instanceof AbstractTowerBlockEntity tower)) {
            return;
        }
        Font font = minecraft.font;

        List<Component> stats = TowerStatSummary.lines(tower.definition().stats(true), tower.levelMultiplier(), 0.0D);
        int columns = TowerStatSummary.columns(font, stats, WIDTH - 2 * PAD);
        int statRows = (stats.size() + columns - 1) / columns;
        // Amélioration seulement quand elle est possible : tour pas au maximum, et phase où l'on
        // construit (gratuite à la Taverne).
        GamePhase phase = GamePhase.of(level);
        boolean showUpgrade = tower.canUpgrade() && phase.allowsTowerBuilding();

        int height = UiStyle.HEADER_HEIGHT + 4
                + PIP_SIZE + 4          // niveau
                + 9 + 4                 // jauge de PV
                + (statRows > 0 ? statRows * LINE_HEIGHT + 2 : 0)
                + (showUpgrade ? LINE_HEIGHT + 4 : 0)
                + PAD - 2;

        int x = Math.min(g.guiWidth() / 2 + OFFSET_RIGHT_OF_CROSSHAIR, g.guiWidth() - WIDTH - 2);
        int y = g.guiHeight() / 2 - height / 2;
        UiStyle.titledPanel(g, font, tower.definition().displayName(), x, y, WIDTH, height);

        int left = x + PAD;
        int inner = WIDTH - 2 * PAD;
        int line = y + UiStyle.HEADER_HEIGHT + 4;

        // Niveau : « Niv. 2 » puis une pastille par niveau.
        Component levelText = Component.translatable("dungeon_defenders.tower.inspect_level", tower.getTowerLevel());
        g.text(font, levelText, left, line - 1, UiStyle.TEXT);
        UiStyle.pips(g, left + font.width(levelText) + 4, line, tower.getTowerLevel(), tower.getMaxTowerLevel(), PIP_SIZE, UiStyle.GOLD);
        line += PIP_SIZE + 4;

        // Jauge de PV avec le nombre par-dessus.
        double fraction = tower.getMaxHealth() <= 0 ? 0.0D : (double) tower.getHealth() / tower.getMaxHealth();
        UiStyle.bar(g, left, line, inner, 9, fraction, UiStyle.healthColor(fraction));
        g.centeredText(font, Component.translatable("dungeon_defenders.tower.inspect_health", tower.getHealth(), tower.getMaxHealth()),
                left + inner / 2, line + 1, UiStyle.TEXT);
        line += 9 + 4;

        int columnWidth = inner / columns;
        for (int i = 0; i < stats.size(); i++) {
            g.text(font, stats.get(i), left + (i % columns) * columnWidth, line + (i / columns) * LINE_HEIGHT, UiStyle.DIM);
        }
        line += statRows > 0 ? statRows * LINE_HEIGHT + 2 : 0;

        if (showUpgrade) {
            UiStyle.divider(g, left, line, inner);
            line += 5;
            int cost = phase == GamePhase.TAVERN ? 0 : tower.getUpgradeCost();
            int keyWidth = UiStyle.keyCap(g, font, ModKeyMappings.UPGRADE_TOWER.getTranslatedKeyMessage(), left, line);
            Component text = cost == 0
                    ? Component.translatable("dungeon_defenders.tower.inspect_upgrade_free")
                    : Component.translatable("dungeon_defenders.tower.inspect_upgrade", cost);
            g.text(font, text, left + keyWidth + 4, line, UiStyle.MANA);
        }
    }
}
