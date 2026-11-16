package com.github.c0c0tier.dungeon_defenders.ability;

import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.TowerScan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

// Sort 1 de l'Ingénieur : "Réparation d'urgence". Rend d'un coup la moitié de leurs PV max à
// toutes les tours à 10 blocs du joueur. Cher et long à recharger : c'est le bouton de secours
// quand une ligne cède, pas un entretien courant (pour ça : la Réparation générique, ou la
// Station de réparation).
//
// Toutes les valeurs sont des valeurs de test, à ajuster en jeu.
public final class EmergencyRepairAbility implements BurstAbility {

    public static final EmergencyRepairAbility INSTANCE = new EmergencyRepairAbility();

    private static final int MANA_COST = 70;
    private static final int COOLDOWN_TICKS = 400;
    private static final double RADIUS = 10.0D;
    private static final double HEALTH_RATIO = 0.5D;

    private EmergencyRepairAbility() {
    }

    @Override
    public Component displayName() {
        return Component.translatable("dungeon_defenders.ability.emergency_repair");
    }

    @Override
    public Item icon() {
        return Items.ANVIL;
    }

    @Override
    public int manaCost() {
        return MANA_COST;
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public void activate(ServerPlayer player) {
        ServerLevel level = player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
        for (AbstractTowerBlockEntity tower : TowerScan.towersNear(level, player.position(), RADIUS)) {
            tower.repair((int) Math.ceil(tower.getMaxHealth() * HEALTH_RATIO));
            BlockPos pos = tower.getBlockPos();
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5D, pos.getY() + 1.7D, pos.getZ() + 0.5D, 8, 0.3D, 0.2D, 0.3D, 0.0D);
        }
    }
}
