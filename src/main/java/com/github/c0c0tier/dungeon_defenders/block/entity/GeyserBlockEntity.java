package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

// "Geyser" (Géomancien) : quand des monstres sont à portée, les projette en l'air. Ils perdent
// quelques PV tout de suite, puis d'autres en retombant, et surtout ils ne frappent rien pendant
// leur vol. Portée courte, cadence lente.
public class GeyserBlockEntity extends AbstractPulseTowerBlockEntity {

    public GeyserBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.GEYSER_BE.get(), pos, state, TowerDefinition.GEYSER);
    }

    @Override
    protected int pulseIntervalTicks() {
        return stats().intValue(TowerStat.ATTACK_INTERVAL_TICKS);
    }

    @Override
    protected void pulse(ServerLevel level) {
        List<Monster> targets = monstersInRange(level, stats().get(TowerStat.RANGE));
        if (targets.isEmpty()) {
            return;
        }
        float damage = (float) (stats().get(TowerStat.DAMAGE) * damageMultiplier());
        double lift = stats().get(TowerStat.KNOCKBACK_STRENGTH);
        for (Monster monster : targets) {
            monster.hurt(level.damageSources().generic(), damage);
            // Après hurt : le recul vanilla du coup écraserait sinon l'élan vertical.
            monster.setDeltaMovement(monster.getDeltaMovement().x, lift, monster.getDeltaMovement().z);
            monster.hurtMarked = true;
            level.sendParticles(ParticleTypes.CLOUD, monster.getX(), monster.getY(), monster.getZ(),
                    8, 0.2D, 0.5D, 0.2D, 0.05D);
        }
    }
}
