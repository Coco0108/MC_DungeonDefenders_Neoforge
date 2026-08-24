package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModBlockTags {

    // Regroupe tous les blocs de la catégorie de tours "Blockade" (voir
    // AbstractBlockadeBlockEntity et entity/ai/AttackBlockadeGoal) — ajouter un bloc à ce tag
    // (data/dungeon_defenders/tags/block/blockades.json) suffit pour qu'AttackBlockadeGoal le
    // vise automatiquement, sans toucher au goal.
    public static final TagKey<Block> BLOCKADES =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "blockades"));

    private ModBlockTags() {
    }
}
