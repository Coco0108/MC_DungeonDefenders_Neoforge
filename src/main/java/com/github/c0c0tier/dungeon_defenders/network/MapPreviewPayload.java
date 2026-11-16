package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.MapPreviews;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Client : l'aperçu d'une map créée en jeu, envoyé avant l'ouverture de l'écran de choix (voir
// TavernCrystalBlock) et à tous les joueurs quand un mappeur vient d'en capturer un. Handler côté
// client uniquement (DungeonDefendersModClient), comme les autres paquets clientbound.
// equals/hashCode par défaut d'un record comparent le tableau par référence : sans importance
// ici, ce paquet n'est jamais comparé ni mis en clé.
public record MapPreviewPayload(Identifier structureId, byte[] png) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MapPreviewPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "map_preview"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MapPreviewPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, MapPreviewPayload::structureId,
            ByteBufCodecs.byteArray(MapPreviews.MAX_BYTES), MapPreviewPayload::png,
            MapPreviewPayload::new
    );

    @Override
    public Type<MapPreviewPayload> type() {
        return TYPE;
    }
}
