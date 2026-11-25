package com.github.c0c0tier.dungeon_defenders;

import net.neoforged.neoforge.common.ModConfigSpec;

// Constantes de gameplay ajustables sans recompiler (fichier généré au premier lancement dans
// config/dungeon_defenders-common.toml). Enregistré dans DungeonDefendersMod via
// ModContainer#registerConfig — voir doc/05-etat-et-problemes-connus.md, "Pistes prioritaires".
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue DEFAULT_HEALTH = BUILDER
            .comment("PV maximum (et de départ) du Cristal d'Eternia.")
            .defineInRange("defaultHealth", 100, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue DAMAGE_PER_HIT = BUILDER
            .comment("Dégâts infligés par coup par un monstre de mêlée contre une tour ou le "
                    + "Cristal d'Eternia (AttackPriorityTargetGoal).")
            .defineInRange("damagePerHit", 5, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SEARCH_RANGE = BUILDER
            .comment("Portée (en blocs) à laquelle un monstre détecte le Cristal d'Eternia, "
                    + "que ce soit pour l'attaquer au corps à corps ou à distance.")
            .defineInRange("searchRange", 16, 1, 64);

    public static final ModConfigSpec.IntValue DEFAULT_TIME_OF_DAY = BUILDER
            .comment("Heure figée du monde, en ticks (0 = lever du soleil, 6000 = midi, 12000 = "
                    + "coucher, 18000 = minuit) : celle de la taverne, et de toute map qui ne fixe "
                    + "pas la sienne dans son bloc de configuration. Le cycle jour/nuit est arrêté.")
            .defineInRange("defaultTimeOfDay", 6000, 0, 23999);

    static final ModConfigSpec SPEC = BUILDER.build();
}
