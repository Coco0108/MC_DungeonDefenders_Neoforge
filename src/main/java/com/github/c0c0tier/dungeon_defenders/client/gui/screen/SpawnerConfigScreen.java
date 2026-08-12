package com.github.c0c0tier.dungeon_defenders.client.gui.screen;

import com.github.c0c0tier.dungeon_defenders.block.entity.SpawnerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.menu.SpawnerConfigMenu;
import com.github.c0c0tier.dungeon_defenders.network.SpawnerConfigPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

// Écran de config du spawner, sans slot ni item : que des champs numériques + un bouton.
// Pré-rempli à partir de la copie cliente (déjà synchronisée) du SpawnerBlockEntity ; au clic
// sur "Valider", envoie un SpawnerConfigPayload au serveur, qui l'applique via
// SpawnerBlockEntity.applyConfig(...) après revérification. Voir SpawnerBlock (clic droit
// sans shift ouvre cet écran) et 05-etat-et-problemes-connus.md (composition figée à
// zombie/squelette pour l'instant, pas de liste éditable).
public class SpawnerConfigScreen extends Screen implements MenuAccess<SpawnerConfigMenu> {

    private static final int FIELD_WIDTH = 70;
    private static final int FIELD_HEIGHT = 16;
    private static final int ROW_HEIGHT = 32;
    private static final int TEXT_COLOR = 0xFFFFFF;

    private final SpawnerConfigMenu menu;

    private EditBox intervalField;
    private EditBox radiusField;
    private EditBox waveStartField;
    private EditBox waveEndField;
    private EditBox zombieField;
    private EditBox skeletonField;

    public SpawnerConfigScreen(SpawnerConfigMenu menu, Inventory playerInventory, Component title) {
        super(title);
        this.menu = menu;
    }

    @Override
    public SpawnerConfigMenu getMenu() {
        return this.menu;
    }

    @Override
    protected void init() {
        super.init();

        SpawnerBlockEntity spawner = resolveSpawner();
        int interval = spawner != null ? spawner.getIntervalTicks() : 20;
        int radius = spawner != null ? spawner.getSpawnRadius() : 0;
        int waveStart = spawner != null ? spawner.getWaveStart() : 1;
        int waveEnd = spawner != null ? spawner.getWaveEnd() : ModAttachments.MAX_WAVE;
        int zombieBase = spawner != null ? spawner.getBaseCount(EntityType.ZOMBIE) : 15;
        int skeletonBase = spawner != null ? spawner.getBaseCount(EntityType.SKELETON) : 5;

        int centerX = this.width / 2;
        int top = this.height / 2 - (ROW_HEIGHT * 6 + 30) / 2;

        this.intervalField = addRow(centerX, top, "dungeon_defenders.spawner.config_interval", interval);
        this.radiusField = addRow(centerX, top + ROW_HEIGHT, "dungeon_defenders.spawner.config_radius", radius);
        this.waveStartField = addRow(centerX, top + ROW_HEIGHT * 2, "dungeon_defenders.spawner.config_wave_start", waveStart);
        this.waveEndField = addRow(centerX, top + ROW_HEIGHT * 3, "dungeon_defenders.spawner.config_wave_end", waveEnd);
        this.zombieField = addRow(centerX, top + ROW_HEIGHT * 4, "dungeon_defenders.spawner.config_zombie", zombieBase);
        this.skeletonField = addRow(centerX, top + ROW_HEIGHT * 5, "dungeon_defenders.spawner.config_skeleton", skeletonBase);

        this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.spawner.config_confirm"),
                        button -> onConfirm())
                .bounds(centerX - FIELD_WIDTH / 2, top + ROW_HEIGHT * 6 + 6, FIELD_WIDTH, FIELD_HEIGHT)
                .build());
    }

    /** Crée un champ numérique sur une rangée ; le libellé associé est dessiné dans extractRenderState. */
    private EditBox addRow(int centerX, int y, String labelKey, int initialValue) {
        Component label = Component.translatable(labelKey);
        EditBox field = new EditBox(this.font, centerX - FIELD_WIDTH / 2, y + 11, FIELD_WIDTH, FIELD_HEIGHT, label);
        field.setMaxLength(6);
        field.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        field.setValue(String.valueOf(initialValue));
        return this.addRenderableWidget(field);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        guiGraphics.centeredText(this.font, this.getTitle(), centerX, this.intervalField.getY() - 24, TEXT_COLOR);

        drawLabel(guiGraphics, "dungeon_defenders.spawner.config_interval", centerX, this.intervalField.getY());
        drawLabel(guiGraphics, "dungeon_defenders.spawner.config_radius", centerX, this.radiusField.getY());
        drawLabel(guiGraphics, "dungeon_defenders.spawner.config_wave_start", centerX, this.waveStartField.getY());
        drawLabel(guiGraphics, "dungeon_defenders.spawner.config_wave_end", centerX, this.waveEndField.getY());
        drawLabel(guiGraphics, "dungeon_defenders.spawner.config_zombie", centerX, this.zombieField.getY());
        drawLabel(guiGraphics, "dungeon_defenders.spawner.config_skeleton", centerX, this.skeletonField.getY());
    }

    private void drawLabel(GuiGraphicsExtractor guiGraphics, String key, int centerX, int fieldY) {
        guiGraphics.centeredText(this.font, Component.translatable(key), centerX, fieldY - 11, TEXT_COLOR);
    }

    private void onConfirm() {
        BlockPos pos = this.menu.pos();
        SpawnerConfigPayload payload = new SpawnerConfigPayload(
                pos,
                parseOr(this.intervalField, 20),
                parseOr(this.radiusField, 0),
                parseOr(this.waveStartField, 1),
                parseOr(this.waveEndField, ModAttachments.MAX_WAVE),
                parseOr(this.zombieField, 0),
                parseOr(this.skeletonField, 0)
        );

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(payload.toVanillaServerbound());
        }

        this.onClose();
    }

    private static int parseOr(EditBox field, int fallback) {
        try {
            return Integer.parseInt(field.getValue());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private SpawnerBlockEntity resolveSpawner() {
        Level level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(this.menu.pos()) instanceof SpawnerBlockEntity spawner) {
            return spawner;
        }
        return null;
    }
}
