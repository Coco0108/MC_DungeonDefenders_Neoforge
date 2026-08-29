package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Clientbound : envoyé par ModEvents.grantScore à chaque gain de score, en plus de la sync de
// ModAttachments.SCORE (le total). Contrairement à l'attachment, qui ne transporte que la
// nouvelle valeur, ce paquet porte le montant, la source du gain (ScoreSource, transmise par
// ordinal comme le reste des enums réseau du mod) ET, quand pertinent, l'ennemi tué
// (SpawnableEnemy, même principe) pour l'icône du popup — c'est ce qui permet à
// ScoreGainOverlay d'afficher "+10 Ennemi tué" avec l'œuf du zombie plutôt qu'un "+10" muet
// reconstitué en devinant la différence entre deux valeurs de SCORE. Type enregistré côté
// partagé (ModNetworking), mais le handler n'existe que côté client (DungeonDefendersModClient)
// — voir ce fichier pour le pourquoi.
public record ScoreGainPayload(int amount, int sourceOrdinal, int enemyOrdinal) implements CustomPacketPayload {

    // Sentinelle : aucun ennemi associé à ce gain (toute future source sans ennemi précis, ex.
    // fin de vague/de map) — pas d'Optional<Integer> sur le réseau, ce mod n'utilise ce patron
    // nulle part ailleurs, un entier négatif suffit et reste lisible.
    public static final int NO_ENEMY = -1;

    public static final CustomPacketPayload.Type<ScoreGainPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "score_gain"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScoreGainPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ScoreGainPayload::amount,
            ByteBufCodecs.VAR_INT, ScoreGainPayload::sourceOrdinal,
            ByteBufCodecs.VAR_INT, ScoreGainPayload::enemyOrdinal,
            ScoreGainPayload::new
    );

    @Override
    public Type<ScoreGainPayload> type() {
        return TYPE;
    }
}
