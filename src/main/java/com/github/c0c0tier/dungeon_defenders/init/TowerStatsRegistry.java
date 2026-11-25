package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.network.TowerStatsPayload;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

// Les chiffres data-driven des tours (doc/data-driven/tours.md), même mécanique que les ennemis
// (init/EnemyRegistry) : un reload listener vanilla lit data/<namespace>/dungeon_defenders/
// tower/<id>.json au démarrage du serveur et à chaque /reload, puis les envoie aux clients
// (OnDatapackSyncEvent).
//
// Différence avec les ennemis : la LISTE des tours est fixée par le code (TowerDefinition), un
// JSON ne fait que régler leurs chiffres. D'où un repli systématique : JSON absent ou invalide
// -> valeurs par défaut du code pour cette tour, avec un message. Une tour a toujours des stats.
//
// **Deux copies distinctes**, serveur et client, dans cette même classe (elle est commune : les
// block entities de tours, chargés des deux côtés, y lisent leurs PV max). En solo les deux
// existent dans la même JVM ; les garder séparées évite qu'un bug de synchro passe inaperçu
// jusqu'au multijoueur. La copie client n'est remplie que par le paquet du serveur.
@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public final class TowerStatsRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "tower_stats");
    private static final FileToIdConverter FILES = FileToIdConverter.json(DungeonDefendersMod.MODID + "/tower");

    // Remplacées en bloc, jamais modifiées en place.
    private static volatile Map<TowerDefinition, TowerStats> serverStats = Map.of();
    private static volatile Map<TowerDefinition, TowerStats> clientStats = Map.of();

    private TowerStatsRegistry() {
    }

    /** Les chiffres en vigueur d'une tour ; ses valeurs par défaut si rien n'a (encore) été chargé/reçu. */
    public static TowerStats get(TowerDefinition tower, boolean clientSide) {
        return (clientSide ? clientStats : serverStats).getOrDefault(tower, tower.defaultStats());
    }

    /** Vrai si un JSON valide a été chargé pour cette tour côté serveur (pour les gametests). */
    public static boolean hasLoadedFile(TowerDefinition tower) {
        return serverStats.containsKey(tower);
    }

    public static TowerStatsPayload toPayload() {
        Map<String, Map<String, Double>> statsByTower = new HashMap<>();
        for (TowerDefinition tower : TowerDefinition.values()) {
            statsByTower.put(tower.identifier().toString(), get(tower, false).toNamedMap());
        }
        return new TowerStatsPayload(statsByTower);
    }

    /** Remplace la copie client. Appelé par le handler du paquet (DungeonDefendersModClient). */
    public static void replaceClient(TowerStatsPayload payload) {
        Map<TowerDefinition, TowerStats> received = new EnumMap<>(TowerDefinition.class);
        payload.statsByTower().forEach((id, named) -> {
            Identifier identifier = Identifier.tryParse(id);
            TowerDefinition tower = identifier == null ? null : TowerDefinition.byIdentifier(identifier);
            if (tower == null) {
                return; // tour d'une autre version du mod : ignorée
            }
            // Superposé aux défauts : une stat que le serveur n'aurait pas envoyée garde sa valeur.
            TowerStats sent = TowerStats.fromNamedMap(named);
            TowerStats.Builder merged = TowerStats.builder();
            for (TowerStat stat : TowerStat.values()) {
                if (tower.defaultStats().has(stat)) {
                    merged.with(stat, sent.getOr(stat, tower.defaultStats().get(stat)));
                }
            }
            received.put(tower, merged.build());
        });
        clientStats = Map.copyOf(received);
    }

    /** Remplace la copie serveur à partir des JSON bruts. Public pour les gametests. */
    public static void install(Map<Identifier, JsonElement> files) {
        Map<TowerDefinition, TowerStats> loaded = new EnumMap<>(TowerDefinition.class);
        files.forEach((id, json) -> {
            TowerDefinition tower = TowerDefinition.byIdentifier(id);
            if (tower == null) {
                LOGGER.warn("Stats de tour ignorées : {} ne correspond à aucune tour du mod (fichier {}).",
                        id, FILES.idToFile(id));
                return;
            }
            TowerStats.parse(json, tower.defaultStats())
                    .ifSuccess(stats -> loaded.put(tower, stats))
                    .ifError(error -> LOGGER.error(
                            "Stats de la tour {} invalides ({}) : {} — les valeurs par défaut du code sont utilisées.",
                            id, FILES.idToFile(id), error.message()));
        });

        for (TowerDefinition tower : TowerDefinition.values()) {
            if (!files.containsKey(tower.identifier())) {
                LOGGER.warn("Aucun fichier de stats pour la tour {} : les valeurs par défaut du code sont utilisées.",
                        tower.identifier());
            }
        }

        serverStats = Map.copyOf(loaded);
        LOGGER.info("Stats de tours chargées : {} sur {}", loaded.size(), TowerDefinition.values().length);
    }

    @SubscribeEvent
    static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(LISTENER_ID, new Listener());
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        TowerStatsPayload payload = toPayload();
        event.getRelevantPlayers().forEach(player -> player.connection.send(payload.toVanillaClientbound()));
    }

    private static final class Listener extends SimpleJsonResourceReloadListener<JsonElement> {
        Listener() {
            // Codec "JSON brut" : la validation se fait dans install(), tour par tour, parce que
            // les stats autorisées dépendent de la tour.
            super(ExtraCodecs.JSON, FILES);
        }

        @Override
        protected void apply(Map<Identifier, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            install(files);
        }
    }
}
