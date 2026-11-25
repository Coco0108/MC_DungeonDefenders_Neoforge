package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.ability.BurstAbility;
import com.github.c0c0tier.dungeon_defenders.ability.ChannelAbility;
import com.github.c0c0tier.dungeon_defenders.ability.HealAbility;
import com.github.c0c0tier.dungeon_defenders.ability.HeroAbility;
import com.github.c0c0tier.dungeon_defenders.ability.RepairAbility;
import com.github.c0c0tier.dungeon_defenders.client.ClientAbilityState;
import com.github.c0c0tier.dungeon_defenders.init.AbilitySlot;
import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Quatre emplacements de compétences en bas à gauche de l'écran, juste à droite des losanges
// vie/mana (voir HudLayout), au-dessus de la barre d'expérience — comme dans le jeu de
// référence : soin sur soi, sort 1 du héros, sort 2 du héros, réparation de tour, dans cet
// ordre, de gauche à droite. Même ordre que init.AbilitySlot et ModKeyMappings.ABILITY_KEYS.
//
// Chaque slot affiche l'icône de sa compétence (HealAbility/RepairAbility sont génériques ;
// spell1()/spell2() viennent du héros du joueur), et son état :
//  - en recharge (compétence en salve) : un voile sombre qui descend, et les secondes restantes ;
//  - en cours de canalisation : bordure claire ;
//  - mana insuffisant pour la lancer : emplacement entièrement grisé.
// La recharge et la canalisation viennent de ClientAbilityState ; le serveur reste seul juge.
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
    private static final int CHANNELING_BORDER_COLOR = 0xFFFFFF55;
    private static final int COOLDOWN_VEIL_COLOR = 0xC0000000;
    private static final int NO_MANA_VEIL_COLOR = 0xA0101010;
    private static final int COOLDOWN_TEXT_COLOR = 0xFFFFFFFF;
    private static final int TICKS_PER_SECOND = 20;

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
        HeroAbility[] abilities = {
                HealAbility.INSTANCE,
                hero.spell1(),
                hero.spell2(),
                RepairAbility.INSTANCE
        };
        AbilitySlot[] slots = AbilitySlot.values();
        int mana = player.getData(ModAttachments.MANA);

        for (int i = 0; i < abilities.length; i++) {
            AbilitySlot slot = slots[i];
            int centerX = firstCenterX + i * (RADIUS * 2 + GAP);
            int border = ClientAbilityState.isChanneling(slot) ? CHANNELING_BORDER_COLOR : BORDER_COLOR;
            CircleSlot.render(guiGraphics, centerX, centerY, RADIUS, FILL_COLOR, border);
            guiGraphics.item(new ItemStack(abilities[i].icon()), centerX - ICON_SIZE / 2, centerY - ICON_SIZE / 2);

            int remaining = ClientAbilityState.remainingTicks(slot);
            if (remaining > 0) {
                CircleSlot.renderVeil(guiGraphics, centerX, centerY, RADIUS,
                        ClientAbilityState.cooldownFraction(slot), COOLDOWN_VEIL_COLOR);
                // Arrondi au-dessus : "1" tant qu'il reste moins d'une seconde, jamais "0".
                String seconds = Integer.toString((remaining + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
                guiGraphics.centeredText(minecraft.font, seconds, centerX,
                        centerY - minecraft.font.lineHeight / 2, COOLDOWN_TEXT_COLOR);
            } else if (mana < manaNeeded(abilities[i])) {
                CircleSlot.renderVeil(guiGraphics, centerX, centerY, RADIUS, 1.0F, NO_MANA_VEIL_COLOR);
            }
        }
    }

    // Mana minimal pour que la compétence fasse quelque chose : son coût entier pour une salve,
    // un tick de canalisation pour une compétence maintenue.
    private static int manaNeeded(HeroAbility ability) {
        if (ability instanceof BurstAbility burst) {
            return burst.manaCost();
        }
        if (ability instanceof ChannelAbility channel) {
            return channel.manaCostPerTick();
        }
        return 0;
    }
}
