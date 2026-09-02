package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.block.entity.MapConfigBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Découverte des maps jouables, à l'exécution, sans liste écrite en dur.
//
// **Comment l'extensibilité tombe gratuitement** : le gestionnaire de structures de Minecraft
// cherche dans trois familles de sources — le dossier `generated/` de la sauvegarde (là où un
// bloc de structure vanilla écrit), puis toutes les ressources chargées (datapacks ET jars de
// mods), tous namespaces confondus. Vérifié dans son constructeur et dans
// ResourceManagerTemplateSource#list, qui passe par listMatchingResources.
//
// En filtrant seulement sur le CHEMIN (`map/*`) et jamais sur le namespace, on découvre donc :
// - les maps en cours de création dans le monde courant (aucun fichier à transmettre),
// - la campagne livrée dans le jar du mod,
// - n'importe quel pack tiers publié sous forme de jar ou de datapack, sans une ligne de code
//   ni la moindre inscription de sa part.
//
// Le namespace fait office de pack (voir MapDefinition#packId).
public final class MapRegistry {

    // Campagne d'abord, puis les autres packs par ordre alphabétique de namespace ; à
    // l'intérieur d'un pack, l'ordre choisi par le créateur, puis l'identifiant pour départager.
    // Un ordre totalement déterministe est indispensable : sans ça la liste changerait de place
    // d'un lancement à l'autre, au gré de l'ordre de parcours des fichiers.
    private static final Comparator<MapDefinition> ORDER = Comparator
            .comparing((MapDefinition map) -> map.isCampaign() ? 0 : 1)
            .thenComparing(MapDefinition::packId)
            .thenComparingInt(MapDefinition::order)
            .thenComparing(MapDefinition::mapId);

    private MapRegistry() {
    }

    /**
     * Toutes les maps disponibles, triées. Recalculée à chaque appel plutôt que mise en cache :
     * l'opération n'a lieu qu'à l'ouverture de l'écran de choix, et une map qu'on vient de
     * sauvegarder avec un bloc de structure doit apparaître immédiatement, sans relancer le
     * serveur.
     */
    public static List<MapDefinition> discover(ServerLevel level) {
        StructureTemplateManager manager = level.getStructureManager();
        return manager.listTemplates()
                .filter(MapDefinition::isMapStructure)
                .map(id -> read(manager, id))
                .flatMap(Optional::stream)
                .sorted(ORDER)
                .toList();
    }

    /** Retrouve une map par l'identifiant de sa structure, ou vide si elle n'existe plus. */
    public static Optional<MapDefinition> find(ServerLevel level, Identifier structureId) {
        if (!MapDefinition.isMapStructure(structureId)) {
            return Optional.empty();
        }
        return read(level.getStructureManager(), structureId);
    }

    // Lit les réglages DANS le template, sans poser la structure : filterBlocks renvoie les
    // blocs demandés avec leur NBT. Une map sans bloc de configuration reste valide, avec les
    // valeurs par défaut (décidé avec le joueur) — d'où le null passé à toDefinition.
    private static Optional<MapDefinition> read(StructureTemplateManager manager, Identifier structureId) {
        Optional<StructureTemplate> template = manager.get(structureId);
        if (template.isEmpty()) {
            return Optional.empty();
        }

        List<StructureTemplate.StructureBlockInfo> configBlocks = template.get()
                .filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), ModBlocks.MAP_CONFIG.get());

        return Optional.of(MapConfigBlockEntity.toDefinition(
                structureId,
                configBlocks.isEmpty() ? null : configBlocks.getFirst().nbt()));
    }
}
