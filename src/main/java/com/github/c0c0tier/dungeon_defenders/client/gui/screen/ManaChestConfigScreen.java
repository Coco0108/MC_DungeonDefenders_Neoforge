package com.github.c0c0tier.dungeon_defenders.client.gui.screen;

import com.github.c0c0tier.dungeon_defenders.block.entity.ManaChestBlockEntity;
import com.github.c0c0tier.dungeon_defenders.menu.ManaChestConfigMenu;
import com.github.c0c0tier.dungeon_defenders.network.ManaChestConfigPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

// Écran de config du coffre de mana : un seul champ (quantité de mana), même principe que
// SpawnerConfigScreen mais sans les lignes dynamiques (une seule valeur scalaire). Pré-rempli
// à partir de la copie cliente (déjà synchronisée) du ManaChestBlockEntity. Au clic sur
// "Valider", envoie un ManaChestConfigPayload au serveur — voir ManaChestBlock (clic droit en
// créatif ouvre cet écran) et 05-etat-et-problemes-connus.md.
public class ManaChestConfigScreen extends Screen implements MenuAccess<ManaChestConfigMenu> {

    private static final int FIELD_WIDTH = 70;
    private static final int FIELD_HEIGHT = 16;
    private static final int ROW_HEIGHT = 32;
    private static final int TEXT_COLOR = 0xFFFFFF;

    private final ManaChestConfigMenu menu;
    private EditBox manaAmountField;

    public ManaChestConfigScreen(ManaChestConfigMenu menu, Inventory playerInventory, Component title) {
        super(title);
        this.menu = menu;
    }

    @Override
    public ManaChestConfigMenu getMenu() {
        return this.menu;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int y = this.height / 2 - FIELD_HEIGHT / 2;

        this.manaAmountField = new EditBox(
                this.font, centerX - FIELD_WIDTH / 2, y, FIELD_WIDTH, FIELD_HEIGHT, Component.empty());
        this.manaAmountField.setMaxLength(6);
        this.manaAmountField.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        this.manaAmountField.setValue(String.valueOf(resolveInitialManaAmount()));
        this.addRenderableWidget(this.manaAmountField);

        this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.mana_chest.config_confirm"),
                        button -> onConfirm())
                .bounds(centerX - FIELD_WIDTH / 2, y + ROW_HEIGHT, FIELD_WIDTH, FIELD_HEIGHT)
                .build());
    }

    private int resolveInitialManaAmount() {
        Level level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(this.menu.pos()) instanceof ManaChestBlockEntity chest) {
            return chest.getManaAmount();
        }
        return 25;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        guiGraphics.centeredText(this.font, this.getTitle(), centerX, this.manaAmountField.getY() - 24, TEXT_COLOR);
        guiGraphics.centeredText(this.font, Component.translatable("dungeon_defenders.mana_chest.config_amount"),
                centerX, this.manaAmountField.getY() - 11, TEXT_COLOR);
    }

    private void onConfirm() {
        int manaAmount = parseOr(this.manaAmountField.getValue(), 25);
        ManaChestConfigPayload payload = new ManaChestConfigPayload(this.menu.pos(), manaAmount);

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(payload.toVanillaServerbound());
        }

        this.onClose();
    }

    private static int parseOr(String text, int fallback) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
