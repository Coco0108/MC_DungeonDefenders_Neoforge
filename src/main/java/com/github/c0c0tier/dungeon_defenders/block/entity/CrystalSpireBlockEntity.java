package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

// "Flèche de cristal" (Géomancien) : frappe le monstre le plus proche, puis son éclair rebondit
// sur le monstre non encore touché le plus proche du précédent, et ainsi de suite. Chaque rebond
// fait moins mal. Faible contre un monstre seul, redoutable contre un groupe serré — par exemple
// enlisé par un Totem de boue.
public class CrystalSpireBlockEntity extends AbstractPulseTowerBlockEntity {

    // Chaque rebond garde 70 % des dégâts du précédent.
    private static final double JUMP_DAMAGE_RATIO = 0.7D;

    public CrystalSpireBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.CRYSTAL_SPIRE_BE.get(), pos, state, TowerDefinition.CRYSTAL_SPIRE);
    }

    @Override
    protected int pulseIntervalTicks() {
        return stats().intValue(TowerStat.ATTACK_INTERVAL_TICKS);
    }

    @Override
    protected void pulse(ServerLevel level) {
        // Les rebonds peuvent sortir de la portée de la tour : on élargit la liste d'autant.
        double jump = stats().get(TowerStat.SPLASH_RADIUS);
        int maxTargets = stats().intValue(TowerStat.CHAIN_TARGETS);
        List<Monster> candidates = new ArrayList<>(monstersInRange(level, stats().get(TowerStat.RANGE) + jump * maxTargets));

        Vec3 from = Vec3.atCenterOf(this.worldPosition);
        double reach = stats().get(TowerStat.RANGE);
        double damage = stats().get(TowerStat.DAMAGE) * damageMultiplier();

        for (int hit = 0; hit < maxTargets; hit++) {
            Monster next = null;
            double nextDistSq = reach * reach;
            for (Monster monster : candidates) {
                double distSq = monster.position().distanceToSqr(from.x, monster.getY(), from.z);
                if (distSq <= nextDistSq) {
                    nextDistSq = distSq;
                    next = monster;
                }
            }
            if (next == null) {
                return;
            }
            candidates.remove(next);
            next.hurt(level.damageSources().generic(), (float) damage);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, next.getX(), next.getY() + 1.0D, next.getZ(),
                    6, 0.2D, 0.4D, 0.2D, 0.05D);

            from = next.position();
            reach = jump;
            damage *= JUMP_DAMAGE_RATIO;
        }
    }
}
