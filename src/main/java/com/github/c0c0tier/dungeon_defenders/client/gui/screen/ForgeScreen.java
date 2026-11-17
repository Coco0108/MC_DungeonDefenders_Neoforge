package com.github.c0c0tier.dungeon_defenders.client.gui.screen;

import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.loot.Equipment;
import com.github.c0c0tier.dungeon_defenders.loot.LootWeapon;
import com.github.c0c0tier.dungeon_defenders.network.ForgeActionPayload;
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

// Écran de la Forge (clic droit sur le bloc). Une rangée d'onglets, dont un seul existe pour
// l'instant : l'INVENTAIRE, la seule façon pour le joueur de voir son butin (il n'a pas
// d'inventaire portatif). Les autres onglets viendront — « on verra les précisions plus tard ».
//
// Onglet Inventaire : à gauche une grille paginée (Equipment.PAGE_SIZE cases par page, cadre de
// la couleur de la rareté) ; à droite l'arme équipée du héros actuel, puis le détail de la case
// choisie avec ses actions (Équiper, Détruire).
//
// Pas de Menu : tout vient d'attachments joueur synchronisés (FORGE_INVENTORY,
// EQUIPPED_WEAPONS). Un clic envoie un paquet et l'écran se redessine quand le serveur renvoie
// l'attachment, sans rien prédire. Les cases sont de vrais boutons (clic et clavier gérés par
// le jeu), sur lesquels l'icône de l'arme est dessinée.
public class ForgeScreen extends Screen {

    private static final int COLUMNS = 5;
    private static final int CELL = 22;
    private static final int CELL_BUTTON = 20;
    private static final int GRID_TOP = 78;
    private static final int LINE_HEIGHT = 10;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DIM_COLOR = 0xFFAAAAAA;
    private static final int TITLE_COLOR = 0xFFFFD700;
    private static final int SELECTED_COLOR = 0xFFFFFFFF;

    private final List<Button> cells = new ArrayList<>();
    private Button previousPage;
    private Button nextPage;
    private Button equipButton;
    private Button discardButton;
    private Button unequipButton;

    private int page;
    // Index ABSOLU dans l'inventaire (pas dans la page) ; -1 = aucune case choisie.
    private int selected = -1;
    private int gridLeft;
    private int panelLeft;

    public ForgeScreen() {
        super(Component.translatable("dungeon_defenders.forge.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.cells.clear();
        this.gridLeft = this.width / 2 - 170;
        this.panelLeft = this.width / 2 - 40;

        // Onglets. Le second n'est là que pour montrer où les suivants prendront place.
        Button inventoryTab = this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.forge.tab.inventory"), b -> { })
                .bounds(this.gridLeft, 44, 90, 20).build());
        inventoryTab.active = false;
        Button soonTab = this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.forge.tab.soon"), b -> { })
                .bounds(this.gridLeft + 94, 44, 90, 20).build());
        soonTab.active = false;

        for (int i = 0; i < Equipment.PAGE_SIZE; i++) {
            int slot = i;
            int x = this.gridLeft + (i % COLUMNS) * CELL;
            int y = GRID_TOP + (i / COLUMNS) * CELL;
            this.cells.add(this.addRenderableWidget(Button.builder(Component.empty(), b -> this.selected = this.page * Equipment.PAGE_SIZE + slot)
                    .bounds(x, y, CELL_BUTTON, CELL_BUTTON).build()));
        }

        int pagerY = GRID_TOP + (Equipment.PAGE_SIZE / COLUMNS) * CELL + 4;
        this.previousPage = this.addRenderableWidget(Button.builder(Component.literal("<"), b -> changePage(-1))
                .bounds(this.gridLeft, pagerY, 20, 20).build());
        this.nextPage = this.addRenderableWidget(Button.builder(Component.literal(">"), b -> changePage(1))
                .bounds(this.gridLeft + COLUMNS * CELL - 22, pagerY, 20, 20).build());

        this.unequipButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.forge.unequip"), b -> send(ForgeActionPayload.UNEQUIP, 0))
                .bounds(this.panelLeft, GRID_TOP + 62, 100, 20).build());
        this.equipButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.forge.equip"), b -> send(ForgeActionPayload.EQUIP, this.selected))
                .bounds(this.panelLeft, GRID_TOP + 152, 100, 20).build());
        this.discardButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.forge.discard"), b -> {
                            send(ForgeActionPayload.DISCARD, this.selected);
                            this.selected = -1;
                        })
                .bounds(this.panelLeft + 104, GRID_TOP + 152, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20).build());
        refresh();
    }

    private void changePage(int delta) {
        this.page = Math.max(0, Math.min(pageCount() - 1, this.page + delta));
        this.selected = -1;
    }

    private static int pageCount() {
        return Equipment.CAPACITY / Equipment.PAGE_SIZE;
    }

    private static void send(int action, int index) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null && index >= 0) {
            connection.send(new ForgeActionPayload(action, index).toVanillaServerbound());
        }
    }

    @Override
    public void tick() {
        super.tick();
        refresh();
    }

    // Le serveur revérifie tout ; cacher et griser évite seulement des clics sans effet.
    private void refresh() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        List<LootWeapon> items = Equipment.inventory(player);
        if (this.selected >= items.size()) {
            this.selected = -1;
        }
        boolean outOfCombat = player.level().getData(ModAttachments.GAME_PHASE) != GamePhase.COMBAT.ordinal();

        for (int i = 0; i < this.cells.size(); i++) {
            this.cells.get(i).visible = this.page * Equipment.PAGE_SIZE + i < items.size();
        }
        this.previousPage.active = this.page > 0;
        this.nextPage.active = this.page < pageCount() - 1;

        LootWeapon chosen = this.selected >= 0 ? items.get(this.selected) : null;
        this.equipButton.visible = chosen != null;
        this.discardButton.visible = chosen != null;
        this.equipButton.active = outOfCombat && chosen != null && chosen.heroDefinition() == HeroDefinition.of(player);
        this.discardButton.active = outOfCombat;
        this.unequipButton.visible = Equipment.equipped(player) != null;
        this.unequipButton.active = outOfCombat && items.size() < Equipment.CAPACITY;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        List<LootWeapon> items = Equipment.inventory(player);

        guiGraphics.centeredText(this.font, this.getTitle(), this.width / 2, 20, TEXT_COLOR);

        // Grille : icône et cadre de rareté par-dessus chaque bouton visible.
        for (int i = 0; i < Equipment.PAGE_SIZE; i++) {
            int index = this.page * Equipment.PAGE_SIZE + i;
            if (index >= items.size()) {
                break;
            }
            LootWeapon weapon = items.get(index);
            int x = this.gridLeft + (i % COLUMNS) * CELL;
            int y = GRID_TOP + (i / COLUMNS) * CELL;
            guiGraphics.item(iconOf(weapon), x + 2, y + 2);
            guiGraphics.outline(x, y, CELL_BUTTON, CELL_BUTTON, weapon.lootRarity().argb());
            if (index == this.selected) {
                guiGraphics.outline(x - 1, y - 1, CELL_BUTTON + 2, CELL_BUTTON + 2, SELECTED_COLOR);
            }
        }
        if (items.isEmpty()) {
            guiGraphics.textWithWordWrap(this.font, Component.translatable("dungeon_defenders.forge.empty"),
                    this.gridLeft, GRID_TOP + 4, COLUMNS * CELL, DIM_COLOR);
        }

        int pagerY = GRID_TOP + (Equipment.PAGE_SIZE / COLUMNS) * CELL + 4;
        guiGraphics.centeredText(this.font, Component.translatable("dungeon_defenders.forge.page", this.page + 1, pageCount()),
                this.gridLeft + COLUMNS * CELL / 2, pagerY + 6, TEXT_COLOR);
        guiGraphics.text(this.font, Component.translatable("dungeon_defenders.forge.count", items.size(), Equipment.CAPACITY),
                this.gridLeft, pagerY + 26, DIM_COLOR);

        // Arme équipée du héros actuel.
        guiGraphics.text(this.font,
                Component.translatable("dungeon_defenders.forge.equipped", HeroDefinition.of(player).displayName()),
                this.panelLeft, GRID_TOP, TITLE_COLOR);
        LootWeapon equipped = Equipment.equipped(player);
        if (equipped == null) {
            guiGraphics.text(this.font, Component.translatable("dungeon_defenders.forge.base_weapon"),
                    this.panelLeft, GRID_TOP + 12, DIM_COLOR);
        } else {
            describe(guiGraphics, equipped, GRID_TOP + 12);
        }

        // Détail de la case choisie.
        if (this.selected >= 0 && this.selected < items.size()) {
            describe(guiGraphics, items.get(this.selected), GRID_TOP + 96);
        }
    }

    // Nom (dans la couleur de la rareté) puis une ligne par bonus : cinq lignes au plus.
    private void describe(GuiGraphicsExtractor guiGraphics, LootWeapon weapon, int top) {
        guiGraphics.text(this.font, weapon.displayName(), this.panelLeft, top, TEXT_COLOR);
        int y = top + LINE_HEIGHT;
        for (Component line : weapon.statLines()) {
            guiGraphics.text(this.font, line, this.panelLeft + 6, y, TEXT_COLOR);
            y += LINE_HEIGHT;
        }
    }

    // L'item de l'arme du héros concerné ; un simple ItemStack neuf, sans nom ni données.
    private static ItemStack iconOf(LootWeapon weapon) {
        HeroDefinition owner = weapon.heroDefinition();
        return new ItemStack(owner == null ? HeroDefinition.DEFAULT.weapon() : owner.weapon());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
