package com.github.c0c0tier.dungeon_defenders.atelier;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ce qui a été lu de l'Atelier au démarrage (voir {@link AtelierLoader} et doc/ATELIER.md), figé pour toute la
 * session de jeu : les fiches valides, et pour chaque monstre ses animations et l'instant d'impact de chacune.
 */
public final class Atelier {
    private static Map<String, FicheMonstre> monstres = Map.of();
    private static Map<String, List<String>> animations = Map.of();
    private static Map<String, Map<String, Impact>> impacts = Map.of();
    private static Set<String> sons = Set.of();
    private static Set<Identifier> glowmasks = Set.of();
    private static Map<String, List<String>> rejetees = Map.of();

    private Atelier() {
    }

    static void initMonstres(Map<String, FicheMonstre> valides, Map<String, List<String>> noms,
            Map<String, Map<String, Impact>> instants, Set<String> evenements, Set<Identifier> lumineuses,
            Map<String, List<String>> invalides) {
        monstres = Collections.unmodifiableMap(valides);
        animations = Collections.unmodifiableMap(noms);
        impacts = Collections.unmodifiableMap(instants);
        sons = Collections.unmodifiableSet(evenements);
        glowmasks = Collections.unmodifiableSet(lumineuses);
        rejetees = Collections.unmodifiableMap(invalides);
    }

    /** Les monstres chargés, par id. */
    public static Map<String, FicheMonstre> monstres() {
        return monstres;
    }

    public static @Nullable FicheMonstre monstre(String id) {
        return monstres.get(id);
    }

    /** La fiche d'un type d'entité de l'Atelier (nommé d'après sa fiche, {@code dungeon_defenders:<id>}), sinon null. */
    public static @Nullable FicheMonstre pourType(EntityType<?> type) {
        return monstres.get(EntityType.getKey(type).getPath());
    }

    /** Les noms des animations du .animation.json d'un monstre chargé, triés. */
    public static List<String> animations(String id) {
        return animations.getOrDefault(id, List.of());
    }

    /** Quand une animation d'un monstre porte son coup (son marqueur {@code impact}, ou sa moitié). */
    public static Impact impact(String id, String animation) {
        return impacts.getOrDefault(id, Map.of()).getOrDefault(animation, new Impact(0, false, 0));
    }

    /** Les événements sonores du sounds.json du mod, sans namespace ({@code entity.orc.grogne}), triés. */
    public static Set<String> sons() {
        return sons;
    }

    /** Vrai si cette texture de monstre a son calque lumineux {@code <texture>_glowmask.png}. */
    public static boolean aUnGlowmask(Identifier texture) {
        return glowmasks.contains(texture);
    }

    /** Les fiches ignorées au démarrage, par nom de fichier, avec chaque problème. */
    public static Map<String, List<String>> rejetees() {
        return rejetees;
    }
}
