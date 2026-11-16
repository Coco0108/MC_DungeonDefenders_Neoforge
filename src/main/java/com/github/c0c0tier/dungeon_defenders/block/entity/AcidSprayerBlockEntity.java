package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.entity.Corrosion;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

// "Projecteur d'acide" (Alchimiste) : arrose TOUS les monstres d'un cône court et large devant
// lui. Les dégâts sont faibles ; l'intérêt est la corrosion (entity/Corrosion) : un monstre
// arrosé subit plus de dégâts de toutes les sources pendant quelques secondes.
public class AcidSprayerBlockEntity extends AbstractPulseTowerBlockEntity {

    private static final int CORROSION_DURATION_TICKS = 60;

    public AcidSprayerBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.ACID_SPRAYER_BE.get(), pos, state, TowerDefinition.ACID_SPRAYER);
    }

    @Override
    protected int pulseIntervalTicks() {
        return stats().intValue(TowerStat.ATTACK_INTERVAL_TICKS);
    }

    @Override
    protected void pulse(ServerLevel level) {
        BlockState state = getBlockState();
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return;
        }
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        List<Monster> targets = monstersInCone(level, stats().get(TowerStat.RANGE),
                stats().get(TowerStat.CONE_ANGLE_DEGREES), facing);
        if (targets.isEmpty()) {
            return;
        }

        float damage = (float) (stats().get(TowerStat.DAMAGE) * damageMultiplier());
        double corrosionFactor = 1.0D + stats().get(TowerStat.VULNERABILITY_PERCENT) / 100.0D;
        long corrodedUntil = level.getGameTime() + CORROSION_DURATION_TICKS;

        for (Monster monster : targets) {
            // Les dégâts d'abord : la corrosion posée par ce jet ne s'applique pas à lui-même.
            monster.hurt(level.damageSources().generic(), damage);
            Corrosion.apply(monster, corrosionFactor, corrodedUntil);
            level.sendParticles(ParticleTypes.ITEM_SLIME,
                    monster.getX(), monster.getY() + 1.0D, monster.getZ(), 3, 0.2D, 0.3D, 0.2D, 0.0D);
        }
    }
}
