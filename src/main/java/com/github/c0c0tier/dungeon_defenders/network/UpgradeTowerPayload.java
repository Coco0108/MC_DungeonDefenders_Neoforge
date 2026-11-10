package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Serverbound : le joueur demande à améliorer la tour qu'il regarde (touche dédiée, voir
// client/TowerInspectClientEvents). Tout est revérifié côté serveur (phase, distance, niveau
// maximum, mana) dans ModNetworking#handleUpgradeTower — même forme que RemoveTowerPayload.
public record UpgradeTowerPayload(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UpgradeTowerPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "upgrade_tower"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeTowerPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, UpgradeTowerPayload::pos,
            UpgradeTowerPayload::new
    );

    @Override
    public Type<UpgradeTowerPayload> type() {
        return TYPE;
    }
}
