package com.github.c0c0tier.dungeon_defenders;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

// L'Overworld est maintenant un monde vide (voir
// data/minecraft/dimension/overworld.json, qui remplace son générateur par le préréglage
// vanilla "The Void") : sans point de spawn fixe ni sol, un nouveau joueur tomberait
// indéfiniment dans le vide dès sa première connexion. Cette classe fixe le point de spawn du
// monde et (re)pose le contenu de la taverne à chaque chargement du monde.
//
// Pourquoi reposer le contenu à CHAQUE chargement, plutôt qu'une seule fois : la taverne est
// censée suivre le même principe que les maps (voir doc/05-etat-et-problemes-connus.md,
// "Système de maps/structures") — sa structure sera reposée à cet emplacement fixe à chaque
// fois qu'on y "entre", plutôt que construite une fois pour toutes. Sans ça, une mise à jour
// du mod qui change la structure de la taverne resterait invisible sur une sauvegarde
// existante (le joueur garderait l'ancienne version, posée lors de sa toute première
// connexion). Recharger à chaque fois est le déclencheur le plus simple qui garantit que la
// version affichée correspond toujours à celle livrée avec le mod installé.
//
// En attendant la vraie structure `.nbt`, la "taverne" posée ici n'est qu'une plateforme
// provisoire en dur — mais le principe (reposer à chaque chargement) restera le même une fois
// remplacée par un vrai chargement de structure.
@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public class TavernSpawn {

    /** Position où le joueur doit apparaître : X/Z à 0,0 comme demandé, Y choisi arbitrairement pour la plateforme provisoire. */
    public static final BlockPos SPAWN_POS = new BlockPos(0, 65, 0);
    private static final int PLATFORM_RADIUS = 4;

    @SubscribeEvent
    static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || serverLevel.dimension() != Level.OVERWORLD) {
            return;
        }

        // Remplace le point de spawn "trouvé" par le jeu (qui chercherait un sol solide —
        // inexistant dans un monde vide) par une position fixe.
        serverLevel.setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD, SPAWN_POS, 0.0F, 0.0F));
        buildPlaceholderPlatform(serverLevel);
    }

    /**
     * Plateforme carrée, un bloc sous SPAWN_POS — représente pour l'instant tout le contenu de
     * la taverne. Idempotent (sans effet si déjà posée), volontairement rejoué à chaque
     * chargement du monde : voir le commentaire de classe pour pourquoi. Le jour où une vraie
     * structure `.nbt` existe, cette méthode deviendra "charger la structure de la taverne à
     * SPAWN_POS", appelée au même endroit, avec le même principe de rechargement systématique.
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
