package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé par le bouton "Abandonner le niveau" ajouté au menu pause (voir
// client/PauseMenuClientEvents), une fois la confirmation acceptée : déclenche
// MapInstance.returnToTavern(...) côté serveur. Aucun champ, c'est un simple signal — comme
// StartGamePayload, son pendant à l'aller.
//
// Doublon assumé avec la commande /dd_leave, qui fait la même chose : celle-ci reste comme
// harnais de test (utilisable à tout moment, y compris pour déboguer), le bouton est le chemin
// destiné aux joueurs.
public record LeaveMapPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LeaveMapPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "leave_map"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LeaveMapPayload> STREAM_CODEC =
            StreamCodec.unit(new LeaveMapPayload());

    @Override
    public Type<LeaveMapPayload> type() {
        return TYPE;
    }
}
