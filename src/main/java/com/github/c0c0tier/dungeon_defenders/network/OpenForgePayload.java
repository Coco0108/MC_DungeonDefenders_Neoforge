package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé par ForgeBlock (serveur) au joueur qui clique droit sur la Forge, pour ouvrir
// ForgeScreen côté client. Même raison d'être qu'OpenMapSelectionPayload : le bloc est chargé
// sur un serveur dédié et ne peut donc nommer aucune classe cliente. Aucun champ : l'écran lit
// l'inventaire et l'arme équipée dans des attachments joueur déjà synchronisés.
public record OpenForgePayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenForgePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "open_forge"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenForgePayload> STREAM_CODEC =
            StreamCodec.unit(new OpenForgePayload());

    @Override
    public Type<OpenForgePayload> type() {
        return TYPE;
    }
}
