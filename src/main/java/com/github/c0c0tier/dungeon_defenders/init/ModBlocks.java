package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.BouncerBlockadeBlock;
import com.github.c0c0tier.dungeon_defenders.block.BowlingBallTurretBlock;
import com.github.c0c0tier.dungeon_defenders.block.EterniaCrystalBlock;
import com.github.c0c0tier.dungeon_defenders.block.HarpoonTurretBlock;
import com.github.c0c0tier.dungeon_defenders.block.ManaChestBlock;
import com.github.c0c0tier.dungeon_defenders.block.MortarTurretBlock;
import com.github.c0c0tier.dungeon_defenders.block.PlayerSpawnBlock;
import com.github.c0c0tier.dungeon_defenders.block.SliceNDiceBlockadeBlock;
import com.github.c0c0tier.dungeon_defenders.block.SpawnerBlock;
import com.github.c0c0tier.dungeon_defenders.block.SpikeBlockadeBlock;
import com.github.c0c0tier.dungeon_defenders.block.TavernCrystalBlock;
import com.github.c0c0tier.dungeon_defenders.block.TowerBlockItem;
import com.github.c0c0tier.dungeon_defenders.block.TrainingDummyBlock;
import com.github.c0c0tier.dungeon_defenders.item.ManaFillWandItem;
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

    // 3ter. Reste du roster de l'Écuyer (2026-08-29), discuté et validé tour par tour avec le
    // joueur avant d'être codé — voir doc/02-gameplay.md pour le détail de chacune.
    public static final DeferredBlock<BouncerBlockadeBlock> BOUNCER_BLOCKADE = BLOCKS.registerBlock("bouncer_blockade",
            BouncerBlockadeBlock::new,
            properties -> properties.strength(2.0F));

    public static final DeferredItem<BlockItem> BOUNCER_BLOCKADE_ITEM =
            ITEMS.registerItem("bouncer_blockade", properties -> new TowerBlockItem(BOUNCER_BLOCKADE.get(), properties));

    public static final DeferredBlock<SliceNDiceBlockadeBlock> SLICE_N_DICE_BLOCKADE = BLOCKS.registerBlock("slice_n_dice_blockade",
            SliceNDiceBlockadeBlock::new,
            properties -> properties.strength(2.0F));

    public static final DeferredItem<BlockItem> SLICE_N_DICE_BLOCKADE_ITEM =
            ITEMS.registerItem("slice_n_dice_blockade", properties -> new TowerBlockItem(SLICE_N_DICE_BLOCKADE.get(), properties));

    public static final DeferredBlock<BowlingBallTurretBlock> BOWLING_BALL_TURRET = BLOCKS.registerBlock("bowling_ball_turret",
            BowlingBallTurretBlock::new,
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> BOWLING_BALL_TURRET_ITEM =
            ITEMS.registerItem("bowling_ball_turret", properties -> new TowerBlockItem(BOWLING_BALL_TURRET.get(), properties));

    public static final DeferredBlock<MortarTurretBlock> MORTAR_TURRET = BLOCKS.registerBlock("mortar_turret",
            MortarTurretBlock::new,
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> MORTAR_TURRET_ITEM =
            ITEMS.registerItem("mortar_turret", properties -> new TowerBlockItem(MORTAR_TURRET.get(), properties));

    // 4. Baguette de test : retire 10 de mana au clic droit, pour vérifier le HUD (mana)
    public static final DeferredItem<ManaTestWandItem> MANA_TEST_WAND =
            ITEMS.registerItem("mana_test_wand", ManaTestWandItem::new);

    // 4bis. Baguette de test symétrique : remplit le mana au maximum au clic droit, pour
    // tester la pose de tours sans farmer des cristaux de mana à chaque essai.
    public static final DeferredItem<ManaFillWandItem> MANA_FILL_WAND =
            ITEMS.registerItem("mana_fill_wand", ManaFillWandItem::new);

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

    // 7. Coffre de mana : meuble de map (comme le cristal de la taverne), pas de PV, donne du
    // mana au clic droit en survie (une fois par vague), configurable en créatif (voir
    // ManaChestBlock).
    public static final DeferredBlock<ManaChestBlock> MANA_CHEST = BLOCKS.registerBlock("mana_chest",
            ManaChestBlock::new,
            properties -> properties.strength(2.5F));

    public static final DeferredItem<BlockItem> MANA_CHEST_ITEM =
            ITEMS.registerSimpleBlockItem("mana_chest", MANA_CHEST);

    // 8. Bloc de spawn joueur (plan Excel, feuille "Idées" > "CHOIX DE MAP") : marqueur posé
    // par le créateur de map à l'endroit où les joueurs doivent apparaître. Aucun comportement
    // au clic : repéré et consommé par MapInstance.startGame() (scan de la zone + téléportation
    // + auto-suppression), toute la logique vit là-bas, pas ici. Depuis 2026-08-30, invisible et
    // traversable comme le spawner (voir PlayerSpawnBlock) — d'où une classe dédiée, là où
    // c'était auparavant un registerSimpleBlock. Voir MapInstance et doc/02-gameplay.md.
    public static final DeferredBlock<PlayerSpawnBlock> PLAYER_SPAWN = BLOCKS.registerBlock("player_spawn",
            PlayerSpawnBlock::new,
            properties -> properties.strength(2.0F));

    public static final DeferredItem<BlockItem> PLAYER_SPAWN_ITEM =
            ITEMS.registerSimpleBlockItem("player_spawn", PLAYER_SPAWN);

    // 9. Support de mannequin d'entraînement : garantit qu'un TrainingDummyEntity existe juste
    // au-dessus (cible immobile et indestructible pour mesurer les dégâts des tours). Invisible
    // et traversable comme le spawner — toute la logique vit dans TrainingDummyBlockEntity.
    public static final DeferredBlock<TrainingDummyBlock> TRAINING_DUMMY = BLOCKS.registerBlock("training_dummy",
            TrainingDummyBlock::new,
            properties -> properties.strength(2.0F));

    public static final DeferredItem<BlockItem> TRAINING_DUMMY_ITEM =
            ITEMS.registerSimpleBlockItem("training_dummy", TRAINING_DUMMY);

    // Connexion au bus d'événements
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}