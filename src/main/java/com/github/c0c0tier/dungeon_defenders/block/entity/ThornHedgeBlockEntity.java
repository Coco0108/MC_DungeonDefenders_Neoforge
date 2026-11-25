package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

// "Haie de ronces" (Géomancien) : un mur qui ne fait rien tant qu'on ne le touche pas, mais qui
// rend les coups : chaque fois qu'un monstre le frappe, tous les monstres collés à lui sont
// blessés. Plus les monstres s'acharnent dessus, plus il est rentable — à l'inverse du Spike
// Blockade de l'Écuyer, qui pique à cadence fixe.
public class ThornHedgeBlockEntity extends AbstractTowerBlockEntity {

    public ThornHedgeBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.THORN_HEDGE_BE.get(), pos, state, TowerDefinition.THORN_HEDGE);
    }

    @Override
    public int getAiPriority() {
        return AiAttackTarget.PRIORITY_BLOCK;
    }

    @Override
    protected void onDamaged(ServerLevel level) {
        thorns(level);
    }

    // Le dernier coup compte aussi : la haie pique une dernière fois en tombant.
    @Override
    protected void onDestroyedByMonsters(ServerLevel level) {
        thorns(level);
    }

    private void thorns(ServerLevel level) {
        float damage = (float) (stats().get(TowerStat.CONTACT_DAMAGE) * damageMultiplier());
        AABB area = new AABB(this.worldPosition).inflate(stats().get(TowerStat.CONTACT_RANGE));
        for (Monster monster : level.getEntitiesOfClass(Monster.class, area)) {
            monster.hurt(level.damageSources().cactus(), damage);
            level.sendParticles(ParticleTypes.CRIT, monster.getX(), monster.getY() + 1.0D, monster.getZ(),
                    3, 0.2D, 0.3D, 0.2D, 0.0D);
        }
    }
}
