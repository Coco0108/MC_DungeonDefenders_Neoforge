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
// indéfiniment dans le vide dès sa première connexion. En attendant la vraie structure de la
// taverne (voir doc/05-etat-et-problemes-connus.md, "Système de maps/structures"), cette
// classe fixe le point de spawn du monde et pose une plateforme provisoire en dur — à
// supprimer une fois la structure posée à la main à cet endroit.
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

    /** Plateforme carrée, un bloc sous SPAWN_POS. Idempotent : sans effet si déjà posée (rejoué à chaque chargement du monde). */
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
