package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

// "Cuve instable" (Alchimiste) : un mur passif (aucun dégât au contact, donc priorité IA
// "block" : les monstres de mêlée s'y attaquent en premier) qui explose quand les monstres le
// détruisent, et blesse alors tous ceux qui l'entourent. Aucun dégât de terrain, comme la
// Tourelle Mortier. Un retrait volontaire par le joueur ne déclenche rien.
public class VolatileVatBlockEntity extends AbstractTowerBlockEntity {

    public VolatileVatBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.VOLATILE_VAT_BE.get(), pos, state, TowerDefinition.VOLATILE_VAT);
    }

    @Override
    public int getAiPriority() {
        return AiAttackTarget.PRIORITY_BLOCK;
    }

    @Override
    protected void onDestroyedByMonsters(ServerLevel level) {
        BlockPos pos = this.worldPosition;
        level.sendParticles(ParticleTypes.EXPLOSION,
                pos.getX() + 0.5D, pos.getY() + 0.75D, pos.getZ() + 0.5D, 6, 0.8D, 0.5D, 0.8D, 0.0D);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0F, 1.0F);

        float damage = (float) (stats().get(TowerStat.DAMAGE) * damageMultiplier());
        AABB area = new AABB(pos).inflate(stats().get(TowerStat.SPLASH_RADIUS));
        for (Monster monster : level.getEntitiesOfClass(Monster.class, area)) {
            monster.hurt(level.damageSources().generic(), damage);
        }
    }
}
