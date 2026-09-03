package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé par HeroSelectionScreen au clic sur "Choisir" : change le héros du joueur.
//
// Porte un ordinal, comme SetDifficultyPayload et PlaceTowerPayload — validé côté serveur avant
// toute indexation, on ne fait jamais confiance à une valeur reçue par le réseau.
public record SelectHeroPayload(int heroOrdinal) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectHeroPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "select_hero"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectHeroPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SelectHeroPayload::heroOrdinal,
                    SelectHeroPayload::new);

    @Override
    public Type<SelectHeroPayload> type() {
        return TYPE;
    }
}
