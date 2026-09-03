package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.ability.HealAbility;
import com.github.c0c0tier.dungeon_defenders.ability.RepairAbility;
import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Quatre emplacements de compétences en bas à gauche de l'écran, juste à droite des losanges
// vie/mana (voir HudLayout), au-dessus de la barre d'expérience — comme dans le jeu de
// référence : soin sur soi, sort 1 du héros, sort 2 du héros, réparation de tour, dans cet
// ordre, de gauche à droite. Même ordre que init.AbilitySlot et ModKeyMappings.ABILITY_KEYS.
//
// Chaque slot affiche l'icône de sa compétence (HealAbility/RepairAbility sont génériques ;
// spell1()/spell2() viennent du héros du joueur). **Ce que ce HUD ne fait PAS encore** :
// aucun indicateur de recharge (Circular Slice), aucune mise en avant visuelle pendant une
// canalisation, aucun grisé quand le mana manque — polish volontairement reporté, voir
// 05-etat-et-problemes-connus.md.
public class AbilitySlotsOverlay implements GuiLayer {

    private static final int RADIUS = 14;
    private static final int GAP = 4;
    // Espace entre le losange mana, le plus à droite du groupe (HudLayout), et le premier
    // slot.
    private static final int GROUP_GAP = 10;
    private static final int FILL_COLOR = 0xFF2B2B2B;
    private static final int BORDER_COLOR = 0xFF000000;
    // Les icônes d'item vanilla font 16px ; centrées dans le cercle en soustrayant la moitié.
    private static final int ICON_SIZE = 16;

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

        int manaDiamondRight = HudLayout.MARGIN + HudLayout.DIAMOND_RADIUS * 4 + HudLayout.DIAMOND_GAP;
        int firstCenterX = manaDiamondRight + GROUP_GAP + RADIUS;

        HeroDefinition hero = HeroDefinition.of(player);
        Item[] icons = {
                HealAbility.INSTANCE.icon(),
                hero.spell1().icon(),
                hero.spell2().icon(),
                RepairAbility.INSTANCE.icon()
        };

        for (int i = 0; i < icons.length; i++) {
            int centerX = firstCenterX + i * (RADIUS * 2 + GAP);
            CircleSlot.render(guiGraphics, centerX, centerY, RADIUS, FILL_COLOR, BORDER_COLOR);
            guiGraphics.item(new ItemStack(icons[i]), centerX - ICON_SIZE / 2, centerY - ICON_SIZE / 2);
        }
    }
}
