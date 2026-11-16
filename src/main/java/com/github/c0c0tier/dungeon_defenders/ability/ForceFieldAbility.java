package com.github.c0c0tier.dungeon_defenders.ability;

import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.TowerScan;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

// Sort 2 de l'Ingénieur : "Champ de force". Tant que la touche est maintenue, les tours à 8
// blocs du joueur n'encaissent plus aucun dégât (AbstractTowerBlockEntity#applyShield). Le plus
// cher des sorts maintenus : il achète quelques secondes, pas une vague entière.
//
// Même patron que la Catalyse de l'Alchimiste : bouclier posé pour quelques ticks et renouvelé,
// il tombe seul quand la canalisation s'arrête.
//
// Toutes les valeurs sont des valeurs de test, à ajuster en jeu.
public final class ForceFieldAbility implements ChannelAbility {

    public static final ForceFieldAbility INSTANCE = new ForceFieldAbility();

    private static final int MANA_COST_PER_TICK = 3;
    private static final double RADIUS = 8.0D;
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final int SHIELD_DURATION_TICKS = 15;

    private ForceFieldAbility() {
    }

    @Override
    public Component displayName() {
        return Component.translatable("dungeon_defenders.ability.force_field");
    }

    @Override
    public Item icon() {
        return Items.SHIELD;
    }

    @Override
    public int manaCostPerTick() {
        return MANA_COST_PER_TICK;
    }

    @Override
    public boolean canContinue(ServerPlayer player, @Nullable BlockPos target) {
        return player.getData(ModAttachments.MANA) >= MANA_COST_PER_TICK;
    }

    @Override
    public void applyTick(ServerPlayer player, @Nullable BlockPos target) {
        player.setData(ModAttachments.MANA, player.getData(ModAttachments.MANA) - MANA_COST_PER_TICK);

        ServerLevel level = player.level();
        long now = level.getGameTime();
        if (now % SCAN_INTERVAL_TICKS != 0) {
            return;
        }
        for (AbstractTowerBlockEntity tower : TowerScan.towersNear(level, player.position(), RADIUS)) {
            tower.applyShield(now + SHIELD_DURATION_TICKS);
            BlockPos pos = tower.getBlockPos();
            level.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 1.7D, pos.getZ() + 0.5D, 2, 0.3D, 0.1D, 0.3D, 0.0D);
        }
    }
}
