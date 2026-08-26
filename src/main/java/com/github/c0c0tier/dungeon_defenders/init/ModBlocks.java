package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.EterniaCrystalBlock;
import com.github.c0c0tier.dungeon_defenders.block.HarpoonTurretBlock;
import com.github.c0c0tier.dungeon_defenders.block.SpawnerBlock;
import com.github.c0c0tier.dungeon_defenders.block.SpikeBlockadeBlock;
import com.github.c0c0tier.dungeon_defenders.block.TavernCrystalBlock;
import com.github.c0c0tier.dungeon_defenders.block.TowerBlockItem;
import com.github.c0c0tier.dungeon_defenders.item.ManaTestWandItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
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

    // 3. Spike Blockade (nom repris du plan Excel — Squire) : premier tower du mod, un mur à
    // PV qui pique les monstres à son contact. Voir SpikeBlockadeBlockEntity pour le détail.
    public static final DeferredBlock<SpikeBlockadeBlock> SPIKE_BLOCKADE = BLOCKS.registerBlock("spike_blockade",
            SpikeBlockadeBlock::new,
            properties -> properties.strength(2.0F));

    // TowerBlockItem plutôt que registerSimpleBlockItem : useOn() ne pose plus rien, la roue
    // (TowerWheelScreen) est l'unique façon de poser une tour, toute catégorie confondue.
    public static final DeferredItem<BlockItem> SPIKE_BLOCKADE_ITEM =
            ITEMS.registerItem("spike_blockade", properties -> new TowerBlockItem(SPIKE_BLOCKADE.get(), properties));

    // 3bis. Harpoon Turret (nom repris du plan Excel — Squire) : première tour de la catégorie
    // "Turret", tire dans un cône orienté. Voir HarpoonTurretBlockEntity pour le détail.
    public static final DeferredBlock<HarpoonTurretBlock> HARPOON_TURRET = BLOCKS.registerBlock("harpoon_turret",
            HarpoonTurretBlock::new,
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> HARPOON_TURRET_ITEM =
            ITEMS.registerItem("harpoon_turret", properties -> new TowerBlockItem(HARPOON_TURRET.get(), properties));

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

    // 6. Cristal de la taverne : pas de PV, pas de mécanique de combat — ouvre l'écran de
    // choix de map au clic droit (voir TavernCrystalBlock).
    public static final DeferredBlock<TavernCrystalBlock> TAVERN_CRYSTAL = BLOCKS.registerBlock("tavern_crystal",
            TavernCrystalBlock::new,
            properties -> properties
                    .destroyTime(50.0F)
                    .explosionResistance(1200.0F)
                    .requiresCorrectToolForDrops());

    public static final DeferredItem<BlockItem> TAVERN_CRYSTAL_ITEM =
            ITEMS.registerSimpleBlockItem("tavern_crystal", TAVERN_CRYSTAL);

    // 7. Bloc de spawn joueur (plan Excel, feuille "Idées" > "CHOIX DE MAP") : marqueur posé
    // par le créateur de map à l'endroit où les joueurs doivent apparaître. Bloc plein simple,
    // aucun comportement au clic : repéré et consommé par MapInstance.startGame() (scan de la
    // zone + téléportation + auto-suppression), toute la logique vit là-bas, pas ici. Voir
    // MapInstance et doc/02-gameplay.md.
    public static final DeferredBlock<Block> PLAYER_SPAWN = BLOCKS.registerSimpleBlock("player_spawn",
            properties -> properties.strength(2.0F));

    public static final DeferredItem<BlockItem> PLAYER_SPAWN_ITEM =
            ITEMS.registerSimpleBlockItem("player_spawn", PLAYER_SPAWN);

    // Connexion au bus d'événements
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}