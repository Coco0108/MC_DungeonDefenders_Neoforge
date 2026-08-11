package com.github.c0c0tier.dungeon_defenders.client.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Quatre emplacements de compétences en bas à gauche de l'écran, juste à droite des losanges
// mana/vie (voir HudLayout), au-dessus de la barre d'expérience — comme dans le jeu de
// référence : soin sur soi, sort 1 du héros, sort 2 du héros, réparation de tour, dans cet
// ordre, de gauche à droite. Purement visuel pour l'instant : pas d'icône (viendront plus
// tard, une par slot), pas de clic, pas de cooldown, pas de consommation de mana. Voir
// 05-etat-et-problemes-connus.md.
public class AbilitySlotsOverlay implements GuiLayer {
    // Un slot par future compétence, dans l'ordre d'affichage gauche -> droite. Le contenu
    // (nom) ne sert encore à rien à l'exécution : il documente juste l'ordre attendu, en
    // attendant les vraies icônes et leur logique.
    private static final String[] SLOT_NAMES = {
            "self_heal", "hero_spell_1", "hero_spell_2", "repair_tower"
    };

    private static final int RADIUS = 14;
    private static final int GAP = 4;
    // Espace entre le losange vie (HudLayout) et le premier slot.
    private static final int GROUP_GAP = 10;
    private static final int FILL_COLOR = 0xFF2B2B2B;
    private static final int BORDER_COLOR = 0xFF000000;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }

        // Même ligne de base que les losanges mana/vie (leur pointe basse), pour rester
        // aligné avec eux plutôt que de flotter à une hauteur différente.
        int bottomY = ExperienceOverlay.barTop(guiGraphics) - HudLayout.ROW_GAP;
        int centerY = bottomY - RADIUS;

        int healthDiamondRight = HudLayout.MARGIN + HudLayout.DIAMOND_RADIUS * 4 + HudLayout.DIAMOND_GAP;
        int firstCenterX = healthDiamondRight + GROUP_GAP + RADIUS;

        for (int i = 0; i < SLOT_NAMES.length; i++) {
            int centerX = firstCenterX + i * (RADIUS * 2 + GAP);
            CircleSlot.render(guiGraphics, centerX, centerY, RADIUS, FILL_COLOR, BORDER_COLOR);
        }
    }
}
