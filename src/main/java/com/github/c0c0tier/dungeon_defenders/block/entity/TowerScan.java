package com.github.c0c0tier.dungeon_defenders.block.entity;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

// Trouve les tours posées autour d'un point. Parcourt les block entities des chunks concernés
// plutôt que chaque case du volume : une recherche à 8 blocs coûterait sinon des milliers de
// lectures de blocs, répétées plusieurs fois par seconde.
public final class TowerScan {

    private TowerScan() {
    }

    /** Les tours dont le centre est à {@code radius} blocs ou moins de {@code center}. */
    public static List<AbstractTowerBlockEntity> towersNear(ServerLevel level, Vec3 center, double radius) {
        List<AbstractTowerBlockEntity> found = new ArrayList<>();
        int minChunkX = SectionPos.blockToSectionCoord(Mth.floor(center.x - radius));
        int maxChunkX = SectionPos.blockToSectionCoord(Mth.floor(center.x + radius));
        int minChunkZ = SectionPos.blockToSectionCoord(Mth.floor(center.z - radius));
        int maxChunkZ = SectionPos.blockToSectionCoord(Mth.floor(center.z + radius));
        double radiusSq = radius * radius;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                // getChunkNow : jamais de chargement forcé, un chunk absent n'a pas de tour active.
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof AbstractTowerBlockEntity tower
                            && !tower.isRemoved()
                            && Vec3.atCenterOf(tower.getBlockPos()).distanceToSqr(center) <= radiusSq) {
                        found.add(tower);
                    }
                }
            }
        }
        return found;
    }
}
