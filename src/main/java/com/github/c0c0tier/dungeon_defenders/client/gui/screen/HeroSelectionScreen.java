package com.github.c0c0tier.dungeon_defenders.client.gui.screen;

import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.network.SelectHeroPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// Écran de choix de héros : une colonne de boutons (un par HeroDefinition), le portrait et la
// description du héros survolé, et un bouton de confirmation.
//
// **Une seule option pour l'instant** — l'Écuyer est le seul héros existant. C'est assumé : cet
// écran existe pour valider toute la chaîne (choix, paquet, persistance, synchro, filtrage de la
// roue) sur un cas simple. Ajouter un héros deviendra une entrée d'enum, sans toucher à cet
// écran.
//
// Pas de Menu : rien à lire depuis le serveur pour l'ouvrir, le héros courant vient d'un
// attachment joueur déjà synchronisé — même raisonnement que MapSelectionScreen et
// TowerWheelScreen.
public class HeroSelectionScreen extends Screen {

    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int ICON_SIZE = 32;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int CURRENT_COLOR = 0xFFFFD700;

    private final List<HeroDefinition> heroes = List.of(HeroDefinition.values());
    private final List<Button> heroButtons = new ArrayList<>();

    private int selectedIndex;
    private int panelX;
    private int panelY;

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
        this.heroButtons.clear();

        int listX = this.width / 2 - 150;
        int top = 60;
        for (int i = 0; i < this.heroes.size(); i++) {
            int index = i;
            Button button = this.addRenderableWidget(Button.builder(
                            heroLabel(index), b -> onSelect(index))
                    .bounds(listX, top + i * (BUTTON_HEIGHT + BUTTON_GAP), BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
            this.heroButtons.add(button);
        }

        this.panelX = this.width / 2 + 10;
        this.panelY = top;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.hero_selection.confirm"), b -> onConfirm())
                .bounds(this.width / 2 - BUTTON_WIDTH / 2, this.height - 40, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    // Le héros actuellement porté par le joueur est marqué, pour qu'on voie tout de suite si le
    // bouton survolé est celui qu'on a déjà.
    private Component heroLabel(int index) {
        HeroDefinition hero = this.heroes.get(index);
        Component base = hero.displayName();
        if (hero == currentHero()) {
            return Component.literal("✔ ").append(base);
        }
        return index == this.selectedIndex ? Component.literal("▸ ").append(base) : base;
    }

    private void onSelect(int index) {
        this.selectedIndex = index;
        for (int i = 0; i < this.heroButtons.size(); i++) {
            this.heroButtons.get(i).setMessage(heroLabel(i));
        }
    }

    private void onConfirm() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new SelectHeroPayload(this.heroes.get(this.selectedIndex).ordinal()).toVanillaServerbound());
        }
        this.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.centeredText(this.font, this.getTitle(), this.width / 2, 24, TEXT_COLOR);

        HeroDefinition hero = this.heroes.get(this.selectedIndex);
        // Faute de portrait dédié, l'icône est la première tour du héros — même esprit
        // placeholder que les textures vanilla réutilisées ailleurs dans le mod.
        guiGraphics.item(new ItemStack(hero.icon()), this.panelX, this.panelY);
        guiGraphics.text(this.font, hero.displayName(), this.panelX + ICON_SIZE + 6, this.panelY + 4, TEXT_COLOR);
        guiGraphics.text(this.font, hero.description(), this.panelX, this.panelY + ICON_SIZE + 8, TEXT_COLOR);

        int y = this.panelY + ICON_SIZE + 8 + this.font.lineHeight * 2;
        guiGraphics.text(this.font, Component.translatable("dungeon_defenders.hero_selection.towers"),
                this.panelX, y, CURRENT_COLOR);
        y += this.font.lineHeight + 2;
        for (var tower : hero.towers()) {
            guiGraphics.text(this.font, tower.displayName(), this.panelX + 6, y, TEXT_COLOR);
            y += this.font.lineHeight;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
