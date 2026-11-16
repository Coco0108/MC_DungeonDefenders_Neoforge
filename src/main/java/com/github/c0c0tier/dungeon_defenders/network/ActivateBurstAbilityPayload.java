package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé une fois, à l'appui de la touche d'une compétence en salve (SPELL_1 aujourd'hui,
// Circular Slice) — pas de touche maintenue, pas de canalisation : voir BurstAbility.
public record ActivateBurstAbilityPayload(int slotOrdinal) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ActivateBurstAbilityPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "activate_burst_ability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActivateBurstAbilityPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ActivateBurstAbilityPayload::slotOrdinal,
                    ActivateBurstAbilityPayload::new);

    @Override
    public Type<ActivateBurstAbilityPayload> type() {
        return TYPE;
    }
}
