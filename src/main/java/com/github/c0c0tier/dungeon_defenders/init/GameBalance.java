package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.network.GameBalancePayload;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

// Les réglages globaux d'équilibrage (BalanceValue), troisième volet du data-driven après les
// ennemis et les tours (doc/data-driven/equilibrage.md). Même mécanique : un reload listener
// vanilla lit data/<namespace>/dungeon_defenders/balance/*.json au démarrage et à chaque /reload,
// puis envoie le tout aux clients (OnDatapackSyncEvent).
//
// Un fichier = un objet plat `"clé": nombre`. Clé inconnue, valeur non numérique, non entière là
// où il le faut ou sous son minimum : le fichier ENTIER est refusé avec un message, et ses
// réglages gardent leur valeur par défaut — jamais un fichier à moitié appliqué. Une clé absente
// garde simplement sa valeur par défaut.
//
// Deux copies distinctes, serveur et client, comme TowerStatsRegistry (même raison : le solo).
@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public final class GameBalance {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "balance");
    private static final FileToIdConverter FILES = FileToIdConverter.json(DungeonDefendersMod.MODID + "/balance");

    private static volatile Map<BalanceValue, Double> serverValues = Map.of();
    private static volatile Map<BalanceValue, Double> clientValues = Map.of();

    private GameBalance() {
    }

    /** La valeur en vigueur d'un réglage ; sa valeur par défaut si rien n'a (encore) été chargé/reçu. */
    public static double get(BalanceValue value, boolean clientSide) {
        return (clientSide ? clientValues : serverValues).getOrDefault(value, value.defaultValue());
    }

    public static int getInt(BalanceValue value, boolean clientSide) {
        return (int) Math.round(get(value, clientSide));
    }

    public static GameBalancePayload toPayload() {
        Map<String, Double> values = new HashMap<>();
        for (BalanceValue value : BalanceValue.values()) {
            values.put(value.networkName(), get(value, false));
        }
        return new GameBalancePayload(values);
    }

    /** Remplace la copie client. Appelé par le handler du paquet (DungeonDefendersModClient). */
    public static void replaceClient(GameBalancePayload payload) {
        Map<BalanceValue, Double> received = new EnumMap<>(BalanceValue.class);
        for (BalanceValue value : BalanceValue.values()) {
            Double sent = payload.values().get(value.networkName());
            if (sent != null) {
                received.put(value, sent);
            }
        }
        clientValues = Map.copyOf(received);
    }

    /** Remplace la copie serveur à partir des JSON bruts. Public pour les gametests. */
    public static void install(Map<Identifier, JsonElement> files) {
        Map<BalanceValue, Double> loaded = new EnumMap<>(BalanceValue.class);
        files.forEach((id, json) -> {
            if (!DungeonDefendersMod.MODID.equals(id.getNamespace())) {
                // Les réglages sont ceux du mod : un pack les modifie en remplaçant le fichier du
                // mod (même namespace), pas en en ajoutant un sous le sien.
                LOGGER.warn("Fichier d'équilibrage ignoré : {} n'est pas sous le namespace {} ({}).",
                        id, DungeonDefendersMod.MODID, FILES.idToFile(id));
                return;
            }
            parseFile(id.getPath(), json)
                    .ifSuccess(loaded::putAll)
                    .ifError(error -> LOGGER.error(
                            "Fichier d'équilibrage {} invalide : {} — les valeurs par défaut du code sont utilisées pour ce fichier.",
                            FILES.idToFile(id), error.message()));
        });

        serverValues = Map.copyOf(loaded);
        LOGGER.info("Réglages d'équilibrage chargés : {} sur {} (les autres gardent leur valeur par défaut)",
                loaded.size(), BalanceValue.values().length);
    }

    /** Valide un fichier balance/<file>.json et renvoie les réglages qu'il fixe. Public pour les gametests. */
    public static DataResult<Map<BalanceValue, Double>> parseFile(String file, JsonElement json) {
        Map<String, BalanceValue> known = Arrays.stream(BalanceValue.values())
                .filter(value -> value.file().equals(file))
                .collect(Collectors.toMap(BalanceValue::key, value -> value));
        if (known.isEmpty()) {
            return DataResult.error(() -> "aucun réglage ne vit dans un fichier nommé \"" + file + "\"");
        }
        if (!(json instanceof JsonObject object)) {
            return DataResult.error(() -> "un objet JSON est attendu");
        }

        Map<BalanceValue, Double> parsed = new EnumMap<>(BalanceValue.class);
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            BalanceValue value = known.get(key);
            if (value == null) {
                return DataResult.error(() -> "clé inconnue \"" + key + "\" (clés de ce fichier : "
                        + String.join(", ", known.keySet()) + ")");
            }
            JsonElement element = entry.getValue();
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
                return DataResult.error(() -> "\"" + key + "\" doit être un nombre");
            }
            double number = element.getAsDouble();
            if (Double.isNaN(number) || Double.isInfinite(number)) {
                return DataResult.error(() -> "\"" + key + "\" doit être un nombre fini");
            }
            if (value.isInteger() && number != Math.rint(number)) {
                return DataResult.error(() -> "\"" + key + "\" doit être un nombre entier (reçu " + number + ")");
            }
            if (number < value.min()) {
                return DataResult.error(() -> "\"" + key + "\" doit valoir au moins " + value.min() + " (reçu " + number + ")");
            }
            parsed.put(value, number);
        }
        return DataResult.success(parsed);
    }

    @SubscribeEvent
    static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(LISTENER_ID, new Listener());
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        GameBalancePayload payload = toPayload();
        event.getRelevantPlayers().forEach(player -> player.connection.send(payload.toVanillaClientbound()));
    }

    private static final class Listener extends SimpleJsonResourceReloadListener<JsonElement> {
        Listener() {
            super(ExtraCodecs.JSON, FILES);
        }

        @Override
        protected void apply(Map<Identifier, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            install(files);

            // Le multiplicateur de difficulté entre dans le total d'ennemis de la vague : un
            // /reload doit le recalculer, sinon le compteur resterait sur l'ancienne valeur.
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.execute(() -> server.getAllLevels().forEach(level -> {
                    if (!level.getData(ModAttachments.ACTIVE_SPAWNERS).isEmpty()) {
                        PhaseTransitions.recomputeWaveEnemiesTotal(level);
                    }
                }));
            }
        }
    }
}
