package com.github.c0c0tier.dungeon_defenders.atelier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * When an animation lands its blow: the first {@code impact} instruction of its timeline (an instruction keyframe
 * written {@code impact;} in Blockbench), or the middle of the animation when it has none. Damage and effects are
 * applied at that moment, the animation itself starts straight away.
 *
 * @param secondes time from the start of the animation
 * @param marqueur true if it comes from an {@code impact} marker, false for the mid-animation fallback
 * @param longueur length of the whole animation, in seconds
 *
 * <p>Repris tel quel de l'Atelier (fiche/Impact.java) : le serveur lit cet instant dans le .animation.json au
 * démarrage, GeckoLib ne lisant les instructions de timeline que côté client.
 */
public record Impact(float secondes, boolean marqueur, float longueur) {
    public static final String INSTRUCTION = "impact";

    /**
     * Reads the impact of one animation of a {@code .animation.json}. Problems that make the marker unusable go to
     * {@code errors}; a second marker, which is ignored, goes to {@code warnings}.
     */
    static Impact read(String name, JsonObject animation, List<String> errors, List<String> warnings) {
        List<Float> markers = new ArrayList<>();
        if (animation.get("timeline") instanceof JsonObject timeline) {
            for (Map.Entry<String, JsonElement> keyframe : timeline.entrySet()) {
                Float time = seconds(keyframe.getKey());
                if (time == null) {
                    errors.add("animation « " + name + " » : instant « " + keyframe.getKey()
                            + " » de la timeline invalide (nombre de secondes attendu)");
                } else if (instructions(keyframe.getValue()).contains(INSTRUCTION)) {
                    markers.add(time);
                }
            }
        }
        markers.sort(null);
        float length = length(animation);
        for (float marker : markers) {
            if (marker > length) {
                errors.add("animation « " + name + " » : marqueur « impact » à " + marker
                        + " s, après la fin de l'animation (" + length + " s) : il ne serait jamais atteint");
            }
        }
        if (markers.size() > 1) {
            warnings.add("animation « " + name + " » : " + markers.size() + " marqueurs « impact », seul le premier ("
                    + markers.getFirst() + " s) compte");
        }
        return markers.isEmpty() ? new Impact(length / 2, false, length) : new Impact(markers.getFirst(), true, length);
    }

    /** Instructions of a timeline keyframe: a string or a list of strings, each holding {@code ;}-separated lines. */
    private static List<String> instructions(JsonElement value) {
        List<String> out = new ArrayList<>();
        List<JsonElement> lines = value.isJsonArray() ? value.getAsJsonArray().asList() : List.of(value);
        for (JsonElement line : lines) {
            if (line.isJsonPrimitive()) {
                for (String instruction : line.getAsString().split("[;\\n]")) {
                    out.add(instruction.trim());
                }
            }
        }
        return out;
    }

    /** {@code animation_length}, or the last keyframe when Blockbench left it out (same rule as the Établi). */
    private static float length(JsonObject animation) {
        if (animation.get("animation_length") instanceof JsonElement length && length.isJsonPrimitive()
                && length.getAsJsonPrimitive().isNumber()) {
            return length.getAsFloat();
        }
        float last = 0;
        if (animation.get("bones") instanceof JsonObject bones) {
            for (JsonElement bone : bones.asMap().values()) {
                if (bone instanceof JsonObject channels) {
                    for (JsonElement channel : channels.asMap().values()) {
                        if (channel instanceof JsonObject keyframes) {
                            for (String key : keyframes.keySet()) {
                                Float time = seconds(key);
                                last = time == null ? last : Math.max(last, time);
                            }
                        }
                    }
                }
            }
        }
        return last;
    }

    private static Float seconds(String key) {
        try {
            float time = Float.parseFloat(key.trim());
            return Float.isFinite(time) && time >= 0 ? time : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
