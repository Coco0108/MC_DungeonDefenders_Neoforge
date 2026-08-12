package com.github.c0c0tier.dungeon_defenders.entity.ai;

import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

// Variante à distance de AttackEterniaCrystalGoal : converge jusqu'à portée de tir (pas
// jusqu'au corps à corps), tend l'arc, puis tire une flèche sur le Cristal d'Eternia à
// intervalle régulier. Même logique "harnais" que la version mêlée : les dégâts sont
// appliqués directement au cristal sur un minuteur, pas via une vraie détection de collision
// de la flèche — la flèche n'est là que pour le rendu visuel du tir (pertinent puisque le
// cristal n'est pas une entité, une flèche vanilla ne saurait pas le "toucher" toute seule).
//
// Dégâts, cadence et portée de tir configurables par le constructeur (voir
// AbstractEterniaCrystalAttackGoal) : réutilisable tel quel par un futur ennemi à distance
// avec d'autres chiffres — pas besoin d'une nouvelle classe juste pour ça. Rien ici n'est
// spécifique au squelette, qui fournit juste le PathfinderMob et son arc déjà équipé.
public class RangedAttackEterniaCrystalGoal extends AbstractEterniaCrystalAttackGoal {

    public static final int DEFAULT_DAMAGE_PER_HIT = 3;
    public static final int DEFAULT_TICKS_BETWEEN_SHOTS = 20;
    public static final double DEFAULT_SHOOT_RANGE = 10.0D;

    private static final double SPEED_MODIFIER = 1.2D;
    // Temps de tension de l'arc avant le tir, en ticks (visuel : le mob lève son arc). Pas
    // exposé au constructeur, contrairement aux dégâts/cadence/portée : c'est un détail de
    // timing d'animation, pas un levier d'équilibrage entre archétypes d'ennemis.
    private static final int DRAW_TICKS = 20;
    private static final float ARROW_VELOCITY = 1.6F;
    private static final float ARROW_INACCURACY = 10.0F;

    private final int ticksBetweenShots;
    private int hitCooldown;
    private int drawTicks;

    public RangedAttackEterniaCrystalGoal(PathfinderMob mob) {
        this(mob, DEFAULT_DAMAGE_PER_HIT, DEFAULT_TICKS_BETWEEN_SHOTS, DEFAULT_SHOOT_RANGE);
    }

    public RangedAttackEterniaCrystalGoal(PathfinderMob mob, int damagePerHit, int ticksBetweenShots, double shootRange) {
        super(mob, SPEED_MODIFIER, shootRange, damagePerHit);
        this.ticksBetweenShots = ticksBetweenShots;
    }

    @Override
    public void start() {
        super.start();
        this.hitCooldown = 0;
        this.drawTicks = 0;
    }

    @Override
    public void stop() {
        super.stop();
        if (this.mob.isUsingItem()) {
            this.mob.stopUsingItem();
        }
        this.drawTicks = 0;
    }

    @Override
    protected void onReachedTarget(@Nullable EterniaCrystalBlockEntity crystal) {
        // À portée : se tourne vers le cristal (MoveToBlockGoal ne le fait plus une fois
        // arrivé, contrairement à la navigation pendant l'approche).
        this.mob.getLookControl().setLookAt(
                this.blockPos.getX() + 0.5D, this.blockPos.getY() + 1.5D, this.blockPos.getZ() + 0.5D);

        if (this.drawTicks > 0) {
            this.drawTicks--;
            if (this.drawTicks == 0) {
                shoot(crystal);
            }
            return;
        }

        if (this.hitCooldown > 0) {
            this.hitCooldown--;
            return;
        }

        this.mob.startUsingItem(InteractionHand.MAIN_HAND);
        this.drawTicks = DRAW_TICKS;
    }

    @Override
    protected void onTargetLost() {
        this.hitCooldown = 0;
        if (this.drawTicks > 0) {
            this.mob.stopUsingItem();
            this.drawTicks = 0;
        }
    }

    private void shoot(@Nullable EterniaCrystalBlockEntity crystal) {
        this.mob.stopUsingItem();
        this.hitCooldown = this.ticksBetweenShots;

        if (crystal == null || !(this.mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        spawnArrow(serverLevel);
        crystal.damage(this.damagePerHit);
    }

    private void spawnArrow(ServerLevel level) {
        ItemStack bowStack = this.mob.getMainHandItem().is(Items.BOW)
                ? this.mob.getMainHandItem()
                : new ItemStack(Items.BOW);
        Arrow arrow = new Arrow(level, this.mob, bowStack, new ItemStack(Items.ARROW));

        double targetX = this.blockPos.getX() + 0.5D;
        double targetY = this.blockPos.getY() + 1.5D;
        double targetZ = this.blockPos.getZ() + 0.5D;

        double dx = targetX - this.mob.getX();
        double dy = targetY - arrow.getY();
        double dz = targetZ - this.mob.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        // Léger arc vers le haut pour compenser la gravité de la flèche sur la distance,
        // même calcul que les tirs vanilla (skeleton, dispenser...).
        arrow.shoot(dx, dy + horizontalDistance * 0.2D, dz, ARROW_VELOCITY, ARROW_INACCURACY);
        this.mob.swing(InteractionHand.MAIN_HAND);
        level.addFreshEntity(arrow);
    }
}
