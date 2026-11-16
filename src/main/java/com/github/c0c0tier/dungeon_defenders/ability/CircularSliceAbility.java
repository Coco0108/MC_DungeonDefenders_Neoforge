package com.github.c0c0tier.dungeon_defenders.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

// Sort 1 de l'Écuyer (emplacement SPELL_1) — l'un des deux sorts confirmés du jeu de référence
// pour ce héros : une attaque tournante à 360° autour du joueur.
//
// **Simplification assumée** : le jeu de référence touche deux fois ("hits twice"), ici un seul
// coup avec les dégâts cumulés — pas de raison technique, juste plus simple pour une première
// version, à revoir si le ressenti en jeu le demande.
//
// 60 mana, 3 secondes de recharge (~60 ticks) : ce sont les VRAIES valeurs du jeu de référence,
// confirmées (pas des valeurs de test comme ailleurs dans le mod) — voir doc/02-gameplay.md.
public final class CircularSliceAbility implements BurstAbility {

    public static final CircularSliceAbility INSTANCE = new CircularSliceAbility();

    private static final int MANA_COST = 60;
    private static final int COOLDOWN_TICKS = 60;
    private static final double RADIUS = 4.0D;
    private static final float DAMAGE = 6.0F;
    // Repousse les monstres loin du joueur — valeur de test, pas confirmée par le jeu de
    // référence (lui ne documente que "fort recul", pas de nombre).
    private static final float KNOCKBACK_STRENGTH = 0.6F;

    private CircularSliceAbility() {
    }

    @Override
    public Component displayName() {
        return Component.translatable("dungeon_defenders.ability.circular_slice");
    }

    @Override
    public Item icon() {
        return Items.IRON_SWORD;
    }

    @Override
    public int manaCost() {
        return MANA_COST;
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public void activate(ServerPlayer player) {
        ServerLevel level = player.level();
        AABB area = new AABB(player.blockPosition()).inflate(RADIUS);

        for (Monster monster : level.getEntitiesOfClass(Monster.class, area)) {
            monster.hurt(level.damageSources().playerAttack(player), DAMAGE);

            // Même formule que AbstractBlockadeBlockEntity (Bouncer Blockade) : "position de la
            // source moins position du monstre", testée en jeu — le signe inverse attirerait
            // les monstres au lieu de les repousser.
            double dx = player.getX() - monster.getX();
            double dz = player.getZ() - monster.getZ();
            monster.knockback(KNOCKBACK_STRENGTH, dx, dz);
        }
    }
}
