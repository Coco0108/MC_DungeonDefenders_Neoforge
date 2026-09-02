package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé par MapConfigScreen au clic sur "Supprimer cette map", après confirmation : retire la
// structure de la sauvegarde du monde. Ne peut donc effacer qu'une map créée en jeu — une map
// livrée dans un jar (la campagne, un pack tiers) n'est pas modifiable, c'est un fichier de
// ressource en lecture seule.
public record DeleteMapPayload(Identifier structureId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DeleteMapPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "delete_map"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteMapPayload> STREAM_CODEC =
            StreamCodec.composite(
                    Identifier.STREAM_CODEC, DeleteMapPayload::structureId,
                    DeleteMapPayload::new);

    @Override
    public Type<DeleteMapPayload> type() {
        return TYPE;
    }
}
