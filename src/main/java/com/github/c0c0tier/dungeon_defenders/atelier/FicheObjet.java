package com.github.c0c0tier.dungeon_defenders.atelier;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Rarity;

/**
 * Repris de {@code fiche/FicheObjet.java} de l'Atelier. Déployé ici en {@code data/dungeon_defenders/atelier_objets/<id>.json},
 * l'objet {@code dungeon_defenders:<id>} (voir doc/ATELIER.md).
 *
 * <p>An item sheet: {@code data/atelier/objets/<id>.json}, the item {@code atelier:<id>}. Its texture is
 * {@code assets/atelier/textures/item/<id>.png} (16 × 16); the build generates its model and its name. Format documented
 * in CLAUDE.md ("Objets et blocs").
 *
 * @param nomEn English name; the French one by default
 * @param pile stack size, 1 to 99 (vanilla 64; 16 for eggs and pearls, 1 for tools)
 * @param brillant the enchantment glint
 * @param nourriture eaten like vanilla food when present
 */
public record FicheObjet(String id, String nom, Optional<String> nomEn, int pile, Rarete rarete, boolean brillant,
        Optional<Nourriture> nourriture, List<String> destinations) {
    public static final Codec<FicheObjet> CODEC = RecordCodecBuilder.create(i -> i.group(
            Fields.required("id", Codec.STRING).forGetter(FicheObjet::id),
            Fields.required("nom", Codec.STRING).forGetter(FicheObjet::nom),
            Fields.maybe("nom_en", Codec.STRING).forGetter(FicheObjet::nomEn),
            Fields.optional("pile", Codec.intRange(1, 99), 64).forGetter(FicheObjet::pile),
            Fields.optional("rarete", Rarete.CODEC, Rarete.COMMUN).forGetter(FicheObjet::rarete),
            Fields.optional("brillant", Codec.BOOL, false).forGetter(FicheObjet::brillant),
            Fields.maybe("nourriture", Nourriture.CODEC).forGetter(FicheObjet::nourriture),
            Fields.optional("destinations", Codec.STRING.listOf(), List.of()).forGetter(FicheObjet::destinations)
    ).apply(i, FicheObjet::new));

    /** Where its texture must be, in the mod jar. */
    public String texturePath() {
        return "assets/" + DungeonDefendersMod.MODID + "/textures/item/" + id + ".png";
    }

    /**
     * Vanilla food values: {@code faim} half-drumsticks restored (bread 5, steak 8), {@code saturation} modifier (bread
     * 0.6, steak 0.8), {@code toujours}: edible even when not hungry, like a golden apple.
     */
    public record Nourriture(int faim, float saturation, boolean toujours) {
        public static final Codec<Nourriture> CODEC = RecordCodecBuilder.create(i -> i.group(
                Fields.required("faim", Codec.intRange(0, 20)).forGetter(Nourriture::faim),
                Fields.optional("saturation", ExtraCodecs.NON_NEGATIVE_FLOAT, 0.6F).forGetter(Nourriture::saturation),
                Fields.optional("toujours", Codec.BOOL, false).forGetter(Nourriture::toujours)
        ).apply(i, Nourriture::new));
    }

    /** The color of its name, as vanilla rarities: white, yellow, aqua, magenta. */
    public enum Rarete implements StringRepresentable {
        COMMUN("commun", Rarity.COMMON),
        PEU_COMMUN("peu_commun", Rarity.UNCOMMON),
        RARE("rare", Rarity.RARE),
        EPIQUE("epique", Rarity.EPIC);

        public static final Codec<Rarete> CODEC = Fields.choice(Rarete.values());

        private final String name;
        public final Rarity rarity;

        Rarete(String name, Rarity rarity) {
            this.name = name;
            this.rarity = rarity;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
