package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.entity.BowlingBallEntity;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
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

    // Chiffres : data-driven, voir data/dungeon_defenders/dungeon_defenders/tower/
    // bowling_ball_turret.json et TowerDefinition.BOWLING_BALL_TURRET pour les valeurs par défaut.
    //
    // projectile_extra_distance : la boule continue au-delà d'un premier ennemi touché
    // (perforation) jusqu'à "range + projectile_extra_distance", indépendamment du nombre
    // d'ennemis rencontrés en chemin.
    public BowlingBallTurretBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.BOWLING_BALL_TURRET_BE.get(), pos, state, TowerDefinition.BOWLING_BALL_TURRET);
    }

    // Redéfinit entièrement le tir par défaut (flèche cosmétique + dégâts directs à une seule
    // cible) : ici, une vraie BowlingBallEntity est lancée, et c'est ELLE qui applique les
    // dégâts via sa propre collision — pas de target.hurt(...) ici, ce serait un double dégât
    // sur la première cible touchée.
    @Override
    protected void fireAt(ServerLevel level, BlockPos pos, Monster target, Direction facing) {
        Vec3 origin = muzzlePosition(pos, facing);
        // Uniquement horizontale (composante Y à 0), pas visée vers target.getEyeY() : ça
        // faisait partir la boule vers le haut façon tir à l'arbalète (signalé en jeu,
        // 2026-08-29). Une boule qui roule au sol garde la hauteur de tir fixe, elle ne monte
        // ni ne descend pour viser — combiné à setNoGravity(true) dans BowlingBallEntity, elle
        // file maintenant en ligne droite parfaitement horizontale.
        Vec3 direction = new Vec3(
                target.getX() - origin.x,
                0.0D,
                target.getZ() - origin.z
        ).normalize();

        BowlingBallEntity ball = new BowlingBallEntity(
                level, origin.x, origin.y, origin.z, direction,
                stats().floatValue(TowerStat.PROJECTILE_VELOCITY), getDamage(),
                getRange() + stats().get(TowerStat.PROJECTILE_EXTRA_DISTANCE));
        level.addFreshEntity(ball);
    }
}
