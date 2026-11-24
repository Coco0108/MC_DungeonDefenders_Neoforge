package com.github.c0c0tier.dungeon_defenders.client.gui.screen;

import com.github.c0c0tier.dungeon_defenders.client.gui.UiStyle;
import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import com.github.c0c0tier.dungeon_defenders.network.SelectHeroPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// Écran de choix de héros, dans un panneau : à gauche une carte par héros (icône de son arme,
// nom, « actuel » sur celui qu'on joue) ; à droite la fiche du héros choisi : grande icône,
// description, ses six tours (infobulle au survol avec description et coût), ses deux sorts et
// son arme. Un bouton confirme.
//
// Ajouter un héros est une entrée d'enum (HeroDefinition), sans toucher à cet écran ; au-delà
// de six héros, les cartes ne tiendraient plus en hauteur et il faudra une liste défilante.
//
// Pas de Menu : rien à lire depuis le serveur pour l'ouvrir, le héros courant vient d'un
// attachment joueur déjà synchronisé — même raisonnement que MapSelectionScreen et
// TowerWheelScreen.
public class HeroSelectionScreen extends Screen {

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 214;
    private static final int CARD_WIDTH = 104;
    private static final int CARD_HEIGHT = 26;
    private static final int CARD_GAP = 4;
    private static final int PAD = 8;
    private static final int LINE_HEIGHT = 10;
    private static final int CARD_SELECTED_FILL = 0xFF3A2D1C;

    private final List<HeroDefinition> heroes = List.of(HeroDefinition.values());

    private int selectedIndex;
    private int left;
    private int top;
    private int detailLeft;
    private int detailWidth;

    public HeroSelectionScreen() {
        super(Component.translatable("dungeon_defenders.hero_selection.title"));
        this.selectedIndex = Math.max(0, this.heroes.indexOf(currentHero()));
    }

    private static HeroDefinition currentHero() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null
                ? HeroDefinition.DEFAULT
                : HeroDefinition.values()[player.getData(ModAttachments.HERO)];
    }

    @Override
    protected void init() {
        super.init();
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = Math.max(4, (this.height - PANEL_HEIGHT) / 2);
        this.detailLeft = this.left + PAD + CARD_WIDTH + 10;
        this.detailWidth = this.left + PANEL_WIDTH - PAD - this.detailLeft;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.hero_selection.confirm"), b -> onConfirm())
                .bounds(this.left + PANEL_WIDTH - PAD - 100, this.top + PANEL_HEIGHT - 26, 100, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose())
                .bounds(this.left + PAD, this.top + PANEL_HEIGHT - 26, CARD_WIDTH, 20)
                .build());
    }

    private int cardTop(int index) {
        return this.top + UiStyle.HEADER_HEIGHT + 8 + index * (CARD_HEIGHT + CARD_GAP);
    }

    private void onConfirm() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new SelectHeroPayload(this.heroes.get(this.selectedIndex).ordinal()).toVanillaServerbound());
        }
        this.onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int i = 0; i < this.heroes.size(); i++) {
            if (UiStyle.isInside(event.x(), event.y(), this.left + PAD, cardTop(i), CARD_WIDTH, CARD_HEIGHT)) {
                this.selectedIndex = i;
                if (doubleClick) {
                    onConfirm();
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        UiStyle.titledPanel(g, this.font, this.getTitle(), this.left, this.top, PANEL_WIDTH, PANEL_HEIGHT);
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        HeroDefinition current = currentHero();
        for (int i = 0; i < this.heroes.size(); i++) {
            renderCard(g, i, current, mouseX, mouseY);
        }
        renderDetail(g, this.heroes.get(this.selectedIndex), mouseX, mouseY);
    }

    private void renderCard(GuiGraphicsExtractor g, int index, HeroDefinition current, int mouseX, int mouseY) {
        HeroDefinition hero = this.heroes.get(index);
        int x = this.left + PAD;
        int y = cardTop(index);
        boolean selected = index == this.selectedIndex;
        boolean hovered = UiStyle.isInside(mouseX, mouseY, x, y, CARD_WIDTH, CARD_HEIGHT);

        UiStyle.inset(g, x, y, CARD_WIDTH, CARD_HEIGHT);
        if (selected) {
            g.fill(x + 1, y + 1, x + CARD_WIDTH - 1, y + CARD_HEIGHT - 1, CARD_SELECTED_FILL);
            g.outline(x, y, CARD_WIDTH, CARD_HEIGHT, UiStyle.GOLD);
        } else if (hovered) {
            g.fill(x + 1, y + 1, x + CARD_WIDTH - 1, y + CARD_HEIGHT - 1, UiStyle.HOVER_FILL);
        }
        g.item(new ItemStack(hero.icon()), x + 5, y + 5);
        g.text(this.font, hero.displayName(), x + 26, y + (hero == current ? 4 : 9), selected ? UiStyle.GOLD : UiStyle.TEXT);
        if (hero == current) {
            g.text(this.font, Component.translatable("dungeon_defenders.hero_selection.current"), x + 26, y + 14, UiStyle.POSITIVE);
        }
    }

    private void renderDetail(GuiGraphicsExtractor g, HeroDefinition hero, int mouseX, int mouseY) {
        int x = this.detailLeft;
        int y = this.top + UiStyle.HEADER_HEIGHT + 8;

        // Grande icône (x2) et nom.
        UiStyle.inset(g, x, y, 36, 36);
        g.pose().pushMatrix();
        g.pose().translate(x + 2, y + 2);
        g.pose().scale(2.0F);
        g.item(new ItemStack(hero.icon()), 0, 0);
        g.pose().popMatrix();
        g.text(this.font, hero.displayName(), x + 44, y + 6, UiStyle.GOLD);
        g.text(this.font, Component.translatable("dungeon_defenders.hero_selection.weapon",
                new ItemStack(hero.weapon()).getHoverName()), x + 44, y + 20, UiStyle.DIM);
        y += 42;

        List<FormattedCharSequence> description = this.font.split(hero.description(), this.detailWidth);
        for (FormattedCharSequence line : description) {
            g.text(this.font, line, x, y, UiStyle.TEXT);
            y += LINE_HEIGHT;
        }
        y += 4;

        // Les tours : une case par tour, infobulle au survol.
        g.text(this.font, Component.translatable("dungeon_defenders.hero_selection.towers"), x, y, UiStyle.GOLD);
        y += LINE_HEIGHT + 2;
        List<TowerDefinition> towers = hero.towers();
        TowerDefinition hoveredTower = null;
        for (int i = 0; i < towers.size(); i++) {
            int slotX = x + i * 22;
            boolean hovered = UiStyle.isInside(mouseX, mouseY, slotX, y, 18, 18);
            UiStyle.itemSlot(g, towers.get(i).icon().getDefaultInstance(), slotX, y, 0, hovered);
            if (hovered) {
                hoveredTower = towers.get(i);
            }
        }
        y += 24;

        g.text(this.font, Component.translatable("dungeon_defenders.hero_selection.spells"), x, y, UiStyle.GOLD);
        y += LINE_HEIGHT + 2;
        g.text(this.font, Component.literal("• ").append(hero.spell1().displayName()), x + 4, y, UiStyle.TEXT);
        g.text(this.font, Component.literal("• ").append(hero.spell2().displayName())
                        .append(Component.translatable("dungeon_defenders.hero_selection.held").withStyle(ChatFormatting.GRAY)),
                x + 4, y + LINE_HEIGHT, UiStyle.TEXT);

        if (hoveredTower != null) {
            // Découpée à la main : une infobulle de composants ne passe pas à la ligne.
            List<FormattedCharSequence> tooltip = new ArrayList<>();
            tooltip.add(hoveredTower.displayName().copy().withStyle(ChatFormatting.GOLD).getVisualOrderText());
            tooltip.addAll(this.font.split(hoveredTower.description(), 200));
            tooltip.add(Component.translatable("dungeon_defenders.tower_wheel.mana_cost",
                    hoveredTower.stats(true).intValue(TowerStat.MANA_COST)).withStyle(ChatFormatting.AQUA).getVisualOrderText());
            g.setTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
