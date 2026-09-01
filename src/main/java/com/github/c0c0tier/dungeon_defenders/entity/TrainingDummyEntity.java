package com.github.c0c0tier.dungeon_defenders.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

// Mannequin d'entraînement : une cible immobile et indestructible, pour mesurer les dégâts des
// tours sans monter une vraie vague. Demandé par le joueur (2026-08-31) pour la taverne.
//
// **Pourquoi une sous-classe de Zombie** et pas de Monster directement — deux raisons, toutes
// deux imposées par l'existant :
//
// 1. Les tourelles ne ciblent que des `Monster` (`AbstractTurretBlockEntity#findTarget` fait un
//    `getEntitiesOfClass(Monster.class, ...)`). Le mannequin DOIT donc en être un, sinon aucune
//    tour ne lui tire dessus et il ne sert à rien.
// 2. Passer par Zombie permet de réutiliser tel quel le `ZombieRenderer` vanilla (typé sur
//    `Zombie`, donc valide pour une sous-classe), sans avoir à créer un modèle ni une texture —
//    même esprit que `ManaCrystalEntity extends ExperienceOrb` + `ExperienceOrbRenderer`.
//    **Limite assumée** : le mannequin ressemble donc à un zombie immobile, pas à un mannequin
//    de paille. À remplacer par un vrai modèle le jour où il y en a un.
//
// Tout ce qui fait de ce zombie un mannequin est neutralisé ci-dessous : aucun goal, pas d'IA,
// pas de combustion au soleil, pas de repoussement, pas de despawn, et une vie qui se remet au
// maximum à chaque coup encaissé.
public class TrainingDummyEntity extends Zombie {

    // Volontairement énorme plutôt qu'une vraie invulnérabilité : les dégâts sont réellement
    // appliqués (les événements de dégâts se déclenchent normalement, ce qui laisse la porte
    // ouverte à un futur compteur de DPS), puis la vie est remise au maximum. La marge sert à
    // ce qu'aucun coup unique ne puisse descendre à 0 avant cette remise à niveau — la source
    // la plus violente du mod est très loin du compte.
    public static final float DUMMY_MAX_HEALTH = 1024.0F;

    public TrainingDummyEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        // setNoAi coupe isEffectiveAi(), qui conditionne à la fois le tick des goals et la
        // simulation de déplacement (vérifié dans LivingEntity/Mob) : le mannequin ne bouge
        // donc pas d'un pouce, et ne subit même pas la gravité — pratique, le bloc qui l'invoque
        // est lui-même invisible et traversable, il n'y a rien sous ses pieds.
        this.setNoAi(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        // `add` écrase une valeur déjà posée par Zombie.createAttributes() (simple `put` dans
        // une map, vérifié dans AttributeSupplier.Builder) : ces quatre lignes redéfinissent
        // donc bien les valeurs héritées du zombie.
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, DUMMY_MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                // 1.0 annule complètement le calcul de repoussement dans
                // LivingEntity#knockback (`power *= 1.0 - KNOCKBACK_RESISTANCE`) : le Bouncer
                // Blockade peut le frapper sans le déplacer.
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    // Aucun goal du tout : ni les goals de déplacement/attaque du zombie, ni ses goals de
    // ciblage. Le mannequin ne cherche personne et n'attaque personne.
    //
    // À noter : ModEvents.onMonsterSpawn ignore explicitement cette classe, sinon tout Monster
    // qui rejoint le monde reçoit le goal d'attaque du Cristal d'Eternia.
    @Override
    protected void registerGoals() {
    }

    // Un zombie brûle au soleil ; la taverne est à ciel ouvert.
    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    // Ne disparaît jamais parce qu'un joueur s'est éloigné : c'est un élément de décor
    // fonctionnel, pas un monstre de vague.
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // Ni poussé par les autres entités, ni poussant les autres : il reste exactement où le bloc
    // l'a posé, et ne déplace pas un joueur qui viendrait se coller à lui.
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    /**
     * Encaisse le coup normalement — animation de dégât, événements, tout se déclenche — puis
     * remonte la vie au maximum. C'est ce qui rend le mannequin inusable sans le rendre
     * invulnérable : une future mesure de DPS pourra s'accrocher aux dégâts réellement reçus.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        this.setHealth(this.getMaxHealth());
        return hurt;
    }

    // Filet de sécurité pour tout ce qui ne passe pas par hurtServer (poison, noyade, dégâts
    // appliqués directement...) : la vie est de toute façon remise au maximum à chaque tick.
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.getHealth() < this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }
}
