package com.github.c0c0tier.dungeon_defenders.entity.ai;

import com.github.c0c0tier.dungeon_defenders.Config;
import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import org.jspecify.annotations.Nullable;

// Base commune aux goals qui font converger un mob vers le Cristal d'Eternia pour l'attaquer.
// Un seul sous-classeur aujourd'hui : RangedAttackEterniaCrystalGoal (utilisé par les archers,
// qui ignorent Blockade/Turret — voir AttackPriorityTargetGoal pour les monstres de mêlée,
// qui n'étendent PAS cette classe-ci : leur cible varie selon un système de priorité à
// plusieurs paliers, pas seulement le cristal). Porte le ciblage/déplacement et les dégâts,
// configurables par le constructeur pour qu'un futur ennemi à distance puisse réutiliser
// RangedAttackEterniaCrystalGoal tel quel avec d'autres chiffres, sans nouvelle classe.
//
// Sous-classer directement cette classe-ci n'a d'intérêt que pour un **nouveau style
// d'attaque à distance sur le cristal spécifiquement** — un ennemi de mêlée n'en a plus besoin
// depuis AttackPriorityTargetGoal.
public abstract class AbstractEterniaCrystalAttackGoal extends MoveToBlockGoal {

    /** Dégâts infligés par attaque réussie — lu par les sous-classes dans leur onReachedTarget(...). */
    protected final int damagePerHit;

    private final double acceptedDistance;

    protected AbstractEterniaCrystalAttackGoal(
            PathfinderMob mob, double speedModifier, double acceptedDistance, int damagePerHit) {
        super(mob, speedModifier, Config.SEARCH_RANGE.get());
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
