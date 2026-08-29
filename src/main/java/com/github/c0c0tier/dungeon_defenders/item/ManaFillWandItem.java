package com.github.c0c0tier.dungeon_defenders.item;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

// Harnais de test, symétrique de ManaTestWandItem (qui retire du mana) : remplit le mana au
// maximum au clic droit, pour tester la pose de tours sans avoir à farmer des cristaux de mana
// à chaque essai.
public class ManaFillWandItem extends Item {

    public ManaFillWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Le client se contente de prédire le succès ; la logique tourne côté serveur.
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        int currentMana = player.getData(ModAttachments.MANA);
        if (currentMana >= ModAttachments.MAX_MANA) {
            player.sendSystemMessage(Component.translatable("dungeon_defenders.mana_fill_wand.full"));
            return InteractionResult.FAIL;
        }

        player.setData(ModAttachments.MANA, ModAttachments.MAX_MANA);
        player.syncData(ModAttachments.MANA);
        player.sendSystemMessage(Component.translatable(
                "dungeon_defenders.mana_fill_wand.used", ModAttachments.MAX_MANA));

        return InteractionResult.SUCCESS;
    }
}
