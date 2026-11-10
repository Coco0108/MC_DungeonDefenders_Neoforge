package com.github.c0c0tier.dungeon_defenders.init;

// Les réglages GLOBAUX d'équilibrage qu'un JSON peut modifier (doc/data-driven/equilibrage.md) :
// tout ce qui n'appartient ni à un ennemi ni à une tour en particulier. Chaque valeur vit dans
// data/<namespace>/dungeon_defenders/balance/<fichier>.json, sous la clé indiquée, et a ici sa
// valeur PAR DÉFAUT — celle d'avant la migration, utilisée telle quelle si le fichier manque ou
// est invalide.
//
// Ajouter un réglage : une ligne ici, sa clé dans le JSON correspondant, et lire
// GameBalance.get(...) là où la constante était utilisée. Rien d'autre (chargement, /reload,
// synchro client : tout est générique).
public enum BalanceValue {

    // balance/difficulty.json — multiplicateur du nombre d'ennemis par spawner (DifficultyScaling).
    DIFFICULTY_EASY_MULTIPLIER("difficulty", "easy_multiplier", 0.75D, 0.01D, false),
    DIFFICULTY_NORMAL_MULTIPLIER("difficulty", "normal_multiplier", 1.0D, 0.01D, false),
    DIFFICULTY_HARD_MULTIPLIER("difficulty", "hard_multiplier", 1.5D, 0.01D, false),
    // +10 % d'ennemis par vague au-delà de la première.
    PER_WAVE_INCREASE("difficulty", "per_wave_increase", 0.1D, 0.0D, false),

    // balance/towers.json — part du coût en mana rendue quand le joueur retire une tour.
    TOWER_REFUND_RATIO("towers", "refund_ratio", 0.5D, 0.0D, false),

    // balance/progression.json — expérience nécessaire pour passer un niveau.
    XP_PER_LEVEL("progression", "xp_per_level", 100.0D, 1.0D, true);

    private final String file;
    private final String key;
    private final double defaultValue;
    private final double min;
    private final boolean integer;

    BalanceValue(String file, String key, double defaultValue, double min, boolean integer) {
        this.file = file;
        this.key = key;
        this.defaultValue = defaultValue;
        this.min = min;
        this.integer = integer;
    }

    /** Nom du fichier (sans extension) dans le dossier balance/. */
    public String file() {
        return this.file;
    }

    public String key() {
        return this.key;
    }

    public double defaultValue() {
        return this.defaultValue;
    }

    public double min() {
        return this.min;
    }

    public boolean isInteger() {
        return this.integer;
    }

    /** Identifiant unique sur le réseau : `<fichier>/<clé>`. */
    public String networkName() {
        return this.file + "/" + this.key;
    }
}
