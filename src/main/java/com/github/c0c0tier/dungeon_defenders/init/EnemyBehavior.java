package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.entity.ai.AttackPriorityTargetGoal;
import com.github.c0c0tier.dungeon_defenders.entity.ai.RangedAttackEterniaCrystalGoal;
import com.github.c0c0tier.dungeon_defenders.entity.ai.SeekEterniaCrystalGoal;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;

import java.util.Map;

// "Types de comportement en Java, paramètres en JSON" (doc/data-driven/ennemis.md, §3) : le
// code définit un petit nombre de TYPES d'IA ; chaque JSON d'ennemi choisit le sien via
// "behavior": { "type": ..., ...paramètres }. Aucune logique d'IA n'est décrite en JSON.
//
// Les deux types reproduisent exactement la répartition qui existait avant (le test
// `instanceof AbstractSkeleton` de ModEvents.onMonsterSpawn), avec les mêmes goals, les mêmes
// priorités et les mêmes valeurs par défaut — voir legacyFor(...), qui sert aussi de repli pour
// tout monstre sans définition.
//
// Pas de registre NeoForge pour les types : une simple table suffit tant qu'aucun addon n'a
// besoin d'ajouter les siens.
public sealed interface EnemyBehavior permits EnemyBehavior.MeleePriority, EnemyBehavior.RangedCrystal {

    Identifier MELEE_PRIORITY_ID = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "melee_priority");
    Identifier RANGED_CRYSTAL_ID = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "ranged_crystal");

    /** Ajoute les goals de ce comportement au monstre qui vient d'apparaître. */
    void applyGoals(PathfinderMob mob);

    Identifier typeId();

    /**
     * Mêlée à paliers de priorité (Block > Corps à corps > Cristal > Tourelle), puis convergence
     * vers le cristal. Aucun paramètre : les dégâts restent lus dans Config.DAMAGE_PER_HIT et les
     * vitesses sont des constantes des goals, comme avant la migration.
     */
    record MeleePriority() implements EnemyBehavior {
        public static final MeleePriority INSTANCE = new MeleePriority();
        static final MapCodec<MeleePriority> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public void applyGoals(PathfinderMob mob) {
            mob.goalSelector.addGoal(0, new AttackPriorityTargetGoal(mob));
            mob.goalSelector.addGoal(1, new SeekEterniaCrystalGoal(mob));
        }

        @Override
        public Identifier typeId() {
            return MELEE_PRIORITY_ID;
        }
    }

    /**
     * Tir à distance sur le cristal uniquement (ignore Blockade/Turret), puis convergence vers le
     * cristal hors de portée. Paramètres optionnels, défauts = constantes historiques de
     * RangedAttackEterniaCrystalGoal.
     */
    record RangedCrystal(int damagePerShot, int ticksBetweenShots, double shootRange) implements EnemyBehavior {
        public static final RangedCrystal DEFAULTS = new RangedCrystal(
                RangedAttackEterniaCrystalGoal.DEFAULT_DAMAGE_PER_HIT,
                RangedAttackEterniaCrystalGoal.DEFAULT_TICKS_BETWEEN_SHOTS,
                RangedAttackEterniaCrystalGoal.DEFAULT_SHOOT_RANGE);

        static final MapCodec<RangedCrystal> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("damage_per_shot", DEFAULTS.damagePerShot)
                        .forGetter(RangedCrystal::damagePerShot),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("ticks_between_shots", DEFAULTS.ticksBetweenShots)
                        .forGetter(RangedCrystal::ticksBetweenShots),
                Codec.doubleRange(0.5D, 256.0D).optionalFieldOf("shoot_range", DEFAULTS.shootRange)
                        .forGetter(RangedCrystal::shootRange)
        ).apply(instance, RangedCrystal::new));

        @Override
        public void applyGoals(PathfinderMob mob) {
            mob.goalSelector.addGoal(1, new RangedAttackEterniaCrystalGoal(mob, this.damagePerShot, this.ticksBetweenShots, this.shootRange));
            mob.goalSelector.addGoal(2, new SeekEterniaCrystalGoal(mob));
        }

        @Override
        public Identifier typeId() {
            return RANGED_CRYSTAL_ID;
        }
    }

    Map<Identifier, MapCodec<? extends EnemyBehavior>> CODECS_BY_TYPE = Map.of(
            MELEE_PRIORITY_ID, MeleePriority.CODEC,
            RANGED_CRYSTAL_ID, RangedCrystal.CODEC);

    // partialDispatch plutôt que dispatch : un "type" inconnu doit produire une ERREUR de parse
    // (le fichier est alors ignoré et loggé par le chargeur vanilla), pas une exception.
    Codec<EnemyBehavior> CODEC = Identifier.CODEC.partialDispatch(
            "type",
            behavior -> DataResult.success(behavior.typeId()),
            type -> {
                MapCodec<? extends EnemyBehavior> codec = CODECS_BY_TYPE.get(type);
                return codec != null
                        ? DataResult.success(codec)
                        : DataResult.error(() -> "Type de comportement d'ennemi inconnu : " + type
                                + " (connus : " + CODECS_BY_TYPE.keySet() + ")");
            });

    /**
     * Le comportement qu'aurait reçu ce monstre avant la migration : distance pour tout
     * AbstractSkeleton (squelette, stray, wither skeleton...), mêlée pour le reste. Utilisé en
     * repli pour tout monstre SANS définition (un zombie /summon d'un type sans JSON, un stray...) :
     * sans ça, ils perdraient l'IA qu'ils avaient avant.
     */
    static EnemyBehavior legacyFor(PathfinderMob mob) {
        return mob instanceof AbstractSkeleton ? RangedCrystal.DEFAULTS : MeleePriority.INSTANCE;
    }
}
