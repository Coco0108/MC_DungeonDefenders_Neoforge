package com.github.c0c0tier.dungeon_defenders;

import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.ModBlocks;
import com.github.c0c0tier.dungeon_defenders.init.PhaseTransitions;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

// L'Overworld est un monde vide (voir data/minecraft/dimension/overworld.json, qui remplace son
// générateur par le préréglage vanilla "The Void") : sans point de spawn fixe ni sol, un nouveau
// joueur tomberait indéfiniment dans le vide dès sa première connexion. Cette classe fixe le
// point de spawn du monde et (re)pose la taverne à chaque chargement du monde.
//
// Pourquoi reposer le contenu à CHAQUE chargement, plutôt qu'une seule fois : la taverne suit le
// même principe que les maps (voir doc/05-etat-et-problemes-connus.md, "Système de
// maps/structures") — sa structure est reposée à cet emplacement fixe à chaque fois qu'on y
// "entre". Sans ça, une mise à jour du mod qui change la structure de la taverne resterait
// invisible sur une sauvegarde existante (le joueur garderait l'ancienne version, posée lors de
// sa toute première connexion).
//
// Depuis le 2026-08-31, la taverne est une vraie structure `.nbt`
// (data/dungeon_defenders/structure/tavern.nbt) et non plus une plateforme en dur. La
// plateforme reste comme **repli** si le fichier est absent : le mod doit rester jouable même
// sans structure livrée, sinon un monde vide sans sol rend le jeu injouable.
//
// Le nettoyage de zone remet à zéro les blocs ET les entités (voir clearZone) : la structure
// peut donc contenir des entités (décor, et à terme le mannequin d'entraînement des tours) sans
// qu'elles se dupliquent à chaque redémarrage.
@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public class TavernSpawn {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Position d'arrivée par défaut dans la taverne, et centre horizontal de sa structure.
     * Utilisée telle quelle si la structure ne contient pas de marqueur {@code player_spawn}
     * (voir {@link #arrivalPos}).
     */
    public static final BlockPos SPAWN_POS = new BlockPos(0, 65, 0);

    /** Structure posée à {@link #SPAWN_POS} : data/dungeon_defenders/structure/tavern.nbt. */
    public static final Identifier TAVERN_STRUCTURE =
            Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "tavern");

    private static final int PLATFORM_RADIUS = 4;
    // Marge ajoutée autour de l'emprise de la structure avant de la reposer : une version
    // précédente plus grande (ou l'ancienne plateforme de repli) doit disparaître entièrement,
    // sinon ses restes flottent autour de la nouvelle taverne.
    private static final int CLEAR_MARGIN = 4;

    private TavernSpawn() {
    }

    @SubscribeEvent
    static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || serverLevel.dimension() != Level.OVERWORLD) {
            return;
        }

        BlockPos arrival = placeTavern(serverLevel);

        // Tout le monde apparaît ici : le monde démarre donc à la Taverne, jamais au milieu
        // d'une partie qu'une sauvegarde aurait figée en Construction ou en Combat.
        PhaseTransitions.enterTavern(serverLevel);

        // Remplace le point de spawn "trouvé" par le jeu (qui chercherait un sol solide —
        // inexistant dans un monde vide) par la position d'arrivée réelle de la taverne : c'est
        // aussi là que réapparaît un joueur mort en pleine map.
        serverLevel.setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD, arrival, 0.0F, 0.0F));
    }

    /**
     * Où poser les joueurs qui entrent dans la taverne : le marqueur {@code player_spawn} de sa
     * structure s'il y en a un, sinon {@link #SPAWN_POS}.
     *
     * <p>Recalculé à chaque appel plutôt que mémorisé dans un champ statique : le gestionnaire
     * de structures garde déjà le template en cache, et un état statique de plus survivrait mal
     * à un rechargement de monde. Appelé rarement (retour à la taverne, chargement du monde).
     *
     * <p>Contrairement au marqueur d'une map ({@code MapInstance#findAndConsumeSpawnMarker}), il
     * n'est **pas consommé** : on revient à la taverne en permanence, il doit rester en place.
     * C'est sans conséquence visuelle depuis que le bloc est invisible et traversable (voir
     * {@code PlayerSpawnBlock}).
     */
    public static BlockPos arrivalPos(ServerLevel level) {
        Optional<StructureTemplate> template = level.getStructureManager().get(TAVERN_STRUCTURE);
        if (template.isEmpty()) {
            return SPAWN_POS;
        }
        BlockPos marker = findSpawnMarker(template.get(), originOf(template.get()));
        return marker != null ? marker : SPAWN_POS;
    }

    /**
     * Nettoie l'emplacement puis y pose la structure de la taverne. Repli sur l'ancienne
     * plateforme en dur si le fichier de structure n'existe pas.
     *
     * @return la position d'arrivée des joueurs.
     */
    private static BlockPos placeTavern(ServerLevel level) {
        Optional<StructureTemplate> loaded = level.getStructureManager().get(TAVERN_STRUCTURE);
        if (loaded.isEmpty()) {
            LOGGER.warn(
                    "Structure de taverne introuvable ({}) : repli sur la plateforme provisoire.", TAVERN_STRUCTURE);
            Zone zone = zoneOf(level);
            clearZone(level, zone.from(), zone.size());
            buildPlaceholderPlatform(level);
            return SPAWN_POS;
        }

        StructureTemplate template = loaded.get();
        BlockPos origin = originOf(template);

        Zone zone = zoneOf(level);
        clearZone(level, zone.from(), zone.size());

        // UPDATE_CLIENTS (et pas UPDATE_ALL) : on ne veut pas déclencher une cascade de mises à
        // jour de voisinage sur chaque bloc posé, seulement que les clients voient le résultat —
        // même choix que le bloc de structure vanilla.
        template.placeInWorld(level, origin, origin, placeSettings(), RandomSource.create(), Block.UPDATE_CLIENTS);

        BlockPos marker = findSpawnMarker(template, origin);
        return marker != null ? marker : SPAWN_POS;
    }

    /** L'emprise occupée par la taverne dans le monde, marge de nettoyage comprise. */
    private record Zone(BlockPos from, Vec3i size) {
    }

    // Une seule définition de "la zone de la taverne", partagée par le nettoyage avant pose et
    // par la suppression des tours d'essai : les deux doivent couvrir exactement le même volume,
    // sinon une tour posée dans un coin échapperait au ménage.
    private static Zone zoneOf(ServerLevel level) {
        Optional<StructureTemplate> template = level.getStructureManager().get(TAVERN_STRUCTURE);
        if (template.isEmpty()) {
            int radius = PLATFORM_RADIUS + CLEAR_MARGIN;
            return new Zone(
                    SPAWN_POS.offset(-radius, -CLEAR_MARGIN, -radius),
                    new Vec3i(2 * radius + 1, 2 * CLEAR_MARGIN + 1, 2 * radius + 1));
        }
        BlockPos origin = originOf(template.get());
        Vec3i size = template.get().getSize();
        return new Zone(
                origin.offset(-CLEAR_MARGIN, -CLEAR_MARGIN, -CLEAR_MARGIN),
                size.offset(2 * CLEAR_MARGIN, 2 * CLEAR_MARGIN, 2 * CLEAR_MARGIN));
    }

    /**
     * Retire les tours posées dans la taverne. Appelée au lancement d'une partie
     * ({@code MapInstance#startGame}) : la Taverne sert de zone d'essai libre, mais ces tours
     * n'ont rien à faire là pendant qu'on joue, et sans ça elles resteraient plantées jusqu'au
     * prochain chargement du monde.
     *
     * <p>Balaye la zone bloc par bloc faute de registre de tours (contrairement aux spawners et
     * aux coffres de mana, qui ont le leur) : c'est un coût ponctuel, payé une fois au clic sur
     * "Jouer", pas à chaque tick.
     */
    public static void clearTestTowers(ServerLevel level) {
        Zone zone = zoneOf(level);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < zone.size().getX(); x++) {
            for (int y = 0; y < zone.size().getY(); y++) {
                for (int z = 0; z < zone.size().getZ(); z++) {
                    pos.set(zone.from().getX() + x, zone.from().getY() + y, zone.from().getZ() + z);
                    if (level.getBlockEntity(pos) instanceof AbstractTowerBlockEntity) {
                        // false : pas de drop d'item, même convention que la suppression via la
                        // touche dédiée et que la destruction au combat.
                        level.destroyBlock(pos, false);
                    }
                }
            }
        }
    }

    // La structure est centrée horizontalement sur SPAWN_POS, et sa couche la plus basse est
    // posée juste SOUS SPAWN_POS (le sol de la taverne à y-1, les joueurs debout dessus). Le
    // point d'arrivée exact reste réglable sans recaler toute la structure, grâce au marqueur
    // player_spawn (voir arrivalPos).
    private static BlockPos originOf(StructureTemplate template) {
        Vec3i size = template.getSize();
        return new BlockPos(
                SPAWN_POS.getX() - size.getX() / 2,
                SPAWN_POS.getY() - 1,
                SPAWN_POS.getZ() - size.getZ() / 2);
    }

    // Réglages par défaut : les entités de la structure (cadres, supports à armure, futur
    // mannequin d'entraînement...) SONT posées. C'est `clearZone` qui empêche leur duplication à
    // chaque rechargement, en supprimant les entités de la zone juste avant — voir son
    // commentaire.
    private static StructurePlaceSettings placeSettings() {
        return new StructurePlaceSettings();
    }

    // Cherche le marqueur dans le TEMPLATE plutôt que dans le monde : filterBlocks renvoie déjà
    // des positions absolues (une fois l'origine appliquée), donc pas besoin de balayer un
    // volume bloc par bloc comme le fait MapInstance. Le premier trouvé gagne, comme pour les
    // maps ; un seul marqueur est attendu.
    private static @Nullable BlockPos findSpawnMarker(StructureTemplate template, BlockPos origin) {
        List<StructureTemplate.StructureBlockInfo> markers =
                template.filterBlocks(origin, placeSettings(), ModBlocks.PLAYER_SPAWN.get());
        return markers.isEmpty() ? null : markers.getFirst().pos();
    }

    /**
     * Remet la zone à zéro : d'abord les blocs (tout en air), puis les entités.
     *
     * <p><b>Cet ordre est délibéré.</b> Écrire un bloc force le chunk correspondant à être
     * chargé ; balayer les blocs en premier garantit donc que tous les chunks de la zone sont
     * chargés avant qu'on interroge leurs entités. À l'inverse, chercher les entités d'abord —
     * pendant {@code LevelEvent.Load}, avant que quoi que ce soit ne soit chargé — n'en
     * trouverait aucune, et la structure en poserait un exemplaire de plus à chaque démarrage.
     *
     * <p>Les joueurs sont évidemment épargnés. Tout le reste part : entités décoratives de la
     * structure (reposées juste après par {@code placeInWorld}), objets au sol qu'un joueur
     * aurait laissés, et le futur mannequin d'entraînement — c'est exactement ce qu'on veut,
     * chaque chargement du monde remet la taverne dans l'état livré avec le mod.
     */
    private static void clearZone(ServerLevel level, BlockPos from, Vec3i size) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    pos.set(from.getX() + x, from.getY() + y, from.getZ() + z);
                    level.setBlockAndUpdate(pos, air);
                }
            }
        }

        AABB zone = new AABB(
                from.getX(), from.getY(), from.getZ(),
                from.getX() + size.getX(), from.getY() + size.getY(), from.getZ() + size.getZ());
        for (Entity entity : level.getEntitiesOfClass(Entity.class, zone, e -> !(e instanceof Player))) {
            entity.discard();
        }
    }

    /**
     * Repli quand aucune structure de taverne n'est livrée : une plateforme carrée, un bloc sous
     * {@link #SPAWN_POS}, juste pour que le monde vide reste praticable.
     */
    private static void buildPlaceholderPlatform(ServerLevel level) {
        BlockState floor = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                pos.set(SPAWN_POS.getX() + x, SPAWN_POS.getY() - 1, SPAWN_POS.getZ() + z);
                level.setBlockAndUpdate(pos, floor);
            }
        }
    }
}
