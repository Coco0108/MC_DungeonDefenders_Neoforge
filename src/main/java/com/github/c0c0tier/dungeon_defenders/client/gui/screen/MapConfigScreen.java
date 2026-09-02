package com.github.c0c0tier.dungeon_defenders.client.gui.screen;

import com.github.c0c0tier.dungeon_defenders.block.entity.MapConfigBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.MapDefinition;
import com.github.c0c0tier.dungeon_defenders.menu.MapConfigMenu;
import com.github.c0c0tier.dungeon_defenders.network.MapConfigPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

// Écran de configuration d'une map, ouvert par MapConfigBlock (créatif uniquement). Même patron
// que SpawnerConfigScreen : pré-rempli depuis la copie cliente du block entity, déjà
// synchronisée, et un paquet part au clic sur "Valider".
//
// Cet écran ne contient PAS de suppression : le bloc de configuration ne connaît pas
// l'identifiant de sa propre structure (celui-ci est choisi bien plus tard, au moment de
// sauvegarder avec un bloc de structure), il faudrait donc le retaper à la main. La suppression
// vit dans l'écran de CHOIX de map, où l'identifiant est connu et où l'on voit ce qu'on efface —
// déplacée là sur décision du joueur (2026-09-02).
public class MapConfigScreen extends Screen implements MenuAccess<MapConfigMenu> {

    private static final int FIELD_WIDTH = 140;
    private static final int FIELD_HEIGHT = 16;
    private static final int ROW_HEIGHT = 32;
    private static final int BUTTON_WIDTH = 140;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private final MapConfigMenu menu;

    private String nameText = "";
    private String orderText = String.valueOf(MapDefinition.DEFAULT_ORDER);
    private String waveCountText = String.valueOf(MapDefinition.DEFAULT_WAVE_COUNT);
    private String multiplierText = String.valueOf(MapDefinition.DEFAULT_SCORE_MULTIPLIER);
    private boolean loadedFromBlock;

    private EditBox nameField;
    private EditBox orderField;
    private EditBox waveCountField;
    private EditBox multiplierField;

    public MapConfigScreen(MapConfigMenu menu, Inventory playerInventory, Component title) {
        super(title);
        this.menu = menu;
    }

    @Override
    public MapConfigMenu getMenu() {
        return this.menu;
    }

    @Override
    protected void init() {
        super.init();
        if (!this.loadedFromBlock) {
            loadFromBlock();
            this.loadedFromBlock = true;
        }

        int centerX = this.width / 2;
        int top = Math.max(24, this.height / 2 - (5 * ROW_HEIGHT) / 2);
        int y = top;

        this.nameField = addField(centerX, y, this.nameText, 48, null);
        y += ROW_HEIGHT;
        this.orderField = addField(centerX, y, this.orderText, 4, MapConfigScreen::isInteger);
        y += ROW_HEIGHT;
        this.waveCountField = addField(centerX, y, this.waveCountText, 3, MapConfigScreen::isInteger);
        y += ROW_HEIGHT;
        this.multiplierField = addField(centerX, y, this.multiplierText, 6, MapConfigScreen::isDecimal);
        y += ROW_HEIGHT;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.map_config.confirm"), button -> onConfirm())
                .bounds(centerX - BUTTON_WIDTH / 2, y + 6, BUTTON_WIDTH, FIELD_HEIGHT)
                .build());
    }

    private void loadFromBlock() {
        MapConfigBlockEntity block = resolveBlock();
        if (block == null) {
            return;
        }
        this.nameText = block.getDisplayName();
        this.orderText = String.valueOf(block.getOrder());
        this.waveCountText = String.valueOf(block.getWaveCount());
        this.multiplierText = String.valueOf(block.getScoreMultiplier());
    }

    private @Nullable MapConfigBlockEntity resolveBlock() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        return minecraft.level.getBlockEntity(this.menu.pos()) instanceof MapConfigBlockEntity block ? block : null;
    }

    private EditBox addField(int centerX, int y, String value, int maxLength, @Nullable Predicate<String> filter) {
        EditBox field = new EditBox(this.font, centerX - FIELD_WIDTH / 2, y + 11, FIELD_WIDTH, FIELD_HEIGHT, Component.empty());
        field.setMaxLength(maxLength);
        if (filter != null) {
            field.setFilter(filter);
        }
        field.setValue(value);
        return this.addRenderableWidget(field);
    }

    private static boolean isInteger(String text) {
        return text.isEmpty() || text.chars().allMatch(Character::isDigit);
    }

    private static boolean isDecimal(String text) {
        return text.isEmpty() || text.chars().allMatch(c -> Character.isDigit(c) || c == '.');
    }

    private void onConfirm() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new MapConfigPayload(
                    this.menu.pos(),
                    this.nameField.getValue(),
                    parseInt(this.orderField.getValue(), MapDefinition.DEFAULT_ORDER),
                    parseInt(this.waveCountField.getValue(), MapDefinition.DEFAULT_WAVE_COUNT),
                    parseFloat(this.multiplierField.getValue())).toVanillaServerbound());
        }
        this.onClose();
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static float parseFloat(String text) {
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException exception) {
            return MapDefinition.DEFAULT_SCORE_MULTIPLIER;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        guiGraphics.centeredText(this.font, this.title, centerX, Math.max(8, this.height / 2 - (5 * ROW_HEIGHT) / 2 - 16), TEXT_COLOR);
        label(guiGraphics, centerX, this.nameField, "dungeon_defenders.map_config.name");
        label(guiGraphics, centerX, this.orderField, "dungeon_defenders.map_config.order");
        label(guiGraphics, centerX, this.waveCountField, "dungeon_defenders.map_config.wave_count");
        label(guiGraphics, centerX, this.multiplierField, "dungeon_defenders.map_config.score_multiplier");
    }

    private void label(GuiGraphicsExtractor guiGraphics, int centerX, EditBox field, String key) {
        if (field != null) {
            guiGraphics.centeredText(this.font, Component.translatable(key), centerX, field.getY() - 11, TEXT_COLOR);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
