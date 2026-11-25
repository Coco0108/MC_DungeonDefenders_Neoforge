package com.github.c0c0tier.dungeon_defenders.init;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

// Les chiffres d'UNE tour : une valeur par TowerStat qu'elle utilise. Immuable. Construit soit
// dans le code (valeurs par défaut de TowerDefinition), soit en superposant un JSON à ces valeurs
// par défaut (parse) : toute stat absente du JSON garde donc sa valeur par défaut, et le jeu de
// stats d'une tour est toujours complet.
public final class TowerStats {

    private final Map<TowerStat, Double> values;

    private TowerStats(Map<TowerStat, Double> values) {
        this.values = Collections.unmodifiableMap(new EnumMap<>(values));
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean has(TowerStat stat) {
        return this.values.containsKey(stat);
    }

    /** @throws IllegalStateException si cette tour ne déclare pas cette stat : une erreur de code, pas de données. */
    public double get(TowerStat stat) {
        Double value = this.values.get(stat);
        if (value == null) {
            throw new IllegalStateException("Stat " + stat.jsonName() + " non déclarée pour cette tour");
        }
        return value;
    }

    public double getOr(TowerStat stat, double fallback) {
        return this.values.getOrDefault(stat, fallback);
    }

    public int intValue(TowerStat stat) {
        return (int) Math.round(get(stat));
    }

    public float floatValue(TowerStat stat) {
        return (float) get(stat);
    }

    /** Forme réseau/affichage : nom JSON de la stat -> valeur. */
    public Map<String, Double> toNamedMap() {
        Map<String, Double> named = new HashMap<>();
        this.values.forEach((stat, value) -> named.put(stat.jsonName(), value));
        return named;
    }

    /** Inverse de {@link #toNamedMap()} ; les noms inconnus sont ignorés (paquet d'une autre version). */
    public static TowerStats fromNamedMap(Map<String, Double> named) {
        Map<TowerStat, Double> values = new EnumMap<>(TowerStat.class);
        named.forEach((name, value) -> {
            TowerStat stat = TowerStat.byJsonName(name);
            if (stat != null) {
                values.put(stat, value);
            }
        });
        return new TowerStats(values);
    }

    /**
     * Superpose un JSON aux valeurs par défaut d'une tour. Refuse (erreur, rien n'est appliqué)
     * une stat inconnue, une stat que cette tour n'utilise pas, une valeur non numérique, non
     * entière là où il le faut, ou sous son minimum.
     */
    public static DataResult<TowerStats> parse(JsonElement json, TowerStats defaults) {
        if (!(json instanceof JsonObject object)) {
            return DataResult.error(() -> "un objet JSON est attendu");
        }

        String allowed = defaults.values.keySet().stream().map(TowerStat::jsonName).collect(Collectors.joining(", "));
        Map<TowerStat, Double> merged = new EnumMap<>(defaults.values);
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String name = entry.getKey();
            TowerStat stat = TowerStat.byJsonName(name);
            if (stat == null) {
                return DataResult.error(() -> "stat inconnue \"" + name + "\" (stats de cette tour : " + allowed + ")");
            }
            if (!defaults.has(stat)) {
                return DataResult.error(() -> "la stat \"" + name + "\" n'est pas utilisée par cette tour (stats de cette tour : " + allowed + ")");
            }
            JsonElement element = entry.getValue();
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
                return DataResult.error(() -> "\"" + name + "\" doit être un nombre");
            }
            double value = element.getAsDouble();
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return DataResult.error(() -> "\"" + name + "\" doit être un nombre fini");
            }
            if (stat.isInteger() && value != Math.rint(value)) {
                return DataResult.error(() -> "\"" + name + "\" doit être un nombre entier (reçu " + value + ")");
            }
            if (value < stat.min()) {
                return DataResult.error(() -> "\"" + name + "\" doit valoir au moins " + stat.min() + " (reçu " + value + ")");
            }
            merged.put(stat, value);
        }
        return DataResult.success(new TowerStats(merged));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TowerStats stats && this.values.equals(stats.values);
    }

    @Override
    public int hashCode() {
        return this.values.hashCode();
    }

    @Override
    public String toString() {
        return this.values.entrySet().stream()
                .map(entry -> entry.getKey().jsonName() + "=" + entry.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }

    public static final class Builder {
        private final Map<TowerStat, Double> values = new EnumMap<>(TowerStat.class);

        private Builder() {
        }

        public Builder with(TowerStat stat, double value) {
            this.values.put(stat, value);
            return this;
        }

        public TowerStats build() {
            return new TowerStats(this.values);
        }
    }
}
