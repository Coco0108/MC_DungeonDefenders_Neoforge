package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// "Pylône catalyseur" (Alchimiste) : n'attaque pas, mais augmente les dégâts de toutes les tours
// voisines, quel que soit le héros qui les a posées (AbstractTowerBlockEntity#applyDamageBoost).
// Les pylônes ne se cumulent pas entre eux : une tour ne garde que le bonus le plus fort.
public class CatalystPylonBlockEntity extends AbstractPulseTowerBlockEntity {

    private static final int PULSE_INTERVAL_TICKS = 20;
    // Un peu plus long que l'intervalle : le bonus ne clignote pas entre deux impulsions, et
    // s'éteint de lui-même une seconde et demie après la destruction du pylône.
    private static final int BOOST_DURATION_TICKS = 30;

    public CatalystPylonBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.CATALYST_PYLON_BE.get(), pos, state, TowerDefinition.CATALYST_PYLON);
    }

    @Override
    protected int pulseIntervalTicks() {
        return PULSE_INTERVAL_TICKS;
    }

    @Override
    protected void pulse(ServerLevel level) {
        // Le bonus monte avec le niveau d'amélioration du pylône.
        double factor = 1.0D + stats().get(TowerStat.BOOST_PERCENT) / 100.0D * levelMultiplier();
        long until = level.getGameTime() + BOOST_DURATION_TICKS;
        Vec3 center = Vec3.atCenterOf(this.worldPosition);

        for (AbstractTowerBlockEntity tower : TowerScan.towersNear(level, center, stats().get(TowerStat.RANGE))) {
            if (tower == this) {
                continue;
            }
            tower.applyDamageBoost(factor, until);
            BlockPos pos = tower.getBlockPos();
            level.sendParticles(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5D, pos.getY() + 1.7D, pos.getZ() + 0.5D, 2, 0.2D, 0.1D, 0.2D, 0.0D);
        }
    }
}
