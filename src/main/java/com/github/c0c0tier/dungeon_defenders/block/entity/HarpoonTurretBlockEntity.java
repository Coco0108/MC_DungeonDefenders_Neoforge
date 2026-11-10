package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// "Harpoon Turret" (nom repris du plan Excel du joueur, feuille Tours — Squire) : premier
// membre concret de la catégorie "Turret" (voir AbstractTurretBlockEntity). Se contente de
// fixer ses propres stats — toute la logique de tir/ciblage vit dans la base.
public class HarpoonTurretBlockEntity extends AbstractTurretBlockEntity {

    // Chiffres (PV, coût, portée, cône, dégâts, cadence) : data-driven, voir
    // data/dungeon_defenders/dungeon_defenders/tower/harpoon_turret.json et
    // TowerDefinition.HARPOON_TURRET pour les valeurs par défaut.
    public HarpoonTurretBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.HARPOON_TURRET_BE.get(), pos, state, TowerDefinition.HARPOON_TURRET);
    }
}
