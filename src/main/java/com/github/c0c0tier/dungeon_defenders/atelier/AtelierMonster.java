package com.github.c0c0tier.dungeon_defenders.atelier;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.keyframehandler.AutoPlayingSoundKeyframeHandler;
import com.geckolib.util.GeckoLibUtil;
import com.github.c0c0tier.dungeon_defenders.entity.ai.AnimatedAttacker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * L'unique classe d'entité de tous les monstres de l'Atelier (doc/ATELIER.md). Tout ce qui est propre à un monstre vient
 * de sa {@link FicheMonstre}, retrouvée d'après le type d'entité (dont l'id est celui du monstre). Adaptée de
 * {@code entity/AtelierMonster.java} de l'Atelier, sans capacités, monnaie vivante, cachettes ni apparition naturelle.
 *
 * <p>C'est un {@link Monster}, pas un simple PathfinderMob : dans Dungeon Defenders, les tours, les armes des héros, les
 * vagues et l'IA des ennemis ({@code ModEvents.onMonsterSpawn}, {@code EnemyBehavior}) ne visent que des Monster.
 *
 * <ul>
 *   <li>Animations : {@code idle} et {@code walk} selon le mouvement, {@code attack} à chaque coup, {@code hurt} quand il
 *       est blessé (s'il n'est pas en train de frapper), {@code death} à sa mort (le corps reste le temps de l'animation).</li>
 *   <li>Coups : les dégâts tombent au marqueur {@code impact;} de l'animation {@code attack} ({@link Impact}), qu'il frappe
 *       un joueur ou, via les goals de Dungeon Defenders, une tour ou le cristal ({@link AnimatedAttacker}). Un nouveau
 *       coup ne part ni avant la fin de l'animation, ni avant {@code stats.recharge_attaque} secondes.</li>
 *   <li>Variantes : choisie au hasard à l'apparition (la texture principale compte comme une variante), synchronisée,
 *       sauvegardée sous {@code Variante} (aussi pour {@code /summon … {Variante:"rouge"}}).</li>
 * </ul>
 */
public class AtelierMonster extends Monster implements GeoEntity, AnimatedAttacker {
    /** Le contrôleur GeckoLib qui joue toutes les animations. */
    public static final String ANIM_CONTROLLER = "main";
    /** Temps de fondu du contrôleur avant qu'une animation déclenchée commence vraiment, en ticks. */
    private static final int ANIM_TRANSITION_TICKS = 4;
    /** Jusqu'où, en blocs, une cible qui recule pendant le geste est encore touchée à l'impact. */
    private static final double DODGE_MARGIN = 1.0D;
    /** Vitesse de vol / vitesse de marche : à peu près le rapport de l'abeille et du perroquet vanilla. */
    private static final float FLYING_SPEED_RATIO = 2.0F;
    /** Durée maximale d'une animation de mort : le corps ne reste jamais plus longtemps. */
    private static final int MAX_DEATH_TICKS = 100;
    private static final String VARIANTE_NBT = "Variante";
    private static final EntityDataAccessor<Byte> VARIANTE = SynchedEntityData.defineId(AtelierMonster.class, EntityDataSerializers.BYTE);

    // Pas d'initialiseur exprès : registerGoals() tourne dans le constructeur parent et a déjà besoin de la fiche.
    private @Nullable FicheMonstre fiche;
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    /** Ticks avant la fin de l'animation d'attaque : rien de neuf ne part avant, pour ne jamais la couper. */
    private int attackTicksLeft;
    /** Ticks avant le prochain coup possible ({@code recharge_attaque}, jamais moins que l'animation). */
    private int attackCooldown;
    /** Le coup en cours, appliqué à son impact ; null s'il n'y en a pas. */
    private @Nullable Runnable pendingHit;
    private int ticksToHit;

    public AtelierMonster(EntityType<? extends AtelierMonster> type, Level level) {
        super(type, level);
        if (fiche().vole()) {
            // « vol » : comme l'abeille vanilla, il reste suspendu quand il s'arrête (aucune gravité).
            this.moveControl = new FlyingMoveControl(this, 20, true);
            this.setNoGravity(true);
        }
    }

    /** Les attributs d'un type de monstre, lus dans sa fiche. */
    public static AttributeSupplier.Builder createAttributes(FicheMonstre fiche) {
        AttributeSupplier.Builder attributes = Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, fiche.stats().pv())
                .add(Attributes.ATTACK_DAMAGE, fiche.stats().degats())
                .add(Attributes.MOVEMENT_SPEED, fiche.stats().vitesse());
        if (fiche.vole()) {
            // Lu par FlyingMoveControl en l'air ; MOVEMENT_SPEED reste pour quand il touche le sol.
            attributes.add(Attributes.FLYING_SPEED, fiche.stats().vitesse() * FLYING_SPEED_RATIO);
        }
        return attributes;
    }

    public FicheMonstre fiche() {
        if (this.fiche == null) {
            this.fiche = Atelier.pourType(this.getType());
        }
        return this.fiche;
    }

    // --- Variante -----------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANTE, (byte) -1);
    }

    /** Index de la variante dans la fiche, -1 pour la texture principale. */
    public int variante() {
        return this.entityData.get(VARIANTE);
    }

    public void setVariante(int index) {
        this.entityData.set(VARIANTE, (byte) (index >= 0 && index < fiche().variantes().size() ? index : -1));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        if (!fiche().variantes().isEmpty()) {
            // La texture principale et chaque variante, à chances égales.
            setVariante(this.random.nextInt(fiche().variantes().size() + 1) - 1);
        }
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        int index = variante();
        if (index >= 0) {
            output.putString(VARIANTE_NBT, fiche().variantes().get(index).id());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String id = input.getStringOr(VARIANTE_NBT, "");
        int index = -1;
        for (int i = 0; i < fiche().variantes().size(); i++) {
            if (fiche().variantes().get(i).id().equals(id)) {
                index = i;
            }
        }
        setVariante(index);
    }

    /** Son nom : celui des fichiers de langue s'il y est, sinon le {@code nom} de sa fiche. */
    @Override
    protected Component getTypeName() {
        return Component.translatableWithFallback(this.getType().getDescriptionId(), fiche().nom());
    }

    // --- Déplacement ----------------------------------------------------------------------------

    /** Appelé par le constructeur de Mob, où la fiche est déjà disponible (retrouvée d'après le type). */
    @Override
    protected PathNavigation createNavigation(Level level) {
        if (!fiche().vole()) {
            return super.createNavigation(level);
        }
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        // Un monstre volant ne prend jamais de dégâts de chute (comme l'abeille, le perroquet et l'allay).
        if (!fiche().vole()) {
            super.checkFallDamage(ya, onGround, onState, pos);
        }
    }

    /** Un volant qui monte ou descend compte aussi comme en mouvement : GeckoLib choisit walk ou idle d'après ça. */
    @Override
    public void calculateEntityAnimation(boolean useY) {
        super.calculateEntityAnimation(useY || fiche().vole());
    }

    // --- IA ------------------------------------------------------------------------------------

    /**
     * La réaction aux joueurs, d'après le {@code comportement} de la fiche. Le reste (tours, cristal) vient de son
     * {@code enemy/<id>.json}, ajouté à son apparition par {@code ModEvents.onMonsterSpawn}, comme pour un zombie.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        FicheMonstre.Comportement comportement = fiche().comportement();
        if (comportement.combat()) {
            this.goalSelector.addGoal(2, new AnimatedMeleeGoal());
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        }
        if (comportement == FicheMonstre.Comportement.HOSTILE) {
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
        this.goalSelector.addGoal(6, fiche().vole() ? new WaterAvoidingRandomFlyingGoal(this, 1.0D)
                : new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /**
     * Poursuit sa cible comme un zombie, mais son coup passe par {@link #beginAttack} : animation {@code attack}, dégâts à
     * l'impact si la cible est encore à portée (avec une petite marge pour un recul pendant le geste).
     */
    private final class AnimatedMeleeGoal extends MeleeAttackGoal {
        AnimatedMeleeGoal() {
            super(AtelierMonster.this, 1.0D, false);
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (!canPerformAttack(target)) {
                return;
            }
            boolean started = beginAttack(() -> {
                if (target.isAlive() && level() instanceof ServerLevel level
                        && getAttackBoundingBox(DODGE_MARGIN).intersects(target.getBoundingBox())) {
                    doHurtTarget(level, target);
                }
            });
            if (started) {
                resetAttackCooldown();
            }
        }
    }

    @Override
    public boolean beginAttack(Runnable hit) {
        if (this.pendingHit != null || this.attackTicksLeft > 0 || this.attackCooldown > 0 || !this.isAlive()) {
            return false;
        }
        int recharge = Math.round(fiche().stats().rechargeAttaque() * 20.0F);
        String animation = playAnimation("attack");
        if (animation == null) {
            // Pas d'animation d'attaque : le coup tombe tout de suite.
            hit.run();
            this.attackCooldown = recharge;
            return true;
        }
        Impact impact = Atelier.impact(fiche().id(), animation);
        this.pendingHit = hit;
        this.ticksToHit = ANIM_TRANSITION_TICKS + Math.round(impact.secondes() * 20.0F);
        this.attackTicksLeft = ANIM_TRANSITION_TICKS + Math.round(impact.longueur() * 20.0F);
        this.attackCooldown = Math.max(this.attackTicksLeft, recharge);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.attackTicksLeft > 0) {
            this.attackTicksLeft--;
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.pendingHit != null && --this.ticksToHit <= 0) {
            Runnable hit = this.pendingHit;
            this.pendingHit = null;
            if (this.isAlive()) {
                hit.run();
            }
        }
    }

    // --- Blessure et mort --------------------------------------------------------------------

    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource source, float damage) {
        super.actuallyHurt(level, source, damage);
        // Joué seulement entre deux coups : il ne coupe jamais une attaque.
        if (this.isAlive() && this.pendingHit == null && this.attackTicksLeft <= 0) {
            playAnimation("hurt");
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.pendingHit = null;
        playAnimation("death");
    }

    /** Avec une animation {@code death}, le corps reste le temps qu'elle se joue (au plus {@link #MAX_DEATH_TICKS}). */
    @Override
    protected void tickDeath() {
        int ticks = deathTicks();
        if (ticks <= 0) {
            super.tickDeath();
            return;
        }
        this.deathTime++;
        if (this.deathTime >= ticks && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    /** Durée de l'animation de mort, transition comprise, en ticks ; 0 sans rôle {@code death}. */
    public int deathTicks() {
        return fiche().animation("death")
                .map(name -> Math.min(MAX_DEATH_TICKS,
                        ANIM_TRANSITION_TICKS + Math.round(Atelier.impact(fiche().id(), name).longueur() * 20.0F)))
                .orElse(0);
    }

    // --- Sons ----------------------------------------------------------------------------------

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        SoundEvent sound = son("ambient");
        return sound != null ? sound : super.getAmbientSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        SoundEvent sound = son("hurt");
        return sound != null ? sound : super.getHurtSound(source);
    }

    @Override
    protected SoundEvent getDeathSound() {
        SoundEvent sound = son("death");
        return sound != null ? sound : super.getDeathSound();
    }

    private @Nullable SoundEvent son(String role) {
        return fiche().son(role).map(nom -> AtelierSounds.get(fiche().evenementSon(nom))).orElse(null);
    }

    // --- Animations (GeckoLib) ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        FicheMonstre f = fiche();
        RawAnimation idle = RawAnimation.begin().thenLoop(f.animation("idle").orElseThrow());
        RawAnimation walk = RawAnimation.begin().thenLoop(f.animation("walk").orElseThrow());
        // Revient à idle/walk une fois qu'une animation déclenchée (attaque, blessure) s'est jouée.
        AnimationController<AtelierMonster> controller = new ReturningAnimationController<>(ANIM_CONTROLLER,
                ANIM_TRANSITION_TICKS, test -> test.setAndContinue(test.isMoving() ? walk : idle));
        // Keyframes de son du fichier d'animation (« sound_effects »), jouées côté client à la position du monstre.
        controller.setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>());
        // N'importe quel rôle de la fiche peut être joué une fois à la demande (voir playAnimation). La mort reste
        // figée sur sa dernière image jusqu'à ce que le corps disparaisse.
        f.rolesAnimation().forEach((role, name) -> controller.triggerableAnim(role, "death".equals(role)
                ? RawAnimation.begin().thenPlayAndHold(name) : RawAnimation.begin().thenPlay(name)));
        controllers.add(controller);
    }

    /** Joue une fois l'animation d'un rôle de la fiche, puis revient à idle/walk. Renvoie son nom, ou null sans ce rôle. */
    public @Nullable String playAnimation(String role) {
        String name = fiche().animation(role).orElse(null);
        if (name != null) {
            triggerAnim(ANIM_CONTROLLER, role);
        }
        return name;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }
}
