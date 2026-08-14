package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé par MapSelectionScreen (client) au clic sur "Jouer", juste après SetDifficultyPayload
// — déclenche MapInstance.startGame(...) côté serveur. Pas de champ : le choix de map lui-même
// n'a pas encore d'effet (un seul placeholder générique pour l'instant, voir
// 05-etat-et-problemes-connus.md).
public record StartGamePayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StartGamePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "start_game"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StartGamePayload> STREAM_CODEC =
            StreamCodec.unit(new StartGamePayload());

    @Override
    public Type<StartGamePayload> type() {
        return TYPE;
    }
}
