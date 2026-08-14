package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé par MapSelectionScreen (client) au clic sur "Jouer", appliqué côté serveur par
// ModNetworking à ModAttachments.DIFFICULTY. Ne couvre pour l'instant que la difficulté — le
// choix de map lui-même n'a pas encore d'effet (pas de système de chargement de map), voir
// 05-etat-et-problemes-connus.md.
public record SetDifficultyPayload(int difficultyOrdinal) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetDifficultyPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "set_difficulty"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetDifficultyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetDifficultyPayload::difficultyOrdinal,
            SetDifficultyPayload::new
    );

    @Override
    public Type<SetDifficultyPayload> type() {
        return TYPE;
    }
}
