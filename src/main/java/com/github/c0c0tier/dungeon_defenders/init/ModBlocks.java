package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.EterniaCrystalBlock;
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

    // Connexion au bus d'événements
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}