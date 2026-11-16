package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// "Tourelle longue portée" (Ingénieur) : l'opposé de la Tourelle à répétition. Cône très étroit,
// portée double, un gros tir toutes les quatre secondes. Elle se place au bout d'un couloir.
public class LongshotTurretBlockEntity extends AbstractTurretBlockEntity {

    public LongshotTurretBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.LONGSHOT_TURRET_BE.get(), pos, state, TowerDefinition.LONGSHOT_TURRET);
    }
}
