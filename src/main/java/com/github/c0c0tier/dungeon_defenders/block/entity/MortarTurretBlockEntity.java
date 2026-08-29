package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

// "Mortar Turret" (nom repris du plan Excel du joueur, feuille Tours — Squire) : troisième
// membre concret de la catégorie "Turret". Confirmé avec le joueur (2026-08-29) : contrairement
// à Bowling Ball Turret (perforation, une boule qui continue en ligne droite), on veut ici de
// vrais dégâts de zone façon explosion à l'impact — sans dégât de terrain, décidé explicitement.
// Réutilise donc le tir cosmétique de la base (spawnArrow, hérité tel quel) mais applique les
// dégâts à TOUS les monstres dans un rayon autour de la cible plutôt qu'à elle seule — même
// principe de scan par AABB que AbstractBlockadeBlockEntity#serverTick, appliqué une fois au
// point d'impact plutôt qu'en continu autour du bloc.
public class MortarTurretBlockEntity extends AbstractTurretBlockEntity {

    public static final int MAX_HEALTH = 20;
    // Le plus cher des quatre nouvelles tours : dégâts de zone, valeur de test comme le reste.
    public static final int MANA_COST = 70;
    public static final double RANGE = 14.0D;
    private static final float DAMAGE = 8.0F;
    // Cadence la plus lente du roster (2x Harpoon) : compense la puissance des dégâts de zone.
    private static final long ATTACK_INTERVAL_TICKS = 60L;
    private static final double SPLASH_RADIUS = 2.0D;

    public MortarTurretBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.MORTAR_TURRET_BE.get(), pos, state,
                MAX_HEALTH, MANA_COST, RANGE, 45.0D, DAMAGE, ATTACK_INTERVAL_TICKS);
    }

    // Redéfinit uniquement l'application des dégâts (zone plutôt qu'une seule cible) ; le tir
    // cosmétique reste inchangé (spawnArrow, hérité de la base — même limite assumée que
    // Harpoon Turret : la "explosion" est instantanée à l'envoi, pas retardée jusqu'à l'arrivée
    // visuelle de la flèche).
    @Override
    protected void fireAt(ServerLevel level, BlockPos pos, Monster target, Direction facing) {
        spawnArrow(level, pos, target, facing);

        AABB splashArea = new AABB(target.blockPosition()).inflate(SPLASH_RADIUS);
        for (Monster monster : level.getEntitiesOfClass(Monster.class, splashArea)) {
            monster.hurt(level.damageSources().generic(), getDamage());
        }
    }
}
