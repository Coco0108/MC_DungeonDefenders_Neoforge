package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

// "Totem de boue" (Géomancien) : toutes les quelques secondes, enlise presque sur place les
// monstres de son rayon pendant un court instant. Là où l'Alambic givrant de l'Alchimiste
// ralentit un peu et en permanence, le Totem arrête net, par à-coups.
public class MudTotemBlockEntity extends AbstractPulseTowerBlockEntity {

    // Lenteur VII : le monstre ne bouge plus (-105 % de vitesse, plafonné à l'arrêt).
    private static final int ROOT_AMPLIFIER = 6;

    public MudTotemBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.MUD_TOTEM_BE.get(), pos, state, TowerDefinition.MUD_TOTEM);
    }

    @Override
    protected int pulseIntervalTicks() {
        return stats().intValue(TowerStat.ATTACK_INTERVAL_TICKS);
    }

    @Override
    protected void pulse(ServerLevel level) {
        int duration = stats().intValue(TowerStat.LINGER_TICKS);
        for (Monster monster : monstersInRange(level, stats().get(TowerStat.RANGE))) {
            monster.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, ROOT_AMPLIFIER), null);
            level.sendParticles(ParticleTypes.SPLASH, monster.getX(), monster.getY() + 0.2D, monster.getZ(),
                    6, 0.3D, 0.1D, 0.3D, 0.0D);
        }
    }
}
