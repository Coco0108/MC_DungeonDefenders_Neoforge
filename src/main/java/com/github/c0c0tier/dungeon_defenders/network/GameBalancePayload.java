package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

// Clientbound : les réglages globaux d'équilibrage (voir init/GameBalance), envoyés à la
// connexion et après chaque /reload. Le client en a besoin partout où il affiche un nombre qui en
// dépend (aperçu du spawner, barre d'expérience...). Clé : BalanceValue#networkName ; une clé
// inconnue du client est ignorée. Handler côté client uniquement (DungeonDefendersModClient).
public record GameBalancePayload(Map<String, Double> values) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GameBalancePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "game_balance"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GameBalancePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.DOUBLE), GameBalancePayload::values,
            GameBalancePayload::new);

    @Override
    public Type<GameBalancePayload> type() {
        return TYPE;
    }
}
