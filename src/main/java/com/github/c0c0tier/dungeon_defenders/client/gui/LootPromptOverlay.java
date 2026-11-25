package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.client.LootPickupClientEvents;
import com.github.c0c0tier.dungeon_defenders.client.ModKeyMappings;
import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import com.github.c0c0tier.dungeon_defenders.loot.LootWeapon;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.GuiLayer;

import java.util.List;

// Affiché quand un objet de butin est à portée de ramassage (LootPickupClientEvents) : un
// panneau sous le viseur, bordé de la couleur de sa rareté, avec l'icône, le nom, les bonus
// comparés à l'arme que son héros porte déjà (LootComparison : écart vert ou rouge), et les deux
// touches. Si l'arme est celle d'un autre héros, seule la touche Ranger est proposée (le serveur
// refuserait de l'équiper).
public class LootPromptOverlay implements GuiLayer {

    private static final int TOP_OFFSET = 18;
    private static final int LINE_HEIGHT = 10;
    private static final int PAD = 5;
    private static final int MIN_WIDTH = 150;

    @Override
    public void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LootPickupClientEvents.Target target = LootPickupClientEvents.target();
        LocalPlayer player = minecraft.player;
        if (target == null || player == null || minecraft.options.hideGui) {
            return;
        }
        Font font = minecraft.font;
        LootWeapon weapon = target.weapon();
        HeroDefinition owner = weapon.heroDefinition();
        boolean ownHero = owner == HeroDefinition.of(player);

        List<Component> bonuses = LootComparison.lines(weapon, LootComparison.reference(player, weapon), true);
        Component otherHero = owner != null && !ownHero
                ? Component.translatable("dungeon_defenders.forge.other_hero", owner.displayName())
                : null;

        int width = Math.max(MIN_WIDTH, font.width(weapon.displayName()) + 24 + 2 * PAD);
        for (Component line : bonuses) {
            width = Math.max(width, font.width(line) + 24 + 2 * PAD);
        }
        int height = PAD + Math.max(18, LINE_HEIGHT * (1 + bonuses.size())) + 4
                + (otherHero != null ? LINE_HEIGHT : 0)
                + LINE_HEIGHT + 4 + PAD - 2;

        int x = g.guiWidth() / 2 - width / 2;
        int y = g.guiHeight() / 2 + TOP_OFFSET;
        UiStyle.panel(g, x, y, width, height);
        g.outline(x, y, width, height, weapon.lootRarity().argb());

        int line = y + PAD;
        UiStyle.itemSlot(g, new ItemStack(owner == null ? HeroDefinition.DEFAULT.weapon() : owner.weapon()), x + PAD, line, 0, false);
        int textLeft = x + PAD + 24;
        g.text(font, weapon.displayName(), textLeft, line + 1, UiStyle.TEXT);
        int bonusY = line + 1 + LINE_HEIGHT;
        for (Component bonus : bonuses) {
            g.text(font, bonus, textLeft, bonusY, UiStyle.TEXT);
            bonusY += LINE_HEIGHT;
        }
        line += Math.max(18, LINE_HEIGHT * (1 + bonuses.size())) + 4;

        if (otherHero != null) {
            g.text(font, otherHero, x + PAD, line, UiStyle.NEGATIVE);
            line += LINE_HEIGHT;
        }

        UiStyle.divider(g, x + PAD, line - 2, width - 2 * PAD);
        line += 3;
        int keyX = x + PAD;
        keyX += UiStyle.keyCap(g, font, ModKeyMappings.LOOT_STORE.getTranslatedKeyMessage(), keyX, line) + 3;
        Component store = Component.translatable("dungeon_defenders.loot.action.store");
        g.text(font, store, keyX, line, UiStyle.TEXT);
        if (ownHero) {
            keyX += font.width(store) + 12;
            keyX += UiStyle.keyCap(g, font, ModKeyMappings.LOOT_EQUIP.getTranslatedKeyMessage(), keyX, line) + 3;
            g.text(font, Component.translatable("dungeon_defenders.loot.action.equip"), keyX, line, UiStyle.TEXT);
        }
    }
}
