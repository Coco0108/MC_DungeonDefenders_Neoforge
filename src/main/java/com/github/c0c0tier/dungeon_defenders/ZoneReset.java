package com.github.c0c0tier.dungeon_defenders;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Retire les entités d'une pose précédente de la taverne ou d'une map, y compris celles qui ne sont pas encore chargées
 * au moment où la zone est nettoyée.
 *
 * <p><b>Le problème</b> (signalé le 2026-11-28) : {@code clearZone} cherche les entités de la zone juste après avoir écrit
 * ses blocs. Écrire un bloc charge les blocs du chunk, mais ses entités sauvegardées arrivent plus tard, de façon
 * asynchrone : le nettoyage ne les voit pas, et chaque chargement du monde ajoutait un exemplaire de plus du support
 * d'armure de la taverne.
 *
 * <p><b>La solution</b> : chaque pose ouvre une <i>génération</i> (un nombre tiré au hasard) pour sa zone. Toute entité
 * qui apparaît ensuite dans la zone (celles de la structure, un objet jeté, un monstre) la reçoit dans ses données
 * persistantes. Une entité rechargée depuis la sauvegarde dans la zone sans la génération en cours vient d'une pose
 * précédente : elle n'est pas ajoutée au monde (événement annulé), donc retirée. Celles de la pose en cours survivent
 * à un déchargement de chunk dans la même session (retour de map).
 */
@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public final class ZoneReset {
    static final String TAG = DungeonDefendersMod.MODID + ":generation_de_zone";

    private record Zone(ResourceKey<Level> dimension, AABB box, long generation) {
    }

    private static final Map<String, Zone> ZONES = new ConcurrentHashMap<>();

    private ZoneReset() {
    }

    /**
     * Ouvre une nouvelle génération pour la zone {@code key} ({@code "taverne"}, {@code "map"}), à appeler juste avant
     * de nettoyer puis de poser. Remplace la précédente génération de cette clé.
     */
    public static void begin(ServerLevel level, String key, AABB box) {
        ZONES.put(key, new Zone(level.dimension(), box, ThreadLocalRandom.current().nextLong()));
    }

    @SubscribeEvent
    static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!keep(event.getEntity(), level.dimension(), event.loadedFromDisk())) {
            event.setCanceled(true);
        }
    }

    /**
     * Marque l'entité si elle apparaît dans une zone, et dit si elle doit rester : {@code false} pour une entité
     * rechargée depuis la sauvegarde qui vient d'une pose précédente.
     */
    public static boolean keep(Entity entity, ResourceKey<Level> dimension, boolean loadedFromDisk) {
        if (entity instanceof Player) {
            return true;
        }
        for (Zone zone : ZONES.values()) {
            if (zone.dimension() != dimension || !zone.box().contains(entity.position())) {
                continue;
            }
            var data = entity.getPersistentData();
            if (!loadedFromDisk) {
                data.putLong(TAG, zone.generation());
                return true;
            }
            return data.contains(TAG) && data.getLong(TAG).orElse(0L) == zone.generation();
        }
        return true;
    }

    /** Ferme la zone {@code key} (gametests). */
    public static void end(String key) {
        ZONES.remove(key);
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        ZONES.clear();
    }
}
