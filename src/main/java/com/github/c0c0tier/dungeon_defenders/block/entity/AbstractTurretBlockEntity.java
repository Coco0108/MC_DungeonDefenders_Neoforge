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
// catégories de tours. Priorité IA la plus basse (voir AiAttackTarget) : un monstre de mêlée
// (entity/ai/AttackPriorityTargetGoal.java) ne s'attaque à un turret qu'en tout dernier
// recours, rien de plus proche/prioritaire à portée.
//
// fireAt/spawnArrow/muzzlePosition sont protected (pas private, 2026-08-29) : Bowling Ball
// Turret et Mortar Turret redéfinissent fireAt pour leurs propres mécaniques (perforation
// réelle, dégâts de zone) — voir chaque block entity pour le détail.
public abstract class AbstractTurretBlockEntity extends AbstractTowerBlockEntity {

    private static final float ARROW_VELOCITY = 3.0F;
    private static final float ARROW_INACCURACY = 1.0F;

    // Décale l'origine du tir hors du cube plein du bloc : une flèche qui apparaît DANS la
    // géométrie de collision du bloc sous elle se fige immédiatement au premier tick
    // (AbstractArrow#tick considère qu'elle est "in ground" dès que sa position de spawn est
    // contenue dans la forme de collision du bloc sous elle) — vrai aussi bien pour une flèche
    // purement visuelle (spawnArrow) que pour une vraie flèche à collision réelle (voir
    // BowlingBallEntity) : sans ce décalage, aucun tir n'est jamais visible ni fonctionnel.
    private static final double MUZZLE_OFFSET = 0.6D;

    private final double range;
    private final double coneAngleDegrees;
    private final float damage;
    private final long attackIntervalTicks;

    private long lastFireTick;

    protected AbstractTurretBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            int maxHealth, int manaCost, double range, double coneAngleDegrees,
            float damage, long attackIntervalTicks) {
        super(type, pos, state, maxHealth, manaCost);
        this.range = range;
        this.coneAngleDegrees = coneAngleDegrees;
        this.damage = damage;
        this.attackIntervalTicks = attackIntervalTicks;
        // -attackIntervalTicks (pas Long.MIN_VALUE, la valeur d'origine) : permet de tirer dès
        // la première cible trouvée sans délai, tout en évitant l'overflow de
        // `now - lastFireTick` qu'un sentinel trop extrême provoque — Long.MIN_VALUE fait
        // déborder la soustraction vers un nombre toujours négatif, empêchant tout tir pour
        // toujours (bug trouvé en construisant le reste du roster de tours, 2026-08-29 : toute
        // tour de cette base — donc aussi Harpoon Turret — ne tirait en réalité jamais).
        this.lastFireTick = -attackIntervalTicks;
    }

    protected float getDamage() {
        return this.damage;
    }

    @Override
    public int getAiPriority() {
        return AiAttackTarget.PRIORITY_RANGED_TOWER;
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
        turret.fireAt(serverLevel, pos, target, facing);
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

    // Comportement par défaut (Harpoon Turret) : flèche purement visuelle + dégâts appliqués
    // directement à la cible unique, comme RangedAttackEterniaCrystalGoal pour le squelette
    // archer. Bowling Ball Turret et Mortar Turret redéfinissent entièrement cette méthode.
    protected void fireAt(ServerLevel level, BlockPos pos, Monster target, Direction facing) {
        spawnArrow(level, pos, target, facing);
        target.hurt(level.damageSources().generic(), this.damage);
    }

    // Protected final (pas private) : Mortar Turret réutilise cette flèche cosmétique telle
    // quelle pour son propre tir, seuls ses dégâts diffèrent (zone plutôt qu'une seule cible).
    // Bowling Ball Turret NE l'utilise PAS : il a besoin d'une vraie collision, pas d'une
    // flèche cosmétique — voir BowlingBallEntity.
    protected final void spawnArrow(ServerLevel level, BlockPos pos, Monster target, Direction facing) {
        Vec3 origin = muzzlePosition(pos, facing);

        // Pas de LivingEntity propriétaire (c'est un bloc) : constructeur Arrow sans owner.
        Arrow arrow = new Arrow(level, origin.x, origin.y, origin.z, new ItemStack(Items.ARROW), null);

        double dx = target.getX() - origin.x;
        double dy = target.getEyeY() - origin.y;
        double dz = target.getZ() - origin.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        // Léger arc vers le haut pour compenser la gravité sur la distance, même calcul que
        // RangedAttackEterniaCrystalGoal (et les tirs vanilla : squelette, dispenser...).
        arrow.shoot(dx, dy + horizontalDistance * 0.2D, dz, ARROW_VELOCITY, ARROW_INACCURACY);
        level.addFreshEntity(arrow);
    }

    protected final Vec3 muzzlePosition(BlockPos pos, Direction facing) {
        return new Vec3(
                pos.getX() + 0.5D + facing.getStepX() * MUZZLE_OFFSET,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D + facing.getStepZ() * MUZZLE_OFFSET);
    }
}
