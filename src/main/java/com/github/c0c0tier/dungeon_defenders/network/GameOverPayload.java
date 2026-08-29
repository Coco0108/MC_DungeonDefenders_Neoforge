package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé par PhaseTransitions.onVictory/onDefeat (serveur) à chaque joueur, pour ouvrir
// GameOverScreen côté client (voir DungeonDefendersModClient#onRegisterClientPayloadHandlers).
// Premier paquet clientbound du mod (les autres vont tous du client vers le serveur) : le
// TYPE/STREAM_CODEC est enregistré dans ModNetworking (chargé des deux côtés) mais le handler
// lui-même vit côté client uniquement (RegisterClientPayloadHandlersEvent), pour ne jamais
// charger de classe cliente (Minecraft, Screen...) sur un serveur dédié.
public record GameOverPayload(boolean victory) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GameOverPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "game_over"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GameOverPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, GameOverPayload::victory,
            GameOverPayload::new
    );

    @Override
    public Type<GameOverPayload> type() {
        return TYPE;
    }
}
