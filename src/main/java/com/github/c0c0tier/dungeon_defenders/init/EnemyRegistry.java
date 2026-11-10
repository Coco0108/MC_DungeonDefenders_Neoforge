package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.network.EnemyDefinitionsPayload;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Les ennemis data-driven, côté SERVEUR (doc/data-driven/ennemis.md, §2).
//
// **Chargement** : un reload listener vanilla (SimpleJsonResourceReloadListener) lit tous les
// data/<namespace>/dungeon_defenders/enemy/*.json de tous les packs (jar du mod, jars tiers,
// datapacks). Ajouté via AddServerReloadListenersEvent, qui se déclenche au démarrage du
// serveur PUIS à chaque /reload : modifier un JSON et taper /reload suffit, sans redémarrer.
// Choisi plutôt qu'un registre datapack NeoForge, qui aurait donné la synchro client gratuite
// mais n'est chargé qu'une fois au lancement du monde (registres dynamiques gelés ensuite).
//
// **JSON invalide** : le chargeur vanilla logge "Couldn't parse data file ..." et ignore ce
// fichier ; les autres se chargent normalement. Jamais de crash.
//
// **Synchro client** : EnemyDefinitionsPayload, envoyé depuis OnDatapackSyncEvent (connexion
// d'un joueur, et après chaque /reload à tout le monde). Côté client, les données vivent dans
// une classe DISTINCTE (client/ClientEnemyDefinitions) : en solo, serveur intégré et client
// partagent la JVM, un seul stockage commun masquerait tout bug de synchro jusqu'au multijoueur.
@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public final class EnemyRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** XP/score d'un monstre qui n'a aucune définition — même valeur qu'avant la migration. */
    public static final int DEFAULT_XP_VALUE = 5;

    private static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "enemies");
    private static final FileToIdConverter FILES = FileToIdConverter.json(DungeonDefendersMod.MODID + "/enemy");

    private static final Comparator<Map.Entry<Identifier, EnemyDefinition>> ORDER =
            Comparator.<Map.Entry<Identifier, EnemyDefinition>>comparingInt(entry -> entry.getValue().order())
                    .thenComparing(entry -> entry.getKey().toString());

    // Remplacés en bloc à chaque reload (jamais modifiés en place) : un lecteur voit toujours
    // un état cohérent, l'ancien ou le nouveau.
    private static volatile Map<Identifier, EnemyDefinition> byId = Map.of();
    private static volatile Map<EntityType<?>, Identifier> idByEntityType = Map.of();
    private static volatile List<Identifier> sortedIds = List.of();

    // Incrémenté à chaque reload : permet aux spawners de ne logger un ennemi inconnu qu'une fois
    // par chargement de données, tout en re-signalant le problème après un /reload s'il persiste.
    private static volatile int generation;

    private EnemyRegistry() {
    }

    /** @return la définition de cet ennemi, ou null s'il n'existe pas (pack retiré, JSON invalide...). */
    public static @Nullable EnemyDefinition byId(Identifier id) {
        return byId.get(id);
    }

    /** @return l'identifiant de l'ennemi dont c'est le type de mob, s'il y en a un. */
    public static Optional<Identifier> idFor(EntityType<?> entityType) {
        return Optional.ofNullable(idByEntityType.get(entityType));
    }

    public static Optional<EnemyDefinition> definitionFor(EntityType<?> entityType) {
        return idFor(entityType).map(byId::get);
    }

    /** XP/score à chaque mort de ce type de mob ; {@link #DEFAULT_XP_VALUE} s'il n'a pas de définition. */
    public static int xpValueFor(EntityType<?> entityType) {
        return definitionFor(entityType).map(EnemyDefinition::xpValue).orElse(DEFAULT_XP_VALUE);
    }

    public static int generation() {
        return generation;
    }

    public static EnemyDefinitionsPayload toPayload() {
        return new EnemyDefinitionsPayload(byId.entrySet().stream()
                .map(entry -> new EnemyDefinitionsPayload.Entry(
                        entry.getKey(),
                        BuiltInRegistries.ENTITY_TYPE.getKey(entry.getValue().entityType()),
                        BuiltInRegistries.ITEM.getKey(entry.getValue().icon()),
                        entry.getValue().order()))
                .toList());
    }

    /** Remplace toutes les définitions. Public pour les gametests, sinon appelé par le reload listener. */
    public static void install(Map<Identifier, EnemyDefinition> loaded) {
        List<Map.Entry<Identifier, EnemyDefinition>> sorted = loaded.entrySet().stream().sorted(ORDER).toList();

        Map<Identifier, EnemyDefinition> newById = new LinkedHashMap<>();
        Map<EntityType<?>, Identifier> newByEntityType = new HashMap<>();
        for (Map.Entry<Identifier, EnemyDefinition> entry : sorted) {
            newById.put(entry.getKey(), entry.getValue());
            Identifier previous = newByEntityType.putIfAbsent(entry.getValue().entityType(), entry.getKey());
            if (previous != null) {
                // Limite assumée de l'étape 1 (doc/data-driven/ennemis.md, §3) : une seule
                // définition par type de mob pour l'IA et l'XP. Le premier par "order" gagne.
                LOGGER.warn("Ennemis {} et {} utilisent le même entity_type ({}) : {} est retenu pour l'IA et l'XP "
                                + "de ce type de mob (le premier par \"order\").",
                        previous, entry.getKey(), BuiltInRegistries.ENTITY_TYPE.getKey(entry.getValue().entityType()), previous);
            }
        }

        byId = Map.copyOf(newById);
        idByEntityType = Map.copyOf(newByEntityType);
        // Liste à part : Map.copyOf ne garantit aucun ordre d'itération.
        sortedIds = List.copyOf(newById.keySet());
        generation++;

        if (newById.isEmpty()) {
            LOGGER.error("Aucun ennemi chargé (data/<namespace>/{}/*.json) : les spawners ne feront rien apparaître. "
                    + "Vérifier les erreurs \"Couldn't parse data file\" plus haut dans le log.", FILES.prefix());
        } else {
            LOGGER.info("{} ennemi(s) chargé(s) : {}", newById.size(), newById.keySet());
        }
    }

    /** Tous les ennemis, dans l'ordre "order" puis identifiant. */
    public static List<Identifier> sortedIds() {
        return sortedIds;
    }

    @SubscribeEvent
    static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(LISTENER_ID, new Listener());
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        EnemyDefinitionsPayload payload = toPayload();
        event.getRelevantPlayers().forEach(player -> player.connection.send(payload.toVanillaClientbound()));
    }

    private static final class Listener extends SimpleJsonResourceReloadListener<EnemyDefinition> {
        Listener() {
            super(EnemyDefinition.CODEC, FILES);
        }

        @Override
        protected void apply(Map<Identifier, EnemyDefinition> loaded, ResourceManager manager, ProfilerFiller profiler) {
            install(loaded);

            // Un /reload peut ajouter ou retirer un ennemi utilisé par un spawner déjà posé : le
            // total de vague (qui exclut les ennemis inconnus) doit suivre, sinon une vague en
            // cours pourrait attendre des monstres qui ne viendront plus. Rien à faire au tout
            // premier chargement (aucun monde encore chargé, la liste des niveaux est vide).
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
