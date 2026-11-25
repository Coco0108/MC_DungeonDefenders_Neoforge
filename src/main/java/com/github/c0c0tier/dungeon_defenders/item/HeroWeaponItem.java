package com.github.c0c0tier.dungeon_defenders.item;

import com.github.c0c0tier.dungeon_defenders.entity.WeaponProjectileEntity;
import com.github.c0c0tier.dungeon_defenders.init.BalanceValue;
import com.github.c0c0tier.dungeon_defenders.init.GameBalance;
import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

// Arme à distance d'un héros : l'attaque de base au clic droit (2026-11-16). Trois armes, une
// seule classe — elles ne diffèrent que par leurs chiffres et par un effet :
//   - Sacoche de fioles (Alchimiste) : une fiole en cloche, qui éclate en blessant une petite zone ;
//   - Pistolet à rivets (Ingénieur) : un rivet rapide et droit, une seule cible, loin ;
//   - Bâton de géomancien (Géomancien) : un éclat de pierre, courte portée, petite zone, ralentit.
// L'Écuyer a une vraie épée (mêlée vanilla, clic gauche), voir ModBlocks.SQUIRE_SWORD.
//
// **Vrai projectile** depuis le 2026-11-24 (avant : tir instantané par rayon) : fire() lance un
// WeaponProjectileEntity qui vole, s'arrête au premier bloc ou au premier monstre, et appelle
// impact() à cet endroit. L'effet est donc le même qu'avant, mais il faut viser une cible qui
// bouge. Vitesse et gravité sont des constantes de Kind (le ressenti du vol, pas de l'équilibrage).
//
// Chiffres : balance/weapons.json (doc/data-driven/equilibrage.md). Les dégâts passent par
// playerAttack, ils reçoivent donc le bonus de niveau du joueur (ModEvents).
public class HeroWeaponItem extends Item {

    public enum Kind {
        FLASK(BalanceValue.WEAPON_FLASK_DAMAGE, BalanceValue.WEAPON_FLASK_RANGE, BalanceValue.WEAPON_FLASK_RADIUS,
                BalanceValue.WEAPON_FLASK_COOLDOWN_TICKS, ParticleTypes.WITCH, SoundEvents.SPLASH_POTION_BREAK, 0,
                Items.SPLASH_POTION, 1.4F, 0.03D, SoundEvents.SPLASH_POTION_THROW),
        RIVET(BalanceValue.WEAPON_RIVET_DAMAGE, BalanceValue.WEAPON_RIVET_RANGE, null,
                BalanceValue.WEAPON_RIVET_COOLDOWN_TICKS, ParticleTypes.CRIT, SoundEvents.IRON_GOLEM_HURT, 0,
                Items.IRON_NUGGET, 3.0F, 0.0D, SoundEvents.PISTON_EXTEND),
        STAFF(BalanceValue.WEAPON_STAFF_DAMAGE, BalanceValue.WEAPON_STAFF_RANGE, BalanceValue.WEAPON_STAFF_RADIUS,
                BalanceValue.WEAPON_STAFF_COOLDOWN_TICKS, ParticleTypes.POOF, SoundEvents.STONE_BREAK, 40,
                Items.FLINT, 1.6F, 0.0D, SoundEvents.STONE_HIT);

        private final BalanceValue damage;
        private final BalanceValue range;
        // null : une seule cible, le monstre touché par le projectile.
        private final @Nullable BalanceValue radius;
        private final BalanceValue cooldownTicks;
        private final ParticleOptions particle;
        private final SoundEvent impactSound;
        // Durée de Lenteur I appliquée aux monstres touchés ; 0 = aucun ralentissement.
        private final int slowTicks;
        // Le projectile : l'item qu'il montre en vol, sa vitesse (blocs par tick), sa gravité
        // (0 = vol droit) et le son du tir.
        private final Item projectileItem;
        private final float speed;
        private final double gravity;
        private final SoundEvent fireSound;

        Kind(BalanceValue damage, BalanceValue range, @Nullable BalanceValue radius, BalanceValue cooldownTicks,
                ParticleOptions particle, SoundEvent impactSound, int slowTicks,
                Item projectileItem, float speed, double gravity, SoundEvent fireSound) {
            this.damage = damage;
            this.range = range;
            this.radius = radius;
            this.cooldownTicks = cooldownTicks;
            this.particle = particle;
            this.impactSound = impactSound;
            this.slowTicks = slowTicks;
            this.projectileItem = projectileItem;
            this.speed = speed;
            this.gravity = gravity;
            this.fireSound = fireSound;
        }
    }

    private final Kind kind;

    public HeroWeaponItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        // L'arme d'un autre héros ne tire pas : elle ne devrait d'ailleurs jamais être dans
        // l'inventaire (voir HeroWeapons#ensure), sauf prise en créatif.
        if (HeroDefinition.of(player).weapon() != this) {
            player.sendSystemMessage(Component.translatable("dungeon_defenders.weapon.wrong_hero"));
            return InteractionResult.FAIL;
        }

        fire(serverLevel, player);
        player.getCooldowns().addCooldown(stack, GameBalance.getInt(this.kind.cooldownTicks, false));
        return InteractionResult.SUCCESS;
    }

    /**
     * Un tir (serveur) : lance le projectile depuis les yeux du joueur, dans la direction de son
     * regard, et le renvoie. Public pour les gametests, qui n'ont pas de vraie main à cliquer.
     */
    public WeaponProjectileEntity fire(ServerLevel level, Player player) {
        WeaponProjectileEntity projectile = new WeaponProjectileEntity(level, player, this,
                new ItemStack(this.kind.projectileItem), GameBalance.get(this.kind.range, false),
                this.kind.speed, this.kind.gravity);
        level.addFreshEntity(projectile);
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), this.kind.fireSound, SoundSource.PLAYERS, 0.5F, 1.6F);
        return projectile;
    }

    /**
     * Effet de l'arme à l'endroit où son projectile s'arrête : le monstre touché seul (rivet), ou
     * tous les monstres dans le rayon (fiole, bâton). {@code direct} = le monstre percuté, s'il y
     * en a un.
     */
    public void impact(ServerLevel level, Player player, Vec3 at, @Nullable Entity direct) {
        level.playSound(null, at.x, at.y, at.z, this.kind.impactSound, SoundSource.PLAYERS, 0.6F, 1.4F);
        float damage = (float) GameBalance.get(this.kind.damage, false);
        if (this.kind.radius == null) {
            if (direct instanceof Monster monster) {
                hit(level, player, monster, damage);
            }
            return;
        }

        double radius = GameBalance.get(this.kind.radius, false);
        level.sendParticles(this.kind.particle, at.x, at.y + 0.2D, at.z,
                12, radius / 2.0D, 0.2D, radius / 2.0D, 0.02D);
        List<Monster> monsters = level.getEntitiesOfClass(Monster.class, new AABB(at, at).inflate(radius),
                monster -> monster.isAlive() && monster.getBoundingBox().distanceToSqr(at) <= radius * radius);
        for (Monster monster : monsters) {
            hit(level, player, monster, damage);
        }
    }

    /** Une particule de traînée derrière le projectile, à chaque tick de vol. */
    public void trailParticle(ServerLevel level, Vec3 at) {
        level.sendParticles(this.kind.particle, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private void hit(ServerLevel level, Player player, Monster monster, float damage) {
        monster.hurtServer(level, level.damageSources().playerAttack(player), damage);
        if (this.kind.slowTicks > 0) {
            monster.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, this.kind.slowTicks, 0), player);
        }
    }
}
