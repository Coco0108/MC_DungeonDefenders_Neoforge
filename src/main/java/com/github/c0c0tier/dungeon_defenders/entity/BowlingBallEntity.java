package com.github.c0c0tier.dungeon_defenders.entity;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Projectile du Bowling Ball Turret (voir block/entity/BowlingBallTurretBlockEntity.java) —
// décidé avec le joueur (2026-08-29) : "on veut vraiment que la boule continue sur une certaine
// longueur même si elle touche un ennemi, elle continue". Contrairement à la flèche purement
// visuelle du Harpoon Turret (AbstractTurretBlockEntity#spawnArrow, dégâts appliqués
// directement par le code appelant, sans vraie collision), celui-ci utilise la VRAIE détection
// de collision d'AbstractArrow, avec un niveau de Perforation qui la laisse traverser plusieurs
// ennemis au lieu de s'arrêter au premier — le mécanisme exact d'une flèche enchantée
// Perforation, réutilisé tel quel (dégâts, son, léger recul de la cible : tout vient gratuitement
// d'AbstractArrow#onHitEntity, rien à réimplémenter).
//
// extends Arrow plutôt qu'un nouvel EntityType custom : le constructeur position (Level, x, y,
// z, ItemStack, ItemStack) hérité d'Arrow force EntityType.ARROW en interne (voir Arrow.java) —
// pas la peine d'enregistrer un EntityType/renderer dédié pour ça, la boule prend donc
// l'apparence d'une flèche vanilla en vol. Limite assumée, comme les autres placeholders
// visuels du mod (le cristal de mana a l'air d'une orbe d'XP, etc.) : pas une vraie boule pour
// l'instant.
public class BowlingBallEntity extends Arrow {

    // AbstractArrow#setPierceLevel est privé (non accessible depuis une sous-classe hors de son
    // propre package) : le seul point d'entrée public pour régler la perforation d'une flèche
    // construite hors d'un arc/arbalète est de passer un "firedFromWeapon" réellement enchanté
    // de Perforation au constructeur — c'est ce que fabrique fakePiercingWeapon ci-dessous,
    // plutôt que de la réflexion sur un champ privé vanilla.
    //
    // Valeur haute : jamais la vraie limite pratique (le rayon du Turret et MAX_DISTANCE_SQ
    // arrêtent la boule bien avant qu'elle n'ait pu toucher ce nombre d'ennemis) — "continue
    // même après avoir touché un ennemi", pas de plafond de cibles réaliste à respecter.
    private static final int PIERCE_LEVEL = 20;

    private double spawnX;
    private double spawnY;
    private double spawnZ;
    private double maxDistanceSq;

    public BowlingBallEntity(ServerLevel level, double x, double y, double z, Vec3 direction,
            float velocity, float damage, double maxDistance) {
        super(level, x, y, z, new ItemStack(Items.SNOWBALL), fakePiercingWeapon(level));
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
        this.maxDistanceSq = maxDistance * maxDistance;
        // Roule en ligne droite plutôt que de retomber en cloche comme une flèche classique —
        // et permet de viser la cible bien à plat sans compensation de gravité (contrairement à
        // AbstractTurretBlockEntity#spawnArrow).
        this.setNoGravity(true);
        this.setBaseDamage(damage);
        this.shoot(direction.x, direction.y, direction.z, velocity, 0.0F);
    }

    // Constructeur EntityType.EntityFactory, requis par le registre vanilla d'EntityType.ARROW
    // pour le chargement depuis le NBT — en pratique jamais vraiment exercé (la boule ne
    // survit jamais assez longtemps pour traverser une sauvegarde/un rechargement, voir tick()
    // ci-dessous), mais obligatoire pour que l'EntityType reste utilisable normalement.
    public BowlingBallEntity(EntityType<? extends Arrow> type, Level level) {
        super(type, level);
    }

    // Fausse arme enchantée de Perforation, jamais donnée à un joueur ni visible où que ce
    // soit : sert uniquement à faire passer PIERCE_LEVEL au constructeur d'AbstractArrow, qui
    // lit l'enchantement via EnchantmentHelper.getPiercingCount(...) et appelle lui-même
    // (en interne, privé) setPierceLevel — voir AbstractArrow.java.
    private static ItemStack fakePiercingWeapon(ServerLevel level) {
        Holder<Enchantment> piercing = level.registryAccess().getOrThrow(Enchantments.PIERCING);
        ItemStack weapon = new ItemStack(Items.CROSSBOW);
        weapon.enchant(piercing, PIERCE_LEVEL);
        return weapon;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()
                && this.position().distanceToSqr(this.spawnX, this.spawnY, this.spawnZ) >= this.maxDistanceSq) {
            this.discard();
        }
    }
}
