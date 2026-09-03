package com.github.c0c0tier.dungeon_defenders.ability;

import com.github.c0c0tier.dungeon_defenders.init.AbilitySlot;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// La canalisation active de chaque joueur (au plus une à la fois) : quel emplacement, et pour
// Repair, quelle tour visée. Lu et fait avancer par ModEvents.onPlayerTick, écrit par
// ModNetworking (démarrage/arrêt côté client).
//
// État purement RUNTIME côté serveur — jamais synchronisé, jamais persisté : une canalisation
// ne survit pas à une reconnexion ni à un redémarrage, ce qui est très bien ainsi (le joueur
// retient la touche, il la relâchera de toute façon).
//
// Map statique plutôt qu'un data attachment NeoForge : cet état est éphémère par nature (il
// n'a jamais besoin d'être sauvegardé ni envoyé au client), un attachment serait donc la
// mauvaise API pour ça. **Limite acceptée** : une entrée orpheline peut rester si un joueur se
// déconnecte en pleine canalisation (pas de nettoyage sur déconnexion) — négligeable sur un
// serveur privé, une poignée d'octets par UUID qui a un jour tenu une touche enfoncée.
public final class PlayerAbilityChannels {

    private record Channel(AbilitySlot slot, @Nullable BlockPos target) {
    }

    private static final Map<UUID, Channel> ACTIVE = new HashMap<>();

    private PlayerAbilityChannels() {
    }

    public static void start(UUID playerId, AbilitySlot slot, @Nullable BlockPos target) {
        ACTIVE.put(playerId, new Channel(slot, target));
    }

    public static void stop(UUID playerId) {
        ACTIVE.remove(playerId);
    }

    public static @Nullable AbilitySlot activeSlot(UUID playerId) {
        Channel channel = ACTIVE.get(playerId);
        return channel != null ? channel.slot() : null;
    }

    public static @Nullable BlockPos activeTarget(UUID playerId) {
        Channel channel = ACTIVE.get(playerId);
        return channel != null ? channel.target() : null;
    }
}
