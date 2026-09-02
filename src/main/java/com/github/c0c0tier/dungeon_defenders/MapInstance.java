package com.github.c0c0tier.dungeon_defenders;

import com.github.c0c0tier.dungeon_defenders.init.MapDefinition;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.ModBlocks;
import com.github.c0c0tier.dungeon_defenders.init.ModChunkTickets;
import com.github.c0c0tier.dungeon_defenders.init.PhaseTransitions;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// "La map actuellement active" : un seul emplacement partagé, réutilisé pour n'importe quelle
// map choisie dans MapSelectionScreen — une seule partie active à la fois sur tout le serveur
// (voir doc/05-etat-et-problemes-connus.md, "Système de maps/structures"), donc pas besoin
// d'une coordonnée par map.
//
// Depuis le 2026-09-02, la map choisie est réellement chargée depuis sa structure `.nbt`, par
// le même mécanisme que la taverne (voir TavernSpawn) : nettoyage de la zone dimensionné sur la
// structure, pose, puis recherche du marqueur de spawn. L'arène placeholder ne sert plus que de
// repli quand aucune structure n'est trouvée.
public final class MapInstance {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Coordonnée partagée par toutes les maps, loin de la taverne (voir TavernSpawn.SPAWN_POS). */
    public static final BlockPos MAP_POS = new BlockPos(10000, 65, 0);
    /** Nom (sans le "/") de la commande de retour à la taverne — voir ModCommands et PhaseTransitions. */
    public static final String RETURN_COMMAND = "dd_leave";

    private static final int PLATFORM_RADIUS = 8;
    // Marge de nettoyage autour de l'emprise réelle : rattrape une map précédente plus grande,
    // et les tours que les joueurs auraient posées en bordure.
    private static final int CLEAR_MARGIN = 10;

    private MapInstance() {
    }

    /**
     * Nettoie l'emplacement, y pose la map demandée, et téléporte tous les joueurs — au
     * {@code PLAYER_SPAWN} de la map si son créateur en a posé un, sinon à {@link #MAP_POS}.
     *
     * @param map la map choisie, ou {@code null} pour l'arène placeholder (aucune map disponible).
     */
    public static void startGame(ServerLevel level, @Nullable MapDefinition map) {
        // La Taverne sert de zone d'essai libre pour les tours : celles qui y traînent n'ont
        // rien à faire là pendant la partie, et sans ce ménage elles resteraient plantées
        // jusqu'au prochain chargement du monde.
        TavernSpawn.clearTestTowers(level);

        // Vague 1, phase Construction, compteurs et score à zéro, plus le nombre de vagues
        // propre à cette map. Explicite depuis que la phase par défaut est TAVERN et non plus
        // BUILD — sans ça, on arriverait sur la map en phase Taverne, tours gratuites et aucune
        // vague.
        PhaseTransitions.startNewGame(level, map != null ? map.waveCount() : MapDefinition.DEFAULT_WAVE_COUNT);

        // Mémorisée pour le bouton "Rejouer" de l'écran de fin de partie, qui doit relancer LA
        // MÊME map alors que l'écran de choix est fermé depuis longtemps côté client.
        level.setData(ModAttachments.CURRENT_MAP, map != null ? map.structureId().toString() : "");
        level.syncData(ModAttachments.CURRENT_MAP);

        BlockPos spawnPos = placeMap(level, map);
        teleportAllPlayers(level, spawnPos);

        // Sans ça, Minecraft ne fait tourner que les chunks proches du groupe : un spawner à
        // l'autre bout de la map cesserait simplement de fonctionner, sans erreur ni message.
        Vec3i size = sizeOf(level, map);
        ModChunkTickets.forceZone(level, zoneFrom(size), zoneSize(size));
    }

    /** Nettoie l'emplacement (plus besoin d'y rester) et ramène tout le monde à la taverne. */
    public static void returnToTavern(ServerLevel level) {
        // Relâché avant le nettoyage : plus personne ne joue ici, rien ne doit rester chargé.
        ModChunkTickets.releaseAll(level);
        clearZone(level, zoneFrom(null), zoneSize(null));
        level.setData(ModAttachments.CURRENT_MAP, "");
        level.syncData(ModAttachments.CURRENT_MAP);
        PhaseTransitions.enterTavern(level);
        // Pas TavernSpawn.SPAWN_POS en dur : si la structure de la taverne pose son propre
        // marqueur player_spawn, c'est lui qui fait foi (voir TavernSpawn#arrivalPos).
        teleportAllPlayers(level, TavernSpawn.arrivalPos(level));
    }

    /**
     * Pose la structure de la map et renvoie la position d'arrivée des joueurs. Repli sur
     * l'arène placeholder si aucune map n'est choisie ou si sa structure a disparu entre le
     * moment où le client l'a listée et le clic sur "Jouer".
     */
    private static BlockPos placeMap(ServerLevel level, @Nullable MapDefinition map) {
        Optional<StructureTemplate> template = map == null
                ? Optional.empty()
                : level.getStructureManager().get(map.structureId());

        if (template.isEmpty()) {
            if (map != null) {
                LOGGER.warn("Structure de map introuvable ({}) : repli sur l'arène provisoire.", map.structureId());
            }
            clearZone(level, zoneFrom(null), zoneSize(null));
            buildPlaceholderArena(level);
            return MAP_POS;
        }

        Vec3i size = template.get().getSize();
        BlockPos origin = originOf(size);

        clearZone(level, zoneFrom(size), zoneSize(size));

        // UPDATE_CLIENTS et pas UPDATE_ALL : pas de cascade de mises à jour de voisinage sur
        // chaque bloc posé, seulement l'affichage — même choix que la taverne et que le bloc de
        // structure vanilla.
        template.get().placeInWorld(level, origin, origin, new StructurePlaceSettings(),
                RandomSource.create(), Block.UPDATE_CLIENTS);

        BlockPos marker = findAndConsumeSpawnMarker(level, zoneFrom(size), zoneSize(size));
        return marker != null ? marker : MAP_POS;
    }

    // Taille réelle de la structure posée, ou null si on est retombé sur l'arène provisoire —
    // les deux zones (nettoyage, force-chargement) doivent couvrir exactement le même volume.
    private static @Nullable Vec3i sizeOf(ServerLevel level, @Nullable MapDefinition map) {
        if (map == null) {
            return null;
        }
        return level.getStructureManager().get(map.structureId())
                .map(StructureTemplate::getSize)
                .orElse(null);
    }

    // La map est centrée horizontalement sur MAP_POS, sa couche la plus basse juste sous MAP_POS
    // — même convention que la taverne, pour qu'un créateur n'ait pas deux règles à retenir.
    private static BlockPos originOf(Vec3i size) {
        return new BlockPos(
                MAP_POS.getX() - size.getX() / 2,
                MAP_POS.getY() - 1,
                MAP_POS.getZ() - size.getZ() / 2);
    }

    // Emprise à nettoyer. Sans structure (repli placeholder), on retombe sur un volume fixe
    // autour de MAP_POS, comme avant.
    private static BlockPos zoneFrom(@Nullable Vec3i size) {
        if (size == null) {
            int radius = PLATFORM_RADIUS + CLEAR_MARGIN;
            return MAP_POS.offset(-radius, -CLEAR_MARGIN, -radius);
        }
        return originOf(size).offset(-CLEAR_MARGIN, -CLEAR_MARGIN, -CLEAR_MARGIN);
    }

    private static Vec3i zoneSize(@Nullable Vec3i size) {
        if (size == null) {
            int radius = PLATFORM_RADIUS + CLEAR_MARGIN;
            return new Vec3i(2 * radius + 1, 2 * CLEAR_MARGIN + 1, 2 * radius + 1);
        }
        return size.offset(2 * CLEAR_MARGIN, 2 * CLEAR_MARGIN, 2 * CLEAR_MARGIN);
    }

    /**
     * Remet la zone à zéro : les blocs d'abord, les entités ensuite (joueurs exceptés). Même
     * ordre délibéré que {@code TavernSpawn#clearZone} — écrire un bloc force le chargement de
     * son chunk, donc balayer les blocs en premier garantit que les entités interrogées ensuite
     * sont bien chargées. Emporte au passage les monstres de la partie précédente.
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

    private static void buildPlaceholderArena(ServerLevel level) {
        BlockState floor = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                pos.set(MAP_POS.getX() + x, MAP_POS.getY() - 1, MAP_POS.getZ() + z);
                level.setBlockAndUpdate(pos, floor);
            }
        }
    }

    // Cherche le marqueur dans le MONDE (et non dans le template comme le fait la taverne) : ici
    // il doit être **retiré** après usage — "se supprime pour ne pas le voir", comme prévu au
    // départ — donc il faut agir sur le bloc réellement posé. Un seul marqueur est attendu par
    // map ; le premier trouvé gagne.
    private static @Nullable BlockPos findAndConsumeSpawnMarker(ServerLevel level, BlockPos from, Vec3i size) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    pos.set(from.getX() + x, from.getY() + y, from.getZ() + z);
                    if (level.getBlockState(pos).is(ModBlocks.PLAYER_SPAWN.get())) {
                        BlockPos found = pos.immutable();
                        level.setBlockAndUpdate(found, Blocks.AIR.defaultBlockState());
                        return found;
                    }
                }
            }
        }
        return null;
    }

    // Tous les joueurs de la Level, pas seulement celui qui a cliqué "Jouer" : une seule
    // partie active à la fois, donc tout le monde part ensemble (voir la discussion dans
    // 05-etat-et-problemes-connus.md).
    private static void teleportAllPlayers(ServerLevel level, BlockPos destination) {
        List<ServerPlayer> players = new ArrayList<>(level.players());
        for (ServerPlayer player : players) {
            player.teleportTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5);
        }
    }
}
