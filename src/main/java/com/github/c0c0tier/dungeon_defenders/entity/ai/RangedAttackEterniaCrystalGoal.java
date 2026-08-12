package com.github.c0c0tier.dungeon_defenders.entity.ai;

import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;

// Variante à distance de AttackEterniaCrystalGoal : converge jusqu'à portée de tir (pas
// jusqu'au corps à corps), tend l'arc, puis tire une flèche sur le Cristal d'Eternia à
// intervalle régulier. Même logique "harnais" que la version mêlée : les dégâts sont
// appliqués directement au cristal sur un minuteur, pas via une vraie détection de collision
// de la flèche — la flèche n'est là que pour le rendu visuel du tir (pertinent puisque le
// cristal n'est pas une entité, une flèche vanilla ne saurait pas le "toucher" toute seule).
//
// Pensé pour être réutilisable tel quel par un futur ennemi à distance : rien ici n'est
// spécifique au squelette (qui se contente de fournir le PathfinderMob et son arc/flèche déjà
// équipés par défaut).
public class RangedAttackEterniaCrystalGoal extends MoveToBlockGoal {

    /** Dégâts par tir — volontairement inférieurs au corps à corps (AttackEterniaCrystalGoal), valeur provisoire à ajuster après tests. */
    public static final int DAMAGE_PER_HIT = 3;
    /** Temps de tension de l'arc avant le tir, en ticks (visuel : le mob lève son arc). */
    public static final int DRAW_TICKS = 20;
    /** Pause après un tir avant de retendre l'arc. Cycle total = DRAW_TICKS + TICKS_BETWEEN_SHOTS. */
    public static final int TICKS_BETWEEN_SHOTS = 20;

    private static final int SEARCH_RANGE = 16;
    // Distance à laquelle le mob s'arrête et commence à tirer, plutôt que de continuer à
    // avancer jusqu'au corps à corps comme AttackEterniaCrystalGoal.
    private static final double SHOOT_RANGE = 10.0D;
    private static final double SPEED_MODIFIER = 1.2D;
    private static final float ARROW_VELOCITY = 1.6F;
    private static final float ARROW_INACCURACY = 10.0F;

    private int hitCooldown;
    private int drawTicks;

    public RangedAttackEterniaCrystalGoal(PathfinderMob mob) {
        super(mob, SPEED_MODIFIER, SEARCH_RANGE);
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.ETERNIA_CRYSTAL.get());
    }

    @Override
    protected BlockPos getMoveToTarget() {
        return this.blockPos;
    }

    @Override
    public double acceptedDistance() {
        return SHOOT_RANGE;
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
    public void tick() {
        super.tick();

        if (!this.isReachedTarget()) {
            this.hitCooldown = 0;
            if (this.drawTicks > 0) {
                this.mob.stopUsingItem();
                this.drawTicks = 0;
            }
            return;
        }

        // À portée : se tourne vers le cristal (MoveToBlockGoal ne le fait plus une fois
        // arrivé, contrairement à la navigation pendant l'approche).
        this.mob.getLookControl().setLookAt(
                this.blockPos.getX() + 0.5D, this.blockPos.getY() + 1.5D, this.blockPos.getZ() + 0.5D);

        if (this.drawTicks > 0) {
            this.drawTicks--;
            if (this.drawTicks == 0) {
                shoot();
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

    private void shoot() {
        this.mob.stopUsingItem();

        if (!(this.mob.level() instanceof ServerLevel serverLevel)
                || !(serverLevel.getBlockEntity(this.blockPos) instanceof EterniaCrystalBlockEntity crystal)) {
            this.hitCooldown = TICKS_BETWEEN_SHOTS;
            return;
        }

        spawnArrow(serverLevel);
        crystal.damage(DAMAGE_PER_HIT);
        this.hitCooldown = TICKS_BETWEEN_SHOTS;
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
