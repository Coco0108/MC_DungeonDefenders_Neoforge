package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

// Envoyé par SpawnerConfigScreen (client) au clic sur "Valider", appliqué côté serveur par
// ModNetworking à SpawnerBlockEntity.applyConfig(...). La composition est une liste de
// longueur variable (entries) : chaque ligne du GUI = un ennemi (par ordinal SpawnableEnemy)
// + son nombre de base. Remplace entièrement la composition existante à l'application.
public record SpawnerConfigPayload(
        BlockPos pos,
        int intervalTicks,
        int spawnRadius,
        int waveStart,
        int waveEnd,
        List<Entry> entries
) implements CustomPacketPayload {

    /** Une ligne de composition : quel ennemi (ordinal SpawnableEnemy) et son nombre de base. */
    public record Entry(int enemyOrdinal, int baseCount) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entry::enemyOrdinal,
                ByteBufCodecs.VAR_INT, Entry::baseCount,
                Entry::new
        );
    }

    public static final CustomPacketPayload.Type<SpawnerConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "spawner_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpawnerConfigPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SpawnerConfigPayload::pos,
            ByteBufCodecs.VAR_INT, SpawnerConfigPayload::intervalTicks,
            ByteBufCodecs.VAR_INT, SpawnerConfigPayload::spawnRadius,
            ByteBufCodecs.VAR_INT, SpawnerConfigPayload::waveStart,
            ByteBufCodecs.VAR_INT, SpawnerConfigPayload::waveEnd,
            ByteBufCodecs.collection(ArrayList::new, Entry.STREAM_CODEC), SpawnerConfigPayload::entries,
            SpawnerConfigPayload::new
    );

    @Override
    public Type<SpawnerConfigPayload> type() {
        return TYPE;
    }
}
