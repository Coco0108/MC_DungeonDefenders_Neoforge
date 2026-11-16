package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

// "Lance-rochers" (Géomancien) : la seule tour qui ne vise pas le monstre le plus proche mais le
// plus COSTAUD (le plus de PV restants) à portée, tout autour d'elle. Un gros coup, suivi d'un
// court étourdissement. C'est la réponse aux brutes et aux vagues de boss, pas à la masse.
public class BoulderThrowerBlockEntity extends AbstractPulseTowerBlockEntity {

    private static final int STUN_AMPLIFIER = 6;

    public BoulderThrowerBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.BOULDER_THROWER_BE.get(), pos, state, TowerDefinition.BOULDER_THROWER);
    }

    @Override
    protected int pulseIntervalTicks() {
        return stats().intValue(TowerStat.ATTACK_INTERVAL_TICKS);
    }

    @Override
    protected void pulse(ServerLevel level) {
        Monster target = null;
        for (Monster monster : monstersInRange(level, stats().get(TowerStat.RANGE))) {
            if (target == null || monster.getHealth() > target.getHealth()) {
                target = monster;
            }
        }
        if (target == null) {
            return;
        }
        target.hurt(level.damageSources().generic(), (float) (stats().get(TowerStat.DAMAGE) * damageMultiplier()));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, stats().intValue(TowerStat.LINGER_TICKS), STUN_AMPLIFIER), null);
        level.sendParticles(ParticleTypes.POOF, target.getX(), target.getY() + 1.0D, target.getZ(),
                10, 0.3D, 0.3D, 0.3D, 0.02D);
        level.playSound(null, target.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
    }
}
