package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

// Clientbound : la liste des ennemis data-driven (voir init/EnemyRegistry), envoyée à chaque
// joueur qui se connecte et à tout le monde après un /reload (OnDatapackSyncEvent). Ne porte que
// ce dont le client a besoin pour AFFICHER un ennemi — icône, nom, ordre, type de mob pour la
// barre de vie — jamais son comportement, qui reste une affaire serveur.
//
// Des identifiants plutôt que des EntityType/Item : le client les résout lui-même dans ses
// registres (voir client/ClientEnemyDefinitions), et un identifiant qu'il ne connaîtrait pas ne
// casse pas le décodage du paquet. Type enregistré côté commun (ModNetworking), handler côté
// client uniquement (DungeonDefendersModClient) — même règle que ScoreGainPayload.
public record EnemyDefinitionsPayload(List<Entry> enemies) implements CustomPacketPayload {

    public record Entry(Identifier id, Identifier entityType, Identifier icon, int order) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Entry::id,
                Identifier.STREAM_CODEC, Entry::entityType,
                Identifier.STREAM_CODEC, Entry::icon,
                ByteBufCodecs.VAR_INT, Entry::order,
                Entry::new);
    }

    public static final CustomPacketPayload.Type<EnemyDefinitionsPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "enemy_definitions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnemyDefinitionsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, Entry.STREAM_CODEC), EnemyDefinitionsPayload::enemies,
            EnemyDefinitionsPayload::new);

    @Override
    public Type<EnemyDefinitionsPayload> type() {
        return TYPE;
    }
}
