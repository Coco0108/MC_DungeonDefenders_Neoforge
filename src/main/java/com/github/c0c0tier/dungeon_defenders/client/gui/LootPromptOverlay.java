package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.client.LootPickupClientEvents;
import com.github.c0c0tier.dungeon_defenders.client.ModKeyMappings;
import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import com.github.c0c0tier.dungeon_defenders.loot.LootWeapon;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;

// Affiché quand un objet de butin est à portée de ramassage (LootPickupClientEvents) : son nom
// dans la couleur de sa rareté, ses bonus, et les deux touches. Centré sous le viseur, là où le
// regard est déjà. Si l'arme est celle d'un autre héros, seule la touche Ranger est proposée
// (le serveur refuserait de l'équiper).
public class LootPromptOverlay implements GuiLayer {

    private static final int TOP_OFFSET = 18;
    private static final int LINE_HEIGHT = 10;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int PROMPT_COLOR = 0xFFFFD700;
    private static final int BACKGROUND = 0x90000000;
    private static final int PADDING = 4;

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LootPickupClientEvents.Target target = LootPickupClientEvents.target();
        if (target == null || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        LootWeapon weapon = target.weapon();

        java.util.List<Component> lines = new java.util.ArrayList<>();
        lines.add(weapon.displayName());
        lines.addAll(weapon.statLines());
        Component storeKey = ModKeyMappings.LOOT_STORE.getTranslatedKeyMessage();
        Component equipKey = ModKeyMappings.LOOT_EQUIP.getTranslatedKeyMessage();
        boolean ownHero = weapon.heroDefinition() == HeroDefinition.of(minecraft.player);
        lines.add(ownHero
                ? Component.translatable("dungeon_defenders.loot.prompt", storeKey, equipKey)
                : Component.translatable("dungeon_defenders.loot.prompt_store_only", storeKey));

        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, minecraft.font.width(line));
        }
        int centerX = guiGraphics.guiWidth() / 2;
        int top = guiGraphics.guiHeight() / 2 + TOP_OFFSET;
        guiGraphics.fill(centerX - width / 2 - PADDING, top - PADDING,
                centerX + width / 2 + PADDING, top + lines.size() * LINE_HEIGHT + PADDING - 1, BACKGROUND);

        for (int i = 0; i < lines.size(); i++) {
            // Le nom porte déjà la couleur de sa rareté ; la dernière ligne est celle des touches.
            int color = i == lines.size() - 1 ? PROMPT_COLOR : TEXT_COLOR;
            guiGraphics.centeredText(minecraft.font, lines.get(i), centerX, top + i * LINE_HEIGHT, color);
        }
    }
}
