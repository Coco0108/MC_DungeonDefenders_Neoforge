package com.github.c0c0tier.dungeon_defenders.client.gui.screen;

import com.github.c0c0tier.dungeon_defenders.client.gui.LootComparison;
import com.github.c0c0tier.dungeon_defenders.client.gui.UiStyle;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.loot.Equipment;
import com.github.c0c0tier.dungeon_defenders.loot.LootRarity;
import com.github.c0c0tier.dungeon_defenders.loot.LootWeapon;
import com.github.c0c0tier.dungeon_defenders.network.ForgeActionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

// Écran de la Forge (clic droit sur le bloc). Un panneau unique, avec des onglets au-dessus :
// l'INVENTAIRE, seule façon pour le joueur de voir son butin (il n'a pas d'inventaire portatif),
// puis un onglet grisé qui marque la place des suivants (« on verra les précisions plus tard »).
//
// Onglet Inventaire :
//   - à gauche, la grille paginée (Equipment.PAGE_SIZE cases par page) : cases creusées, cadre
//     de la couleur de la rareté, armes d'un autre héros assombries, infobulle au survol ;
//   - à droite, l'arme équipée du héros actuel (avec Déséquiper), puis la case choisie, ses
//     bonus comparés à l'arme que son héros porte déjà (LootComparison), et Équiper / Détruire.
//     Détruire demande un second clic (« Confirmer ? ») : rien ne se récupère.
//   - sous la grille, le tri (récent, rareté, dégâts) et le filtre (tous les héros, ou le sien).
//     Ce n'est qu'une VUE : l'inventaire garde son ordre côté serveur, et les actions envoient
//     toujours l'index réel (view() fait la correspondance). Le choix est retenu le temps de la
//     session de jeu, pas sauvegardé.
//
// Pas de Menu : tout vient d'attachments joueur synchronisés (FORGE_INVENTORY,
// EQUIPPED_WEAPONS). Un clic envoie un paquet et l'écran se redessine quand le serveur renvoie
// l'attachment, sans rien prédire. Les cases de la grille sont dessinées à la main et cliquées
// via mouseClicked ; les actions restent de vrais boutons.
public class ForgeScreen extends Screen {

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 206;
    private static final int COLUMNS = 5;
    private static final int SLOT = 18;
    private static final int SLOT_STEP = 20;
    private static final int TAB_WIDTH = 84;
    private static final int TAB_HEIGHT = 16;
    private static final int LINE_HEIGHT = 10;
    private static final int RIGHT_WIDTH = 204;
    private static final int BOX_HEIGHT = 58;
    private static final int OTHER_HERO_VEIL = 0xA0000000;
    private static final int SELECTED_COLOR = 0xFFFFFFFF;

    // Ordre d'affichage de la grille. RECENT = dernier ramassé en premier.
    private enum Sort {
        RECENT, RARITY, DAMAGE;

        Sort next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static Sort sort = Sort.RECENT;
    private static boolean ownHeroOnly;

    private Button sortButton;
    private Button filterButton;
    private Button previousPage;
    private Button nextPage;
    private Button equipButton;
    private Button discardButton;
    private Button unequipButton;

    private int page;
    // Index ABSOLU dans l'inventaire (pas dans la page) ; -1 = aucune case choisie.
    private int selected = -1;
    // Index pour lequel Détruire attend sa confirmation ; -1 = aucun.
    private int confirmDiscard = -1;

    private int panelLeft;
    private int panelTop;
    private int gridLeft;
    private int gridTop;
    private int rightLeft;

    public ForgeScreen() {
        super(Component.translatable("dungeon_defenders.forge.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.panelLeft = (this.width - PANEL_WIDTH) / 2;
        this.panelTop = Math.max(TAB_HEIGHT + 2, (this.height - PANEL_HEIGHT) / 2 + TAB_HEIGHT / 2);
        this.gridLeft = this.panelLeft + 10;
        this.gridTop = this.panelTop + UiStyle.HEADER_HEIGHT + 18;
        this.rightLeft = this.panelLeft + PANEL_WIDTH - RIGHT_WIDTH - 10;

        int gridWidth = COLUMNS * SLOT_STEP - 2;
        int pagerY = this.gridTop + (Equipment.PAGE_SIZE / COLUMNS) * SLOT_STEP + 4;
        this.previousPage = this.addRenderableWidget(Button.builder(Component.literal("<"), b -> changePage(-1))
                .bounds(this.gridLeft, pagerY, 16, 16).build());
        this.nextPage = this.addRenderableWidget(Button.builder(Component.literal(">"), b -> changePage(1))
                .bounds(this.gridLeft + gridWidth - 16, pagerY, 16, 16).build());
        this.sortButton = this.addRenderableWidget(Button.builder(sortLabel(), b -> {
                    sort = sort.next();
                    resetView();
                })
                .bounds(this.gridLeft, pagerY + 20, gridWidth, 14).build());
        this.filterButton = this.addRenderableWidget(Button.builder(filterLabel(), b -> {
                    ownHeroOnly = !ownHeroOnly;
                    resetView();
                })
                .bounds(this.gridLeft, pagerY + 37, gridWidth, 14).build());

        int equippedTop = this.panelTop + UiStyle.HEADER_HEIGHT + 18;
        this.unequipButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.forge.unequip"), b -> send(ForgeActionPayload.UNEQUIP, 0))
                .bounds(this.rightLeft + RIGHT_WIDTH - 70, equippedTop - 14, 70, 12).build());

        int actionsY = this.panelTop + PANEL_HEIGHT - 25;
        this.equipButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.forge.equip"), b -> equipSelected())
                .bounds(this.rightLeft, actionsY, 100, 20).build());
        this.discardButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.forge.discard"), b -> onDiscard())
                .bounds(this.rightLeft + RIGHT_WIDTH - 100, actionsY, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(this.gridLeft, actionsY, gridWidth, 20).build());
        refresh();
    }

    // Le serveur échange l'arme choisie avec l'ancienne SUR PLACE : la case garde son index mais
    // contient désormais une autre arme. On désarme donc une confirmation de Détruire en cours,
    // sinon un clic de plus détruirait l'ancienne arme sans nouvelle confirmation.
    private void equipSelected() {
        send(ForgeActionPayload.EQUIP, this.selected);
        this.confirmDiscard = -1;
    }

    private void onDiscard() {
        if (this.confirmDiscard != this.selected) {
            this.confirmDiscard = this.selected;
            return;
        }
        send(ForgeActionPayload.DISCARD, this.selected);
        select(-1);
    }

    private void select(int index) {
        this.selected = index;
        this.confirmDiscard = -1;
    }

    private void changePage(int delta) {
        this.page = Math.max(0, Math.min(pageCount() - 1, this.page + delta));
        select(-1);
    }

    private void resetView() {
        this.page = 0;
        select(-1);
        this.sortButton.setMessage(sortLabel());
        this.filterButton.setMessage(filterLabel());
    }

    private static Component sortLabel() {
        return Component.translatable("dungeon_defenders.forge.sort",
                Component.translatable("dungeon_defenders.forge.sort." + sort.name().toLowerCase(Locale.ROOT)));
    }

    private static Component filterLabel() {
        return Component.translatable(ownHeroOnly ? "dungeon_defenders.forge.filter.own" : "dungeon_defenders.forge.filter.all");
    }

    /** Index réels de l'inventaire, dans l'ordre d'affichage, après filtre. */
    private static List<Integer> view(LocalPlayer player) {
        List<LootWeapon> items = Equipment.inventory(player);
        String heroId = HeroDefinition.of(player).id();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (!ownHeroOnly || items.get(i).hero().equals(heroId)) {
                indices.add(i);
            }
        }
        Comparator<Integer> newestFirst = Comparator.<Integer>naturalOrder().reversed();
        Comparator<Integer> byDamage = Comparator.comparingDouble((Integer i) -> items.get(i).damage()).reversed();
        switch (sort) {
            case RECENT -> indices.sort(newestFirst);
            case RARITY -> indices.sort(Comparator.comparingInt((Integer i) -> items.get(i).lootRarity().ordinal()).reversed()
                    .thenComparing(byDamage).thenComparing(newestFirst));
            case DAMAGE -> indices.sort(byDamage.thenComparing(newestFirst));
        }
        return indices;
    }

    private int pageCount() {
        LocalPlayer player = Minecraft.getInstance().player;
        int shown = player == null ? 0 : view(player).size();
        return Math.max(1, (shown + Equipment.PAGE_SIZE - 1) / Equipment.PAGE_SIZE);
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
            select(-1);
        }
        this.page = Math.min(this.page, pageCount() - 1);
        boolean outOfCombat = player.level().getData(ModAttachments.GAME_PHASE) != GamePhase.COMBAT.ordinal();

        this.previousPage.active = this.page > 0;
        this.nextPage.active = this.page < pageCount() - 1;

        LootWeapon chosen = this.selected >= 0 ? items.get(this.selected) : null;
        this.equipButton.visible = chosen != null;
        this.discardButton.visible = chosen != null;
        this.equipButton.active = outOfCombat && chosen != null && chosen.heroDefinition() == HeroDefinition.of(player);
        this.discardButton.active = outOfCombat;
        this.discardButton.setMessage(this.confirmDiscard >= 0 && this.confirmDiscard == this.selected
                ? Component.translatable("dungeon_defenders.forge.discard_confirm").withStyle(ChatFormatting.RED)
                : Component.translatable("dungeon_defenders.forge.discard"));
        this.unequipButton.visible = Equipment.equipped(player) != null;
        this.unequipButton.active = outOfCombat && items.size() < Equipment.CAPACITY;
    }

    // Index RÉEL de l'arme sous la souris, -1 sur une case vide ou hors de la grille.
    private int slotIndexAt(double mouseX, double mouseY) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return -1;
        }
        List<Integer> view = view(player);
        for (int i = 0; i < Equipment.PAGE_SIZE; i++) {
            int x = this.gridLeft + (i % COLUMNS) * SLOT_STEP;
            int y = this.gridTop + (i / COLUMNS) * SLOT_STEP;
            int position = this.page * Equipment.PAGE_SIZE + i;
            if (UiStyle.isInside(mouseX, mouseY, x, y, SLOT, SLOT)) {
                return position < view.size() ? view.get(position) : -1;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        LocalPlayer player = Minecraft.getInstance().player;
        int index = slotIndexAt(event.x(), event.y());
        if (player != null && index >= 0 && index < Equipment.inventory(player).size()) {
            select(index);
            // Double-clic sur une arme de son héros : l'équiper tout de suite.
            if (doubleClick && this.equipButton.active) {
                equipSelected();
            }
            refresh();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            super.extractRenderState(g, mouseX, mouseY, partialTick);
            return;
        }
        // Le panneau d'abord, les boutons (super) par-dessus.
        renderTabs(g);
        HeroDefinition hero = HeroDefinition.of(player);
        UiStyle.titledPanel(g, this.font, Component.translatable("dungeon_defenders.forge.title_hero", this.getTitle(), hero.displayName()),
                this.panelLeft, this.panelTop, PANEL_WIDTH, PANEL_HEIGHT);
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        List<LootWeapon> items = Equipment.inventory(player);
        renderGrid(g, items, view(player), hero, mouseX, mouseY);
        renderEquipped(g, player, hero);
        renderSelection(g, player, items);
    }

    // Onglet actif collé au panneau (même fond, pas de bord bas), les autres en retrait.
    private void renderTabs(GuiGraphicsExtractor g) {
        int y = this.panelTop - TAB_HEIGHT + 2;
        int x = this.panelLeft + 8;
        UiStyle.panel(g, x, y, TAB_WIDTH, TAB_HEIGHT + 2);
        g.centeredText(this.font, Component.translatable("dungeon_defenders.forge.tab.inventory"), x + TAB_WIDTH / 2, y + 5, UiStyle.GOLD);

        int soon = x + TAB_WIDTH + 2;
        UiStyle.inset(g, soon, y + 2, TAB_WIDTH, TAB_HEIGHT - 2);
        g.centeredText(this.font, Component.translatable("dungeon_defenders.forge.tab.soon"), soon + TAB_WIDTH / 2, y + 6, UiStyle.DIM);
    }

    private void renderGrid(GuiGraphicsExtractor g, List<LootWeapon> items, List<Integer> view, HeroDefinition hero, int mouseX, int mouseY) {
        int gridWidth = COLUMNS * SLOT_STEP - 2;
        g.text(this.font, Component.translatable("dungeon_defenders.forge.tab.inventory"), this.gridLeft, this.gridTop - 12, UiStyle.GOLD);
        Component count = Component.translatable("dungeon_defenders.forge.count", items.size(), Equipment.CAPACITY);
        g.text(this.font, count, this.gridLeft + gridWidth - this.font.width(count), this.gridTop - 12, UiStyle.DIM);

        int hovered = slotIndexAt(mouseX, mouseY);
        for (int i = 0; i < Equipment.PAGE_SIZE; i++) {
            int position = this.page * Equipment.PAGE_SIZE + i;
            int x = this.gridLeft + (i % COLUMNS) * SLOT_STEP;
            int y = this.gridTop + (i / COLUMNS) * SLOT_STEP;
            if (position >= view.size()) {
                UiStyle.inset(g, x, y, SLOT, SLOT);
                continue;
            }
            int index = view.get(position);
            LootWeapon weapon = items.get(index);
            LootRarity rarity = weapon.lootRarity();
            int frame = rarity == LootRarity.COMMON ? 0 : rarity.argb();
            UiStyle.itemSlot(g, iconOf(weapon), x, y, frame, index == hovered);
            if (weapon.heroDefinition() != hero) {
                g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, OTHER_HERO_VEIL);
            }
            if (index == this.selected) {
                g.outline(x - 1, y - 1, SLOT + 2, SLOT + 2, SELECTED_COLOR);
            }
        }

        int pagerY = this.gridTop + (Equipment.PAGE_SIZE / COLUMNS) * SLOT_STEP + 4;
        g.centeredText(this.font, Component.translatable("dungeon_defenders.forge.page", this.page + 1, pageCount()),
                this.gridLeft + gridWidth / 2, pagerY + 4, UiStyle.TEXT);

        if (view.isEmpty()) {
            g.textWithWordWrap(this.font, Component.translatable(items.isEmpty() ? "dungeon_defenders.forge.empty" : "dungeon_defenders.forge.filtered_empty"),
                    this.gridLeft + 2, this.gridTop + 2, gridWidth - 4, UiStyle.DIM);
        }

        if (hovered >= 0 && hovered < items.size()) {
            LootWeapon weapon = items.get(hovered);
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(weapon.displayName());
            tooltip.addAll(weapon.statLines());
            HeroDefinition owner = weapon.heroDefinition();
            if (owner != null && owner != hero) {
                tooltip.add(Component.translatable("dungeon_defenders.forge.other_hero", owner.displayName()).withStyle(ChatFormatting.GRAY));
            }
            g.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
        }
    }

    private void renderEquipped(GuiGraphicsExtractor g, LocalPlayer player, HeroDefinition hero) {
        int top = this.panelTop + UiStyle.HEADER_HEIGHT + 18;
        g.text(this.font, Component.translatable("dungeon_defenders.forge.equipped"), this.rightLeft, top - 12, UiStyle.GOLD);
        LootWeapon equipped = Equipment.equipped(player);
        UiStyle.inset(g, this.rightLeft, top, RIGHT_WIDTH, BOX_HEIGHT);
        UiStyle.itemSlot(g, new ItemStack(hero.weapon()), this.rightLeft + 4, top + 4,
                equipped == null || equipped.lootRarity() == LootRarity.COMMON ? 0 : equipped.lootRarity().argb(), false);
        int textLeft = this.rightLeft + 28;
        if (equipped == null) {
            g.text(this.font, new ItemStack(hero.weapon()).getHoverName(), textLeft, top + 5, UiStyle.TEXT);
            g.text(this.font, Component.translatable("dungeon_defenders.forge.base_weapon"), textLeft, top + 5 + LINE_HEIGHT, UiStyle.DIM);
            return;
        }
        g.text(this.font, equipped.displayName(), textLeft, top + 5, UiStyle.TEXT);
        int y = top + 5 + LINE_HEIGHT;
        for (Component line : LootComparison.lines(equipped, null, false)) {
            g.text(this.font, line, textLeft, y, UiStyle.TEXT);
            y += LINE_HEIGHT;
        }
    }

    private void renderSelection(GuiGraphicsExtractor g, LocalPlayer player, List<LootWeapon> items) {
        int top = this.panelTop + UiStyle.HEADER_HEIGHT + 18 + BOX_HEIGHT + 18;
        g.text(this.font, Component.translatable("dungeon_defenders.forge.selection"), this.rightLeft, top - 12, UiStyle.GOLD);
        UiStyle.inset(g, this.rightLeft, top, RIGHT_WIDTH, BOX_HEIGHT);
        LootWeapon chosen = this.selected >= 0 && this.selected < items.size() ? items.get(this.selected) : null;
        if (chosen == null) {
            g.textWithWordWrap(this.font, Component.translatable("dungeon_defenders.forge.select_hint"),
                    this.rightLeft + 6, top + 6, RIGHT_WIDTH - 12, UiStyle.DIM);
            return;
        }
        LootRarity rarity = chosen.lootRarity();
        UiStyle.itemSlot(g, iconOf(chosen), this.rightLeft + 4, top + 4, rarity == LootRarity.COMMON ? 0 : rarity.argb(), false);
        int textLeft = this.rightLeft + 28;
        g.text(this.font, chosen.displayName(), textLeft, top + 5, UiStyle.TEXT);
        int y = top + 5 + LINE_HEIGHT;
        @Nullable LootWeapon reference = LootComparison.reference(player, chosen);
        for (Component line : LootComparison.lines(chosen, reference, true)) {
            g.text(this.font, line, textLeft, y, UiStyle.TEXT);
            y += LINE_HEIGHT;
        }
        HeroDefinition owner = chosen.heroDefinition();
        if (owner != null && owner != HeroDefinition.of(player)) {
            g.text(this.font, Component.translatable("dungeon_defenders.forge.other_hero", owner.displayName()),
                    this.rightLeft + 6, top + BOX_HEIGHT + 2, UiStyle.NEGATIVE);
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
