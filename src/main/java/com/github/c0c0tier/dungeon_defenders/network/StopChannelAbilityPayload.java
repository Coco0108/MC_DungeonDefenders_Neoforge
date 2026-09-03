package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé au relâchement d'une touche de compétence maintenue. Aucun champ : un joueur ne
// canalise jamais qu'une seule compétence à la fois (voir PlayerAbilityChannels), inutile de
// préciser laquelle arrêter. Sans effet si rien n'était en cours — le handler se contente de
// retirer une éventuelle entrée, comme StartGamePayload et les autres paquets "signal" du mod.
public record StopChannelAbilityPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StopChannelAbilityPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "stop_channel_ability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StopChannelAbilityPayload> STREAM_CODEC =
            StreamCodec.unit(new StopChannelAbilityPayload());

    @Override
    public Type<StopChannelAbilityPayload> type() {
        return TYPE;
    }
}
