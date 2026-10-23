package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Envoyé par TavernCrystalBlock (serveur) au joueur qui clique droit sur le cristal de la
// taverne, pour ouvrir MapSelectionScreen côté client — même mécanique que GameOverPayload :
// TYPE/STREAM_CODEC enregistrés dans ModNetworking (chargé des deux côtés), handler enregistré
// côté client uniquement (DungeonDefendersModClient#onRegisterClientPayloadHandlers).
//
// Pourquoi un paquet plutôt qu'un simple `if (level.isClientSide()) Minecraft.getInstance()
// .setScreen(...)` dans le bloc (la version d'origine) : un serveur dédié n'embarque AUCUNE
// classe cliente, et charger TavernCrystalBlock (ce que fait ModBlocks au démarrage du mod)
// force la JVM à résoudre toutes les classes citées dans ses méthodes — y compris celles de la
// branche `isClientSide` qui ne s'exécute jamais sur un serveur. Résultat constaté en jeu
// (2026-08-30, serveur dédié du joueur) : NoClassDefFoundError sur
// net/minecraft/client/gui/screens/Screen dès constructMods, le mod ne charge pas du tout.
// Un test de côté à l'exécution ne suffit donc pas : il faut qu'aucune classe cliente ne soit
// NOMMÉE dans une classe chargée par le serveur.
//
// Aucun champ : le paquet ne porte aucune donnée (la liste des maps est statique côté client,
// la difficulté vient d'un attachment de Level déjà synchronisé — voir MapSelectionScreen),
// c'est un simple signal "ouvre cet écran". D'où StreamCodec.unit, qui n'écrit ni ne lit un
// seul octet.
public record OpenMapSelectionPayload() implements CustomPacketPayload {

    public static final OpenMapSelectionPayload INSTANCE = new OpenMapSelectionPayload();

    public static final CustomPacketPayload.Type<OpenMapSelectionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "open_map_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMapSelectionPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<OpenMapSelectionPayload> type() {
        return TYPE;
    }
}
