package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé par ManaChestConfigScreen (client) au clic sur "Valider", appliqué côté serveur par
// ModNetworking à ManaChestBlockEntity.applyConfig(...) après revérification.
public record ManaChestConfigPayload(BlockPos pos, int manaAmount) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ManaChestConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "mana_chest_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ManaChestConfigPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ManaChestConfigPayload::pos,
            ByteBufCodecs.VAR_INT, ManaChestConfigPayload::manaAmount,
            ManaChestConfigPayload::new
    );

    @Override
    public Type<ManaChestConfigPayload> type() {
        return TYPE;
    }
}
