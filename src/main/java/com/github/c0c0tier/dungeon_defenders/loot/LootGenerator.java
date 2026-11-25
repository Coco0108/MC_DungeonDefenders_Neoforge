package com.github.c0c0tier.dungeon_defenders.loot;

import com.github.c0c0tier.dungeon_defenders.init.BalanceValue;
import com.github.c0c0tier.dungeon_defenders.init.GameBalance;
import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

// Fabrique les armes de butin et les pose au sol.
//
// La force d'une arme = multiplicateur de sa rareté x progression de la vague (+power_per_wave
// par vague au-delà de la première). Elle a toujours un bonus de dégâts, plus 0 à 3 bonus
// secondaires selon la rareté, tirés sans remise parmi PV, vitesse et dégâts des tours.
public final class LootGenerator {

    // Fourchettes d'un bonus à force 1 (Commun, vague 1). Constantes : c'est la forme des
    // objets, pas un réglage de rythme.
    private static final double DAMAGE_MIN = 0.04D;
    private static final double DAMAGE_MAX = 0.08D;
    private static final double HEALTH_MIN = 1.0D;
    private static final double HEALTH_MAX = 3.0D;
    private static final double SPEED_MIN = 0.01D;
    private static final double SPEED_MAX = 0.03D;
    private static final double TOWER_DAMAGE_MIN = 0.03D;
    private static final double TOWER_DAMAGE_MAX = 0.06D;

    private LootGenerator() {
    }

    public static LootRarity rollRarity(RandomSource random) {
        int total = 0;
        for (LootRarity rarity : LootRarity.values()) {
            total += rarity.weight();
        }
        int roll = random.nextInt(total);
        for (LootRarity rarity : LootRarity.values()) {
            roll -= rarity.weight();
            if (roll < 0) {
                return rarity;
            }
        }
        return LootRarity.COMMON;
    }

    /** Une arme pour {@code hero}, de la rareté donnée, à la force de la vague {@code wave}. */
    public static LootWeapon roll(RandomSource random, HeroDefinition hero, LootRarity rarity, int wave) {
        double power = rarity.multiplier()
                * (1.0D + Math.max(0, wave - 1) * GameBalance.get(BalanceValue.LOOT_POWER_PER_WAVE, false));

        double damage = round(between(random, DAMAGE_MIN, DAMAGE_MAX) * power, 100.0D);
        double health = 0.0D;
        double speed = 0.0D;
        double towerDamage = 0.0D;

        List<Integer> secondaries = new ArrayList<>(List.of(0, 1, 2));
        for (int i = 0; i < rarity.secondaryBonuses() && !secondaries.isEmpty(); i++) {
            switch (secondaries.remove(random.nextInt(secondaries.size()))) {
                case 0 -> health = Math.max(1.0D, Math.round(between(random, HEALTH_MIN, HEALTH_MAX) * power));
                case 1 -> speed = round(between(random, SPEED_MIN, SPEED_MAX) * power, 100.0D);
                default -> towerDamage = round(between(random, TOWER_DAMAGE_MIN, TOWER_DAMAGE_MAX) * power, 100.0D);
            }
        }
        return new LootWeapon(hero.id(), rarity.name(), Math.max(0.01D, damage), health, speed, towerDamage);
    }

    /**
     * Une arme au hasard, pour le héros d'un des joueurs présents (du butin que personne ne peut
     * équiper n'intéresse personne), ou pour un héros quelconque s'il n'y a aucun joueur.
     */
    public static LootWeapon rollFor(ServerLevel level, int wave) {
        RandomSource random = level.getRandom();
        List<ServerPlayer> players = level.players();
        HeroDefinition hero = players.isEmpty()
                ? HeroDefinition.values()[random.nextInt(HeroDefinition.values().length)]
                : HeroDefinition.of(players.get(random.nextInt(players.size())));
        return roll(random, hero, rollRarity(random), wave);
    }

    /**
     * Pose {@code weapon} au sol. L'objet ne se ramasse JAMAIS en marchant dessus (le joueur n'a
     * pas d'inventaire portatif) : seules les touches Ranger/Équiper le prennent, voir
     * Equipment#pickUp. Il brille pour être repéré dans la mêlée et disparaît au bout de cinq
     * minutes, comme tout objet au sol.
     */
    public static ItemEntity drop(ServerLevel level, Vec3 position, LootWeapon weapon) {
        ItemEntity entity = new ItemEntity(level, position.x, position.y + 0.25D, position.z, weapon.toStack(), 0.0D, 0.2D, 0.0D);
        entity.setNeverPickUp();
        entity.setGlowingTag(true);
        level.addFreshEntity(entity);
        return entity;
    }

    private static double between(RandomSource random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private static double round(double value, double precision) {
        return Math.round(value * precision) / precision;
    }
}
