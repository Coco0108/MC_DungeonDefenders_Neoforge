package com.github.c0c0tier.dungeon_defenders.client;

import com.github.c0c0tier.dungeon_defenders.init.EnemyDefinition;
import com.github.c0c0tier.dungeon_defenders.network.EnemyDefinitionsPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// Copie CLIENT des ennemis data-driven, reçue du serveur par EnemyDefinitionsPayload (à la
// connexion et après chaque /reload). Volontairement distincte d'init/EnemyRegistry (côté
// serveur) : en solo, serveur intégré et client partagent la JVM, et un stockage commun
// masquerait tout bug de synchro jusqu'au premier test en multijoueur.
//
// Ne sert qu'à l'AFFICHAGE : écran de config du spawner, icônes au-dessus du spawner, popup de
// score, barre de vie des monstres. Client-only : ne jamais la nommer depuis du code chargé par
// le serveur dédié (voir tools/verifier-dist.py).
public final class ClientEnemyDefinitions {

    /** Un ennemi tel que le client le connaît, identifiants déjà résolus dans ses registres. */
    public record Entry(Identifier id, EntityType<?> entityType, Item icon, int order) {
    }

    private static volatile Map<Identifier, Entry> byId = Map.of();
    private static volatile List<Entry> sorted = List.of();
    private static volatile Set<EntityType<?>> entityTypes = Set.of();

    private ClientEnemyDefinitions() {
    }

    /** Remplace toute la copie client. Appelé par le handler du paquet (DungeonDefendersModClient). */
    public static void replace(List<EnemyDefinitionsPayload.Entry> received) {
        Map<Identifier, Entry> newById = new HashMap<>();
        Set<EntityType<?>> newEntityTypes = new HashSet<>();
        for (EnemyDefinitionsPayload.Entry entry : received) {
            // Le serveur n'envoie que des identifiants qu'il a lui-même validés ; un client dont
            // les registres différeraient (mod manquant) retombe sur un rendu neutre plutôt que
            // de planter.
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.containsKey(entry.entityType())
                    ? BuiltInRegistries.ENTITY_TYPE.getValue(entry.entityType())
                    : null;
            Item icon = BuiltInRegistries.ITEM.containsKey(entry.icon())
                    ? BuiltInRegistries.ITEM.getValue(entry.icon())
                    : Items.BARRIER;
            if (entityType != null) {
                newEntityTypes.add(entityType);
            }
            newById.put(entry.id(), new Entry(entry.id(), entityType, icon, entry.order()));
        }

        byId = Map.copyOf(newById);
        sorted = newById.values().stream()
                .sorted(Comparator.comparingInt(Entry::order).thenComparing(entry -> entry.id().toString()))
                .toList();
        entityTypes = Set.copyOf(newEntityTypes);
    }

    /** Tous les ennemis connus, dans l'ordre "order" puis identifiant (bouton « cycler »). */
    public static List<Entry> sorted() {
        return sorted;
    }

    public static Optional<Entry> get(Identifier id) {
        return Optional.ofNullable(byId.get(id));
    }

    public static boolean isKnown(Identifier id) {
        return byId.containsKey(id);
    }

    /** Icône de l'ennemi, ou une barrière s'il est inconnu (pack retiré, faute de frappe...). */
    public static Item iconOrBarrier(Identifier id) {
        return get(id).map(Entry::icon).orElse(Items.BARRIER);
    }

    /**
     * Nom affiché : la traduction {@code <namespace>.enemy.<nom>} si elle existe, sinon
     * l'identifiant brut — c'est aussi ce que voit le créateur pour un ennemi inconnu.
     */
    public static Component displayName(Identifier id) {
        return Component.translatableWithFallback(EnemyDefinition.translationKey(id), id.toString());
    }

    /** Vrai si ce type de mob est celui d'un ennemi connu (barre de vie des monstres). */
    public static boolean isEnemyEntityType(EntityType<?> entityType) {
        return entityTypes.contains(entityType);
    }
}
