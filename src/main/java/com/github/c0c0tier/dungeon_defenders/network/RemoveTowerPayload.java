package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé par TowerRemovalClientEvents (client) au clic gauche pendant le mode suppression de
// tour (touche ModKeyMappings.REMOVE_TOWER_MODE), appliqué côté serveur par
// ModNetworking.handleRemoveTower. La position visée est revalidée intégralement côté serveur
// (portée, phase, présence d'une AbstractTowerBlockEntity) — jamais faire confiance au client
// pour dire "c'est bien une tour", même mode actif ou pas.
public record RemoveTowerPayload(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RemoveTowerPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "remove_tower"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveTowerPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RemoveTowerPayload::pos,
            RemoveTowerPayload::new
    );

    @Override
    public Type<RemoveTowerPayload> type() {
        return TYPE;
    }
}
