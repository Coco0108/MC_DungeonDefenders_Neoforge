package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// "Tourelle à répétition" (Ingénieur) : une Turret classique (flèche + dégâts à une cible, voir
// AbstractTurretBlockEntity) réglée sur une cadence très élevée et de petits dégâts. Seuls ses
// chiffres la distinguent du Harpoon Turret.
public class RepeaterTurretBlockEntity extends AbstractTurretBlockEntity {

    public RepeaterTurretBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.REPEATER_TURRET_BE.get(), pos, state, TowerDefinition.REPEATER_TURRET);
    }
}
