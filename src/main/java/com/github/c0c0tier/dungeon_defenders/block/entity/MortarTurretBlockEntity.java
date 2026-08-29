package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// "Mortar Turret" (nom repris du plan Excel du joueur, feuille Tours — Squire) : troisième
// membre concret de la catégorie "Turret". Confirmé avec le joueur (2026-08-29) : contrairement
// à Bowling Ball Turret (perforation, une boule qui continue en ligne droite), on veut ici de
// vrais dégâts de zone façon explosion à l'impact — sans dégât de terrain, décidé explicitement.
// Applique les dégâts à TOUS les monstres dans un rayon autour de la cible plutôt qu'à elle
// seule — même principe de scan par AABB que AbstractBlockadeBlockEntity#serverTick, appliqué
// une fois au point d'impact plutôt qu'en continu autour du bloc.
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

    // Redéfinit entièrement le tir par défaut : plus de flèche cosmétique héritée de
    // spawnArrow (retirée, 2026-08-29) — signalée en jeu comme partant vers le ciel sans
    // jamais visiblement retomber, peu satisfaisant pour un impact censé être instantané (les
    // dégâts, eux, l'ont toujours été, jamais liés à l'arrivée réelle d'une flèche). Remplacée
    // par une particule d'explosion au point d'impact, jouée au même instant que les dégâts —
    // seulement visuelle, ParticleTypes.EXPLOSION_EMITTER ne touche ni bloc ni terrain
    // (contrairement à un vrai Explosion vanilla), cohérent avec "sans dégât de terrain".
    @Override
    protected void fireAt(ServerLevel level, BlockPos pos, Monster target, Direction facing) {
        Vec3 impact = target.position();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, impact.y, impact.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);

        AABB splashArea = new AABB(target.blockPosition()).inflate(SPLASH_RADIUS);
        for (Monster monster : level.getEntitiesOfClass(Monster.class, splashArea)) {
            monster.hurt(level.damageSources().generic(), getDamage());
        }
    }
}
