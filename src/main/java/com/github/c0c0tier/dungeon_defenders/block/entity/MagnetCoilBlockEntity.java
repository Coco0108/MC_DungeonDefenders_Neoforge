package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;

// "Bobine magnétique" (Ingénieur) : aucun dégât. Tire vers elle les monstres à portée, deux fois
// par seconde : ils piétinent autour d'elle au lieu d'avancer. À poser dans le champ de tir des
// tourelles, ou à côté d'un Marteau-pilon. L'inverse exact du Bouncer Blockade de l'Écuyer.
public class MagnetCoilBlockEntity extends AbstractPulseTowerBlockEntity {

    public MagnetCoilBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.MAGNET_COIL_BE.get(), pos, state, TowerDefinition.MAGNET_COIL);
    }

    @Override
    protected int pulseIntervalTicks() {
        return stats().intValue(TowerStat.ATTACK_INTERVAL_TICKS);
    }

    @Override
    protected void pulse(ServerLevel level) {
        float strength = stats().floatValue(TowerStat.KNOCKBACK_STRENGTH);
        double centerX = this.worldPosition.getX() + 0.5D;
        double centerZ = this.worldPosition.getZ() + 0.5D;
        for (Monster monster : monstersInRange(level, stats().get(TowerStat.RANGE))) {
            // Entity#knockback pousse À L'OPPOSÉ du vecteur donné (voir le commentaire de
            // AbstractBlockadeBlockEntity) : "monstre moins source" attire donc vers la source.
            monster.knockback(strength, monster.getX() - centerX, monster.getZ() - centerZ);
        }
    }
}
