package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// "Harpoon Turret" (nom repris du plan Excel du joueur, feuille Tours — Squire) : premier
// membre concret de la catégorie "Turret" (voir AbstractTurretBlockEntity). Se contente de
// fixer ses propres stats — toute la logique de tir/ciblage vit dans la base.
public class HarpoonTurretBlockEntity extends AbstractTurretBlockEntity {

    public static final int MAX_HEALTH = 20;
    public static final int MANA_COST = 50;
    private static final double RANGE = 12.0D;
    private static final double CONE_ANGLE_DEGREES = 45.0D;
    private static final float DAMAGE = 6.0F;
    private static final long ATTACK_INTERVAL_TICKS = 30L;

    public HarpoonTurretBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.HARPOON_TURRET_BE.get(), pos, state,
                MAX_HEALTH, MANA_COST, RANGE, CONE_ANGLE_DEGREES, DAMAGE, ATTACK_INTERVAL_TICKS);
    }
}
