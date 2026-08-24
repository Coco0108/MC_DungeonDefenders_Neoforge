package com.github.c0c0tier.dungeon_defenders.client.gui.screen;

import com.github.c0c0tier.dungeon_defenders.client.ModKeyMappings;
import com.github.c0c0tier.dungeon_defenders.client.TowerPlacementState;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

// Roue radiale de sélection des tours (voir doc/02-gameplay.md). Un secteur par
// TowerDefinition, disposé en cercle autour du centre de l'écran. Deux façons de confirmer une
// sélection : cliquer directement sur un secteur (mouseClicked), ou relâcher la touche qui a
// ouvert la roue (keyReleased) après l'avoir maintenue en visant le secteur voulu au curseur —
// les deux passent par confirmSelection(int) et se basent sur le même calcul d'angle
// souris-centre. Ouvert directement côté client par TowerPlacementClientEvents, sans Menu : pas
// d'état à synchroniser depuis le serveur pour l'ouvrir (contrairement à SpawnerConfigScreen).
public class TowerWheelScreen extends Screen {

    private static final int RADIUS = 60;
    // En dessous de cette distance au centre, aucun secteur n'est considéré survolé — évite
    // qu'un simple frémissement de la souris au centre sélectionne un secteur au hasard.
    private static final double DEAD_ZONE_RADIUS = 20.0D;
    private static final int ICON_SIZE = 16;
    private static final int HIGHLIGHT_COLOR = 0x80FFFF55;
    private static final int TEXT_COLOR = 0xFFFFFF;

    private final List<TowerDefinition> towers = List.of(TowerDefinition.values());
    private int hoveredIndex = -1;

    public TowerWheelScreen() {
        super(Component.translatable("dungeon_defenders.tower_wheel.title"));
    }

    private int computeHoveredIndex(double mouseX, double mouseY) {
        if (this.towers.isEmpty()) {
            return -1;
        }

        double dx = mouseX - this.width / 2.0D;
        double dy = mouseY - this.height / 2.0D;
        if (dx * dx + dy * dy < DEAD_ZONE_RADIUS * DEAD_ZONE_RADIUS) {
            return -1;
        }

        // 0 = haut de l'écran, angle croissant dans le sens horaire.
        double angle = Math.atan2(dx, -dy);
        if (angle < 0.0D) {
            angle += 2.0D * Math.PI;
        }

        double segment = 2.0D * Math.PI / this.towers.size();
        return (int) Math.round(angle / segment) % this.towers.size();
    }

    private int iconX(int index) {
        double angle = index * (2.0D * Math.PI / this.towers.size());
        return this.width / 2 - ICON_SIZE / 2 + (int) Math.round(Math.sin(angle) * RADIUS);
    }

    private int iconY(int index) {
        double angle = index * (2.0D * Math.PI / this.towers.size());
        return this.height / 2 - ICON_SIZE / 2 - (int) Math.round(Math.cos(angle) * RADIUS);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        this.hoveredIndex = computeHoveredIndex(mouseX, mouseY);

        int centerX = this.width / 2;
        guiGraphics.centeredText(this.font, this.getTitle(), centerX, this.height / 2 - RADIUS - 24, TEXT_COLOR);

        for (int i = 0; i < this.towers.size(); i++) {
            int x = iconX(i);
            int y = iconY(i);

            if (i == this.hoveredIndex) {
                guiGraphics.fill(x - 3, y - 3, x + ICON_SIZE + 3, y + ICON_SIZE + 3, HIGHLIGHT_COLOR);
            }
            guiGraphics.item(this.towers.get(i).icon().getDefaultInstance(), x, y);
        }

        if (this.hoveredIndex >= 0) {
            TowerDefinition hovered = this.towers.get(this.hoveredIndex);
            guiGraphics.centeredText(this.font, hovered.displayName(), centerX, this.height / 2 + RADIUS + 12, TEXT_COLOR);
            guiGraphics.centeredText(this.font,
                    Component.translatable("dungeon_defenders.tower_wheel.mana_cost", hovered.manaCost()),
                    centerX, this.height / 2 + RADIUS + 24, TEXT_COLOR);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int index = computeHoveredIndex(event.x(), event.y());
        if (index >= 0) {
            confirmSelection(index);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (ModKeyMappings.TOWER_WHEEL.matches(event)) {
            confirmSelection(this.hoveredIndex);
            return true;
        }
        return super.keyReleased(event);
    }

    private void confirmSelection(int index) {
        if (index < 0 || index >= this.towers.size()) {
            this.onClose();
            return;
        }
        TowerPlacementState.start(this.towers.get(index));
        this.onClose();
    }
}
