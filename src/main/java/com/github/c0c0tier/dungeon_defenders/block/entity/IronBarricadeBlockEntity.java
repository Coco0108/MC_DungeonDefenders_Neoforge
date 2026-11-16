package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

// "Barricade blindée" (Ingénieur) : mur passif (aucun dégât, priorité IA "block") avec beaucoup
// de PV, qui se répare seul à petit débit. Une barricade entamée entre deux vagues est donc
// revenue à neuf sans y dépenser de mana.
public class IronBarricadeBlockEntity extends AbstractPulseTowerBlockEntity {

    public IronBarricadeBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.IRON_BARRICADE_BE.get(), pos, state, TowerDefinition.IRON_BARRICADE);
    }

    @Override
    public int getAiPriority() {
        return AiAttackTarget.PRIORITY_BLOCK;
    }

    @Override
    protected int pulseIntervalTicks() {
        return stats().intValue(TowerStat.ATTACK_INTERVAL_TICKS);
    }

    @Override
    protected void pulse(ServerLevel level) {
        if (getHealth() < getMaxHealth()) {
            repair(stats().intValue(TowerStat.HEAL_AMOUNT));
        }
    }
}
