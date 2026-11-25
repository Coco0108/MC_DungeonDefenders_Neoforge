package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Optional;

// Envoyé par AbilityClientEvents au moment où une touche de compétence maintenue (HEAL,
// SPELL_2, ou REPAIR — jamais SPELL_1, une salve, voir ActivateBurstAbilityPayload) est
// enfoncée. `target` porte la position visée pour Repair (calculée côté client par raycast au
// moment du clic, pas revisée pendant que la touche reste tenue — voir doc/02-gameplay.md pour
// la simplification assumée), vide pour Heal/Blood Rage qui ciblent le joueur lui-même.
//
// Générique par emplacement (slotOrdinal) plutôt qu'un paquet par compétence nommée : le
// serveur résout laquelle appliquer à partir du héros du joueur — voir
// ModNetworking#handleStartChannelAbility.
public record StartChannelAbilityPayload(int slotOrdinal, Optional<BlockPos> target) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StartChannelAbilityPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "start_channel_ability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StartChannelAbilityPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, StartChannelAbilityPayload::slotOrdinal,
                    ByteBufCodecs.optional(BlockPos.STREAM_CODEC), StartChannelAbilityPayload::target,
                    StartChannelAbilityPayload::new);

    @Override
    public Type<StartChannelAbilityPayload> type() {
        return TYPE;
    }
}
