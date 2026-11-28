package com.github.c0c0tier.dungeon_defenders.atelier;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
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
    private static Map<String, FicheObjet> objets = Map.of();
    private static Map<String, FicheBloc> blocs = Map.of();
    private static Map<String, List<double[]>> formes = Map.of();
    private static Map<String, Map<String, Impact>> impactsBlocs = Map.of();

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

    static void initObjets(Map<String, FicheObjet> validesObjets, Map<String, FicheBloc> validesBlocs,
            Map<String, List<double[]>> boites, Map<String, Map<String, Impact>> instantsBlocs, Set<Identifier> lumineusesBlocs) {
        objets = Collections.unmodifiableMap(validesObjets);
        blocs = Collections.unmodifiableMap(validesBlocs);
        formes = Collections.unmodifiableMap(boites);
        impactsBlocs = Collections.unmodifiableMap(instantsBlocs);
        Set<Identifier> toutes = new HashSet<>(glowmasks);
        toutes.addAll(lumineusesBlocs);
        glowmasks = Collections.unmodifiableSet(toutes);
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

    /** Vrai si cette texture de monstre ou de bloc animé a son calque lumineux {@code <texture>_glowmask.png}. */
    public static boolean aUnGlowmask(Identifier texture) {
        return glowmasks.contains(texture);
    }

    /** Les fiches ignorées au démarrage, par nom de fichier, avec chaque problème. */
    public static Map<String, List<String>> rejetees() {
        return rejetees;
    }

    /** Les objets de atelier_objets/, par id. */
    public static Map<String, FicheObjet> objets() {
        return objets;
    }

    /** Les blocs de atelier_blocs/, par id. */
    public static Map<String, FicheBloc> blocs() {
        return blocs;
    }

    /** Les boîtes d'un bloc {@code modele}, en pixels ({@code [x0, y0, z0, x1, y1, z1]}) ; vide pour un cube. */
    public static List<double[]> forme(String blocId) {
        return formes.getOrDefault(blocId, List.of());
    }

    /** Le modèle animé d'un bloc de l'Atelier ({@code anime} dans sa fiche), s'il en a un et qu'il a été chargé. */
    public static Optional<FicheBloc.Anime> anime(String blocId) {
        FicheBloc bloc = blocs.get(blocId);
        return bloc == null ? Optional.empty() : bloc.anime();
    }

    /**
     * Quand un rôle d'un bloc animé porte son effet (le marqueur {@code impact} de son animation, ou sa moitié), et la
     * durée de cette animation ; {@code Impact(0, false, 0)} si le bloc ou le rôle n'existe pas.
     */
    public static Impact impactBloc(String blocId, String role) {
        return impactsBlocs.getOrDefault(blocId, Map.of()).getOrDefault(role, new Impact(0, false, 0));
    }
}
