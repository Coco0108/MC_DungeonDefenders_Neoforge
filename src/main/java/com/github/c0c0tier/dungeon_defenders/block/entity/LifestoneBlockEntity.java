package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

// "Pierre de vie" (Géomancien) : la seule tour qui s'occupe des JOUEURS. Elle rend régulièrement
// quelques PV à ceux qui se tiennent dans son rayon, sans mana et sans touche à maintenir — un
// point de repli où se soigner pendant un combat. Le débit monte avec son niveau d'amélioration.
public class LifestoneBlockEntity extends AbstractPulseTowerBlockEntity {

    public LifestoneBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.LIFESTONE_BE.get(), pos, state, TowerDefinition.LIFESTONE);
    }

    @Override
    protected int pulseIntervalTicks() {
        return stats().intValue(TowerStat.ATTACK_INTERVAL_TICKS);
    }

    @Override
    protected void pulse(ServerLevel level) {
        double range = stats().get(TowerStat.RANGE);
        float amount = (float) (stats().intValue(TowerStat.HEAL_AMOUNT) * levelMultiplier());
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(this.worldPosition).inflate(range))) {
            if (!player.isAlive() || player.getHealth() >= player.getMaxHealth()) {
                continue;
            }
            player.heal(amount);
            level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 2.0D, player.getZ(),
                    1, 0.2D, 0.1D, 0.2D, 0.0D);
        }
    }
}
