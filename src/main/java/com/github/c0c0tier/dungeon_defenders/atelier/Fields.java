package com.github.c0c0tier.dungeon_defenders.atelier;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;

import net.minecraft.util.StringRepresentable;

/**
 * Fields of monster sheets whose decoding errors name the field: {@code `hitbox.largeur`: Value must be positive: 0}
 * instead of the bare codec message. Use these instead of {@code fieldOf}/{@code optionalFieldOf} in sheet codecs.
 * Same helper as in DQM Craft.
 *
 * <p>Repris tel quel de l'Atelier (fiche/Fields.java) : les messages d'erreur des fiches sont les mêmes
 * dans les deux mods.
 */
public final class Fields {
    private Fields() {
    }

    public static <A> MapCodec<A> required(String name, Codec<A> codec) {
        return named(name, codec).fieldOf(name);
    }

    public static <A> MapCodec<A> optional(String name, Codec<A> codec, A defaultValue) {
        return named(name, codec).optionalFieldOf(name, defaultValue);
    }

    public static <A> MapCodec<Optional<A>> maybe(String name, Codec<A> codec) {
        return named(name, codec).optionalFieldOf(name);
    }

    /** One of {@code values}, by its serialized name; an unknown name lists the accepted ones. */
    public static <E extends StringRepresentable> Codec<E> choice(E[] values) {
        return Codec.STRING.comapFlatMap(name -> Stream.of(values).filter(value -> value.getSerializedName().equals(name))
                .findFirst().map(DataResult::success)
                .orElseGet(() -> DataResult.error(() -> "valeur inconnue « " + name + " » (valeurs possibles : "
                        + Stream.of(values).map(StringRepresentable::getSerializedName).collect(Collectors.joining(", ")) + ")")),
                StringRepresentable::getSerializedName);
    }

    private static <A> Codec<A> named(String name, Codec<A> codec) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
                return codec.decode(ops, input).mapError(error -> withPath(name, error));
            }

            @Override
            public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
                return codec.encode(input, ops, prefix);
            }

            @Override
            public String toString() {
                return name + ": " + codec;
            }
        };
    }

    /** Prepends {@code name} to the path of each error; several errors of one object are joined by "; ". */
    static String withPath(String name, String error) {
        return Arrays.stream(error.split("; "))
                .map(part -> part.startsWith("`") ? "`" + name + "." + part.substring(1) : "`" + name + "`: " + part)
                .collect(Collectors.joining("; "));
    }
}
