package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Monster;

import java.util.List;

// "Marteau-pilon" (Ingénieur) : frappe le sol à intervalle régulier dès qu'un monstre est à
// portée. Tous ceux qui l'entourent sont blessés et projetés au loin. Portée courte : c'est une
// tour de première ligne, plus solide que les tourelles.
public class PistonSlammerBlockEntity extends AbstractPulseTowerBlockEntity {

    public PistonSlammerBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.PISTON_SLAMMER_BE.get(), pos, state, TowerDefinition.PISTON_SLAMMER);
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
        BlockPos pos = this.worldPosition;
        level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D,
                20, 1.5D, 0.1D, 1.5D, 0.02D);

        float damage = (float) (stats().get(TowerStat.DAMAGE) * damageMultiplier());
        float knockback = stats().floatValue(TowerStat.KNOCKBACK_STRENGTH);
        for (Monster monster : targets) {
            monster.hurt(level.damageSources().generic(), damage);
            monster.knockback(knockback, (pos.getX() + 0.5D) - monster.getX(), (pos.getZ() + 0.5D) - monster.getZ());
        }
    }
}
