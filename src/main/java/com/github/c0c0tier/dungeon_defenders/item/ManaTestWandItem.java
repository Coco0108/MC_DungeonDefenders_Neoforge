package com.github.c0c0tier.dungeon_defenders.item;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Harnais de test : retire du mana au clic droit pour vérifier l'affichage du HUD. */
public class ManaTestWandItem extends Item {
    private static final int MANA_COST = 10;

    public ManaTestWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Le client se contente de prédire le succès ; la logique tourne côté serveur.
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        int currentMana = player.getData(ModAttachments.MANA);
        if (currentMana <= 0) {
            player.sendSystemMessage(Component.translatable("dungeon_defenders.mana_test_wand.empty"));
            return InteractionResult.FAIL;
        }

        int newMana = Math.max(0, currentMana - MANA_COST);
        player.setData(ModAttachments.MANA, newMana);
        player.syncData(ModAttachments.MANA);
        player.sendSystemMessage(Component.translatable(
                "dungeon_defenders.mana_test_wand.used", MANA_COST, newMana, ModAttachments.MAX_MANA));

        return InteractionResult.SUCCESS;
    }
}
