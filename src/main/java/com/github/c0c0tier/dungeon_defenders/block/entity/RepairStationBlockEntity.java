package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

// "Station de réparation" (Ingénieur) : rend régulièrement des PV aux tours voisines entamées,
// quel que soit leur héros. Elle ne se répare pas elle-même. Le débit monte avec son niveau
// d'amélioration.
public class RepairStationBlockEntity extends AbstractPulseTowerBlockEntity {

    public RepairStationBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.REPAIR_STATION_BE.get(), pos, state, TowerDefinition.REPAIR_STATION);
    }

    @Override
    protected int pulseIntervalTicks() {
        return stats().intValue(TowerStat.ATTACK_INTERVAL_TICKS);
    }

    @Override
    protected void pulse(ServerLevel level) {
        int amount = (int) Math.round(stats().intValue(TowerStat.HEAL_AMOUNT) * levelMultiplier());
        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        for (AbstractTowerBlockEntity tower : TowerScan.towersNear(level, center, stats().get(TowerStat.RANGE))) {
            if (tower == this || tower.getHealth() >= tower.getMaxHealth()) {
                continue;
            }
            tower.repair(amount);
            BlockPos pos = tower.getBlockPos();
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5D, pos.getY() + 1.7D, pos.getZ() + 0.5D, 3, 0.3D, 0.1D, 0.3D, 0.0D);
        }
    }
}
