package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

// Catégorie de tours "à impulsions" (roster de l'Alchimiste, 2026-11-16) : la tour agit à
// intervalle régulier sur tout ce qui l'entoure (ralentir, arroser, renforcer ses voisines)
// plutôt que de viser une cible unique comme une Turret ou de piquer au contact comme une
// Blockade. Chaque tour concrète ne dit que deux choses : tous les combien de ticks elle agit,
// et ce qu'elle fait alors.
//
// Priorité IA "tourelle" (la plus basse) : ce sont des tours de soutien posées en retrait, un
// monstre de mêlée ne s'y attaque qu'en dernier recours.
public abstract class AbstractPulseTowerBlockEntity extends AbstractTowerBlockEntity {

    private long lastPulseTick = Long.MIN_VALUE / 2;

    protected AbstractPulseTowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            TowerDefinition definition) {
        super(type, pos, state, definition);
    }

    @Override
    public int getAiPriority() {
        return AiAttackTarget.PRIORITY_RANGED_TOWER;
    }

    /** Nombre de ticks entre deux impulsions (lu à chaque tick : peut venir d'une stat). */
    protected abstract int pulseIntervalTicks();

    /** Une impulsion : l'effet de la tour. */
    protected abstract void pulse(ServerLevel level);

    public final void serverTick(ServerLevel level) {
        long now = level.getGameTime();
        if (now - this.lastPulseTick < pulseIntervalTicks()) {
            return;
        }
        this.lastPulseTick = now;
        pulse(level);
    }

    /** Les monstres à {@code range} blocs ou moins (distance horizontale), tout autour. */
    protected final List<Monster> monstersInRange(ServerLevel level, double range) {
        return monstersInCone(level, range, TowerDefinition.FULL_CIRCLE_DEGREES, Direction.NORTH);
    }

    /**
     * Les monstres à portée dans un cône centré sur {@code facing} — même géométrie que le
     * ciblage des Turrets (AbstractTurretBlockEntity#findTarget), mais en gardant tout le monde
     * au lieu du plus proche.
     */
    protected final List<Monster> monstersInCone(ServerLevel level, double range, double coneAngleDegrees, Direction facing) {
        return monstersAround(level, Vec3.atCenterOf(this.worldPosition), range, coneAngleDegrees, facing);
    }

    protected static List<Monster> monstersAround(ServerLevel level, Vec3 origin, double range,
            double coneAngleDegrees, Direction facing) {
        List<Monster> found = new ArrayList<>();
        AABB searchArea = new AABB(origin, origin).inflate(range);
        Vec3 facingVec = new Vec3(facing.getStepX(), 0.0D, facing.getStepZ());

        for (Monster monster : level.getEntitiesOfClass(Monster.class, searchArea)) {
            if (!monster.isAlive()) {
                continue;
            }
            Vec3 offset = new Vec3(monster.getX() - origin.x, 0.0D, monster.getZ() - origin.z);
            double distSq = offset.lengthSqr();
            if (distSq > range * range) {
                continue;
            }
            if (coneAngleDegrees < TowerDefinition.FULL_CIRCLE_DEGREES && distSq > 1.0E-4D) {
                double cos = facingVec.dot(offset.normalize());
                double angle = Math.toDegrees(Math.acos(Mth.clamp(cos, -1.0D, 1.0D)));
                if (angle > coneAngleDegrees / 2.0D) {
                    continue;
                }
            }
            found.add(monster);
        }
        return found;
    }
}
