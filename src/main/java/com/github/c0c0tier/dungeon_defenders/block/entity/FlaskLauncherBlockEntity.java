package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// "Lance-fioles" (Alchimiste) : tire tout autour de lui, sans orientation. Sa fiole ne blesse pas
// à l'impact : elle laisse une flaque brûlante qui blesse, deux fois par seconde, tout monstre
// qui s'y trouve. À l'inverse de la Tourelle Mortier (une explosion, tout de suite), c'est une
// tour qui punit les monstres qui restent sur place — donc excellente devant un mur.
//
// Les flaques vivent dans ce block entity, en mémoire : elles durent quelques secondes et
// disparaissent avec la tour.
public class FlaskLauncherBlockEntity extends AbstractPulseTowerBlockEntity {

    private static final int PUDDLE_TICK_INTERVAL = 10;

    private record Puddle(Vec3 center, long untilGameTime) {
    }

    private final List<Puddle> puddles = new ArrayList<>();
    private long lastFireTick = Long.MIN_VALUE / 2;

    public FlaskLauncherBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.FLASK_LAUNCHER_BE.get(), pos, state, TowerDefinition.FLASK_LAUNCHER);
    }

    @Override
    protected int pulseIntervalTicks() {
        return PUDDLE_TICK_INTERVAL;
    }

    /** Nombre de flaques encore actives (lu par les gametests). */
    public int activePuddles() {
        return this.puddles.size();
    }

    @Override
    protected void pulse(ServerLevel level) {
        long now = level.getGameTime();
        burn(level, now);

        if (now - this.lastFireTick < stats().intValue(TowerStat.ATTACK_INTERVAL_TICKS)) {
            return;
        }
        Monster target = nearest(monstersInRange(level, stats().get(TowerStat.RANGE)));
        if (target == null) {
            return;
        }
        this.lastFireTick = now;
        this.puddles.add(new Puddle(target.position(), now + stats().intValue(TowerStat.LINGER_TICKS)));
        level.playSound(null, target.blockPosition(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    private void burn(ServerLevel level, long now) {
        double radius = stats().get(TowerStat.SPLASH_RADIUS);
        float damage = (float) (stats().get(TowerStat.DAMAGE) * damageMultiplier());

        Iterator<Puddle> iterator = this.puddles.iterator();
        while (iterator.hasNext()) {
            Puddle puddle = iterator.next();
            if (now >= puddle.untilGameTime()) {
                iterator.remove();
                continue;
            }
            Vec3 center = puddle.center();
            level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.1D, center.z,
                    8, radius / 2.0D, 0.05D, radius / 2.0D, 0.0D);
            for (Monster monster : monstersAround(level, center, radius, TowerDefinition.FULL_CIRCLE_DEGREES, Direction.NORTH)) {
                monster.hurt(level.damageSources().generic(), damage);
            }
        }
    }

    private Monster nearest(List<Monster> monsters) {
        Vec3 origin = Vec3.atCenterOf(this.worldPosition);
        Monster nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (Monster monster : monsters) {
            double distSq = monster.distanceToSqr(origin);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = monster;
            }
        }
        return nearest;
    }
}
