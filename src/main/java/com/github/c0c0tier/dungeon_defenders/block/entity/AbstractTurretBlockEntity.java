package com.github.c0c0tier.dungeon_defenders.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Catégorie de tours "Turret" (voir doc/05-etat-et-problemes-connus.md, section Système de
// tours) : attaque à distance plutôt que de bloquer le passage. Contrairement à Blockade, c'est
// la tour elle-même qui agit (scan + tir à chaque tick), pas un Goal porté par un monstre — même
// principe que SpawnerBlockEntity, qui scanne/agit tout seul sans dépendre d'une IA externe.
//
// Le tir est limité à un cône fixe autour de HORIZONTAL_FACING (le bloc doit déclarer cette
// propriété — voir HarpoonTurretBlock) : angle constant à l'origine, la largeur du cône à
// l'extrémité augmente avec la portée (pure trigonométrie, pas un angle qui s'élargit avec la
// distance). coneAngleDegrees >= 360 = omnidirectionnel, pas de filtre d'angle.
//
// PV/coût mana/persistance/sync viennent de AbstractTowerBlockEntity, commun à toutes les
// catégories de tours. Pas de goal d'IA pour qu'un monstre attaque un turret pour l'instant
// (décidé avec le joueur) : les PV existent, mais rien ne les vise encore.
public abstract class AbstractTurretBlockEntity extends AbstractTowerBlockEntity {

    private static final float ARROW_VELOCITY = 3.0F;
    private static final float ARROW_INACCURACY = 1.0F;

    private final double range;
    private final double coneAngleDegrees;
    private final float damage;
    private final long attackIntervalTicks;

    private long lastFireTick = Long.MIN_VALUE;

    protected AbstractTurretBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            int maxHealth, int manaCost, double range, double coneAngleDegrees,
            float damage, long attackIntervalTicks) {
        super(type, pos, state, maxHealth, manaCost);
        this.range = range;
        this.coneAngleDegrees = coneAngleDegrees;
        this.damage = damage;
        this.attackIntervalTicks = attackIntervalTicks;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractTurretBlockEntity turret) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        long now = serverLevel.getGameTime();
        if (now - turret.lastFireTick < turret.attackIntervalTicks) {
            return;
        }

        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return;
        }
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        Monster target = turret.findTarget(serverLevel, pos, facing);
        if (target == null) {
            return;
        }

        turret.lastFireTick = now;
        turret.fireAt(serverLevel, pos, target);
    }

    private @Nullable Monster findTarget(ServerLevel level, BlockPos pos, Direction facing) {
        AABB searchArea = new AABB(pos).inflate(this.range);
        Vec3 origin = Vec3.atCenterOf(pos);
        Vec3 facingVec = new Vec3(facing.getStepX(), 0.0D, facing.getStepZ());

        Monster nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (Monster monster : level.getEntitiesOfClass(Monster.class, searchArea)) {
            Vec3 horizontalOffset = new Vec3(monster.getX() - origin.x, 0.0D, monster.getZ() - origin.z);
            double distSq = horizontalOffset.lengthSqr();
            if (distSq > this.range * this.range) {
                continue;
            }

            if (this.coneAngleDegrees < 360.0D && distSq > 1.0E-4D) {
                double cos = facingVec.dot(horizontalOffset.normalize());
                double angle = Math.toDegrees(Math.acos(Mth.clamp(cos, -1.0D, 1.0D)));
                if (angle > this.coneAngleDegrees / 2.0D) {
                    continue;
                }
            }

            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = monster;
            }
        }

        return nearest;
    }

    private void fireAt(ServerLevel level, BlockPos pos, Monster target) {
        spawnArrow(level, pos, target);
        target.hurt(level.damageSources().generic(), this.damage);
    }

    // Flèche purement visuelle, comme RangedAttackEterniaCrystalGoal pour le squelette archer :
    // les dégâts sont appliqués directement (voir fireAt), pas via une détection de collision.
    private void spawnArrow(ServerLevel level, BlockPos pos, Monster target) {
        double originX = pos.getX() + 0.5D;
        double originY = pos.getY() + 0.5D;
        double originZ = pos.getZ() + 0.5D;

        // Pas de LivingEntity propriétaire (c'est un bloc) : constructeur Arrow sans owner.
        Arrow arrow = new Arrow(level, originX, originY, originZ, new ItemStack(Items.ARROW), null);

        double dx = target.getX() - originX;
        double dy = target.getEyeY() - originY;
        double dz = target.getZ() - originZ;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        // Léger arc vers le haut pour compenser la gravité sur la distance, même calcul que
        // RangedAttackEterniaCrystalGoal (et les tirs vanilla : squelette, dispenser...).
        arrow.shoot(dx, dy + horizontalDistance * 0.2D, dz, ARROW_VELOCITY, ARROW_INACCURACY);
        level.addFreshEntity(arrow);
    }
}
