package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

// Clientbound : les chiffres data-driven des tours (voir init/TowerStatsRegistry), envoyés à
// chaque joueur qui se connecte et à tout le monde après un /reload (OnDatapackSyncEvent). Le
// client en a besoin pour la roue (coût en mana), l'aperçu de portée à la pose, et la barre de
// vie des tours (PV max).
//
// Forme volontairement souple : id de tour -> (nom de stat -> valeur). Une tour ou une stat que
// le client ne connaît pas est simplement ignorée. Type enregistré côté commun (ModNetworking),
// handler côté client uniquement (DungeonDefendersModClient) — même règle que ScoreGainPayload.
public record TowerStatsPayload(Map<String, Map<String, Double>> statsByTower) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TowerStatsPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "tower_stats"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Map<String, Double>> STATS_CODEC =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.DOUBLE);

    public static final StreamCodec<RegistryFriendlyByteBuf, TowerStatsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, STATS_CODEC), TowerStatsPayload::statsByTower,
            TowerStatsPayload::new);

    @Override
    public Type<TowerStatsPayload> type() {
        return TYPE;
    }
}
