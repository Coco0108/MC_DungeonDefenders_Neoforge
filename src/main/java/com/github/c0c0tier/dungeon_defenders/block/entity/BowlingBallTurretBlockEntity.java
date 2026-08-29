package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.entity.BowlingBallEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// "Bowling Ball Turret" (nom repris du plan Excel du joueur, feuille Tours — Squire) : deuxième
// membre concret de la catégorie "Turret". Contrairement à Harpoon Turret (une seule cible),
// tire une vraie boule qui traverse plusieurs ennemis alignés sans s'arrêter au premier —
// décidé avec le joueur (2026-08-29), voir BowlingBallEntity pour le mécanisme (perforation
// réelle d'AbstractArrow, pas une simulation).
public class BowlingBallTurretBlockEntity extends AbstractTurretBlockEntity {

    public static final int MAX_HEALTH = 20;
    public static final int MANA_COST = 55;
    public static final double RANGE = 12.0D;
    private static final float DAMAGE = 5.0F;
    private static final long ATTACK_INTERVAL_TICKS = 40L;
    private static final float BALL_VELOCITY = 1.5F;
    // La boule continue au-delà d'un premier ennemi touché (perforation) jusqu'à cette
    // distance, indépendamment du nombre d'ennemis rencontrés en chemin.
    private static final double MAX_BALL_DISTANCE = RANGE + 4.0D;

    public BowlingBallTurretBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.BOWLING_BALL_TURRET_BE.get(), pos, state,
                MAX_HEALTH, MANA_COST, RANGE, 45.0D, DAMAGE, ATTACK_INTERVAL_TICKS);
    }

    // Redéfinit entièrement le tir par défaut (flèche cosmétique + dégâts directs à une seule
    // cible) : ici, une vraie BowlingBallEntity est lancée, et c'est ELLE qui applique les
    // dégâts via sa propre collision — pas de target.hurt(...) ici, ce serait un double dégât
    // sur la première cible touchée.
    @Override
    protected void fireAt(ServerLevel level, BlockPos pos, Monster target, Direction facing) {
        Vec3 origin = muzzlePosition(pos, facing);
        Vec3 direction = new Vec3(
                target.getX() - origin.x,
                target.getEyeY() - origin.y,
                target.getZ() - origin.z
        ).normalize();

        BowlingBallEntity ball = new BowlingBallEntity(
                level, origin.x, origin.y, origin.z, direction, BALL_VELOCITY, getDamage(), MAX_BALL_DISTANCE);
        level.addFreshEntity(ball);
    }
}
