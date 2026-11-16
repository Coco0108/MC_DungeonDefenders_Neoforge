package com.github.c0c0tier.dungeon_defenders.item;

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
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Arme à distance d'un héros : l'attaque de base au clic droit (2026-11-16). Trois armes, une
// seule classe — elles ne diffèrent que par leurs chiffres et par un effet :
//   - Sacoche de fioles (Alchimiste) : éclate à l'endroit visé, blesse une petite zone ;
//   - Pistolet à rivets (Ingénieur) : une seule cible, loin, cadence élevée ;
//   - Bâton de géomancien (Géomancien) : courte portée, petite zone, ralentit.
// L'Écuyer a une vraie épée (mêlée vanilla, clic gauche), voir ModBlocks.SQUIRE_SWORD.
//
// **Tir instantané, pas de projectile** : un rayon part des yeux du joueur, s'arrête au premier
// bloc ou au premier monstre, et l'effet s'applique à ce point. Une traînée de particules dessine
// le tir. Choix assumé : ni entité ni renderer à écrire, donc rien qui ne puisse être vérifié
// que par un gametest. Un vrai projectile (fiole qui vole) pourra remplacer le rayon plus tard.
//
// Chiffres : balance/weapons.json (doc/data-driven/equilibrage.md). Les dégâts passent par
// playerAttack, ils reçoivent donc le bonus de niveau du joueur (ModEvents).
public class HeroWeaponItem extends Item {

    public enum Kind {
        FLASK(BalanceValue.WEAPON_FLASK_DAMAGE, BalanceValue.WEAPON_FLASK_RANGE, BalanceValue.WEAPON_FLASK_RADIUS,
                BalanceValue.WEAPON_FLASK_COOLDOWN_TICKS, ParticleTypes.WITCH, SoundEvents.SPLASH_POTION_BREAK, 0),
        RIVET(BalanceValue.WEAPON_RIVET_DAMAGE, BalanceValue.WEAPON_RIVET_RANGE, null,
                BalanceValue.WEAPON_RIVET_COOLDOWN_TICKS, ParticleTypes.CRIT, SoundEvents.PISTON_EXTEND, 0),
        STAFF(BalanceValue.WEAPON_STAFF_DAMAGE, BalanceValue.WEAPON_STAFF_RANGE, BalanceValue.WEAPON_STAFF_RADIUS,
                BalanceValue.WEAPON_STAFF_COOLDOWN_TICKS, ParticleTypes.POOF, SoundEvents.STONE_BREAK, 40);

        private final BalanceValue damage;
        private final BalanceValue range;
        // null : une seule cible, le monstre touché par le rayon.
        private final BalanceValue radius;
        private final BalanceValue cooldownTicks;
        private final ParticleOptions particle;
        private final SoundEvent sound;
        // Durée de Lenteur I appliquée aux monstres touchés ; 0 = aucun ralentissement.
        private final int slowTicks;

        Kind(BalanceValue damage, BalanceValue range, BalanceValue radius, BalanceValue cooldownTicks,
                ParticleOptions particle, SoundEvent sound, int slowTicks) {
            this.damage = damage;
            this.range = range;
            this.radius = radius;
            this.cooldownTicks = cooldownTicks;
            this.particle = particle;
            this.sound = sound;
            this.slowTicks = slowTicks;
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

    /** Un tir (serveur). Public pour les gametests, qui n'ont pas de vraie main à cliquer. */
    public void fire(ServerLevel level, Player player) {
        double range = GameBalance.get(this.kind.range, false);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(range));

        BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 stop = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player, eye, stop, searchBox, entity -> entity instanceof Monster && entity.isAlive(), eye.distanceToSqr(stop));
        Entity direct = entityHit == null ? null : entityHit.getEntity();
        Vec3 impact = entityHit == null ? stop : entityHit.getLocation();

        trail(level, eye, impact);
        level.playSound(null, impact.x, impact.y, impact.z, this.kind.sound, SoundSource.PLAYERS, 0.6F, 1.4F);

        float damage = (float) GameBalance.get(this.kind.damage, false);
        if (this.kind.radius == null) {
            if (direct instanceof Monster monster) {
                hit(level, player, monster, damage);
            }
            return;
        }

        double radius = GameBalance.get(this.kind.radius, false);
        level.sendParticles(this.kind.particle, impact.x, impact.y + 0.2D, impact.z,
                12, radius / 2.0D, 0.2D, radius / 2.0D, 0.02D);
        List<Monster> monsters = level.getEntitiesOfClass(Monster.class, new AABB(impact, impact).inflate(radius),
                monster -> monster.isAlive() && monster.getBoundingBox().distanceToSqr(impact) <= radius * radius);
        for (Monster monster : monsters) {
            hit(level, player, monster, damage);
        }
    }

    private void hit(ServerLevel level, Player player, Monster monster, float damage) {
        monster.hurtServer(level, level.damageSources().playerAttack(player), damage);
        if (this.kind.slowTicks > 0) {
            monster.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, this.kind.slowTicks, 0), player);
        }
    }

    // Un point de particule par bloc, en partant un peu devant les yeux pour ne pas boucher la vue.
    private void trail(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-3D) {
            return;
        }
        Vec3 step = delta.scale(1.0D / length);
        for (double travelled = 1.5D; travelled < length; travelled += 1.0D) {
            Vec3 point = from.add(step.scale(travelled));
            level.sendParticles(this.kind.particle, point.x, point.y - 0.2D, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
