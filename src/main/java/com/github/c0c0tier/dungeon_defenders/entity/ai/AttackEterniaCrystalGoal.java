package com.github.c0c0tier.dungeon_defenders.entity.ai;

import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import org.jspecify.annotations.Nullable;

// Fait converger un mob vers le Cristal d'Eternia le plus proche et le lui fait frapper une
// fois arrivé. Dégâts et cadence configurables par le constructeur (voir
// AbstractEterniaCrystalAttackGoal) : réutilisable tel quel pour un futur ennemi de mêlée avec
// d'autres chiffres — pas besoin d'une nouvelle classe juste pour ça.
public class AttackEterniaCrystalGoal extends AbstractEterniaCrystalAttackGoal {

    public static final int DEFAULT_DAMAGE_PER_HIT = 5;
    public static final int DEFAULT_TICKS_BETWEEN_HITS = 20;

    private static final double SPEED_MODIFIER = 1.2D;
    // Distance suffisante pour qu'un mob collé au cristal (boîte de 3 de haut) soit
    // considéré "arrivé" depuis le sol.
    private static final double ACCEPTED_DISTANCE = 2.1D;

    private final int ticksBetweenHits;
    private int hitCooldown;

    public AttackEterniaCrystalGoal(PathfinderMob mob) {
        this(mob, DEFAULT_DAMAGE_PER_HIT, DEFAULT_TICKS_BETWEEN_HITS);
    }

    public AttackEterniaCrystalGoal(PathfinderMob mob, int damagePerHit, int ticksBetweenHits) {
        super(mob, SPEED_MODIFIER, ACCEPTED_DISTANCE, damagePerHit);
        this.ticksBetweenHits = ticksBetweenHits;
    }

    @Override
    public void start() {
        super.start();
        this.hitCooldown = 0;
    }

    @Override
    protected void onReachedTarget(@Nullable EterniaCrystalBlockEntity crystal) {
        // Le cooldown est un champ du goal (et non mob.tickCount) pour que le rythme reste
        // correct même si le mob s'éloigne puis revient (voir onTargetLost).
        if (this.hitCooldown > 0) {
            this.hitCooldown--;
            return;
        }
        if (crystal == null) {
            return;
        }

        crystal.damage(this.damagePerHit);
        this.mob.swing(InteractionHand.MAIN_HAND);
        this.hitCooldown = this.ticksBetweenHits;
    }

    @Override
    protected void onTargetLost() {
        this.hitCooldown = 0;
    }
}
