package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Clientbound : envoyé par ModNetworking#handleActivateBurstAbility au joueur dont une
// compétence en salve vient de partir, pour que son HUD (AbilitySlotsOverlay) affiche la
// recharge. Le serveur reste seul juge de la recharge (AbilityCooldowns) : ce paquet n'est
// qu'un affichage. On transmet une DURÉE et non un instant de fin, pour ne pas dépendre de la
// synchronisation de l'horloge du monde entre serveur et client.
public record AbilityCooldownPayload(int slotOrdinal, int cooldownTicks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AbilityCooldownPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "ability_cooldown"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityCooldownPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AbilityCooldownPayload::slotOrdinal,
            ByteBufCodecs.VAR_INT, AbilityCooldownPayload::cooldownTicks,
            AbilityCooldownPayload::new
    );

    @Override
    public Type<AbilityCooldownPayload> type() {
        return TYPE;
    }
}
