package com.github.c0c0tier.dungeon_defenders.client.gui.screen;

import com.github.c0c0tier.dungeon_defenders.client.gui.UiStyle;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import com.github.c0c0tier.dungeon_defenders.init.HeroStat;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.network.SpendStatPointPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Écran des caractéristiques (touche K), dans un panneau : en tête le niveau et les points à
// répartir ; puis une rangée par caractéristique, avec son nom, ses points en jauge, son effet
// actuel et un bouton "+" ; en bas, Tout reprendre et Terminé.
//
// Pas de Menu, comme HeroSelectionScreen : tout ce qu'il affiche vient d'attachments joueur déjà
// synchronisés (LEVEL, HERO_STATS). Un clic envoie un paquet ; l'affichage se met à jour quand
// le serveur renvoie l'attachment, sans rien prédire côté client.
public class HeroStatsScreen extends Screen {

    private static final int PANEL_WIDTH = 280;
    private static final int ROW_HEIGHT = 30;
    private static final int PAD = 10;
    private static final int BAR_WIDTH = 110;

    private final List<Button> plusButtons = new ArrayList<>();
    private Button resetButton;
    private int left;
    private int top;
    private int rowsTop;

    public HeroStatsScreen() {
        super(Component.translatable("dungeon_defenders.stats.title"));
    }

    private static int panelHeight() {
        return UiStyle.HEADER_HEIGHT + 30 + HeroStat.values().length * ROW_HEIGHT + 30;
    }

    @Override
    protected void init() {
        super.init();
        this.plusButtons.clear();
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = Math.max(4, (this.height - panelHeight()) / 2);
        this.rowsTop = this.top + UiStyle.HEADER_HEIGHT + 30;

        for (HeroStat stat : HeroStat.values()) {
            int y = this.rowsTop + stat.ordinal() * ROW_HEIGHT + 3;
            this.plusButtons.add(this.addRenderableWidget(Button.builder(Component.literal("+"), b -> send(stat.ordinal()))
                    .bounds(this.left + PANEL_WIDTH - PAD - 20, y, 20, 20)
                    .build()));
        }

        int bottom = this.top + panelHeight() - 26;
        this.resetButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.stats.reset"), b -> send(SpendStatPointPayload.RESET))
                .bounds(this.left + PAD, bottom, 110, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(this.left + PANEL_WIDTH - PAD - 110, bottom, 110, 20)
                .build());
        refreshButtons();
    }

    private static void send(int statOrdinal) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new SpendStatPointPayload(statOrdinal).toVanillaServerbound());
        }
    }

    @Override
    public void tick() {
        super.tick();
        refreshButtons();
    }

    // Le serveur revérifie tout ; griser les boutons évite seulement des clics sans effet.
    private void refreshButtons() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        boolean canSpend = HeroStat.available(player) > 0;
        int maxPoints = HeroStat.maxPoints(player);
        for (HeroStat stat : HeroStat.values()) {
            this.plusButtons.get(stat.ordinal()).active = canSpend && stat.points(player) < maxPoints;
        }
        boolean inCombat = player.level().getData(ModAttachments.GAME_PHASE) == GamePhase.COMBAT.ordinal();
        this.resetButton.active = HeroStat.spent(player) > 0 && !inCombat;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            super.extractRenderState(g, mouseX, mouseY, partialTick);
            return;
        }
        UiStyle.titledPanel(g, this.font, this.getTitle(), this.left, this.top, PANEL_WIDTH, panelHeight());

        // En-tête : héros et niveau à gauche, points à répartir à droite (doré s'il en reste).
        int headerY = this.top + UiStyle.HEADER_HEIGHT + 8;
        g.text(this.font, Component.translatable("dungeon_defenders.stats.hero_level",
                HeroDefinition.of(player).displayName(), player.getData(ModAttachments.LEVEL)), this.left + PAD, headerY, UiStyle.TEXT);
        int available = HeroStat.available(player);
        Component pointsText = Component.translatable("dungeon_defenders.stats.available", available);
        g.text(this.font, pointsText, this.left + PANEL_WIDTH - PAD - this.font.width(pointsText), headerY,
                available > 0 ? UiStyle.GOLD : UiStyle.DIM);
        UiStyle.divider(g, this.left + PAD, headerY + 13, PANEL_WIDTH - 2 * PAD);

        int maxPoints = Math.max(1, HeroStat.maxPoints(player));
        for (HeroStat stat : HeroStat.values()) {
            int y = this.rowsTop + stat.ordinal() * ROW_HEIGHT;
            if (stat.ordinal() % 2 == 1) {
                g.fill(this.left + 4, y, this.left + PANEL_WIDTH - 4, y + ROW_HEIGHT - 2, 0x18FFFFFF);
            }
            int points = stat.points(player);
            g.text(this.font, stat.displayName(), this.left + PAD, y + 4, UiStyle.TEXT);
            int barLeft = this.left + PANEL_WIDTH - PAD - 26 - BAR_WIDTH;
            UiStyle.bar(g, barLeft, y + 2, BAR_WIDTH, 11, (double) points / maxPoints, 0xFFB08A3A);
            g.centeredText(this.font, Component.literal(points + " / " + maxPoints), barLeft + BAR_WIDTH / 2, y + 4, UiStyle.TEXT);
            g.text(this.font,
                    Component.translatable("dungeon_defenders.stat." + stat.id() + ".effect", effectValue(stat, player)),
                    this.left + PAD, y + 16, UiStyle.DIM);
        }

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    // La Vitalité s'exprime en PV, les autres en pourcentage.
    private static String effectValue(HeroStat stat, LocalPlayer player) {
        double bonus = stat.bonus(player);
        if (stat == HeroStat.VITALITY) {
            return String.format(Locale.ROOT, "%.0f", bonus);
        }
        return String.format(Locale.ROOT, "%.0f", bonus * 100.0D);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
