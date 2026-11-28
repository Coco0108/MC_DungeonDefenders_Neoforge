package com.github.c0c0tier.dungeon_defenders.atelier;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * La fiche d'un monstre de l'Atelier, telle que le déploiement l'écrit dans
 * {@code data/dungeon_defenders/atelier_monstres/<id>.json} (format : CLAUDE.md de l'Atelier, « Format de la fiche »).
 * Le codec ne vérifie que la forme du fichier ; {@link AtelierLoader} la confronte aux fichiers reçus.
 *
 * <p>Seule une partie de la fiche est lue par Dungeon Defenders (voir doc/ATELIER.md) : ce qui fait le monstre (modèle,
 * animations, textures, taille, stats, sons, réaction aux joueurs). Les autres sections (capacités, apparition, butin,
 * monnaie vivante…) sont reconnues mais ignorées, avec un avertissement au démarrage : {@link #IGNOREES}.
 */
public record FicheMonstre(String id, String nom, Identifier modele, Identifier animations, Identifier texture,
        List<Variante> variantes, Map<String, String> rolesAnimation, float echelle, Hitbox hitbox, Stats stats,
        Comportement comportement, Deplacement deplacement, Map<String, String> sons) {

    /** Rôles obligatoires : joués automatiquement selon le mouvement. */
    public static final List<String> ROLES_OBLIGATOIRES = List.of("idle", "walk");

    /** Rôles de son joués automatiquement : bruit au repos, blessure, mort. */
    public static final List<String> ROLES_SON = List.of("ambient", "hurt", "death");

    /** Sections de la fiche que Dungeon Defenders ne lit pas (encore) : un avertissement, jamais une erreur. */
    public static final Set<String> IGNOREES = Set.of("capacites", "tir", "apparition", "butin", "fuite", "commerce",
            "cibles", "oeuf", "destinations");

    /** Champs lus, plus le bloc {@code origine} réservé au déploiement : tout autre champ est signalé (faute de frappe ?). */
    public static final Set<String> CONNUES = Set.of("id", "nom", "modele", "animations", "texture", "variantes",
            "roles_animation", "echelle", "hitbox", "stats", "comportement", "deplacement", "sons", "origine");

    public static final Codec<FicheMonstre> CODEC = RecordCodecBuilder.create(i -> i.group(
            Fields.required("id", Codec.STRING).forGetter(FicheMonstre::id),
            Fields.required("nom", Codec.STRING).forGetter(FicheMonstre::nom),
            Fields.required("modele", Identifier.CODEC).forGetter(FicheMonstre::modele),
            Fields.required("animations", Identifier.CODEC).forGetter(FicheMonstre::animations),
            Fields.required("texture", Identifier.CODEC).forGetter(FicheMonstre::texture),
            Fields.optional("variantes", Variante.CODEC.listOf(), List.of()).forGetter(FicheMonstre::variantes),
            Fields.required("roles_animation", Codec.unboundedMap(Codec.STRING, Codec.STRING)).forGetter(FicheMonstre::rolesAnimation),
            Fields.optional("echelle", ExtraCodecs.POSITIVE_FLOAT, 1.0F).forGetter(FicheMonstre::echelle),
            Fields.required("hitbox", Hitbox.CODEC).forGetter(FicheMonstre::hitbox),
            Fields.optional("stats", Stats.CODEC, Stats.DEFAULT).forGetter(FicheMonstre::stats),
            Fields.optional("comportement", Comportement.CODEC, Comportement.PASSIF).forGetter(FicheMonstre::comportement),
            Fields.optional("deplacement", Deplacement.CODEC, Deplacement.SOL).forGetter(FicheMonstre::deplacement),
            Fields.optional("sons", Codec.unboundedMap(Codec.STRING, Codec.STRING), Map.of()).forGetter(FicheMonstre::sons)
    ).apply(i, FicheMonstre::new));

    /** Le nom d'animation associé à un rôle, s'il y en a un. */
    public Optional<String> animation(String role) {
        return Optional.ofNullable(rolesAnimation.get(role));
    }

    /** Le nom du son associé à un rôle ({@code hurt} → {@code aie}), s'il y en a un. */
    public Optional<String> son(String role) {
        return Optional.ofNullable(sons.get(role));
    }

    /**
     * L'événement sonore d'un son du monstre : {@code assets/dungeon_defenders/sounds/entity/<id>/<nom>.ogg} est
     * l'événement {@code dungeon_defenders:entity.<id>.<nom>} du sounds.json.
     */
    public Identifier evenementSon(String nom) {
        return Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "entity." + id + "." + nom);
    }

    public boolean vole() {
        return deplacement == Deplacement.VOL;
    }

    /** La texture d'une variante par son index (-1 ou hors liste : la texture principale). */
    public Identifier texture(int variante) {
        return variante >= 0 && variante < variantes.size() ? variantes.get(variante).texture() : texture;
    }

    public record Variante(String id, Identifier texture) {
        public static final Codec<Variante> CODEC = RecordCodecBuilder.create(i -> i.group(
                Fields.required("id", Codec.STRING).forGetter(Variante::id),
                Fields.required("texture", Identifier.CODEC).forGetter(Variante::texture)
        ).apply(i, Variante::new));
    }

    /** Boîte de collision, en blocs. */
    public record Hitbox(float largeur, float hauteur) {
        public static final Codec<Hitbox> CODEC = RecordCodecBuilder.create(i -> i.group(
                Fields.required("largeur", ExtraCodecs.POSITIVE_FLOAT).forGetter(Hitbox::largeur),
                Fields.required("hauteur", ExtraCodecs.POSITIVE_FLOAT).forGetter(Hitbox::hauteur)
        ).apply(i, Hitbox::new));
    }

    /**
     * Attributs vanilla : points de vie, dégâts par coup, vitesse (zombie : 20, 3, 0,23), et {@code rechargeAttaque} :
     * secondes entre deux coups (défaut 1), jamais moins que l'animation {@code attack}.
     */
    public record Stats(float pv, float degats, float vitesse, float rechargeAttaque) {
        public static final Stats DEFAULT = new Stats(20.0F, 3.0F, 0.25F, 1.0F);
        public static final Codec<Stats> CODEC = RecordCodecBuilder.create(i -> i.group(
                Fields.optional("pv", ExtraCodecs.POSITIVE_FLOAT, DEFAULT.pv()).forGetter(Stats::pv),
                Fields.optional("degats", ExtraCodecs.NON_NEGATIVE_FLOAT, DEFAULT.degats()).forGetter(Stats::degats),
                Fields.optional("vitesse", ExtraCodecs.NON_NEGATIVE_FLOAT, DEFAULT.vitesse()).forGetter(Stats::vitesse),
                Fields.optional("recharge_attaque", ExtraCodecs.POSITIVE_FLOAT, DEFAULT.rechargeAttaque())
                        .forGetter(Stats::rechargeAttaque)
        ).apply(i, Stats::new));
    }

    /**
     * La réaction aux JOUEURS (le comportement dans une vague vient du {@code behavior} de son {@code enemy/<id>.json}) :
     * {@code passif} ne les attaque jamais ; {@code neutre} riposte quand on le frappe ; {@code hostile} les attaque à vue,
     * comme un zombie. {@code fuyard} et {@code marchand} (la monnaie vivante de l'Atelier) ne sont pas lus ici : ils se
     * comportent en {@code passif}, avec un avertissement au démarrage.
     */
    public enum Comportement implements StringRepresentable {
        PASSIF("passif"),
        NEUTRE("neutre"),
        HOSTILE("hostile"),
        FUYARD("fuyard"),
        MARCHAND("marchand");

        public static final Codec<Comportement> CODEC = Fields.choice(Comportement.values());

        private final String name;

        Comportement(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        /** Riposte quand on le frappe (et joue son rôle {@code attack}) : {@code neutre} et {@code hostile}. */
        public boolean combat() {
            return this == NEUTRE || this == HOSTILE;
        }
    }

    /** {@code sol} marche comme un zombie ; {@code vol} vole comme une abeille (vitesse de vol 2 × {@code vitesse}). */
    public enum Deplacement implements StringRepresentable {
        SOL("sol"),
        VOL("vol");

        public static final Codec<Deplacement> CODEC = Fields.choice(Deplacement.values());

        private final String name;

        Deplacement(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
