package com.github.c0c0tier.dungeon_defenders.atelier;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.SoundType;

/**
 * Repris de {@code fiche/FicheBloc.java} de l'Atelier, (v2 : {@code orientation}, {@code type}), SANS la section {@code cache} (bloc-cachette : ignorée ici, avec un
 * avertissement) ni {@code destinations}. Déployé ici en {@code data/dungeon_defenders/atelier_blocs/<id>.json}, le bloc
 * {@code dungeon_defenders:<id>} et son objet ; la forme d'un bloc {@code modele} en
 * {@code data/dungeon_defenders/atelier_formes_bloc/<id>.json} (voir doc/ATELIER.md).
 *
 * <p>A block sheet: {@code data/atelier/blocs/<id>.json}, the block {@code atelier:<id>} and its item. A full cube: its
 * texture is {@code assets/atelier/textures/block/<id>.png}, or per face {@code <id>_top.png}, {@code <id>_bottom.png},
 * {@code <id>_side.png} next to it (the missing ones use the main texture). With {@code modele}, the model of a monster in
 * its hiding pose instead, converted from its .bbmodel ({@code modeles/blocs/<id>/}): the build copies its block model and
 * texture, and its shape ({@link #formePath()}). It always drops itself. The build generates its models, its loot table,
 * its tool tags and its name. Format documented in CLAUDE.md ("Objets et blocs").
 *
 * @param durete time to break it (dirt 0.5, stone 1.5, iron block 5, obsidian 50)
 * @param resistance to explosions; {@code durete} by default (stone 6, obsidian 1200)
 * @param outil the tool that breaks it faster
 * @param niveau from {@code bois} up, it only drops when broken with {@code outil} of at least that tier
 * @param lumiere light it gives, 0 to 15 (torch 14, glowstone 15)
 * @param modele not a cube: the converted model of a monster, with its own shape (selection and collision)
 * @param orientation how it turns when placed ({@link Orientation})
 * @param type a piece of joinery with block states, like its vanilla oak counterpart ({@link Type})
 * @param transparent see-through (glass, leaves): does not hide the faces next to it; the transparent pixels of its
 *        texture are then drawn as holes (or tinted, if half transparent)
 * @param anime a {@code modele} block with an animated model ({@link Anime}), drawn by GeckoLib once placed
 */
public record FicheBloc(String id, String nom, Optional<String> nomEn, float durete, Optional<Float> resistance, Outil outil,
        Niveau niveau, int lumiere, Son son, boolean modele, boolean transparent,
        Orientation orientation, Type type, Optional<Anime> anime) {
    public static final Codec<FicheBloc> CODEC = RecordCodecBuilder.create(i -> i.group(
            Fields.required("id", Codec.STRING).forGetter(FicheBloc::id),
            Fields.required("nom", Codec.STRING).forGetter(FicheBloc::nom),
            Fields.maybe("nom_en", Codec.STRING).forGetter(FicheBloc::nomEn),
            Fields.optional("durete", ExtraCodecs.NON_NEGATIVE_FLOAT, 1.5F).forGetter(FicheBloc::durete),
            Fields.maybe("resistance", ExtraCodecs.NON_NEGATIVE_FLOAT).forGetter(FicheBloc::resistance),
            Fields.optional("outil", Outil.CODEC, Outil.PIOCHE).forGetter(FicheBloc::outil),
            Fields.optional("niveau", Niveau.CODEC, Niveau.AUCUN).forGetter(FicheBloc::niveau),
            Fields.optional("lumiere", Codec.intRange(0, 15), 0).forGetter(FicheBloc::lumiere),
            Fields.optional("son", Son.CODEC, Son.PIERRE).forGetter(FicheBloc::son),
            Fields.optional("modele", Codec.BOOL, false).forGetter(FicheBloc::modele),
            Fields.optional("transparent", Codec.BOOL, false).forGetter(FicheBloc::transparent),
            Fields.optional("orientation", Orientation.CODEC, Orientation.AUCUNE).forGetter(FicheBloc::orientation),
            Fields.optional("type", Type.CODEC, Type.SIMPLE).forGetter(FicheBloc::type),
            Fields.maybe("anime", Anime.CODEC).forGetter(FicheBloc::anime)
    ).apply(i, FicheBloc::new));

    /**
     * The animated model of a {@code modele} block (prompt de l'Atelier v2.3, format proposé par
     * doc/DEMANDE_ATELIER_BLOCS_ANIMES.md et retenu tel quel). The converted model, its texture and its shape still serve
     * the item, the breaking particles, the shape and the fallback; the placed block is drawn by GeckoLib.
     *
     * @param modele {@code dungeon_defenders:geckolib/models/block/<id>.geo.json}
     * @param animations {@code dungeon_defenders:geckolib/animations/block/<id>.animation.json}
     * @param texture {@code dungeon_defenders:textures/geckolib/block/<id>.png} (outside the block atlas), its glowmask
     *        {@code <texture>_glowmask.png} next to it if it has one
     * @param rolesAnimation role ({@link #ROLES}) → animation name; a missing role does not play
     */
    public record Anime(Identifier modele, Identifier animations, Identifier texture, Map<String, String> rolesAnimation) {
        /** The roles a game can play: {@code idle} and {@code opened} loop, the others play once. */
        public static final List<String> ROLES = List.of("idle", "open", "opened", "close", "hurt", "hurt_heavy", "death");

        public static final Codec<Anime> CODEC = RecordCodecBuilder.create(i -> i.group(
                Fields.required("modele", Identifier.CODEC).forGetter(Anime::modele),
                Fields.required("animations", Identifier.CODEC).forGetter(Anime::animations),
                Fields.required("texture", Identifier.CODEC).forGetter(Anime::texture),
                Fields.optional("roles_animation", Codec.unboundedMap(Codec.STRING, Codec.STRING), Map.of())
                        .forGetter(Anime::rolesAnimation)
        ).apply(i, Anime::new));
    }

    /** Where its main texture must be, in the mod jar. */
    public String texturePath() {
        return "assets/" + DungeonDefendersMod.MODID + "/textures/block/" + id + ".png";
    }

    /** Its block model, in the mod jar (written by the build: a cube, or the converted model). */
    public String modelPath() {
        return "assets/" + DungeonDefendersMod.MODID + "/models/block/" + id + ".json";
    }

    /**
     * The state models a {@code modele} joinery block needs, as deployed here ({@code models/block/<id>_<suffix>.json});
     * empty for a {@code simple} block (its single model is {@link #modelPath()}) or a joinery cube (vanilla templates
     * with its texture, written by the deployment).
     */
    public List<String> modelesEtats() {
        return type == Type.SIMPLE || !modele ? List.of()
                : type.modeleFiles(id).stream().map(file -> "assets/" + DungeonDefendersMod.MODID + "/models/block/" + file).toList();
    }

    /**
     * Shape of a {@code modele} block, copied by the build from {@code modeles/blocs/<id>/<id>.forme.json}:
     * {@code {"boites_px": [[x0, y0, z0, x1, y1, z1], …]}}, boxes in pixels of the block (1/16).
     */
    public String formePath() {
        return "data/" + DungeonDefendersMod.MODID + "/atelier_formes_bloc/" + id + ".json";
    }

    /**
     * {@code aucune}: always the same way. {@code horizontale} (a {@code modele} block): its front, modelled towards the
     * north, faces the player who places it (block state {@code facing}, like a furnace); a wall piece, modelled against
     * the south face, thus ends up against the wall the player looks at. {@code axe} (a cube): like a log, along the axis
     * of the face it is placed against (block state {@code axis}), {@code <id>_top.png} on both ends.
     */
    public enum Orientation implements StringRepresentable {
        AUCUNE("aucune"),
        HORIZONTALE("horizontale"),
        AXE("axe");

        public static final Codec<Orientation> CODEC = Fields.choice(Orientation.values());

        private final String name;

        Orientation(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /**
     * {@code simple}: a cube or a {@code modele}. The others behave exactly like their vanilla oak counterpart (vanilla
     * classes: shapes, opening, connections, placement, sounds of oak), with one model per state: the vanilla templates
     * with {@code textures/block/<id>.png}, or with {@code modele} the models of {@code modeles/blocs/<id>/}, named
     * {@code <id>_<suffix>.json} (see {@link #modeles}), drawn on the vanilla template of the same name. The build writes
     * the block states, like vanilla's.
     */
    public enum Type implements StringRepresentable {
        SIMPLE("simple"),
        PORTE("porte", "bas_gauche", "haut_gauche", "bas_droite", "haut_droite"),
        TRAPPE("trappe", "", "haut", "ouverte"),
        ESCALIER("escalier", "", "interieur", "exterieur"),
        DALLE("dalle", "", "haut"),
        BARRIERE("barriere", "poteau", "cote"),
        PORTILLON("portillon", "", "ouvert");

        public static final Codec<Type> CODEC = Fields.choice(Type.values());

        private final String name;
        /** Suffixes of the models a {@code modele} block of this type needs ({@code ""}: {@code <id>.json}). */
        public final List<String> modeles;

        Type(String name, String... modeles) {
            this.name = name;
            this.modeles = List.of(modeles);
        }

        /** The model files of {@code modeles/blocs/<id>/} it needs. */
        public List<String> modeleFiles(String id) {
            return modeles.stream().map(suffix -> id + (suffix.isEmpty() ? "" : "_" + suffix) + ".json").toList();
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public enum Outil implements StringRepresentable {
        AUCUN("aucun"),
        PIOCHE("pioche"),
        HACHE("hache"),
        PELLE("pelle"),
        HOUE("houe");

        public static final Codec<Outil> CODEC = Fields.choice(Outil.values());

        private final String name;

        Outil(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /** {@code aucun}: drops even by hand. Otherwise the lowest tool tier that makes it drop. */
    public enum Niveau implements StringRepresentable {
        AUCUN("aucun"),
        BOIS("bois"),
        PIERRE("pierre"),
        FER("fer"),
        DIAMANT("diamant");

        public static final Codec<Niveau> CODEC = Fields.choice(Niveau.values());

        private final String name;

        Niveau(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /** Sounds when placed, broken and walked on. */
    public enum Son implements StringRepresentable {
        PIERRE("pierre", SoundType.STONE),
        BOIS("bois", SoundType.WOOD),
        TERRE("terre", SoundType.GRAVEL),
        HERBE("herbe", SoundType.GRASS),
        SABLE("sable", SoundType.SAND),
        METAL("metal", SoundType.METAL),
        VERRE("verre", SoundType.GLASS),
        LAINE("laine", SoundType.WOOL),
        AMETHYSTE("amethyste", SoundType.AMETHYST);

        public static final Codec<Son> CODEC = Fields.choice(Son.values());

        private final String name;
        public final SoundType sound;

        Son(String name, SoundType sound) {
            this.name = name;
            this.sound = sound;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
