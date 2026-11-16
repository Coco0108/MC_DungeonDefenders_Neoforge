package com.github.c0c0tier.dungeon_defenders.client.gui.screen;

import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
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

// Écran des caractéristiques (touche K) : les points à répartir, une ligne par caractéristique
// avec ses points, son effet actuel et un bouton "+", et un bouton pour tout reprendre.
//
// Pas de Menu, comme HeroSelectionScreen : tout ce qu'il affiche vient d'attachments joueur déjà
// synchronisés (LEVEL, HERO_STATS). Un clic envoie un paquet ; l'affichage se met à jour quand
// le serveur renvoie l'attachment, sans rien prédire côté client.
public class HeroStatsScreen extends Screen {

    private static final int ROW_HEIGHT = 26;
    private static final int PANEL_WIDTH = 260;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DIM_COLOR = 0xFFAAAAAA;
    private static final int POINTS_COLOR = 0xFFFFD700;

    private final List<Button> plusButtons = new ArrayList<>();
    private Button resetButton;
    private int left;
    private int top;

    public HeroStatsScreen() {
        super(Component.translatable("dungeon_defenders.stats.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.plusButtons.clear();
        this.left = this.width / 2 - PANEL_WIDTH / 2;
        this.top = 70;

        for (HeroStat stat : HeroStat.values()) {
            int y = this.top + stat.ordinal() * ROW_HEIGHT;
            this.plusButtons.add(this.addRenderableWidget(Button.builder(Component.literal("+"), b -> send(stat.ordinal()))
                    .bounds(this.left + PANEL_WIDTH - 20, y, 20, 20)
                    .build()));
        }

        int bottom = this.top + HeroStat.values().length * ROW_HEIGHT + 12;
        this.resetButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.stats.reset"), b -> send(SpendStatPointPayload.RESET))
                .bounds(this.left, bottom, 120, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(this.left + PANEL_WIDTH - 120, bottom, 120, 20)
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
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        guiGraphics.centeredText(this.font, this.getTitle(), this.width / 2, 24, TEXT_COLOR);
        guiGraphics.centeredText(this.font,
                Component.translatable("dungeon_defenders.stats.available", HeroStat.available(player)),
                this.width / 2, 42, POINTS_COLOR);

        for (HeroStat stat : HeroStat.values()) {
            int y = this.top + stat.ordinal() * ROW_HEIGHT;
            guiGraphics.text(this.font,
                    Component.translatable("dungeon_defenders.stats.row", stat.displayName(), stat.points(player), HeroStat.maxPoints(player)),
                    this.left, y + 1, TEXT_COLOR);
            guiGraphics.text(this.font,
                    Component.translatable("dungeon_defenders.stat." + stat.id() + ".effect", effectValue(stat, player)),
                    this.left, y + 11, DIM_COLOR);
        }
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
