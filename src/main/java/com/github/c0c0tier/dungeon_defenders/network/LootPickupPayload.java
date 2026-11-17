package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé par LootPickupClientEvents quand le joueur appuie sur Ranger ou Équiper près d'un objet
// de butin au sol. Le serveur revérifie tout (l'entité existe, c'est du butin, elle est à
// portée) : voir Equipment#pickUp.
public record LootPickupPayload(int entityId, boolean equip) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LootPickupPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "loot_pickup"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LootPickupPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, LootPickupPayload::entityId,
                    ByteBufCodecs.BOOL, LootPickupPayload::equip,
                    LootPickupPayload::new);

    @Override
    public Type<LootPickupPayload> type() {
        return TYPE;
    }
}
