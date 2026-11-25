package com.github.c0c0tier.dungeon_defenders.client.gui.screen;

import com.github.c0c0tier.dungeon_defenders.client.ModKeyMappings;
import com.github.c0c0tier.dungeon_defenders.client.TowerPlacementState;
import com.github.c0c0tier.dungeon_defenders.client.gui.CircleSlot;
import com.github.c0c0tier.dungeon_defenders.client.gui.TowerStatSummary;
import com.github.c0c0tier.dungeon_defenders.client.gui.UiStyle;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

// Roue radiale de sélection des tours (voir doc/02-gameplay.md). Un emplacement rond par tour du
// héros, en cercle autour du centre de l'écran, avec son coût dessous ; au centre, la tour
// survolée en grand. Une fiche à côté de la roue donne son nom, son coût, sa description et ses
// chiffres (TowerStatSummary).
//
// Trois façons de confirmer : cliquer un emplacement, relâcher la touche qui a ouvert la roue en
// visant l'emplacement voulu (même calcul d'angle souris-centre), ou taper son numéro (1 à 9).
// Toutes passent par confirmSelection(int). Une tour trop chère reste choisissable (le serveur
// refusera la pose avec un message), mais son coût est en rouge et son emplacement voilé.
//
// Ouvert directement côté client par TowerPlacementClientEvents, sans Menu : rien à
// synchroniser depuis le serveur pour l'ouvrir.
public class TowerWheelScreen extends Screen {

    private static final int RING_RADIUS = 64;
    private static final int SLOT_RADIUS = 15;
    private static final int HOVERED_SLOT_RADIUS = 17;
    private static final int CENTER_RADIUS = 24;
    // En dessous de cette distance au centre, aucun emplacement n'est considéré survolé : évite
    // qu'un simple frémissement de la souris au centre en sélectionne un au hasard.
    private static final double DEAD_ZONE_RADIUS = 20.0D;
    private static final int ICON_SIZE = 16;
    private static final int CARD_WIDTH = 176;
    private static final int MIN_CARD_WIDTH = 110;
    private static final int CARD_GAP = 14;
    private static final int LINE_HEIGHT = 10;

    private static final int SLOT_FILL = 0xE0201A14;
    private static final int SLOT_BORDER = 0xFF6E5B3E;
    private static final int SLOT_HOVER_FILL = 0xF03A2D1C;
    private static final int UNAFFORDABLE_VEIL = 0x90000000;

    // Seulement les tours du héros du joueur, et non tout le catalogue : c'est ce qui donne un
    // sens au choix de classe. L'autorité reste côté serveur (ModEvents revérifie), ce filtre
    // n'est que l'affichage.
    private final List<TowerDefinition> towers = resolveTowers();
    private int hoveredIndex = -1;

    public TowerWheelScreen() {
        super(Component.translatable("dungeon_defenders.tower_wheel.title"));
    }

    private static List<TowerDefinition> resolveTowers() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null
                ? HeroDefinition.DEFAULT.towers()
                : HeroDefinition.of(player).towers();
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

    private int slotX(int index) {
        double angle = index * (2.0D * Math.PI / this.towers.size());
        return this.width / 2 + (int) Math.round(Math.sin(angle) * RING_RADIUS);
    }

    private int slotY(int index) {
        double angle = index * (2.0D * Math.PI / this.towers.size());
        return this.height / 2 - (int) Math.round(Math.cos(angle) * RING_RADIUS);
    }

    // Gratuit à la Taverne, comme côté serveur (ModEvents) : on n'y gagne pas de mana.
    private static int effectiveCost(TowerDefinition tower) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && GamePhase.of(minecraft.level) == GamePhase.TAVERN) {
            return 0;
        }
        return tower.stats(true).intValue(TowerStat.MANA_COST);
    }

    private static int currentMana() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? 0 : player.getData(ModAttachments.MANA);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        this.hoveredIndex = computeHoveredIndex(mouseX, mouseY);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int mana = currentMana();

        g.centeredText(this.font, this.getTitle(), centerX, centerY - RING_RADIUS - HOVERED_SLOT_RADIUS - 14, UiStyle.GOLD);

        for (int i = 0; i < this.towers.size(); i++) {
            TowerDefinition tower = this.towers.get(i);
            boolean hovered = i == this.hoveredIndex;
            int x = slotX(i);
            int y = slotY(i);
            int radius = hovered ? HOVERED_SLOT_RADIUS : SLOT_RADIUS;
            CircleSlot.render(g, x, y, radius, hovered ? SLOT_HOVER_FILL : SLOT_FILL, hovered ? UiStyle.GOLD : SLOT_BORDER);
            g.item(tower.icon().getDefaultInstance(), x - ICON_SIZE / 2, y - ICON_SIZE / 2);

            int cost = effectiveCost(tower);
            boolean affordable = cost <= mana;
            if (!affordable) {
                CircleSlot.renderVeil(g, x, y, radius, 1.0F, UNAFFORDABLE_VEIL);
            }
            // Numéro de la touche, en haut à gauche de l'emplacement.
            if (i < 9) {
                g.text(this.font, String.valueOf(i + 1), x - radius, y - radius, UiStyle.DIM);
            }
            g.centeredText(this.font, String.valueOf(cost), x, y + radius + 2, affordable ? UiStyle.MANA : UiStyle.NEGATIVE);
        }

        // Centre : la tour survolée en grand, sinon le mana disponible.
        CircleSlot.render(g, centerX, centerY, CENTER_RADIUS, SLOT_FILL, SLOT_BORDER);
        if (this.hoveredIndex >= 0) {
            g.pose().pushMatrix();
            g.pose().translate(centerX, centerY);
            g.pose().scale(2.0F);
            g.item(this.towers.get(this.hoveredIndex).icon().getDefaultInstance(), -ICON_SIZE / 2, -ICON_SIZE / 2);
            g.pose().popMatrix();
        } else {
            g.centeredText(this.font, String.valueOf(mana), centerX, centerY - 8, UiStyle.MANA);
            g.centeredText(this.font, Component.translatable("dungeon_defenders.tower_wheel.mana"), centerX, centerY + 2, UiStyle.DIM);
        }

        if (this.hoveredIndex >= 0) {
            renderCard(g, this.towers.get(this.hoveredIndex), mana);
        } else {
            g.centeredText(this.font, Component.translatable("dungeon_defenders.tower_wheel.hint"),
                    centerX, centerY + RING_RADIUS + HOVERED_SLOT_RADIUS + 14, UiStyle.DIM);
        }
    }

    // Fiche de la tour survolée, à droite de la roue. Sur un écran étroit (grande échelle
    // d'interface), elle rétrécit jusqu'à MIN_CARD_WIDTH et passe ses chiffres sur une colonne ;
    // en dessous, elle va en bas de l'écran, quitte à chevaucher la roue.
    private void renderCard(GuiGraphicsExtractor g, TowerDefinition tower, int mana) {
        int ringRight = this.width / 2 + RING_RADIUS + HOVERED_SLOT_RADIUS + CARD_GAP;
        int cardWidth = Math.min(CARD_WIDTH, this.width - 4 - ringRight);
        boolean beside = cardWidth >= MIN_CARD_WIDTH;
        if (!beside) {
            cardWidth = Math.min(CARD_WIDTH, this.width - 8);
        }
        int innerWidth = cardWidth - 12;
        List<FormattedCharSequence> description = this.font.split(tower.description(), innerWidth);
        List<Component> stats = TowerStatSummary.lines(tower.stats(true), 1.0D, 1.0D);
        int columns = TowerStatSummary.columns(this.font, stats, innerWidth);
        int statRows = (stats.size() + columns - 1) / columns;
        int height = UiStyle.HEADER_HEIGHT + 4
                + LINE_HEIGHT + 4
                + description.size() * LINE_HEIGHT + 4
                + (statRows > 0 ? 4 + statRows * LINE_HEIGHT : 0)
                + 4;

        int x;
        int y;
        if (beside) {
            x = ringRight;
            y = Math.max(2, this.height / 2 - height / 2);
        } else {
            x = this.width / 2 - cardWidth / 2;
            y = this.height - height - 4;
        }

        UiStyle.titledPanel(g, this.font, tower.displayName(), x, y, cardWidth, height);
        int left = x + 6;
        int line = y + UiStyle.HEADER_HEIGHT + 5;

        int cost = effectiveCost(tower);
        Component costText = cost == 0
                ? Component.translatable("dungeon_defenders.tower_wheel.free")
                : Component.translatable("dungeon_defenders.tower_wheel.mana_cost", cost);
        g.text(this.font, costText, left, line, cost <= mana ? UiStyle.MANA : UiStyle.NEGATIVE);
        line += LINE_HEIGHT + 4;

        for (FormattedCharSequence row : description) {
            g.text(this.font, row, left, line, UiStyle.TEXT);
            line += LINE_HEIGHT;
        }

        if (!stats.isEmpty()) {
            line += 2;
            UiStyle.divider(g, left, line, innerWidth);
            line += 6;
            int columnWidth = innerWidth / columns;
            for (int i = 0; i < stats.size(); i++) {
                g.text(this.font, stats.get(i), left + (i % columns) * columnWidth, line + (i / columns) * LINE_HEIGHT, UiStyle.DIM);
            }
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
    public boolean keyPressed(KeyEvent event) {
        // Touches 1 à 9 de la rangée du haut (GLFW_KEY_1 = 49).
        int digit = event.key() - 49;
        if (digit >= 0 && digit < Math.min(9, this.towers.size())) {
            confirmSelection(digit);
            return true;
        }
        return super.keyPressed(event);
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
