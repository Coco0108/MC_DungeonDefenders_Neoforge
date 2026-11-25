package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé par HeroStatsScreen : dépense un point dans une caractéristique (ordinal de HeroStat),
// ou rend tous les points si l'ordinal vaut RESET. Validé côté serveur, comme tout ordinal reçu
// du réseau.
public record SpendStatPointPayload(int statOrdinal) implements CustomPacketPayload {

    public static final int RESET = -1;

    public static final CustomPacketPayload.Type<SpendStatPointPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "spend_stat_point"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpendStatPointPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, SpendStatPointPayload::statOrdinal,
                    SpendStatPointPayload::new);

    @Override
    public Type<SpendStatPointPayload> type() {
        return TYPE;
    }
}
