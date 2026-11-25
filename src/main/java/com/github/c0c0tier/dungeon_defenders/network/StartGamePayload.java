package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé par MapSelectionScreen (client) au clic sur "Jouer", juste après SetDifficultyPayload
// — déclenche MapInstance.startGame(...) côté serveur.
//
// Porte désormais l'identifiant de la structure choisie (2026-09-02). Sans ce champ, le
// carrousel n'était que décoratif : "Jouer" lançait toujours la même arène placeholder quelle
// que soit la map sélectionnée.
public record StartGamePayload(Identifier structureId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StartGamePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "start_game"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StartGamePayload> STREAM_CODEC =
            StreamCodec.composite(
                    Identifier.STREAM_CODEC, StartGamePayload::structureId,
                    StartGamePayload::new);

    @Override
    public Type<StartGamePayload> type() {
        return TYPE;
    }
}
