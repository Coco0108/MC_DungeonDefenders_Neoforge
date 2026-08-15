package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé par TowerPlacementClientEvents (client) au clic droit de confirmation finale de la
// roue de tours, appliqué côté serveur par ModNetworking.handlePlaceTower. towerOrdinal et
// directionOrdinal sont des index (TowerDefinition/Direction) : jamais faire confiance à des
// ordinaux reçus du réseau sans les borner avant indexation, comme les autres payloads
// ordinal-based du mod (voir handleSetDifficulty/handleSpawnerConfig).
public record PlaceTowerPayload(int towerOrdinal, BlockPos pos, int directionOrdinal) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PlaceTowerPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "place_tower"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlaceTowerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlaceTowerPayload::towerOrdinal,
            BlockPos.STREAM_CODEC, PlaceTowerPayload::pos,
            ByteBufCodecs.VAR_INT, PlaceTowerPayload::directionOrdinal,
            PlaceTowerPayload::new
    );

    @Override
    public Type<PlaceTowerPayload> type() {
        return TYPE;
    }
}
