package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

// Les caractéristiques d'un personnage (2026-11-16) : chaque niveau gagné donne des points, que
// le joueur répartit comme il veut (écran HeroStatsScreen, touche K). C'est ce qui rend deux
// personnages de même niveau différents.
//
//   VITALITY      : PV max du joueur (modificateur d'attribut vanilla)
//   POWER         : dégâts infligés par le joueur lui-même (arme, épée, sorts)
//   SWIFTNESS     : vitesse de déplacement (modificateur d'attribut vanilla)
//   TOWER_MASTERY : dégâts des tours QUE CE JOUEUR pose ou améliore
//
// Les points appartiennent au joueur, pas au héros : changer de héros ne les remet pas à zéro.
// Stockés dans l'attachment HERO_STATS (une liste, un entier par caractéristique, dans l'ordre
// de cet enum — n'ajouter de nouvelles caractéristiques qu'EN FIN d'enum). Gain par point et
// plafond : balance/progression.json.
public enum HeroStat {
    VITALITY("vitality", BalanceValue.STAT_VITALITY_HEALTH_PER_POINT),
    POWER("power", BalanceValue.STAT_POWER_DAMAGE_PER_POINT),
    SWIFTNESS("swiftness", BalanceValue.STAT_SWIFTNESS_SPEED_PER_POINT),
    TOWER_MASTERY("tower_mastery", BalanceValue.STAT_TOWER_MASTERY_DAMAGE_PER_POINT);

    private static final Identifier VITALITY_MODIFIER = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "stat_vitality");
    private static final Identifier SWIFTNESS_MODIFIER = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "stat_swiftness");

    private final String id;
    private final BalanceValue perPoint;

    HeroStat(String id, BalanceValue perPoint) {
        this.id = id;
        this.perPoint = perPoint;
    }

    public String id() {
        return this.id;
    }

    public Component displayName() {
        return Component.translatable("dungeon_defenders.stat." + this.id);
    }

    /** Ce que rapporte UN point (PV pour la Vitalité, fraction pour les autres : 0.03 = +3 %). */
    public double perPoint(boolean clientSide) {
        return GameBalance.get(this.perPoint, clientSide);
    }

    /** Points que {@code player} a mis dans cette caractéristique. */
    public int points(Player player) {
        List<Integer> all = player.getData(ModAttachments.HERO_STATS);
        return ordinal() < all.size() ? Math.max(0, all.get(ordinal())) : 0;
    }

    /** Le bonus total de {@code player} pour cette caractéristique : points x gain par point. */
    public double bonus(Player player) {
        return points(player) * perPoint(player.level().isClientSide());
    }

    // --- POINTS ---

    /** Points gagnés depuis le niveau 1 : stat_points_per_level par niveau au-delà du premier. */
    public static int earned(Player player) {
        boolean clientSide = player.level().isClientSide();
        int level = Math.max(1, player.getData(ModAttachments.LEVEL));
        return (level - 1) * GameBalance.getInt(BalanceValue.STAT_POINTS_PER_LEVEL, clientSide);
    }

    public static int spent(Player player) {
        int total = 0;
        for (HeroStat stat : values()) {
            total += stat.points(player);
        }
        return total;
    }

    public static int available(Player player) {
        return Math.max(0, earned(player) - spent(player));
    }

    /** Plafond de points dans une même caractéristique. */
    public static int maxPoints(Player player) {
        return GameBalance.getInt(BalanceValue.STAT_MAX_POINTS, player.level().isClientSide());
    }

    /**
     * Dépense un point dans cette caractéristique (serveur).
     *
     * @return faux s'il ne reste aucun point ou si la caractéristique est au plafond.
     */
    public boolean spend(Player player) {
        if (available(player) <= 0 || points(player) >= maxPoints(player)) {
            return false;
        }
        List<Integer> updated = padded(player);
        updated.set(ordinal(), updated.get(ordinal()) + 1);
        store(player, updated);
        return true;
    }

    /** Rend tous les points dépensés (serveur). */
    public static void reset(Player player) {
        List<Integer> cleared = new ArrayList<>();
        for (int i = 0; i < values().length; i++) {
            cleared.add(0);
        }
        store(player, cleared);
    }

    private static List<Integer> padded(Player player) {
        List<Integer> current = new ArrayList<>(player.getData(ModAttachments.HERO_STATS));
        while (current.size() < values().length) {
            current.add(0);
        }
        return current;
    }

    private static void store(Player player, List<Integer> points) {
        // Liste immuable : un attachment ne doit jamais être modifié en place.
        player.setData(ModAttachments.HERO_STATS, List.copyOf(points));
        player.syncData(ModAttachments.HERO_STATS);
        applyAttributes(player);
    }

    // --- EFFETS ---

    /**
     * Pose (ou met à jour) les modificateurs d'attributs de Vitalité et de Rapidité. À rappeler
     * à chaque connexion et réapparition : ils sont transitoires, jamais sauvegardés avec le
     * joueur, pour qu'un changement dans progression.json s'applique sans reliquat.
     */
    public static void applyAttributes(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.addOrUpdateTransientModifier(new AttributeModifier(
                    VITALITY_MODIFIER, VITALITY.bonus(player), AttributeModifier.Operation.ADD_VALUE));
            // Après une remise à zéro, les PV actuels peuvent dépasser le nouveau maximum.
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(
                    SWIFTNESS_MODIFIER, SWIFTNESS.bonus(player), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }
}
