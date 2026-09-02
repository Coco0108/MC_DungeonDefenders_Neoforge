package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé par MapConfigScreen (client, créatif) au clic sur "Valider" : applique les réglages au
// MapConfigBlockEntity visé. Même patron que SpawnerConfigPayload — le serveur revérifie tout
// (créatif, portée, présence réelle du bloc) avant d'appliquer.
public record MapConfigPayload(
        BlockPos pos,
        String displayName,
        int order,
        int waveCount,
        float scoreMultiplier) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MapConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "map_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MapConfigPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, MapConfigPayload::pos,
                    ByteBufCodecs.STRING_UTF8, MapConfigPayload::displayName,
                    ByteBufCodecs.VAR_INT, MapConfigPayload::order,
                    ByteBufCodecs.VAR_INT, MapConfigPayload::waveCount,
                    ByteBufCodecs.FLOAT, MapConfigPayload::scoreMultiplier,
                    MapConfigPayload::new);

    @Override
    public Type<MapConfigPayload> type() {
        return TYPE;
    }
}
