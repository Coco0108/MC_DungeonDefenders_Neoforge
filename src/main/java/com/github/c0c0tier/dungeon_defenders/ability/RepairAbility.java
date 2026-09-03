package com.github.c0c0tier.dungeon_defenders.ability;

import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

// Compétence générique (emplacement REPAIR), la même pour tout héros — singleton, comme
// HealAbility.
//
// Même ratio que le soin (1 mana = 1 PV/tick), pour ne pas avoir deux nombres différents à
// retenir ; comme lui, valeur de test.
//
// **`AbstractTowerBlockEntity#setHealth` ne plafonne pas au maximum** (vérifié dans sa source :
// `Math.max(0, health)`, pas de `Math.min`) — c'est cette classe qui doit s'arrêter à
// `getMaxHealth()`, pas le block entity.
public final class RepairAbility implements ChannelAbility {

    public static final RepairAbility INSTANCE = new RepairAbility();

    private static final int MANA_COST_PER_TICK = 1;
    private static final int REPAIR_PER_TICK = 1;
    // Le raycast client (MAX_REACH dans AbilityClientEvents) fixe déjà la distance à l'instant
    // du clic ; cette marge ne fait que tolérer un léger déplacement pendant que la touche
    // reste maintenue, sans revalider un raycast complet à chaque tick serveur.
    private static final double MAX_DISTANCE_SQ = 25.0D * 25.0D;

    private RepairAbility() {
    }

    @Override
    public Component displayName() {
        return Component.translatable("dungeon_defenders.ability.repair");
    }

    @Override
    public Item icon() {
        // L'enclume répare des outils en vanilla — l'association la plus directe disponible.
        return Items.ANVIL;
    }

    @Override
    public int manaCostPerTick() {
        return MANA_COST_PER_TICK;
    }

    @Override
    public boolean requiresTarget() {
        return true;
    }

    @Override
    public boolean canContinue(ServerPlayer player, @Nullable BlockPos target) {
        if (target == null || player.getData(ModAttachments.MANA) < MANA_COST_PER_TICK) {
            return false;
        }
        if (player.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5) > MAX_DISTANCE_SQ) {
            return false;
        }
        return player.level().getBlockEntity(target) instanceof AbstractTowerBlockEntity tower
                && tower.getHealth() < tower.getMaxHealth();
    }

    @Override
    public void applyTick(ServerPlayer player, @Nullable BlockPos target) {
        if (!(player.level().getBlockEntity(target) instanceof AbstractTowerBlockEntity tower)) {
            return;
        }
        int currentMana = player.getData(ModAttachments.MANA);
        player.setData(ModAttachments.MANA, currentMana - MANA_COST_PER_TICK);
        tower.setHealth(Math.min(tower.getMaxHealth(), tower.getHealth() + REPAIR_PER_TICK));
    }
}
