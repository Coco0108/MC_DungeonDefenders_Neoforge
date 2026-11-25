package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.MapPreviews;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Serveur : envoyé par le client d'un mappeur (créatif) après une capture d'aperçu — voir
// client/MapPreviewCapture. Le serveur revalide tout (ModNetworking#handleUploadMapPreview).
// equals/hashCode par défaut d'un record comparent le tableau par référence : sans importance
// ici, ce paquet n'est jamais comparé ni mis en clé.
public record UploadMapPreviewPayload(Identifier structureId, byte[] png) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UploadMapPreviewPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "upload_map_preview"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UploadMapPreviewPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, UploadMapPreviewPayload::structureId,
            ByteBufCodecs.byteArray(MapPreviews.MAX_BYTES), UploadMapPreviewPayload::png,
            UploadMapPreviewPayload::new
    );

    @Override
    public Type<UploadMapPreviewPayload> type() {
        return TYPE;
    }
}
