package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.MapDefinition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

// Envoyé par TavernCrystalBlock (serveur) au joueur qui clique droit sur le cristal de la
// taverne, pour ouvrir MapSelectionScreen côté client — TYPE/STREAM_CODEC enregistrés dans
// ModNetworking (chargé des deux côtés), handler enregistré côté client uniquement
// (DungeonDefendersModClient#onRegisterClientPayloadHandlers).
//
// Pourquoi un paquet plutôt qu'un simple `if (level.isClientSide()) Minecraft.getInstance()
// .setScreen(...)` dans le bloc (la version d'origine) : un serveur dédié n'embarque AUCUNE
// classe cliente, et charger TavernCrystalBlock (ce que fait ModBlocks au démarrage du mod)
// force la JVM à résoudre toutes les classes citées dans ses méthodes — y compris celles de la
// branche `isClientSide` qui ne s'exécute jamais sur un serveur. Résultat constaté en jeu
// (2026-08-30, serveur dédié du joueur) : NoClassDefFoundError sur
// net/minecraft/client/gui/screens/Screen dès constructMods, le mod ne charge pas du tout.
//
// **Le paquet porte la liste des maps** (2026-09-02, il n'avait aucun champ auparavant). C'est
// obligatoire : les maps sont découvertes parmi les structures disponibles (voir MapRegistry),
// or le gestionnaire de structures est construit à partir de l'accès au dossier de sauvegarde —
// il n'existe donc que côté serveur. Le client ne peut pas faire cette découverte lui-même.
public record OpenMapSelectionPayload(List<MapDefinition> maps) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenMapSelectionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "open_map_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMapSelectionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    MapDefinition.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenMapSelectionPayload::maps,
                    OpenMapSelectionPayload::new);

    @Override
    public Type<OpenMapSelectionPayload> type() {
        return TYPE;
    }
}
