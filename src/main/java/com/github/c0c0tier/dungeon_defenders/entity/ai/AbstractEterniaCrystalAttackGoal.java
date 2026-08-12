package com.github.c0c0tier.dungeon_defenders.entity.ai;

import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import org.jspecify.annotations.Nullable;

// Base commune aux goals qui font converger un mob vers le Cristal d'Eternia pour l'attaquer
// (voir AttackEterniaCrystalGoal pour le corps à corps, RangedAttackEterniaCrystalGoal pour la
// distance). Porte le ciblage/déplacement — identique dans les deux cas — et les dégâts,
// configurables par le constructeur pour qu'un futur type d'ennemi puisse réutiliser l'une ou
// l'autre sous-classe telle quelle avec ses propres chiffres, sans avoir besoin d'écrire une
// nouvelle classe juste pour une variation de stats.
//
// Sous-classer directement cette classe-ci (plutôt que AttackEterniaCrystalGoal ou
// RangedAttackEterniaCrystalGoal) n'a d'intérêt que pour un **nouveau style d'attaque** — une
// troisième famille, ni corps à corps ni tir à l'arc (une attaque de zone, par exemple).
public abstract class AbstractEterniaCrystalAttackGoal extends MoveToBlockGoal {

    private static final int DEFAULT_SEARCH_RANGE = 16;

    /** Dégâts infligés par attaque réussie — lu par les sous-classes dans leur onReachedTarget(...). */
    protected final int damagePerHit;

    private final double acceptedDistance;

    protected AbstractEterniaCrystalAttackGoal(
            PathfinderMob mob, double speedModifier, double acceptedDistance, int damagePerHit) {
        super(mob, speedModifier, DEFAULT_SEARCH_RANGE);
        this.acceptedDistance = acceptedDistance;
        this.damagePerHit = damagePerHit;
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.ETERNIA_CRYSTAL.get());
    }

    @Override
    protected BlockPos getMoveToTarget() {
        // Vise la BASE du cristal (et non le dessus) : le mob attaque depuis le sol
        // adjacent au lieu d'essayer de grimper dessus.
        return this.blockPos;
    }

    @Override
    public double acceptedDistance() {
        return this.acceptedDistance;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isReachedTarget()) {
            onReachedTarget(findCrystal());
        } else {
            onTargetLost();
        }
    }

    /** @return le block entity du cristal visé, ou null s'il a été cassé entre-temps. */
    protected final @Nullable EterniaCrystalBlockEntity findCrystal() {
        return this.mob.level().getBlockEntity(this.blockPos) instanceof EterniaCrystalBlockEntity crystal
                ? crystal
                : null;
    }

    /** Appelé chaque tick tant que le mob est à portée (acceptedDistance) du cristal. */
    protected abstract void onReachedTarget(@Nullable EterniaCrystalBlockEntity crystal);

    /** Appelé chaque tick tant que le mob n'est PAS (ou plus) à portée ; no-op par défaut. */
    protected void onTargetLost() {
    }
}
