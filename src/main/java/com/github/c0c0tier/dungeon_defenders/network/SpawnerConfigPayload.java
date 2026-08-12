package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé par SpawnerConfigScreen (client) au clic sur "Valider", appliqué côté serveur par
// ModNetworking à SpawnerBlockEntity.applyConfig(...). Un seul paquet, forme figée (pas de
// liste variable) : le GUI n'édite que zombie/squelette pour l'instant, voir
// 05-etat-et-problemes-connus.md.
public record SpawnerConfigPayload(
        BlockPos pos,
        int intervalTicks,
        int spawnRadius,
        int waveStart,
        int waveEnd,
        int zombieBaseCount,
        int skeletonBaseCount
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SpawnerConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "spawner_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpawnerConfigPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SpawnerConfigPayload::pos,
            ByteBufCodecs.VAR_INT, SpawnerConfigPayload::intervalTicks,
            ByteBufCodecs.VAR_INT, SpawnerConfigPayload::spawnRadius,
            ByteBufCodecs.VAR_INT, SpawnerConfigPayload::waveStart,
            ByteBufCodecs.VAR_INT, SpawnerConfigPayload::waveEnd,
            ByteBufCodecs.VAR_INT, SpawnerConfigPayload::zombieBaseCount,
            ByteBufCodecs.VAR_INT, SpawnerConfigPayload::skeletonBaseCount,
            SpawnerConfigPayload::new
    );

    @Override
    public Type<SpawnerConfigPayload> type() {
        return TYPE;
    }
}
