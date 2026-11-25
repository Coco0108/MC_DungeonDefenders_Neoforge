package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import com.github.c0c0tier.dungeon_defenders.init.TowerStats;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Les chiffres d'une tour qui parlent au joueur, en lignes courtes (« Dégâts : 6 »), pour la roue
// des tours et la fiche de la tour regardée. Une ligne par stat que la tour UTILISE (ses
// TowerStats ne contiennent que celles-là) et qui a un sens à l'écran ; les stats techniques
// (vitesse de projectile, distance en plus) sont tues.
//
// L'ordre est fixe, du plus parlant au plus spécifique. Les dégâts et les PV sont multipliés par
// le bonus de niveau de la tour quand on en donne un ; le reste ne dépend pas du niveau. Un
// multiplicateur de PV nul ou négatif omet la ligne des PV (la fiche de tour a déjà sa jauge).
public final class TowerStatSummary {

    private TowerStatSummary() {
    }

    public static List<Component> lines(TowerStats stats, double damageMultiplier, double healthMultiplier) {
        List<Component> lines = new ArrayList<>();
        add(lines, stats, TowerStat.DAMAGE, number(stats, TowerStat.DAMAGE, damageMultiplier));
        add(lines, stats, TowerStat.CONTACT_DAMAGE, number(stats, TowerStat.CONTACT_DAMAGE, damageMultiplier));
        if (stats.has(TowerStat.ATTACK_INTERVAL_TICKS)) {
            add(lines, stats, TowerStat.ATTACK_INTERVAL_TICKS, perSecond(stats.get(TowerStat.ATTACK_INTERVAL_TICKS)));
        } else if (stats.has(TowerStat.CONTACT_INTERVAL_TICKS) && stats.has(TowerStat.CONTACT_DAMAGE)) {
            add(lines, stats, TowerStat.CONTACT_INTERVAL_TICKS, perSecond(stats.get(TowerStat.CONTACT_INTERVAL_TICKS)));
        }
        add(lines, stats, TowerStat.RANGE, number(stats, TowerStat.RANGE, 1.0D));
        add(lines, stats, TowerStat.CONE_ANGLE_DEGREES, number(stats, TowerStat.CONE_ANGLE_DEGREES, 1.0D));
        add(lines, stats, TowerStat.SPLASH_RADIUS, number(stats, TowerStat.SPLASH_RADIUS, 1.0D));
        add(lines, stats, TowerStat.CHAIN_TARGETS, number(stats, TowerStat.CHAIN_TARGETS, 1.0D));
        add(lines, stats, TowerStat.HEAL_AMOUNT, number(stats, TowerStat.HEAL_AMOUNT, 1.0D));
        add(lines, stats, TowerStat.BOOST_PERCENT, number(stats, TowerStat.BOOST_PERCENT, 1.0D));
        add(lines, stats, TowerStat.VULNERABILITY_PERCENT, number(stats, TowerStat.VULNERABILITY_PERCENT, 1.0D));
        add(lines, stats, TowerStat.BONUS_MANA, number(stats, TowerStat.BONUS_MANA, 1.0D));
        if (healthMultiplier > 0.0D) {
            add(lines, stats, TowerStat.MAX_HEALTH, number(stats, TowerStat.MAX_HEALTH, healthMultiplier));
        }
        return lines;
    }

    /** Deux colonnes si toutes les lignes tiennent dans une demi-largeur, sinon une seule. */
    public static int columns(Font font, List<Component> lines, int innerWidth) {
        for (Component line : lines) {
            if (font.width(line) + 6 > innerWidth / 2) {
                return 1;
            }
        }
        return 2;
    }

    private static void add(List<Component> lines, TowerStats stats, TowerStat stat, String value) {
        if (stats.has(stat)) {
            lines.add(Component.translatable("dungeon_defenders.tower_stat." + stat.jsonName(), value));
        }
    }

    private static String number(TowerStats stats, TowerStat stat, double multiplier) {
        if (!stats.has(stat)) {
            return "";
        }
        double value = stats.get(stat) * multiplier;
        if (Math.abs(value - Math.rint(value)) < 0.05D) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    // Un intervalle en ticks devient une cadence lisible : « 2.5 / s » ou « 1 tous les 4 s ».
    private static String perSecond(double intervalTicks) {
        double perSecond = 20.0D / Math.max(1.0D, intervalTicks);
        if (perSecond >= 1.0D) {
            return String.format(Locale.ROOT, "%.1f/s", perSecond);
        }
        return String.format(Locale.ROOT, "1/%.0fs", intervalTicks / 20.0D);
    }
}
