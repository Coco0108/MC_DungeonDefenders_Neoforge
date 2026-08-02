package com.github.c0c0tier.dungeon_defenders.entity.ai;

import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;

/**
 * Fait converger un mob vers le Cristal d'Eternia le plus proche et le lui fait
 * frapper une fois arrivé.
 */
public class AttackEterniaCrystalGoal extends MoveToBlockGoal {

    public static final int DAMAGE_PER_HIT = 5;
    public static final int TICKS_BETWEEN_HITS = 20;
    private static final int SEARCH_RANGE = 16;
    private static final double SPEED_MODIFIER = 1.2D;

    private int hitCooldown;

    public AttackEterniaCrystalGoal(PathfinderMob mob) {
        super(mob, SPEED_MODIFIER, SEARCH_RANGE);
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.ETERNIA_CRYSTAL.get());
    }

    @Override
    protected BlockPos getMoveToTarget() {
        // On vise la BASE du cristal (et non le dessus) : le mob attaque
        // depuis le sol adjacent au lieu d'essayer de grimper dessus.
        return this.blockPos;
    }

    @Override
    public double acceptedDistance() {
        // Distance suffisante pour qu'un mob collé au cristal (boîte de 3 de haut)
        // soit considéré "arrivé" depuis le sol.
        return 2.1D;
    }

    @Override
    public void start() {
        super.start();
        this.hitCooldown = 0;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.isReachedTarget()) {
            this.hitCooldown = 0;
            return;
        }

        if (this.hitCooldown > 0) {
            this.hitCooldown--;
            return;
        }

        // Le cooldown est porté par le goal (et non par mob.tickCount) pour que
        // le rythme reste correct même si le mob s'éloigne puis revient.
        if (this.mob.level().getBlockEntity(this.blockPos) instanceof EterniaCrystalBlockEntity crystal) {
            crystal.damage(DAMAGE_PER_HIT);
            this.mob.swing(InteractionHand.MAIN_HAND);
            this.hitCooldown = TICKS_BETWEEN_HITS;
        }
    }
}
