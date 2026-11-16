package com.github.c0c0tier.dungeon_defenders.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

// Sort 1 du Géomancien : "Séisme". Le sol tremble autour du joueur : tous les monstres à 6 blocs
// sont blessés et projetés en l'air, donc hors d'état de frapper le temps de retomber. Un sort
// pour se dégager ou pour soulager un mur.
//
// Toutes les valeurs sont des valeurs de test, à ajuster en jeu.
public final class EarthquakeAbility implements BurstAbility {

    public static final EarthquakeAbility INSTANCE = new EarthquakeAbility();

    private static final int MANA_COST = 60;
    private static final int COOLDOWN_TICKS = 200;
    private static final double RADIUS = 6.0D;
    private static final float DAMAGE = 5.0F;
    private static final double LIFT = 0.7D;

    private EarthquakeAbility() {
    }

    @Override
    public Component displayName() {
        return Component.translatable("dungeon_defenders.ability.earthquake");
    }

    @Override
    public Item icon() {
        return Items.COBBLESTONE;
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
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F, 0.5F);
        level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.2D, player.getZ(),
                60, RADIUS / 2.0D, 0.1D, RADIUS / 2.0D, 0.02D);

        AABB area = new AABB(player.blockPosition()).inflate(RADIUS);
        for (Monster monster : level.getEntitiesOfClass(Monster.class, area)) {
            monster.hurt(level.damageSources().playerAttack(player), DAMAGE);
            monster.setDeltaMovement(monster.getDeltaMovement().x, LIFT, monster.getDeltaMovement().z);
            monster.hurtMarked = true;
        }
    }
}
