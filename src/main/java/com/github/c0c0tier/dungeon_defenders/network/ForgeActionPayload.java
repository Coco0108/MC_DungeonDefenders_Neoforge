package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé par ForgeScreen : une action sur l'inventaire de la Forge. `index` est la case visée
// (ignorée pour UNEQUIP). Action et index sont validés côté serveur.
public record ForgeActionPayload(int action, int index) implements CustomPacketPayload {

    public static final int EQUIP = 0;
    public static final int DISCARD = 1;
    public static final int UNEQUIP = 2;

    public static final CustomPacketPayload.Type<ForgeActionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "forge_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ForgeActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ForgeActionPayload::action,
                    ByteBufCodecs.VAR_INT, ForgeActionPayload::index,
                    ForgeActionPayload::new);

    @Override
    public Type<ForgeActionPayload> type() {
        return TYPE;
    }
}
