package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.EterniaCrystalBlock;
import com.github.c0c0tier.dungeon_defenders.block.SpawnerBlock;
import com.github.c0c0tier.dungeon_defenders.block.SpikeTrapBlock;
import com.github.c0c0tier.dungeon_defenders.item.ManaTestWandItem;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    // Registres de NeoForge
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DungeonDefendersMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DungeonDefendersMod.MODID);

    // 1. Enregistrement du Bloc (DeferredBlock<EterniaCrystalBlock>)
    // registerBlock applique automatiquement l'ID de registre sur les Properties (requis en 1.21.3+)
    public static final DeferredBlock<EterniaCrystalBlock> ETERNIA_CRYSTAL = BLOCKS.registerBlock("eternia_crystal",
            EterniaCrystalBlock::new,
            properties -> properties
                    .destroyTime(50.0F)
                    .explosionResistance(1200.0F)
                    .requiresCorrectToolForDrops());

    // 2. Enregistrement de l'Item : registerSimpleBlockItem applique aussi l'ID requis
    public static final DeferredItem<BlockItem> ETERNIA_CRYSTAL_ITEM =
            ITEMS.registerSimpleBlockItem("eternia_crystal", ETERNIA_CRYSTAL);

    // 3. Le piège à pics : dégâts au contact, cooldown géré dans SpikeTrapBlock
    public static final DeferredBlock<SpikeTrapBlock> SPIKE_TRAP = BLOCKS.registerBlock("spike_trap",
            SpikeTrapBlock::new,
            properties -> properties.strength(2.0F));

    public static final DeferredItem<BlockItem> SPIKE_TRAP_ITEM =
            ITEMS.registerSimpleBlockItem("spike_trap", SPIKE_TRAP);

    // 4. Baguette de test : retire 10 de mana au clic droit, pour vérifier le HUD (mana)
    public static final DeferredItem<ManaTestWandItem> MANA_TEST_WAND =
            ITEMS.registerItem("mana_test_wand", ManaTestWandItem::new);

    // 5. Spawner : fait apparaître des zombies pendant la phase de combat (voir
    // SpawnerBlockEntity pour l'algorithme). Clic droit = harnais de test qui bascule la phase.
    public static final DeferredBlock<SpawnerBlock> SPAWNER = BLOCKS.registerBlock("spawner",
            SpawnerBlock::new,
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> SPAWNER_ITEM =
            ITEMS.registerSimpleBlockItem("spawner", SPAWNER);

    // Connexion au bus d'événements
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}