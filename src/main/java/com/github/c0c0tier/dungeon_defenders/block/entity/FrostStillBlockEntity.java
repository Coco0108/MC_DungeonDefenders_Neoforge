package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;

// "Alambic givrant" (Alchimiste) : ne fait aucun dégât, mais ralentit tous les monstres dans son
// rayon. Sa valeur vient du temps qu'il fait gagner aux autres tours.
//
// Effet vanilla Lenteur, réappliqué deux fois par seconde avec une durée courte : un monstre qui
// sort du rayon retrouve sa vitesse en une seconde et demie, sans code de nettoyage.
public class FrostStillBlockEntity extends AbstractPulseTowerBlockEntity {

    private static final int PULSE_INTERVAL_TICKS = 10;
    private static final int EFFECT_DURATION_TICKS = 30;

    public FrostStillBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.FROST_STILL_BE.get(), pos, state, TowerDefinition.FROST_STILL);
    }

    @Override
    protected int pulseIntervalTicks() {
        return PULSE_INTERVAL_TICKS;
    }

    @Override
    protected void pulse(ServerLevel level) {
        int amplifier = stats().intValue(TowerStat.EFFECT_LEVEL) - 1;
        for (Monster monster : monstersInRange(level, stats().get(TowerStat.RANGE))) {
            monster.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, EFFECT_DURATION_TICKS, amplifier), null);
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    monster.getX(), monster.getY() + 0.5D, monster.getZ(), 2, 0.2D, 0.3D, 0.2D, 0.0D);
        }
    }
}
