package com.github.c0c0tier.dungeon_defenders.client.gui.screen;

import com.github.c0c0tier.dungeon_defenders.block.entity.SpawnerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.SpawnableEnemy;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

// Écran de config du spawner, sans slot ni item. Composition dynamique : une ligne par ennemi
// (bouton "cycler le type" + champ nombre de base + bouton retirer), plus un bouton "Ajouter"
// (caché une fois tous les SpawnableEnemy utilisés, la liste est fermée). Les champs scalaires
// (intervalle, rayon, plage de vagues) et l'état des lignes sont gardés en mémoire (intervalText,
// rows, ...) et non dans les widgets eux-mêmes, car Ajouter/Retirer une ligne reconstruit tous
// les widgets (rebuildWidgets) pour replacer les lignes suivantes et le bouton Valider.
//
// Pré-rempli à partir de la copie cliente (déjà synchronisée) du SpawnerBlockEntity, uniquement
// à la toute première init() (voir loadedFromSpawner) pour ne pas écraser les modifications en
// cours de l'utilisateur à chaque rebuild. Au clic sur "Valider", envoie un SpawnerConfigPayload
// au serveur, qui l'applique via SpawnerBlockEntity.applyConfig(...) après revérification.
// Voir SpawnerBlock (clic droit sans shift ouvre cet écran) et 05-etat-et-problemes-connus.md.
public class SpawnerConfigScreen extends Screen implements MenuAccess<SpawnerConfigMenu> {

    private static final int FIELD_WIDTH = 70;
    private static final int FIELD_HEIGHT = 16;
    private static final int ROW_HEIGHT = 32;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private static final int CYCLE_BUTTON_WIDTH = 90;
    private static final int COUNT_FIELD_WIDTH = 50;
    private static final int REMOVE_BUTTON_WIDTH = 20;
    private static final int ROW_GAP = 4;
    private static final int ROW_TOTAL_WIDTH = CYCLE_BUTTON_WIDTH + ROW_GAP + COUNT_FIELD_WIDTH + ROW_GAP + REMOVE_BUTTON_WIDTH;

    private final SpawnerConfigMenu menu;

    // État en mémoire, source de vérité entre deux rebuilds (voir commentaire de classe).
    private String intervalText;
    private String radiusText;
    private String waveStartText;
    private String waveEndText;
    private final List<RowState> rows = new ArrayList<>();
    private boolean loadedFromSpawner;

    // Widgets scalaires (recréés à chaque rebuild, mais gardés le temps du rendu courant).
    private EditBox intervalField;
    private EditBox radiusField;
    private EditBox waveStartField;
    private EditBox waveEndField;

    // Widgets de lignes, parallèles à `rows` (recréés à chaque rebuild).
    private final List<EditBox> rowCountFields = new ArrayList<>();
    private final List<Button> rowCycleButtons = new ArrayList<>();

    private static final class RowState {
        SpawnableEnemy enemy;
        String countText;

        RowState(SpawnableEnemy enemy, int baseCount) {
            this.enemy = enemy;
            this.countText = String.valueOf(baseCount);
        }
    }

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
        if (!this.loadedFromSpawner) {
            loadFromSpawner();
            this.loadedFromSpawner = true;
        }
        buildWidgets();
    }

    /** Charge l'état initial depuis la copie cliente du block entity. Une seule fois par ouverture d'écran. */
    private void loadFromSpawner() {
        SpawnerBlockEntity spawner = resolveSpawner();
        this.intervalText = String.valueOf(spawner != null ? spawner.getIntervalTicks() : 20);
        this.radiusText = String.valueOf(spawner != null ? spawner.getSpawnRadius() : 0);
        this.waveStartText = String.valueOf(spawner != null ? spawner.getWaveStart() : 1);
        this.waveEndText = String.valueOf(spawner != null ? spawner.getWaveEnd() : ModAttachments.MAX_WAVE);

        this.rows.clear();
        if (spawner != null) {
            for (SpawnerBlockEntity.SpawnEntry entry : spawner.getEntries()) {
                this.rows.add(new RowState(entry.enemy(), entry.baseCount()));
            }
        }
        if (this.rows.isEmpty()) {
            this.rows.add(new RowState(SpawnableEnemy.ZOMBIE, 15));
            this.rows.add(new RowState(SpawnableEnemy.SKELETON, 5));
        }
    }

    /** Reconstruit tous les widgets à partir de l'état en mémoire (intervalText, rows, ...). */
    private void buildWidgets() {
        this.rowCountFields.clear();
        this.rowCycleButtons.clear();

        boolean showAdd = this.rows.size() < SpawnableEnemy.values().length;
        int totalRows = 4 + this.rows.size() + (showAdd ? 1 : 0) + 1;
        int centerX = this.width / 2;
        int top = Math.max(24, this.height / 2 - (totalRows * ROW_HEIGHT) / 2);
        int y = top;

        this.intervalField = addScalarField(centerX, y, this.intervalText);
        y += ROW_HEIGHT;
        this.radiusField = addScalarField(centerX, y, this.radiusText);
        y += ROW_HEIGHT;
        this.waveStartField = addScalarField(centerX, y, this.waveStartText);
        y += ROW_HEIGHT;
        this.waveEndField = addScalarField(centerX, y, this.waveEndText);
        y += ROW_HEIGHT;

        for (int i = 0; i < this.rows.size(); i++) {
            addEntryRow(centerX, y, i);
            y += ROW_HEIGHT;
        }

        if (showAdd) {
            this.addRenderableWidget(Button.builder(
                            Component.translatable("dungeon_defenders.spawner.config_add"),
                            button -> onAddRow())
                    .bounds(centerX - FIELD_WIDTH / 2, y, FIELD_WIDTH, FIELD_HEIGHT)
                    .build());
            y += ROW_HEIGHT;
        }

        this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.spawner.config_confirm"),
                        button -> onConfirm())
                .bounds(centerX - FIELD_WIDTH / 2, y + 6, FIELD_WIDTH, FIELD_HEIGHT)
                .build());
    }

    private EditBox addScalarField(int centerX, int y, String initialText) {
        EditBox field = new EditBox(this.font, centerX - FIELD_WIDTH / 2, y + 11, FIELD_WIDTH, FIELD_HEIGHT, Component.empty());
        field.setMaxLength(6);
        field.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        field.setValue(initialText);
        return this.addRenderableWidget(field);
    }

    /** Bouton "cycler le type" (auto-libellé par l'enum) + champ nombre + bouton retirer, pour une ligne. */
    private void addEntryRow(int centerX, int y, int rowIndex) {
        RowState row = this.rows.get(rowIndex);
        int left = centerX - ROW_TOTAL_WIDTH / 2;

        Button cycleButton = this.addRenderableWidget(Button.builder(
                        Component.translatable(row.enemy.translationKey()),
                        button -> onCycleEnemy(rowIndex))
                .bounds(left, y, CYCLE_BUTTON_WIDTH, FIELD_HEIGHT)
                .build());
        this.rowCycleButtons.add(cycleButton);

        EditBox countField = new EditBox(this.font,
                left + CYCLE_BUTTON_WIDTH + ROW_GAP, y, COUNT_FIELD_WIDTH, FIELD_HEIGHT,
                Component.translatable("dungeon_defenders.spawner.config_count"));
        countField.setMaxLength(6);
        countField.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        countField.setValue(row.countText);
        this.addRenderableWidget(countField);
        this.rowCountFields.add(countField);

        if (this.rows.size() > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("X"), button -> onRemoveRow(rowIndex))
                    .bounds(left + CYCLE_BUTTON_WIDTH + ROW_GAP + COUNT_FIELD_WIDTH + ROW_GAP, y, REMOVE_BUTTON_WIDTH, FIELD_HEIGHT)
                    .build());
        }
    }

    /** Passe la ligne au prochain SpawnableEnemy pas déjà utilisé par une autre ligne (jamais deux lignes sur le même type). */
    private void onCycleEnemy(int rowIndex) {
        RowState row = this.rows.get(rowIndex);
        SpawnableEnemy candidate = row.enemy;
        for (int i = 0; i < SpawnableEnemy.values().length; i++) {
            candidate = candidate.next();
            if (candidate == row.enemy || !isUsedByOtherRow(rowIndex, candidate)) {
                break;
            }
        }
        row.enemy = candidate;
        this.rowCycleButtons.get(rowIndex).setMessage(Component.translatable(candidate.translationKey()));
    }

    private boolean isUsedByOtherRow(int rowIndex, SpawnableEnemy enemy) {
        for (int i = 0; i < this.rows.size(); i++) {
            if (i != rowIndex && this.rows.get(i).enemy == enemy) {
                return true;
            }
        }
        return false;
    }

    private void onAddRow() {
        syncFieldsToState();
        this.rows.add(new RowState(firstUnusedEnemy(), 0));
        this.rebuildWidgets();
    }

    private void onRemoveRow(int rowIndex) {
        syncFieldsToState();
        this.rows.remove(rowIndex);
        this.rebuildWidgets();
    }

    private SpawnableEnemy firstUnusedEnemy() {
        for (SpawnableEnemy candidate : SpawnableEnemy.values()) {
            if (this.rows.stream().noneMatch(row -> row.enemy == candidate)) {
                return candidate;
            }
        }
        // Ne devrait pas arriver : le bouton Ajouter est caché une fois la liste pleine.
        return SpawnableEnemy.values()[0];
    }

    /** Recopie les valeurs des widgets actuels dans l'état en mémoire, avant qu'un rebuild ne les détruise. */
    private void syncFieldsToState() {
        this.intervalText = this.intervalField.getValue();
        this.radiusText = this.radiusField.getValue();
        this.waveStartText = this.waveStartField.getValue();
        this.waveEndText = this.waveEndField.getValue();
        for (int i = 0; i < this.rows.size(); i++) {
            this.rows.get(i).countText = this.rowCountFields.get(i).getValue();
        }
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
    }

    private void drawLabel(GuiGraphicsExtractor guiGraphics, String key, int centerX, int fieldY) {
        guiGraphics.centeredText(this.font, Component.translatable(key), centerX, fieldY - 11, TEXT_COLOR);
    }

    private void onConfirm() {
        syncFieldsToState();

        List<SpawnerConfigPayload.Entry> entries = new ArrayList<>();
        for (RowState row : this.rows) {
            entries.add(new SpawnerConfigPayload.Entry(row.enemy.ordinal(), parseOr(row.countText, 0)));
        }

        SpawnerConfigPayload payload = new SpawnerConfigPayload(
                this.menu.pos(),
                parseOr(this.intervalText, 20),
                parseOr(this.radiusText, 0),
                parseOr(this.waveStartText, 1),
                parseOr(this.waveEndText, ModAttachments.MAX_WAVE),
                entries
        );

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

    private SpawnerBlockEntity resolveSpawner() {
        Level level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(this.menu.pos()) instanceof SpawnerBlockEntity spawner) {
            return spawner;
        }
        return null;
    }
}
