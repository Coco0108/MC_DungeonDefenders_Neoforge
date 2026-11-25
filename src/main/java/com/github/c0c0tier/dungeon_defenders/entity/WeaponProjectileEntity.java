package com.github.c0c0tier.dungeon_defenders.entity;

import com.github.c0c0tier.dungeon_defenders.init.ModEntities;
import com.github.c0c0tier.dungeon_defenders.item.HeroWeaponItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Projectile visible des armes à distance des héros (2026-11-24) : une fiole, un rivet ou un
// éclat de pierre qui vole, à la place de l'ancien tir instantané par rayon. L'EFFET reste celui
// de l'arme (HeroWeaponItem#impact) : ce projectile ne décide que d'OÙ et QUAND il s'applique.
//
//   - il ne touche que les monstres (canHitEntity) : il traverse les joueurs, mais touche le
//     mannequin d'entraînement (un Zombie, donc un Monster) pour qu'on puisse s'y essayer ; un
//     bloc l'arrête ;
//   - au-delà de la portée de l'arme, il éclate en l'air (armes à zone) ou disparaît (rivet) ;
//   - il n'est pas sauvegardé : rechargé sans son arme, il disparaît au premier tick.
//
// Rendu : ThrownItemRenderer vanilla, qui dessine l'item porté (getItem) — aucun modèle à faire.
public class WeaponProjectileEntity extends ThrowableItemProjectile {

    private @Nullable HeroWeaponItem weapon;
    private Vec3 origin = Vec3.ZERO;
    private double maxDistanceSq;
    private double gravity;

    public WeaponProjectileEntity(EntityType<? extends WeaponProjectileEntity> type, Level level) {
        super(type, level);
    }

    public WeaponProjectileEntity(ServerLevel level, Player owner, HeroWeaponItem weapon, ItemStack look,
            double range, float speed, double gravity) {
        super(ModEntities.WEAPON_PROJECTILE.get(), owner, level, look);
        this.weapon = weapon;
        this.origin = this.position();
        this.maxDistanceSq = range * range;
        this.gravity = gravity;
        this.setNoGravity(gravity <= 0.0D);
        this.shootFromRotation(owner, owner.getXRot(), owner.getYHeadRot(), 0.0F, speed, 0.0F);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IRON_NUGGET;
    }

    @Override
    protected double getDefaultGravity() {
        return this.gravity;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return target instanceof Monster && target.isAlive() && super.canHitEntity(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) {
            return;
        }
        if (this.weapon == null || !(this.getOwner() instanceof Player)) {
            this.discard();
            return;
        }
        this.weapon.trailParticle(level, this.position());
        if (this.position().distanceToSqr(this.origin) > this.maxDistanceSq) {
            burst(level, this.position(), null);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level() instanceof ServerLevel level) {
            burst(level, result.getLocation(), result.getEntity());
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel level) {
            burst(level, result.getLocation(), null);
        }
    }

    private void burst(ServerLevel level, Vec3 at, @Nullable Entity direct) {
        if (this.isRemoved()) {
            return;
        }
        if (this.weapon != null && this.getOwner() instanceof Player player) {
            this.weapon.impact(level, player, at, direct);
        }
        this.discard();
    }
}
