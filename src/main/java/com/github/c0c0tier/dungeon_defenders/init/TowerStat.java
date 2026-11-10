package com.github.c0c0tier.dungeon_defenders.init;

import org.jspecify.annotations.Nullable;

// Les CHIFFRES d'une tour qu'un JSON peut régler (doc/data-driven/tours.md). Chaque tour n'en
// utilise qu'une partie : celles que déclarent ses valeurs par défaut (TowerDefinition). Un JSON
// qui cite une stat que sa tour n'utilise pas est refusé, pour qu'une faute de frappe ou une
// stat sans effet ne passe jamais inaperçue.
//
// Le COMPORTEMENT d'une tour (mur au contact, tir en cône, perforation, zone...) reste en Java :
// seuls ces nombres sont data-driven.
public enum TowerStat {
    // Communes à toutes les tours.
    MAX_HEALTH("max_health", true, 1.0D),
    MANA_COST("mana_cost", true, 0.0D),

    // Blockades : dégâts périodiques aux monstres dans un petit rayon autour du bloc.
    CONTACT_DAMAGE("contact_damage", false, 0.0D),
    CONTACT_INTERVAL_TICKS("contact_interval_ticks", true, 1.0D),
    CONTACT_RANGE("contact_range", false, 0.0D),
    KNOCKBACK_STRENGTH("knockback_strength", false, 0.0D),

    // Turrets : tir dans un cône autour de l'orientation du bloc.
    RANGE("range", false, 0.0D),
    CONE_ANGLE_DEGREES("cone_angle_degrees", false, 1.0D),
    DAMAGE("damage", false, 0.0D),
    ATTACK_INTERVAL_TICKS("attack_interval_ticks", true, 1.0D),

    // Propres à certaines tours.
    SPLASH_RADIUS("splash_radius", false, 0.0D),
    PROJECTILE_VELOCITY("projectile_velocity", false, 0.1D),
    // Distance parcourue par le projectile AU-DELÀ de la portée de ciblage, avant de disparaître.
    PROJECTILE_EXTRA_DISTANCE("projectile_extra_distance", false, 0.0D);

    private final String jsonName;
    private final boolean integer;
    private final double min;

    TowerStat(String jsonName, boolean integer, double min) {
        this.jsonName = jsonName;
        this.integer = integer;
        this.min = min;
    }

    public String jsonName() {
        return this.jsonName;
    }

    /** Vrai si la valeur doit être un nombre entier (PV, coût, intervalles en ticks). */
    public boolean isInteger() {
        return this.integer;
    }

    public double min() {
        return this.min;
    }

    public static @Nullable TowerStat byJsonName(String name) {
        for (TowerStat stat : values()) {
            if (stat.jsonName.equals(name)) {
                return stat;
            }
        }
        return null;
    }
}
