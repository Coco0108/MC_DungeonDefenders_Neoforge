package com.github.c0c0tier.dungeon_defenders;

import com.github.c0c0tier.dungeon_defenders.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// "La map actuellement active" : un seul emplacement partagé, réutilisé pour n'importe quelle
// map choisie dans MapSelectionScreen — une seule partie active à la fois sur tout le serveur
// (voir doc/05-etat-et-problemes-connus.md, "Système de maps/structures"), donc pas besoin
// d'une coordonnée par map. Même principe que TavernSpawn : en attendant une vraie structure
// `.nbt` par map, cette classe pose un placeholder générique à cet emplacement — remplacer
// buildPlaceholderArena() par un vrai chargement de structure sera le seul changement
// nécessaire une fois les vraies maps prêtes.
public final class MapInstance {

    /** Coordonnée partagée par toutes les maps, loin de la taverne (voir TavernSpawn.SPAWN_POS). */
    public static final BlockPos MAP_POS = new BlockPos(10000, 65, 0);
    /** Nom (sans le "/") de la commande de retour à la taverne — voir ModCommands et PhaseTransitions. */
    public static final String RETURN_COMMAND = "dd_leave";

    private static final int PLATFORM_RADIUS = 8;
    // Zone effacée avant de poser le placeholder, plus large que la plateforme elle-même pour
    // rattraper d'éventuelles tours posées par le joueur autour (aucune pour l'instant, mais
    // la marge ne coûte rien).
    private static final int CLEAR_RADIUS = 10;
    private static final int CLEAR_BELOW = 2;
    private static final int CLEAR_ABOVE = 10;

    private MapInstance() {
    }

    /**
     * Nettoie l'emplacement, pose le placeholder, et téléporte tous les joueurs présents — au
     * {@code PLAYER_SPAWN} de la map si le créateur en a posé un (voir
     * {@link #findAndConsumeSpawnMarker}), sinon à {@link #MAP_POS} comme avant.
     */
    public static void startGame(ServerLevel level) {
        clearZone(level);
        buildPlaceholderArena(level);
        BlockPos spawnPos = findAndConsumeSpawnMarker(level);
        teleportAllPlayers(level, spawnPos != null ? spawnPos : MAP_POS);
    }

    /** Nettoie l'emplacement (plus besoin d'y rester) et ramène tout le monde à la taverne. */
    public static void returnToTavern(ServerLevel level) {
        clearZone(level);
        // Pas TavernSpawn.SPAWN_POS en dur : si la structure de la taverne pose son propre
        // marqueur player_spawn, c'est lui qui fait foi (voir TavernSpawn#arrivalPos).
        teleportAllPlayers(level, TavernSpawn.arrivalPos(level));
    }

    private static void clearZone(ServerLevel level) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -CLEAR_RADIUS; x <= CLEAR_RADIUS; x++) {
            for (int z = -CLEAR_RADIUS; z <= CLEAR_RADIUS; z++) {
                for (int y = -CLEAR_BELOW; y <= CLEAR_ABOVE; y++) {
                    pos.set(MAP_POS.getX() + x, MAP_POS.getY() + y, MAP_POS.getZ() + z);
                    level.setBlockAndUpdate(pos, air);
                }
            }
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

    // Cherche un bloc PLAYER_SPAWN dans la même zone que clearZone/buildPlaceholderArena (le
    // créateur de map le pose là où les joueurs doivent apparaître, plan Excel feuille "Idées"
    // > "CHOIX DE MAP") : s'il en trouve un, le retire ("se supprime pour ne pas le voir",
    // comme prévu) et renvoie sa position ; sinon renvoie null (aucun marqueur posé pour
    // cette map — arrive systématiquement pour l'instant, tant que buildPlaceholderArena() ne
    // pose qu'un sol générique, voir doc/05-etat-et-problemes-connus.md). Un seul marqueur par
    // map est attendu ; le premier trouvé gagne.
    private static @Nullable BlockPos findAndConsumeSpawnMarker(ServerLevel level) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -CLEAR_RADIUS; x <= CLEAR_RADIUS; x++) {
            for (int z = -CLEAR_RADIUS; z <= CLEAR_RADIUS; z++) {
                for (int y = -CLEAR_BELOW; y <= CLEAR_ABOVE; y++) {
                    pos.set(MAP_POS.getX() + x, MAP_POS.getY() + y, MAP_POS.getZ() + z);
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
