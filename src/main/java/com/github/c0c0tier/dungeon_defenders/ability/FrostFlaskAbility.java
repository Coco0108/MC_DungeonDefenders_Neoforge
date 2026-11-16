package com.github.c0c0tier.dungeon_defenders.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Sort 1 de l'Alchimiste : "Fiole de givre". Une fiole se brise à l'endroit visé (jusqu'à 16
// blocs) et fige presque sur place tous les monstres alentour pendant trois secondes, avec
// quelques dégâts. Un sort de contrôle, pour sauver un mur qui cède ou garder un groupe dans une
// flaque du Lance-fioles — pas un sort de dégâts comme la Frappe circulaire de l'Écuyer.
//
// Toutes les valeurs sont des valeurs de test, à ajuster en jeu.
public final class FrostFlaskAbility implements BurstAbility {

    public static final FrostFlaskAbility INSTANCE = new FrostFlaskAbility();

    private static final int MANA_COST = 50;
    private static final int COOLDOWN_TICKS = 160;
    private static final double REACH = 16.0D;
    private static final double RADIUS = 4.0D;
    private static final float DAMAGE = 4.0F;
    private static final int FREEZE_DURATION_TICKS = 60;
    // Lenteur VI : -90 % de vitesse, le monstre avance encore mais à peine.
    private static final int FREEZE_AMPLIFIER = 5;

    private FrostFlaskAbility() {
    }

    @Override
    public Component displayName() {
        return Component.translatable("dungeon_defenders.ability.frost_flask");
    }

    @Override
    public Item icon() {
        return Items.SNOWBALL;
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
        // Le point visé : le premier bloc touché par le regard, ou le bout de la portée dans le
        // vide (HitResult#getLocation vaut alors la fin du rayon).
        Vec3 impact = player.pick(REACH, 1.0F, false).getLocation();

        level.sendParticles(ParticleTypes.SNOWFLAKE, impact.x, impact.y + 0.5D, impact.z,
                60, RADIUS / 2.0D, 0.5D, RADIUS / 2.0D, 0.02D);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);

        AABB area = new AABB(impact, impact).inflate(RADIUS);
        for (Monster monster : level.getEntitiesOfClass(Monster.class, area)) {
            monster.hurt(level.damageSources().playerAttack(player), DAMAGE);
            monster.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, FREEZE_DURATION_TICKS, FREEZE_AMPLIFIER), player);
        }
    }
}
