package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.MapInstance;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Force-chargement de la zone de map pendant une partie.
//
// **Pourquoi c'est indispensable** : Minecraft ne charge et ne fait tourner que les chunks
// proches d'un joueur. Une arène fixe où plusieurs spawners peuvent être loin les uns des autres
// — et loin du groupe — verrait donc ses spawners éloignés simplement cesser de fonctionner,
// sans erreur ni message. Le symptôme serait "certains ennemis n'apparaissent jamais", ce qui
// est particulièrement pénible à diagnostiquer.
//
// Passe par le système de tickets de NeoForge plutôt que par le `/forceload` vanilla : les
// tickets sont attribués à un propriétaire (ici la position de la map), persistés par NeoForge,
// et surtout revalidés au chargement du monde (voir validateTickets).
public final class ModChunkTickets {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Au-delà, on force quand même mais on prévient : une map de plus de 512×512 blocs tiendrait
    // un millier de chunks chargés en permanence, ce qui se sentirait sur un petit serveur. Le
    // seuil est un garde-fou d'alerte, pas une limite de conception.
    private static final int CHUNK_WARNING_THRESHOLD = 1024;

    public static final TicketController MAP = new TicketController(
            Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "map"),
            ModChunkTickets::validateTickets);

    // Ce qui est réellement forcé, pour pouvoir le relâcher exactement. Un état statique se
    // justifie ici (contrairement au reste du mod, où on préfère recalculer) : il faut relâcher
    // les MÊMES chunks que ceux forcés, et la taille de la map peut changer entre-temps si son
    // créateur la re-sauvegarde. La survie à un rechargement du monde est assurée autrement, par
    // validateTickets.
    private static final Map<ResourceKey<Level>, LongSet> FORCED = new HashMap<>();

    private ModChunkTickets() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterTicketControllersEvent.class, event -> event.register(MAP));
    }

    /**
     * Force le chargement (et le tick) de tous les chunks couvrant la zone donnée, après avoir
     * relâché ce qui l'était précédemment.
     */
    public static void forceZone(ServerLevel level, BlockPos from, Vec3i size) {
        releaseAll(level);

        LongSet forced = new LongOpenHashSet();
        int minChunkX = SectionPos.blockToSectionCoord(from.getX());
        int maxChunkX = SectionPos.blockToSectionCoord(from.getX() + size.getX() - 1);
        int minChunkZ = SectionPos.blockToSectionCoord(from.getZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(from.getZ() + size.getZ() - 1);

        int count = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        if (count > CHUNK_WARNING_THRESHOLD) {
            LOGGER.warn("La map force le chargement de {} chunks — c'est beaucoup, surveiller les performances.", count);
        }

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                // true en dernier argument : on veut des chunks qui **tickent** réellement, pas
                // seulement chargés — sans ça les spawners ne tourneraient toujours pas.
                if (MAP.forceChunk(level, ownerOf(level), chunkX, chunkZ, true, true)) {
                    forced.add(ChunkPos.pack(chunkX, chunkZ));
                }
            }
        }

        FORCED.put(level.dimension(), forced);
    }

    /** Relâche tout ce que cette classe avait forcé sur cette Level. Sans effet si rien ne l'était. */
    public static void releaseAll(ServerLevel level) {
        LongSet forced = FORCED.remove(level.dimension());
        if (forced == null) {
            return;
        }
        for (long packed : forced) {
            MAP.forceChunk(level, ownerOf(level), ChunkPos.getX(packed), ChunkPos.getZ(packed), false, true);
        }
    }

    // Le propriétaire des tickets est la position de la map : une seule partie active à la fois
    // sur tout le serveur, donc un seul propriétaire suffit.
    private static BlockPos ownerOf(ServerLevel level) {
        return MapInstance.MAP_POS;
    }

    /**
     * Appelée par NeoForge au chargement du monde, avec les tickets persistés de la session
     * précédente. On les supprime tous : le monde démarre toujours à la taverne (voir
     * {@code TavernSpawn}), aucune partie n'est en cours, donc aucun chunk de map n'a de raison
     * de rester chargé. Sans ça, un serveur arrêté en pleine partie garderait la zone chargée
     * indéfiniment au redémarrage suivant.
     */
    private static void validateTickets(ServerLevel level, TicketHelper ticketHelper) {
        List<BlockPos> owners = new ArrayList<>(ticketHelper.getBlockTickets().keySet());
        for (BlockPos owner : owners) {
            ticketHelper.removeAllTickets(owner);
        }
        FORCED.remove(level.dimension());
    }
}
