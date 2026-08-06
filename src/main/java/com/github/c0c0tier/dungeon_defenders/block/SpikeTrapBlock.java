package com.github.c0c0tier.dungeon_defenders.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.WeakHashMap;

public class SpikeTrapBlock extends Block {

    public static final MapCodec<SpikeTrapBlock> CODEC = simpleCodec(SpikeTrapBlock::new);

    /** Dégâts infligés à chaque déclenchement du piège. */
    private static final float DAMAGE = 2.0F;
    /** Cooldown entre deux déclenchements pour une même entité (1 seconde = 20 ticks). */
    private static final long COOLDOWN_TICKS = 20L;

    // WeakHashMap : ne retient pas les entités mortes/déchargées, évite une fuite mémoire.
    private final Map<Entity, Long> lastTriggerTick = new WeakHashMap<>();

    public SpikeTrapBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide()) {
            return;
        }

        if (entity instanceof Monster monster && monster.isAlive()) {
            long now = level.getGameTime();
            Long lastTrigger = lastTriggerTick.get(monster);

            if (lastTrigger == null || now - lastTrigger >= COOLDOWN_TICKS) {
                lastTriggerTick.put(monster, now);
                monster.hurt(level.damageSources().stalagmite(), DAMAGE);
            }
        }

        super.stepOn(level, pos, state, entity);
    }
}
