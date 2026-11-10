package com.github.c0c0tier.dungeon_defenders.client;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.MapInstance;
import com.github.c0c0tier.dungeon_defenders.TavernSpawn;
import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jspecify.annotations.Nullable;

// Fait vivre le plan plein écran (MapOverlayState/client.gui.MapOverlay) : ModKeyMappings.MAP_OVERLAY
// maintenue affiche, relâchée cache — pas une bascule, comme le Tab vanilla (liste des joueurs).
//
// Pendant que c'est affiché, un scan périodique (toutes les SCAN_INTERVAL_TICKS) repère le
// cristal de la zone actuelle : Cristal d'Eternia en map, cristal de la taverne à la Taverne.
// Borné exactement à la zone connue (ModAttachments.PLAYFIELD_SIZE_X/Y/Z, voir MapInstance et
// TavernSpawn) plutôt qu'à un rayon arbitraire autour du joueur, comme le fait
// MarkerOverlayClientEvents pour ses marqueurs d'édition — ici la zone entière est petite et
// exactement connue, pas la peine de deviner un rayon.
@EventBusSubscriber(modid = DungeonDefendersMod.MODID, value = Dist.CLIENT)
public final class MapOverlayClientEvents {

    private static final int SCAN_INTERVAL_TICKS = 20;

    private static int tickCounter = 0;

    private MapOverlayClientEvents() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        boolean down = ModKeyMappings.MAP_OVERLAY.isDown();
        MapOverlayState.setVisible(down);

        if (!down) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || tickCounter++ % SCAN_INTERVAL_TICKS != 0) {
            return;
        }
        MapOverlayState.setCrystalPos(findCrystal(level));
    }

    private static @Nullable BlockPos findCrystal(ClientLevel level) {
        GamePhase phase = GamePhase.of(level);
        BlockPos anchor = phase == GamePhase.TAVERN ? TavernSpawn.SPAWN_POS : MapInstance.MAP_POS;

        int sizeX = level.getData(ModAttachments.PLAYFIELD_SIZE_X);
        int sizeY = Math.max(1, level.getData(ModAttachments.PLAYFIELD_SIZE_Y));
        int sizeZ = level.getData(ModAttachments.PLAYFIELD_SIZE_Z);
        if (sizeX <= 0 || sizeZ <= 0) {
            return null;
        }

        // Même formule que MapInstance#originOf / TavernSpawn#originOf : centrée
        // horizontalement sur l'ancre, la couche la plus basse un bloc en dessous.
        BlockPos origin = new BlockPos(anchor.getX() - sizeX / 2, anchor.getY() - 1, anchor.getZ() - sizeZ / 2);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    pos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!level.isLoaded(pos)) {
                        continue;
                    }
                    if (phase == GamePhase.TAVERN) {
                        if (level.getBlockState(pos).is(ModBlocks.TAVERN_CRYSTAL.get())) {
                            return pos.immutable();
                        }
                    } else if (level.getBlockEntity(pos) instanceof EterniaCrystalBlockEntity) {
                        return pos.immutable();
                    }
                }
            }
        }
        return null;
    }
}
