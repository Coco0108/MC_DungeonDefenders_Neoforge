package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.BouncerBlockadeBlock;
import com.github.c0c0tier.dungeon_defenders.block.BowlingBallTurretBlock;
import com.github.c0c0tier.dungeon_defenders.block.DirectionalTowerBlock;
import com.github.c0c0tier.dungeon_defenders.block.EterniaCrystalBlock;
import com.github.c0c0tier.dungeon_defenders.block.ForgeBlock;
import com.github.c0c0tier.dungeon_defenders.block.HarpoonTurretBlock;
import com.github.c0c0tier.dungeon_defenders.block.ManaChestBlock;
import com.github.c0c0tier.dungeon_defenders.block.MapConfigBlock;
import com.github.c0c0tier.dungeon_defenders.block.MortarTurretBlock;
import com.github.c0c0tier.dungeon_defenders.block.NoBuildZoneBlock;
import com.github.c0c0tier.dungeon_defenders.block.PlayerSpawnBlock;
import com.github.c0c0tier.dungeon_defenders.block.SimpleTowerBlock;
import com.github.c0c0tier.dungeon_defenders.block.SliceNDiceBlockadeBlock;
import com.github.c0c0tier.dungeon_defenders.block.SpawnerBlock;
import com.github.c0c0tier.dungeon_defenders.block.SpikeBlockadeBlock;
import com.github.c0c0tier.dungeon_defenders.block.TavernCrystalBlock;
import com.github.c0c0tier.dungeon_defenders.block.TowerBlockItem;
import com.github.c0c0tier.dungeon_defenders.block.TrainingDummyBlock;
import com.github.c0c0tier.dungeon_defenders.block.entity.FrostStillBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.AcidSprayerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.VolatileVatBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.CatalystPylonBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.ManaCondenserBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.FlaskLauncherBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.IronBarricadeBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.RepeaterTurretBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.LongshotTurretBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.MagnetCoilBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.RepairStationBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.PistonSlammerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.ThornHedgeBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.MudTotemBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.GeyserBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.BoulderThrowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.CrystalSpireBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.LifestoneBlockEntity;
import com.github.c0c0tier.dungeon_defenders.item.HeroWeaponItem;
import com.github.c0c0tier.dungeon_defenders.item.ManaFillWandItem;
import com.github.c0c0tier.dungeon_defenders.item.ManaTestWandItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
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

    // 3quater. Roster de l'Alchimiste (2026-11-16) : six tours de conception propre au mod, voir
    // doc/02-gameplay.md. Une seule classe de bloc pour toutes (SimpleTowerBlock, ou
    // DirectionalTowerBlock pour celle qui s'oriente) : seul le block entity les distingue.
    public static final DeferredBlock<SimpleTowerBlock> FROST_STILL = BLOCKS.registerBlock("frost_still",
            properties -> new SimpleTowerBlock(properties, FrostStillBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> FROST_STILL_ITEM =
            ITEMS.registerItem("frost_still", properties -> new TowerBlockItem(FROST_STILL.get(), properties));

    public static final DeferredBlock<DirectionalTowerBlock> ACID_SPRAYER = BLOCKS.registerBlock("acid_sprayer",
            properties -> new DirectionalTowerBlock(properties, AcidSprayerBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> ACID_SPRAYER_ITEM =
            ITEMS.registerItem("acid_sprayer", properties -> new TowerBlockItem(ACID_SPRAYER.get(), properties));

    public static final DeferredBlock<SimpleTowerBlock> VOLATILE_VAT = BLOCKS.registerBlock("volatile_vat",
            properties -> new SimpleTowerBlock(properties, VolatileVatBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> VOLATILE_VAT_ITEM =
            ITEMS.registerItem("volatile_vat", properties -> new TowerBlockItem(VOLATILE_VAT.get(), properties));

    public static final DeferredBlock<SimpleTowerBlock> CATALYST_PYLON = BLOCKS.registerBlock("catalyst_pylon",
            properties -> new SimpleTowerBlock(properties, CatalystPylonBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> CATALYST_PYLON_ITEM =
            ITEMS.registerItem("catalyst_pylon", properties -> new TowerBlockItem(CATALYST_PYLON.get(), properties));

    public static final DeferredBlock<SimpleTowerBlock> MANA_CONDENSER = BLOCKS.registerBlock("mana_condenser",
            properties -> new SimpleTowerBlock(properties, ManaCondenserBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> MANA_CONDENSER_ITEM =
            ITEMS.registerItem("mana_condenser", properties -> new TowerBlockItem(MANA_CONDENSER.get(), properties));

    public static final DeferredBlock<SimpleTowerBlock> FLASK_LAUNCHER = BLOCKS.registerBlock("flask_launcher",
            properties -> new SimpleTowerBlock(properties, FlaskLauncherBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> FLASK_LAUNCHER_ITEM =
            ITEMS.registerItem("flask_launcher", properties -> new TowerBlockItem(FLASK_LAUNCHER.get(), properties));

    // Roster de l'Ingénieur (2026-11-16) : tours de conception propre au mod, voir
    // doc/02-gameplay.md. Même bloc générique que celles de l'Alchimiste.
    public static final DeferredBlock<SimpleTowerBlock> IRON_BARRICADE = BLOCKS.registerBlock("iron_barricade",
            properties -> new SimpleTowerBlock(properties, IronBarricadeBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> IRON_BARRICADE_ITEM =
            ITEMS.registerItem("iron_barricade", properties -> new TowerBlockItem(IRON_BARRICADE.get(), properties));

    public static final DeferredBlock<DirectionalTowerBlock> REPEATER_TURRET = BLOCKS.registerBlock("repeater_turret",
            properties -> new DirectionalTowerBlock(properties, RepeaterTurretBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> REPEATER_TURRET_ITEM =
            ITEMS.registerItem("repeater_turret", properties -> new TowerBlockItem(REPEATER_TURRET.get(), properties));

    public static final DeferredBlock<DirectionalTowerBlock> LONGSHOT_TURRET = BLOCKS.registerBlock("longshot_turret",
            properties -> new DirectionalTowerBlock(properties, LongshotTurretBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> LONGSHOT_TURRET_ITEM =
            ITEMS.registerItem("longshot_turret", properties -> new TowerBlockItem(LONGSHOT_TURRET.get(), properties));

    public static final DeferredBlock<SimpleTowerBlock> MAGNET_COIL = BLOCKS.registerBlock("magnet_coil",
            properties -> new SimpleTowerBlock(properties, MagnetCoilBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> MAGNET_COIL_ITEM =
            ITEMS.registerItem("magnet_coil", properties -> new TowerBlockItem(MAGNET_COIL.get(), properties));

    public static final DeferredBlock<SimpleTowerBlock> REPAIR_STATION = BLOCKS.registerBlock("repair_station",
            properties -> new SimpleTowerBlock(properties, RepairStationBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> REPAIR_STATION_ITEM =
            ITEMS.registerItem("repair_station", properties -> new TowerBlockItem(REPAIR_STATION.get(), properties));

    public static final DeferredBlock<SimpleTowerBlock> PISTON_SLAMMER = BLOCKS.registerBlock("piston_slammer",
            properties -> new SimpleTowerBlock(properties, PistonSlammerBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> PISTON_SLAMMER_ITEM =
            ITEMS.registerItem("piston_slammer", properties -> new TowerBlockItem(PISTON_SLAMMER.get(), properties));

    // Roster de du Géomancien (2026-11-16) : tours de conception propre au mod, voir
    // doc/02-gameplay.md. Même bloc générique que celles de l'Alchimiste.
    public static final DeferredBlock<SimpleTowerBlock> THORN_HEDGE = BLOCKS.registerBlock("thorn_hedge",
            properties -> new SimpleTowerBlock(properties, ThornHedgeBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> THORN_HEDGE_ITEM =
            ITEMS.registerItem("thorn_hedge", properties -> new TowerBlockItem(THORN_HEDGE.get(), properties));

    public static final DeferredBlock<SimpleTowerBlock> MUD_TOTEM = BLOCKS.registerBlock("mud_totem",
            properties -> new SimpleTowerBlock(properties, MudTotemBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> MUD_TOTEM_ITEM =
            ITEMS.registerItem("mud_totem", properties -> new TowerBlockItem(MUD_TOTEM.get(), properties));

    public static final DeferredBlock<SimpleTowerBlock> GEYSER = BLOCKS.registerBlock("geyser",
            properties -> new SimpleTowerBlock(properties, GeyserBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> GEYSER_ITEM =
            ITEMS.registerItem("geyser", properties -> new TowerBlockItem(GEYSER.get(), properties));

    public static final DeferredBlock<SimpleTowerBlock> BOULDER_THROWER = BLOCKS.registerBlock("boulder_thrower",
            properties -> new SimpleTowerBlock(properties, BoulderThrowerBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> BOULDER_THROWER_ITEM =
            ITEMS.registerItem("boulder_thrower", properties -> new TowerBlockItem(BOULDER_THROWER.get(), properties));

    public static final DeferredBlock<SimpleTowerBlock> CRYSTAL_SPIRE = BLOCKS.registerBlock("crystal_spire",
            properties -> new SimpleTowerBlock(properties, CrystalSpireBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> CRYSTAL_SPIRE_ITEM =
            ITEMS.registerItem("crystal_spire", properties -> new TowerBlockItem(CRYSTAL_SPIRE.get(), properties));

    public static final DeferredBlock<SimpleTowerBlock> LIFESTONE = BLOCKS.registerBlock("lifestone",
            properties -> new SimpleTowerBlock(properties, LifestoneBlockEntity::new),
            properties -> properties.strength(3.0F));

    public static final DeferredItem<BlockItem> LIFESTONE_ITEM =
            ITEMS.registerItem("lifestone", properties -> new TowerBlockItem(LIFESTONE.get(), properties));

    // Armes de héros (2026-11-16) : une par héros, donnée automatiquement (item/HeroWeapons).
    // L'Écuyer a une épée de mêlée vanilla (stats d'une épée en fer) ; les trois autres tirent au
    // clic droit, voir HeroWeaponItem.
    public static final DeferredItem<Item> SQUIRE_SWORD =
            ITEMS.registerItem("squire_sword", properties -> new Item(properties.sword(ToolMaterial.IRON, 3.0F, -2.4F)));

    public static final DeferredItem<HeroWeaponItem> ALCHEMIST_FLASKS =
            ITEMS.registerItem("alchemist_flasks", properties -> new HeroWeaponItem(properties.stacksTo(1), HeroWeaponItem.Kind.FLASK));

    public static final DeferredItem<HeroWeaponItem> ENGINEER_RIVET_GUN =
            ITEMS.registerItem("engineer_rivet_gun", properties -> new HeroWeaponItem(properties.stacksTo(1), HeroWeaponItem.Kind.RIVET));

    public static final DeferredItem<HeroWeaponItem> GEOMANCER_STAFF =
            ITEMS.registerItem("geomancer_staff", properties -> new HeroWeaponItem(properties.stacksTo(1), HeroWeaponItem.Kind.STAFF));

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
            // Modèle dd_cristal_v2 de l'Atelier : pas un cube plein, et il luit comme le bloc de décor (10).
            properties -> properties
                    .noOcclusion()
                    .lightLevel(state -> 10)
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
            properties -> properties.strength(2.0F).noOcclusion());

    public static final DeferredItem<BlockItem> TRAINING_DUMMY_ITEM =
            ITEMS.registerSimpleBlockItem("training_dummy", TRAINING_DUMMY);

    // 10. Zone interdite à la pose de tours : marqueur posé par le créateur d'une map, une case
    // par bloc (voir NoBuildZoneBlock). Invisible et traversable comme les autres marqueurs ;
    // c'est le simple fait d'occuper la case qui interdit la pose.
    public static final DeferredBlock<NoBuildZoneBlock> NO_BUILD_ZONE = BLOCKS.registerBlock("no_build_zone",
            NoBuildZoneBlock::new,
            properties -> properties.strength(2.0F));

    public static final DeferredItem<BlockItem> NO_BUILD_ZONE_ITEM =
            ITEMS.registerSimpleBlockItem("no_build_zone", NO_BUILD_ZONE);

    // 11. Bloc de configuration de map : nom, ordre, nombre de vagues, multiplicateur de score.
    // Posé DANS la map, ses réglages voyagent donc dans le .nbt de la structure — c'est ce qui
    // permet de créer une map entièrement en jeu (voir MapConfigBlock et MapRegistry).
    public static final DeferredBlock<MapConfigBlock> MAP_CONFIG = BLOCKS.registerBlock("map_config",
            MapConfigBlock::new,
            properties -> properties.strength(2.0F));

    public static final DeferredItem<BlockItem> MAP_CONFIG_ITEM =
            ITEMS.registerSimpleBlockItem("map_config", MAP_CONFIG);

    // 12. Forge : meuble de la taverne, ouvre l'inventaire du joueur (voir ForgeBlock).
    public static final DeferredBlock<ForgeBlock> FORGE = BLOCKS.registerBlock("forge",
            ForgeBlock::new,
            // Modèle dd_forge_v2 de l'Atelier : pas un cube plein, et son foyer éclaire comme le bloc de décor (15 depuis sa fiche v2.3).
            properties -> properties.strength(3.5F).noOcclusion().lightLevel(state -> 15));

    public static final DeferredItem<BlockItem> FORGE_ITEM =
            ITEMS.registerSimpleBlockItem("forge", FORGE);

    // Connexion au bus d'événements
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}