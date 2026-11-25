package com.github.c0c0tier.dungeon_defenders.gametest;

import com.github.c0c0tier.dungeon_defenders.atelier.Atelier;
import com.github.c0c0tier.dungeon_defenders.atelier.AtelierEntities;
import com.github.c0c0tier.dungeon_defenders.atelier.AtelierMonster;
import com.github.c0c0tier.dungeon_defenders.atelier.FicheMonstre;
import com.github.c0c0tier.dungeon_defenders.entity.ai.RangedAttackEterniaCrystalGoal;
import net.minecraft.world.entity.EntitySpawnReason;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.HashMap;
import com.github.c0c0tier.dungeon_defenders.CombatCountdown;
import com.github.c0c0tier.dungeon_defenders.Config;
import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.ModEvents;
import com.github.c0c0tier.dungeon_defenders.WorldTime;
import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.ManaChestBlock;
import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.AcidSprayerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.CatalystPylonBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.FlaskLauncherBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.FrostStillBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.ManaCondenserBlockEntity;
import com.github.c0c0tier.dungeon_defenders.entity.Corrosion;
import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import com.github.c0c0tier.dungeon_defenders.init.HeroStat;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.github.c0c0tier.dungeon_defenders.block.entity.BoulderThrowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.CrystalSpireBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.GeyserBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.IronBarricadeBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.MudTotemBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.MagnetCoilBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.ManaChestBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.PistonSlammerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.RepairStationBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.MapConfigBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.SpawnerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.entity.ai.AttackPriorityTargetGoal;
import com.github.c0c0tier.dungeon_defenders.init.BalanceValue;
import com.github.c0c0tier.dungeon_defenders.init.DifficultyScaling;
import com.github.c0c0tier.dungeon_defenders.init.EnemyBehavior;
import com.github.c0c0tier.dungeon_defenders.init.EnemyDefinition;
import com.github.c0c0tier.dungeon_defenders.init.EnemyRegistry;
import com.github.c0c0tier.dungeon_defenders.init.GameBalance;
import com.github.c0c0tier.dungeon_defenders.init.GameDifficulty;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.LegacyEnemyIds;
import com.github.c0c0tier.dungeon_defenders.init.MapDefinition;
import com.github.c0c0tier.dungeon_defenders.init.MapPreviews;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.ModBlocks;
import com.github.c0c0tier.dungeon_defenders.init.PhaseTransitions;
import com.github.c0c0tier.dungeon_defenders.init.ScoreKeeper;
import com.github.c0c0tier.dungeon_defenders.init.ScoreSource;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import com.github.c0c0tier.dungeon_defenders.init.TowerStats;
import com.github.c0c0tier.dungeon_defenders.init.TowerStatsRegistry;
import com.github.c0c0tier.dungeon_defenders.item.HeroWeaponItem;
import com.github.c0c0tier.dungeon_defenders.item.HeroWeapons;
import com.github.c0c0tier.dungeon_defenders.loot.Equipment;
import com.github.c0c0tier.dungeon_defenders.loot.LootGenerator;
import com.github.c0c0tier.dungeon_defenders.loot.LootRarity;
import com.github.c0c0tier.dungeon_defenders.loot.LootWeapon;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

// Premiers gametests du mod : vérifient de la logique pure de block entity/attachments, sans
// dépendre d'un monstre, d'un joueur ou de plusieurs ticks — les scénarios les plus simples à
// rendre fiables (pas de minuteur, pas d'IA, résultat connu dès le premier tick). Tournent via
// `./gradlew gameTestServer` (voir doc/03-build-et-lancement.md) ou `/test runall` en jeu.
// Structure partagée par les deux : data/dungeon_defenders/structure/gametest/empty.nbt, un
// gabarit 3x3x3 sans le moindre bloc (juste une zone délimitée) — aucun des deux tests n'a
// besoin d'un sol ou d'un décor existant, ils posent/lisent leurs blocs eux-mêmes via
// GameTestHelper, qui n'a pas besoin d'appui pour placer un bloc (contrairement à un joueur qui
// clique, pas de vérification canSurvive).
@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public final class DungeonDefendersGameTests {

    private static final Identifier EMPTY_STRUCTURE =
            Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "gametest/empty");
    private static final int MAX_TICKS = 20;

    private DungeonDefendersGameTests() {
    }

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        // Pas de règle de jeu particulière à activer/désactiver pour ces deux tests (pas de
        // temps qui passe, pas de mob) : un environnement vide suffit.
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "default"));

        event.registerTest(
                Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "eternia_crystal_damage"),
                ModGameTestInstance.create(
                        Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "eternia_crystal_damage"),
                        DungeonDefendersGameTests::eterniaCrystalDamage,
                        new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, 0, true)));

        event.registerTest(
                Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "phase_transitions"),
                ModGameTestInstance.create(
                        Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "phase_transitions"),
                        DungeonDefendersGameTests::phaseTransitions,
                        new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, 0, true)));
        // Passage des ennemis en data-driven (doc/data-driven/ennemis.md) : logique pure,
        // même principe que les deux tests ci-dessus.
        register(event, environment, "enemy_legacy_format", DungeonDefendersGameTests::enemyLegacyFormat);
        register(event, environment, "enemy_shipped_values", DungeonDefendersGameTests::enemyShippedValues);
        register(event, environment, "enemy_unknown_excluded_from_wave", DungeonDefendersGameTests::enemyUnknownExcludedFromWave);
        register(event, environment, "enemy_export_conversion", DungeonDefendersGameTests::enemyExportConversion);
        register(event, environment, "spawner_wave_step", DungeonDefendersGameTests::spawnerWaveStep);
        register(event, environment, "map_preview_storage", DungeonDefendersGameTests::mapPreviewStorage);
        register(event, environment, "enemy_variants", DungeonDefendersGameTests::enemyVariants);
        register(event, environment, "mana_chest_respawn", DungeonDefendersGameTests::manaChestRespawn);
        register(event, environment, "world_time_frozen", DungeonDefendersGameTests::worldTimeFrozen);
        register(event, environment, "monsters_do_not_burn", DungeonDefendersGameTests::monstersDoNotBurn);
        register(event, environment, "score_bonuses", DungeonDefendersGameTests::scoreBonuses);
        register(event, environment, "combat_countdown", DungeonDefendersGameTests::combatCountdown);
        // Stats des tours en data-driven (doc/data-driven/tours.md).
        register(event, environment, "tower_shipped_values", DungeonDefendersGameTests::towerShippedValues);
        register(event, environment, "tower_stats_validation", DungeonDefendersGameTests::towerStatsValidation);
        register(event, environment, "tower_upgrade", DungeonDefendersGameTests::towerUpgrade);
        // Héros Alchimiste et ses tours (doc/02-gameplay.md).
        register(event, environment, "hero_rosters", DungeonDefendersGameTests::heroRosters);
        register(event, environment, "alchemist_support_towers", DungeonDefendersGameTests::alchemistSupportTowers);
        register(event, environment, "alchemist_damage_towers", DungeonDefendersGameTests::alchemistDamageTowers);
        register(event, environment, "engineer_towers", DungeonDefendersGameTests::engineerTowers);
        register(event, environment, "geomancer_towers", DungeonDefendersGameTests::geomancerTowers);
        // Armes de héros (doc/02-gameplay.md).
        register(event, environment, "hero_weapons", DungeonDefendersGameTests::heroWeapons);
        register(event, environment, "hero_stat_points", DungeonDefendersGameTests::heroStatPoints);
        // Butin et inventaire de la Forge (doc/02-gameplay.md).
        register(event, environment, "loot_generation", DungeonDefendersGameTests::lootGeneration);
        register(event, environment, "loot_equipment", DungeonDefendersGameTests::lootEquipment);
        // Réglages globaux en data-driven (doc/data-driven/equilibrage.md).
        register(event, environment, "balance_shipped_values", DungeonDefendersGameTests::balanceShippedValues);
        register(event, environment, "balance_validation", DungeonDefendersGameTests::balanceValidation);
        // Monstres de l'Atelier (doc/ATELIER.md) : plus long, pour laisser l'animation d'attaque arriver à son impact.
        Identifier atelierId = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "atelier_monsters");
        event.registerTest(atelierId, ModGameTestInstance.create(atelierId, DungeonDefendersGameTests::atelierMonsters,
                new TestData<>(environment, EMPTY_STRUCTURE, ATELIER_MAX_TICKS, 0, true)));
    }

    private static final int ATELIER_MAX_TICKS = 200;

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
                                 String name, Consumer<GameTestHelper> test) {
        Identifier id = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, name);
        event.registerTest(id, ModGameTestInstance.create(id, test, new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, 0, true)));
    }

    /**
     * Le Cristal d'Eternia démarre à Config.DEFAULT_HEALTH PV, les encaisse correctement, et le
     * bloc disparaît une fois ses PV à 0 (voir EterniaCrystalBlockEntity#setCrystalHealth).
     */
    static void eterniaCrystalDamage(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ETERNIA_CRYSTAL.get());

        EterniaCrystalBlockEntity crystal = helper.getBlockEntity(pos, EterniaCrystalBlockEntity.class);
        int defaultHealth = Config.DEFAULT_HEALTH.get();
        helper.assertValueEqual(crystal.getCrystalHealth(), defaultHealth, "PV de départ du cristal");

        crystal.damage(10);
        helper.assertValueEqual(crystal.getCrystalHealth(), defaultHealth - 10, "PV du cristal après 10 dégâts");

        crystal.damage(defaultHealth);
        helper.assertBlockNotPresent(ModBlocks.ETERNIA_CRYSTAL.get(), pos);

        helper.succeed();
    }

    /**
     * enterCombat()/enterBuild() (PhaseTransitions) mettent bien à jour GAME_PHASE, et
     * enterBuild() remet WAVE_ENEMIES_KILLED à 0 — la régression corrigée le 2026-08-23 (voir
     * 05-etat-et-problemes-connus.md, "Corrections trouvées lors des tests en jeu").
     */
    static void phaseTransitions(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        PhaseTransitions.enterCombat(level);
        helper.assertValueEqual(level.getData(ModAttachments.GAME_PHASE), GamePhase.COMBAT.ordinal(), "phase après enterCombat");

        level.setData(ModAttachments.WAVE_ENEMIES_KILLED, 7);

        PhaseTransitions.enterBuild(level);
        helper.assertValueEqual(level.getData(ModAttachments.GAME_PHASE), GamePhase.BUILD.ordinal(), "phase après enterBuild");
        helper.assertValueEqual(level.getData(ModAttachments.WAVE_ENEMIES_KILLED), 0, "compteur de tués après enterBuild");

        helper.succeed();
    }

    /**
     * Lecture compatible de l'ancien format des spawners (doc/data-driven/ennemis.md, §5) :
     * nombre -> identifiant via la table figée, avec marqueur de conversion ; identifiant lu tel
     * quel ; ordinal hors table -> legacy_unknown_N ; réécriture toujours en chaîne.
     */
    static void enemyLegacyFormat(GameTestHelper helper) {
        SpawnerBlockEntity.SpawnEntry zombie = parseEntry(helper, IntTag.valueOf(0));
        helper.assertValueEqual(zombie.enemy(), LegacyEnemyIds.ZOMBIE, "ancien 0");
        helper.assertValueEqual(zombie.convertedFromOrdinal(), OptionalInt.of(0), "0 marqué converti");

        SpawnerBlockEntity.SpawnEntry skeleton = parseEntry(helper, IntTag.valueOf(1));
        helper.assertValueEqual(skeleton.enemy(), LegacyEnemyIds.SKELETON, "ancien 1");

        SpawnerBlockEntity.SpawnEntry modern = parseEntry(helper, StringTag.valueOf("dungeon_defenders:skeleton"));
        helper.assertValueEqual(modern.enemy(), LegacyEnemyIds.SKELETON, "nouveau format");
        helper.assertValueEqual(modern.convertedFromOrdinal(), OptionalInt.empty(), "nouveau format non marqué");

        SpawnerBlockEntity.SpawnEntry unknown = parseEntry(helper, IntTag.valueOf(7));
        helper.assertValueEqual(unknown.enemy().toString(), "dungeon_defenders:legacy_unknown_7", "ordinal hors table");

        Tag written = SpawnerBlockEntity.SpawnEntry.CODEC.encodeStart(NbtOps.INSTANCE, zombie).getOrThrow();
        helper.assertTrue(written instanceof CompoundTag compound && compound.get("Enemy") instanceof StringTag,
                "réécriture toujours au nouveau format (chaîne)");
        helper.succeed();
    }

    /**
     * Les deux JSON livrés reprennent exactement les valeurs d'avant la migration (contrainte
     * "aucune valeur de gameplay ne change") : XP 10/15, mêlée pour le zombie, tir 3 dégâts /
     * 20 ticks / 10 blocs pour le squelette.
     */
    static void enemyShippedValues(GameTestHelper helper) {
        EnemyDefinition zombie = EnemyRegistry.byId(LegacyEnemyIds.ZOMBIE);
        EnemyDefinition skeleton = EnemyRegistry.byId(LegacyEnemyIds.SKELETON);
        helper.assertTrue(zombie != null && skeleton != null, "les deux JSON livrés sont chargés");

        helper.assertValueEqual(zombie.entityType(), EntityType.ZOMBIE, "type du zombie");
        helper.assertValueEqual(zombie.xpValue(), 10, "XP du zombie");
        helper.assertValueEqual(zombie.behavior(), EnemyBehavior.MeleePriority.INSTANCE, "IA du zombie");

        helper.assertValueEqual(skeleton.entityType(), EntityType.SKELETON, "type du squelette");
        helper.assertValueEqual(skeleton.xpValue(), 15, "XP du squelette");
        helper.assertValueEqual(skeleton.behavior(), new EnemyBehavior.RangedCrystal(3, 20, 10.0D), "IA du squelette");

        helper.assertValueEqual(EnemyRegistry.xpValueFor(EntityType.DROWNED), EnemyRegistry.DEFAULT_XP_VALUE, "XP d'un monstre sans JSON");
        helper.succeed();
    }

    /**
     * Un ennemi inconnu est exclu du total de vague : sinon la vague attendrait des monstres qui
     * ne viendront jamais (le symptôme corrigé le 2026-09-12).
     */
    static void enemyUnknownExcludedFromWave(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SPAWNER.get());
        SpawnerBlockEntity spawner = helper.getBlockEntity(pos, SpawnerBlockEntity.class);

        level.setData(ModAttachments.CURRENT_WAVE, 1);
        spawner.applyConfig(20, 0, 1, 1, 1, List.of(
                new SpawnerBlockEntity.SpawnEntry(LegacyEnemyIds.ZOMBIE, 4),
                new SpawnerBlockEntity.SpawnEntry(Identifier.fromNamespaceAndPath("pack_absent", "goblin"), 6)));

        PhaseTransitions.recomputeWaveEnemiesTotal(level);
        int expected = Math.max(1, (int) Math.round(4 * DifficultyScaling.getMultiplier(level)));
        helper.assertValueEqual(level.getData(ModAttachments.WAVE_ENEMIES_TOTAL), expected, "total sans l'ennemi inconnu");
        helper.succeed();
    }

    /**
     * Périodicité d'un spawner : « une vague sur N » à partir de sa première vague, dans sa
     * plage. Un pas de 1 (ou absent d'une ancienne map) redonne le comportement d'avant.
     */
    static void spawnerWaveStep(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SPAWNER.get());
        SpawnerBlockEntity spawner = helper.getBlockEntity(pos, SpawnerBlockEntity.class);

        // Vague de boss toutes les 4 vagues, de la 4 à la 12.
        spawner.applyConfig(20, 0, 4, 12, 4, List.of(new SpawnerBlockEntity.SpawnEntry(LegacyEnemyIds.ZOMBIE, 1)));
        for (int wave = 1; wave <= 14; wave++) {
            boolean expected = wave == 4 || wave == 8 || wave == 12;
            helper.assertValueEqual(spawner.isActiveOnWave(wave), expected, "vague " + wave + " avec un pas de 4");
        }

        // Pas de 1 : toute la plage, rien en dehors. Un pas nul ou négatif est ramené à 1.
        spawner.applyConfig(20, 0, 2, 5, 0, List.of(new SpawnerBlockEntity.SpawnEntry(LegacyEnemyIds.ZOMBIE, 1)));
        helper.assertValueEqual(spawner.getWaveStep(), 1, "pas ramené à 1");
        for (int wave = 1; wave <= 6; wave++) {
            helper.assertValueEqual(spawner.isActiveOnWave(wave), wave >= 2 && wave <= 5, "vague " + wave + " avec un pas de 1");
        }
        helper.succeed();
    }

    /**
     * Rangement des aperçus de map côté serveur : une image valide fait l'aller-retour, tout le
     * reste est refusé (mauvaise signature, trop lourde, identifiant qui n'est pas une map ou
     * qui sortirait du dossier des aperçus).
     */
    static void mapPreviewStorage(GameTestHelper helper) {
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        byte[] png = new byte[64];
        byte[] signature = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(signature, 0, png, 0, signature.length);

        Identifier map = Identifier.fromNamespaceAndPath("gametest_pack", "map/apercu_de_test");
        helper.assertTrue(MapPreviews.load(server, map).isEmpty(), "aucun aperçu avant l'enregistrement");
        helper.assertTrue(MapPreviews.save(server, map, png), "image valide acceptée");
        helper.assertTrue(java.util.Arrays.equals(MapPreviews.load(server, map).orElse(null), png), "aller-retour identique");
        try {
            java.nio.file.Files.deleteIfExists(MapPreviews.file(server, map).orElseThrow());
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }

        helper.assertFalse(MapPreviews.save(server, map, new byte[64]), "signature PNG absente");
        byte[] tooLarge = new byte[MapPreviews.MAX_BYTES + 1];
        System.arraycopy(signature, 0, tooLarge, 0, signature.length);
        helper.assertFalse(MapPreviews.save(server, map, tooLarge), "image trop lourde");
        helper.assertFalse(MapPreviews.save(server, Identifier.fromNamespaceAndPath("gametest_pack", "taverne"), png),
                "identifiant qui n'est pas une map");
        helper.assertTrue(MapPreviews.file(server, Identifier.fromNamespaceAndPath("gametest_pack", "map/../../../evasion")).isEmpty(),
                "chemin sortant du dossier des aperçus");
        helper.succeed();
    }

    /** /dd_export réécrit les spawners à l'ancien format, et seulement eux. */
    static void enemyExportConversion(GameTestHelper helper) {
        CompoundTag legacyEntry = new CompoundTag();
        legacyEntry.putInt("Enemy", 1);
        CompoundTag modernEntry = new CompoundTag();
        modernEntry.putString("Enemy", "dungeon_defenders:zombie");
        ListTag entries = new ListTag();
        entries.add(legacyEntry);
        entries.add(modernEntry);

        CompoundTag spawnerNbt = new CompoundTag();
        spawnerNbt.putString("id", "dungeon_defenders:spawner");
        spawnerNbt.put("Entries", entries);
        CompoundTag block = new CompoundTag();
        block.put("nbt", spawnerNbt);
        ListTag blocks = new ListTag();
        blocks.add(block);
        CompoundTag structure = new CompoundTag();
        structure.put("blocks", blocks);

        int converted = LegacyEnemyIds.convertStructure(structure, message -> { });
        helper.assertValueEqual(converted, 1, "une seule entrée à l'ancien format");
        helper.assertValueEqual(legacyEntry.getString("Enemy").orElse(""), "dungeon_defenders:skeleton", "1 -> squelette");
        helper.assertValueEqual(modernEntry.getString("Enemy").orElse(""), "dungeon_defenders:zombie", "nouveau format intact");
        helper.succeed();
    }

    /**
     * Les JSON de tours livrés sont chargés et reprennent exactement les valeurs par défaut
     * du code, c'est-à-dire celles d'avant la migration ("aucune valeur de gameplay ne change").
     * Vérifie aussi qu'une tour posée lit bien ses chiffres par ce chemin.
     */
    static void towerShippedValues(GameTestHelper helper) {
        for (TowerDefinition tower : TowerDefinition.values()) {
            helper.assertTrue(TowerStatsRegistry.hasLoadedFile(tower), "JSON chargé pour " + tower.id());
            helper.assertValueEqual(tower.stats(false), tower.defaultStats(), "stats de " + tower.id());
        }

        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SPIKE_BLOCKADE.get());
        AbstractTowerBlockEntity spike = helper.getBlockEntity(pos, AbstractTowerBlockEntity.class);
        helper.assertValueEqual(spike.getMaxHealth(), 30, "PV max du Spike Blockade");
        helper.assertValueEqual(spike.getHealth(), 30, "PV de départ du Spike Blockade");
        helper.assertValueEqual(spike.getManaCost(), 30, "coût du Spike Blockade");
        helper.succeed();
    }

    /**
     * Un JSON de tour partiel garde les valeurs par défaut des stats absentes ; une stat
     * inconnue, une stat que la tour n'utilise pas, une valeur non numérique, non entière ou
     * sous son minimum sont refusées (la tour garde alors ses valeurs par défaut).
     */
    static void towerStatsValidation(GameTestHelper helper) {
        TowerStats defaults = TowerDefinition.SPIKE_BLOCKADE.defaultStats();

        TowerStats partial = TowerStats.parse(JsonParser.parseString("{\"mana_cost\": 45}"), defaults).getOrThrow();
        helper.assertValueEqual(partial.intValue(TowerStat.MANA_COST), 45, "stat surchargée");
        helper.assertValueEqual(partial.intValue(TowerStat.MAX_HEALTH), 30, "stat absente = défaut");

        for (String invalid : List.of(
                "{\"mana_cots\": 45}",          // faute de frappe
                "{\"range\": 12}",              // stat de tourelle sur une blockade
                "{\"mana_cost\": \"cher\"}",    // pas un nombre
                "{\"max_health\": 12.5}",       // doit être entier
                "{\"max_health\": 0}",          // sous le minimum
                "[30, 30]")) {                  // pas un objet
            helper.assertTrue(TowerStats.parse(JsonParser.parseString(invalid), defaults).isError(), "refusé : " + invalid);
        }
        helper.succeed();
    }

    /**
     * Les JSON d'équilibrage livrés reprennent exactement les valeurs d'avant la migration, et
     * le multiplicateur de difficulté qui en découle est inchangé (Normal, vague 3 : x1.2).
     */
    static void balanceShippedValues(GameTestHelper helper) {
        for (BalanceValue value : BalanceValue.values()) {
            helper.assertValueEqual(GameBalance.get(value, false), value.defaultValue(), "réglage " + value.networkName());
        }
        helper.assertValueEqual(GameDifficulty.EASY.multiplier(false), 0.75D, "Facile");
        helper.assertValueEqual(GameDifficulty.NORMAL.multiplier(false), 1.0D, "Normal");
        helper.assertValueEqual(GameDifficulty.HARD.multiplier(false), 1.5D, "Difficile");

        ServerLevel level = helper.getLevel();
        int previousWave = level.getData(ModAttachments.CURRENT_WAVE);
        int previousDifficulty = level.getData(ModAttachments.DIFFICULTY);
        level.setData(ModAttachments.CURRENT_WAVE, 3);
        level.setData(ModAttachments.DIFFICULTY, GameDifficulty.NORMAL.ordinal());
        helper.assertTrue(Math.abs(DifficultyScaling.getMultiplier(level) - 1.2D) < 1.0E-9D, "Normal, vague 3 : x1.2");
        level.setData(ModAttachments.CURRENT_WAVE, previousWave);
        level.setData(ModAttachments.DIFFICULTY, previousDifficulty);
        helper.succeed();
    }

    /** Un fichier d'équilibrage est refusé en entier à la moindre clé ou valeur invalide. */
    static void balanceValidation(GameTestHelper helper) {
        Map<BalanceValue, Double> parsed = GameBalance.parseFile("difficulty",
                JsonParser.parseString("{\"hard_multiplier\": 2.0}")).getOrThrow();
        helper.assertValueEqual(parsed, Map.of(BalanceValue.DIFFICULTY_HARD_MULTIPLIER, 2.0D), "clé seule surchargée");

        helper.assertTrue(GameBalance.parseFile("difficulty", JsonParser.parseString("{\"hard_multiplierr\": 2.0}")).isError(), "clé inconnue");
        helper.assertTrue(GameBalance.parseFile("difficulty", JsonParser.parseString("{\"hard_multiplier\": 0}")).isError(), "sous le minimum");
        helper.assertTrue(GameBalance.parseFile("progression", JsonParser.parseString("{\"xp_per_level\": 99.5}")).isError(), "doit être entier");
        helper.assertTrue(GameBalance.parseFile("difficulte", JsonParser.parseString("{}")).isError(), "fichier inconnu");
        helper.succeed();
    }

    /**
     * Un coffre de mana posé reste dans le registre ACTIVE_MANA_CHESTS (il en disparaissait quand
     * son block entity en remplaçait un autre à la même position, bug corrigé le 2026-11-10), et
     * respawnAll le referme une fois ouvert.
     */
    static void manaChestRespawn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockPos absolute = helper.absolutePos(pos);
        helper.setBlock(pos, ModBlocks.MANA_CHEST.get());
        helper.assertTrue(level.getData(ModAttachments.ACTIVE_MANA_CHESTS).contains(absolute), "coffre enregistré");

        // Le scénario du bug : un block entity en remplace un autre à la même position, comme
        // quand une structure de map pose le sien par-dessus celui créé avec le bloc.
        level.setBlockEntity(new ManaChestBlockEntity(absolute, level.getBlockState(absolute)));
        helper.assertTrue(level.getData(ModAttachments.ACTIVE_MANA_CHESTS).contains(absolute), "toujours enregistré après remplacement");

        level.setBlock(absolute, level.getBlockState(absolute).setValue(ManaChestBlock.OPENED, true), Block.UPDATE_ALL);
        helper.assertTrue(level.getData(ModAttachments.ACTIVE_MANA_CHESTS).contains(absolute), "toujours enregistré une fois ouvert");

        ManaChestBlock.respawnAll(level);
        helper.assertFalse(level.getBlockState(absolute).getValue(ManaChestBlock.OPENED), "coffre refermé par respawnAll");
        helper.succeed();
    }

    /**
     * L'heure du monde se fige à la valeur demandée (heure propre d'une map), retombe sur
     * l'heure par défaut de la config pour une valeur absente ou hors plage, et un bloc de
     * configuration de map transporte bien son heure.
     */
    static void worldTimeFrozen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<WorldClock> clock = level.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);

        WorldTime.freezeAt(level, 18000);
        helper.assertValueEqual(level.clockManager().getTotalTicks(clock), 18000L, "heure propre à la map");

        WorldTime.freezeAt(level, 99999);
        helper.assertValueEqual(level.clockManager().getTotalTicks(clock), (long) Config.DEFAULT_TIME_OF_DAY.get(), "hors plage = défaut");

        WorldTime.freezeAt(level, WorldTime.USE_DEFAULT);
        helper.assertValueEqual(level.clockManager().getTotalTicks(clock), (long) Config.DEFAULT_TIME_OF_DAY.get(), "heure par défaut");

        Identifier mapId = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "map/test");
        CompoundTag config = new CompoundTag();
        config.putInt("TimeOfDay", 13000);
        helper.assertValueEqual(MapConfigBlockEntity.toDefinition(mapId, config, false).timeOfDay(), 13000, "heure lue dans le bloc de config");
        config.putInt("TimeOfDay", 24000);
        helper.assertValueEqual(MapConfigBlockEntity.toDefinition(mapId, config, false).timeOfDay(),
                MapDefinition.DEFAULT_TIME_OF_DAY, "heure invalide = pas d'heure propre");
        helper.assertValueEqual(MapConfigBlockEntity.toDefinition(mapId, new CompoundTag(), false).timeOfDay(),
                MapDefinition.DEFAULT_TIME_OF_DAY, "map d'avant = pas d'heure propre");
        helper.succeed();
    }

    /**
     * Bonus de fin de vague et ses multiplicateurs "aucun dégât" (valeurs livrées : 50, x1.5,
     * x1.5), multiplicateur de difficulté et multiplicateur de la map sur tout gain de score.
     */
    static void scoreBonuses(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int previousScore = level.getData(ModAttachments.SCORE);
        int previousDifficulty = level.getData(ModAttachments.DIFFICULTY);
        float previousMapMultiplier = level.getData(ModAttachments.MAP_SCORE_MULTIPLIER);
        level.setData(ModAttachments.DIFFICULTY, GameDifficulty.NORMAL.ordinal());
        level.setData(ModAttachments.MAP_SCORE_MULTIPLIER, 1.0F);

        // Sans le multiplicateur "aucune tour perdue" pour les trois premiers cas.
        level.setData(ModAttachments.WAVE_TOWER_LOST, true);

        // Vague parfaite : 50, +25 (aucun dégât subi), +37.5 arrondi à 38 (cristal intact).
        level.setData(ModAttachments.SCORE, 0);
        level.setData(ModAttachments.WAVE_PLAYER_DAMAGED, false);
        level.setData(ModAttachments.WAVE_CRYSTAL_DAMAGED, false);
        ScoreKeeper.awardWaveCleared(level);
        helper.assertValueEqual(level.getData(ModAttachments.SCORE), 113, "vague parfaite");

        // Joueur blessé, cristal intact : 50 + 25.
        level.setData(ModAttachments.SCORE, 0);
        level.setData(ModAttachments.WAVE_PLAYER_DAMAGED, true);
        ScoreKeeper.awardWaveCleared(level);
        helper.assertValueEqual(level.getData(ModAttachments.SCORE), 75, "joueur blessé, cristal intact");

        // Les deux touchés : le bonus de base seul.
        level.setData(ModAttachments.SCORE, 0);
        level.setData(ModAttachments.WAVE_CRYSTAL_DAMAGED, true);
        ScoreKeeper.awardWaveCleared(level);
        helper.assertValueEqual(level.getData(ModAttachments.SCORE), 50, "aucun multiplicateur");

        // Aucune tour perdue, les deux autres touchés : 50 + 12.5 arrondi à 13.
        level.setData(ModAttachments.SCORE, 0);
        level.setData(ModAttachments.WAVE_TOWER_LOST, false);
        ScoreKeeper.awardWaveCleared(level);
        helper.assertValueEqual(level.getData(ModAttachments.SCORE), 63, "aucune tour perdue");

        // Un kill à 10 : inchangé en Normal, x1.5 en Difficile, puis x2 par la map.
        level.setData(ModAttachments.SCORE, 0);
        helper.assertValueEqual(ScoreKeeper.grant(level, 10, ScoreSource.MONSTER_KILLED, Optional.empty()), 10, "kill en Normal");
        level.setData(ModAttachments.DIFFICULTY, GameDifficulty.HARD.ordinal());
        helper.assertValueEqual(ScoreKeeper.grant(level, 10, ScoreSource.MONSTER_KILLED, Optional.empty()), 15, "kill en Difficile");
        level.setData(ModAttachments.MAP_SCORE_MULTIPLIER, 2.0F);
        helper.assertValueEqual(ScoreKeeper.grant(level, 10, ScoreSource.MONSTER_KILLED, Optional.empty()), 30, "kill en Difficile, map x2");

        level.setData(ModAttachments.SCORE, previousScore);
        level.setData(ModAttachments.DIFFICULTY, previousDifficulty);
        level.setData(ModAttachments.MAP_SCORE_MULTIPLIER, previousMapMultiplier);
        level.setData(ModAttachments.WAVE_PLAYER_DAMAGED, false);
        level.setData(ModAttachments.WAVE_CRYSTAL_DAMAGED, false);
        helper.succeed();
    }

    /** Le vote unanime lance un décompte (3 s livrées = 60 ticks) qu'un vote retiré annule. */
    static void combatCountdown(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertValueEqual(GameBalance.getInt(BalanceValue.COMBAT_COUNTDOWN_SECONDS, false), 3, "durée livrée");

        CombatCountdown.start(level);
        helper.assertValueEqual(level.getData(ModAttachments.COMBAT_COUNTDOWN), 60, "décompte lancé");
        helper.assertTrue(CombatCountdown.isRunning(level), "décompte en cours");

        CombatCountdown.cancel(level);
        helper.assertValueEqual(level.getData(ModAttachments.COMBAT_COUNTDOWN), 0, "décompte annulé");
        helper.succeed();
    }

    /**
     * Zombies et squelettes ne brûlent jamais au soleil : avec l'heure figée (midi par défaut),
     * ils prendraient feu en permanence sur une map à ciel ouvert. Le jeu lit pour ça l'attribut
     * d'environnement MONSTERS_BURN, que le mod neutralise (voir data/minecraft/timeline/day.json).
     */
    static void monstersDoNotBurn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 position = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        for (int time : new int[] {0, 6000, 12000, 18000}) {
            WorldTime.freezeAt(level, time);
            helper.assertFalse(level.environmentAttributes().getValue(EnvironmentAttributes.MONSTERS_BURN, position),
                    "pas de brûlure au soleil à l'heure " + time);
        }
        WorldTime.freezeAt(level, WorldTime.USE_DEFAULT);
        helper.succeed();
    }

    /**
     * Deux ennemis peuvent partager un type de mob : une "brute" est un zombie, mais avec ses
     * propres stats, sa propre XP et sa propre IA, parce qu'elle est marquée de son identifiant
     * à l'apparition. Un zombie apparu sans spawner reste, lui, l'ennemi "zombie".
     */
    static void enemyVariants(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Identifier bruteId = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "brute");
        EnemyDefinition brute = EnemyRegistry.byId(bruteId);
        helper.assertTrue(brute != null, "la brute livrée est chargée");
        helper.assertValueEqual(brute.entityType(), EntityType.ZOMBIE, "la brute est un zombie");
        helper.assertValueEqual(EnemyRegistry.idFor(EntityType.ZOMBIE), Optional.of(LegacyEnemyIds.ZOMBIE), "repli par type = zombie");

        Entity spawned = brute.spawn(level, bruteId, helper.absolutePos(new BlockPos(1, 1, 1)));
        helper.assertTrue(spawned instanceof Zombie, "une brute apparaît");
        Zombie zombie = (Zombie) spawned;
        helper.assertValueEqual(zombie.getMaxHealth(), 60.0F, "PV max de la brute");
        helper.assertValueEqual(zombie.getHealth(), 60.0F, "PV de départ de la brute");
        helper.assertValueEqual(EnemyRegistry.idFor(zombie), Optional.of(bruteId), "identité de la brute");
        helper.assertValueEqual(EnemyRegistry.xpValueFor(zombie), 40, "XP de la brute");
        helper.assertTrue(zombie.goalSelector.getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal() instanceof AttackPriorityTargetGoal), "IA de mêlée reçue");
        zombie.discard();
        helper.succeed();
    }

    /**
     * Amélioration d'une tour avec les réglages livrés (4 niveaux, +25 % de PV/dégâts par niveau,
     * un niveau = 75 % du coût de pose) : un Spike Blockade (30 PV, 30 de mana) monte à 38, 45
     * puis 53 PV, chaque niveau coûte 23, et tout le mana investi est compté pour le
     * remboursement.
     */
    static void towerUpgrade(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SPIKE_BLOCKADE.get());
        AbstractTowerBlockEntity tower = helper.getBlockEntity(pos, AbstractTowerBlockEntity.class);

        helper.assertValueEqual(tower.getTowerLevel(), 1, "niveau à la pose");
        helper.assertValueEqual(tower.getMaxTowerLevel(), 4, "niveau maximum");
        helper.assertValueEqual(tower.getUpgradeCost(), 23, "prix d'un niveau");
        helper.assertValueEqual(tower.getInvestedMana(), 30, "mana investi à la pose");

        tower.damage(10);
        helper.assertTrue(tower.upgrade(), "amélioration au niveau 2");
        helper.assertValueEqual(tower.getMaxHealth(), 38, "PV max au niveau 2");
        helper.assertValueEqual(tower.getHealth(), 28, "PV actuels : les 8 PV gagnés s'ajoutent, pas de soin complet");

        helper.assertTrue(tower.upgrade(), "amélioration au niveau 3");
        helper.assertTrue(tower.upgrade(), "amélioration au niveau 4");
        helper.assertValueEqual(tower.getTowerLevel(), 4, "niveau atteint");
        helper.assertValueEqual(tower.getMaxHealth(), 53, "PV max au niveau 4");
        helper.assertValueEqual(tower.getInvestedMana(), 99, "mana investi : pose + 3 niveaux");
        helper.assertFalse(tower.upgrade(), "pas d'amélioration au-delà du maximum");
        helper.succeed();
    }

    /**
     * Chaque tour du catalogue appartient à exactement un héros, chaque héros en a six, et le
     * bloc de chaque tour crée bien un block entity de CETTE tour (une erreur de câblage dans
     * ModBlocks ou DungeonDefendersMod donnerait les stats d'une autre).
     */
    static void heroRosters(GameTestHelper helper) {
        for (TowerDefinition tower : TowerDefinition.values()) {
            long owners = java.util.Arrays.stream(HeroDefinition.values()).filter(hero -> hero.canPlace(tower)).count();
            helper.assertValueEqual(owners, 1L, "nombre de héros pouvant poser " + tower.id());

            BlockPos pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, tower.block());
            AbstractTowerBlockEntity blockEntity = helper.getBlockEntity(pos, AbstractTowerBlockEntity.class);
            helper.assertValueEqual(blockEntity.definition(), tower, "tour du block entity de " + tower.id());
            helper.assertValueEqual(blockEntity.getHealth(), tower.defaultStats().intValue(TowerStat.MAX_HEALTH),
                    "PV de départ de " + tower.id());
        }
        for (HeroDefinition hero : HeroDefinition.values()) {
            helper.assertValueEqual(hero.towers().size(), 6, "nombre de tours de " + hero.id());
        }
        helper.succeed();
    }

    /**
     * Tours de soutien de l'Alchimiste : l'Alambic givrant ralentit un monstre à portée ; le
     * Pylône catalyseur donne +25 % de dégâts à une tour voisine (pas à lui-même), un bonus plus
     * faible ne remplace pas un plus fort, et le bonus monte avec le niveau du pylône ; le
     * Condenseur de mana ne compte que pour les morts dans son rayon, et plus du tout une fois
     * retiré.
     */
    static void alchemistSupportTowers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        BlockPos stillPos = new BlockPos(1, 1, 1);
        helper.setBlock(stillPos, ModBlocks.FROST_STILL.get());
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 1));
        helper.getBlockEntity(stillPos, FrostStillBlockEntity.class).serverTick(level);
        MobEffectInstance slowness = zombie.getEffect(MobEffects.SLOWNESS);
        helper.assertTrue(slowness != null, "monstre ralenti par l'Alambic givrant");
        helper.assertValueEqual(slowness.getAmplifier(), 1, "Lenteur II (effect_level 2)");
        zombie.discard();

        BlockPos pylonPos = new BlockPos(1, 1, 3);
        BlockPos spikePos = new BlockPos(3, 1, 3);
        helper.setBlock(pylonPos, ModBlocks.CATALYST_PYLON.get());
        helper.setBlock(spikePos, ModBlocks.SPIKE_BLOCKADE.get());
        CatalystPylonBlockEntity pylon = helper.getBlockEntity(pylonPos, CatalystPylonBlockEntity.class);
        AbstractTowerBlockEntity spike = helper.getBlockEntity(spikePos, AbstractTowerBlockEntity.class);
        helper.assertValueEqual(spike.damageMultiplier(), 1.0D, "aucun bonus avant l'impulsion");
        pylon.serverTick(level);
        helper.assertValueEqual(spike.damageMultiplier(), 1.25D, "bonus du pylône sur la tour voisine");
        helper.assertValueEqual(pylon.damageMultiplier(), 1.0D, "le pylône ne se renforce pas lui-même");
        spike.applyDamageBoost(1.1D, level.getGameTime() + 100);
        helper.assertValueEqual(spike.damageMultiplier(), 1.25D, "un bonus plus faible ne remplace pas le plus fort");
        spike.applyDamageBoost(1.5D, level.getGameTime() + 100);
        helper.assertValueEqual(spike.damageMultiplier(), 1.5D, "un bonus plus fort l'emporte");

        BlockPos condenserPos = new BlockPos(5, 1, 1);
        helper.setBlock(condenserPos, ModBlocks.MANA_CONDENSER.get());
        Vec3 near = helper.absoluteVec(new Vec3(5.5D, 1.0D, 3.5D));
        Vec3 far = helper.absoluteVec(new Vec3(5.5D, 1.0D, 30.5D));
        helper.assertValueEqual(ManaCondenserBlockEntity.bonusManaAt(level, near), 5, "bonus de mana à portée");
        helper.assertValueEqual(ManaCondenserBlockEntity.bonusManaAt(level, far), 0, "aucun bonus hors de portée");
        helper.getBlockEntity(condenserPos, AbstractTowerBlockEntity.class).upgrade();
        helper.assertValueEqual(ManaCondenserBlockEntity.bonusManaAt(level, near), 6, "bonus au niveau 2 (5 x 1,25 arrondi)");
        helper.setBlock(condenserPos, net.minecraft.world.level.block.Blocks.AIR);
        helper.assertValueEqual(ManaCondenserBlockEntity.bonusManaAt(level, near), 0, "plus de bonus une fois le condenseur retiré");
        helper.succeed();
    }

    /**
     * Tours offensives de l'Alchimiste : la corrosion du Projecteur d'acide fait subir plus de
     * dégâts qu'à un monstre témoin, et seulement dans son cône ; la Cuve instable blesse ses
     * voisins quand elle est détruite au combat, pas quand on la retire ; le Lance-fioles pose
     * une flaque sur un monstre à portée.
     */
    static void alchemistDamageTowers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // Projecteur orienté au nord (par défaut) : le monstre au nord est arrosé, pas celui au sud.
        BlockPos sprayerPos = new BlockPos(3, 1, 4);
        helper.setBlock(sprayerPos, ModBlocks.ACID_SPRAYER.get());
        Zombie sprayed = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 1));
        Zombie behind = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 6));
        helper.getBlockEntity(sprayerPos, AcidSprayerBlockEntity.class).serverTick(level);
        helper.assertTrue(sprayed.getHealth() < sprayed.getMaxHealth(), "monstre dans le cône blessé");
        helper.assertValueEqual(behind.getHealth(), behind.getMaxHealth(), "monstre derrière le projecteur épargné");
        helper.assertValueEqual(Corrosion.factor(sprayed), 1.3D, "corrosion posée (+30 %)");
        helper.assertValueEqual(Corrosion.factor(behind), 1.0D, "pas de corrosion hors du cône");

        // Même coup sur un monstre corrodé et sur un témoin : le corrodé perd plus de PV.
        Zombie corroded = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(6, 1, 1));
        Zombie control = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(6, 1, 3));
        Corrosion.apply(corroded, 1.5D, level.getGameTime() + 100);
        corroded.hurtServer(level, level.damageSources().generic(), 6.0F);
        control.hurtServer(level, level.damageSources().generic(), 6.0F);
        float corrodedLoss = corroded.getMaxHealth() - corroded.getHealth();
        float controlLoss = control.getMaxHealth() - control.getHealth();
        helper.assertTrue(controlLoss > 0.0F, "le témoin est blessé");
        helper.assertTrue(Math.abs(corrodedLoss - controlLoss * 1.5F) < 0.01F,
                "corrodé : " + corrodedLoss + " PV perdus, attendu 1,5 x " + controlLoss);
        sprayed.discard();
        behind.discard();
        corroded.discard();
        control.discard();

        // Cuve instable : retirée à la main, rien ; détruite par des dégâts, explosion.
        BlockPos vatPos = new BlockPos(1, 1, 1);
        Zombie neighbour = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        helper.setBlock(vatPos, ModBlocks.VOLATILE_VAT.get());
        helper.setBlock(vatPos, net.minecraft.world.level.block.Blocks.AIR);
        helper.assertValueEqual(neighbour.getHealth(), neighbour.getMaxHealth(), "aucune explosion au retrait");
        helper.setBlock(vatPos, ModBlocks.VOLATILE_VAT.get());
        helper.getBlockEntity(vatPos, AbstractTowerBlockEntity.class).damage(1000);
        helper.assertTrue(neighbour.getHealth() < neighbour.getMaxHealth(), "explosion à la destruction au combat");
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.AIR, vatPos);
        neighbour.discard();

        BlockPos launcherPos = new BlockPos(5, 1, 5);
        helper.setBlock(launcherPos, ModBlocks.FLASK_LAUNCHER.get());
        FlaskLauncherBlockEntity launcher = helper.getBlockEntity(launcherPos, FlaskLauncherBlockEntity.class);
        launcher.serverTick(level);
        helper.assertValueEqual(launcher.activePuddles(), 0, "pas de tir sans cible");
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(5, 1, 2));
        // serverTick limite lui-même la cadence : on appelle pulse par le tick du jeu suivant.
        helper.runAfterDelay(12, () -> {
            helper.assertValueEqual(launcher.activePuddles(), 1, "une flaque posée sur la cible");
            target.discard();
            helper.succeed();
        });
    }

    /**
     * Tours de l'Ingénieur : la Barricade blindée se répare seule ; la Station de réparation
     * soigne sa voisine mais pas elle-même, sans dépasser les PV max ; un bouclier (Champ de
     * force) annule les dégâts ; la Bobine magnétique tire un monstre vers elle ; le Marteau-pilon
     * blesse et repousse.
     */
    static void engineerTowers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        BlockPos barricadePos = new BlockPos(1, 1, 1);
        helper.setBlock(barricadePos, ModBlocks.IRON_BARRICADE.get());
        IronBarricadeBlockEntity barricade = helper.getBlockEntity(barricadePos, IronBarricadeBlockEntity.class);
        barricade.damage(10);
        helper.assertValueEqual(barricade.getHealth(), 50, "barricade entamée");
        barricade.serverTick(level);
        helper.assertValueEqual(barricade.getHealth(), 51, "la barricade se répare seule");

        barricade.applyShield(level.getGameTime() + 100);
        barricade.damage(30);
        helper.assertValueEqual(barricade.getHealth(), 51, "aucun dégât sous bouclier");

        BlockPos stationPos = new BlockPos(1, 1, 4);
        BlockPos spikePos = new BlockPos(3, 1, 4);
        helper.setBlock(stationPos, ModBlocks.REPAIR_STATION.get());
        helper.setBlock(spikePos, ModBlocks.SPIKE_BLOCKADE.get());
        RepairStationBlockEntity station = helper.getBlockEntity(stationPos, RepairStationBlockEntity.class);
        AbstractTowerBlockEntity spike = helper.getBlockEntity(spikePos, AbstractTowerBlockEntity.class);
        station.damage(5);
        spike.damage(1);
        station.serverTick(level);
        helper.assertValueEqual(spike.getHealth(), 30, "tour voisine réparée, sans dépasser ses PV max");
        helper.assertValueEqual(station.getHealth(), 15, "la station ne se répare pas elle-même");

        BlockPos coilPos = new BlockPos(6, 1, 1);
        helper.setBlock(coilPos, ModBlocks.MAGNET_COIL.get());
        Zombie pulled = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(6, 1, 4));
        pulled.setOnGround(true);
        helper.getBlockEntity(coilPos, MagnetCoilBlockEntity.class).serverTick(level);
        helper.assertTrue(pulled.getDeltaMovement().z < 0.0D, "monstre tiré vers la bobine (z décroissant)");
        pulled.discard();

        BlockPos slammerPos = new BlockPos(4, 1, 7);
        helper.setBlock(slammerPos, ModBlocks.PISTON_SLAMMER.get());
        Zombie slammed = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 1, 5));
        slammed.setOnGround(true);
        helper.getBlockEntity(slammerPos, PistonSlammerBlockEntity.class).serverTick(level);
        helper.assertTrue(slammed.getHealth() < slammed.getMaxHealth(), "monstre blessé par le Marteau-pilon");
        helper.assertTrue(slammed.getDeltaMovement().z < 0.0D, "monstre repoussé loin du Marteau-pilon");
        slammed.discard();
        helper.succeed();
    }

    /**
     * Tours du Géomancien : la Haie de ronces blesse son voisin quand elle encaisse un coup ; le
     * Totem de boue enlise ; le Geyser blesse et projette en l'air ; le Lance-rochers choisit le
     * monstre le plus costaud, pas le plus proche ; l'éclair de la Flèche de cristal rebondit
     * avec des dégâts décroissants et n'atteint pas un monstre trop éloigné. La Pierre de vie
     * (soin des joueurs) n'est pas couverte ici : il faut un vrai joueur.
     */
    static void geomancerTowers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        BlockPos hedgePos = new BlockPos(1, 1, 1);
        helper.setBlock(hedgePos, ModBlocks.THORN_HEDGE.get());
        Zombie attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        helper.assertValueEqual(attacker.getHealth(), attacker.getMaxHealth(), "la haie ne pique pas d'elle-même");
        helper.getBlockEntity(hedgePos, AbstractTowerBlockEntity.class).damage(5);
        helper.assertTrue(attacker.getHealth() < attacker.getMaxHealth(), "la haie rend le coup");
        attacker.discard();

        BlockPos totemPos = new BlockPos(1, 1, 5);
        helper.setBlock(totemPos, ModBlocks.MUD_TOTEM.get());
        Zombie rooted = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 5));
        helper.getBlockEntity(totemPos, MudTotemBlockEntity.class).serverTick(level);
        MobEffectInstance root = rooted.getEffect(MobEffects.SLOWNESS);
        helper.assertTrue(root != null && root.getAmplifier() == 6, "monstre enlisé par le Totem de boue");
        rooted.discard();

        BlockPos geyserPos = new BlockPos(6, 1, 1);
        helper.setBlock(geyserPos, ModBlocks.GEYSER.get());
        Zombie launched = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(6, 1, 3));
        helper.getBlockEntity(geyserPos, GeyserBlockEntity.class).serverTick(level);
        helper.assertTrue(launched.getHealth() < launched.getMaxHealth(), "monstre blessé par le Geyser");
        helper.assertTrue(launched.getDeltaMovement().y > 0.5D, "monstre projeté en l'air");
        launched.discard();

        // Le monstre proche est affaibli, le lointain est intact : c'est le lointain qui est visé.
        BlockPos throwerPos = new BlockPos(10, 1, 1);
        helper.setBlock(throwerPos, ModBlocks.BOULDER_THROWER.get());
        Zombie weakNear = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(10, 1, 3));
        Zombie toughFar = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(10, 1, 8));
        weakNear.setHealth(8.0F);
        helper.getBlockEntity(throwerPos, BoulderThrowerBlockEntity.class).serverTick(level);
        helper.assertValueEqual(weakNear.getHealth(), 8.0F, "le monstre affaibli n'est pas visé");
        helper.assertTrue(toughFar.getHealth() < toughFar.getMaxHealth(), "le monstre le plus costaud est visé");
        weakNear.discard();
        toughFar.discard();

        // Trois monstres à 3 blocs d'intervalle (rebond de 4), un quatrième 9 blocs plus loin.
        BlockPos spirePos = new BlockPos(14, 1, 1);
        helper.setBlock(spirePos, ModBlocks.CRYSTAL_SPIRE.get());
        Zombie first = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(14, 1, 4));
        Zombie second = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(14, 1, 7));
        Zombie third = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(14, 1, 10));
        Zombie tooFar = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(14, 1, 19));
        helper.getBlockEntity(spirePos, CrystalSpireBlockEntity.class).serverTick(level);
        float firstLoss = first.getMaxHealth() - first.getHealth();
        float secondLoss = second.getMaxHealth() - second.getHealth();
        float thirdLoss = third.getMaxHealth() - third.getHealth();
        helper.assertTrue(firstLoss > secondLoss && secondLoss > thirdLoss && thirdLoss > 0.0F,
                "dégâts décroissants à chaque rebond : " + firstLoss + ", " + secondLoss + ", " + thirdLoss);
        helper.assertValueEqual(tooFar.getHealth(), tooFar.getMaxHealth(), "pas de rebond au-delà de la distance de saut");
        first.discard();
        second.discard();
        third.discard();
        tooFar.discard();
        helper.succeed();
    }

    // Fait voler un projectile d'arme jusqu'à son impact, sans attendre les ticks du serveur :
    // le test reste synchrone. 60 ticks couvrent largement la portée de chaque arme.
    private static void flyOut(com.github.c0c0tier.dungeon_defenders.entity.WeaponProjectileEntity projectile) {
        for (int i = 0; i < 60 && !projectile.isRemoved(); i++) {
            projectile.tick();
        }
    }

    /**
     * Armes de héros : chaque héros a la sienne ; changer de héros retire l'ancienne et donne la
     * nouvelle, en un seul exemplaire ; le Pistolet à rivets blesse le monstre visé ; le bonus de
     * niveau (+5 % par niveau) s'applique ; la Sacoche de fioles blesse une zone mais pas un
     * monstre éloigné ; le Bâton ralentit.
     */
    static void heroWeapons(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        java.util.Set<Item> weapons = new java.util.HashSet<>();
        for (HeroDefinition hero : HeroDefinition.values()) {
            helper.assertTrue(weapons.add(hero.weapon()), "arme propre à " + hero.id());
            helper.assertTrue(HeroWeapons.isHeroWeapon(hero.weapon()), "arme reconnue pour " + hero.id());
        }

        player.setData(ModAttachments.HERO, HeroDefinition.ENGINEER.ordinal());
        HeroWeapons.ensure(player);
        helper.assertValueEqual(player.getInventory().countItem(ModBlocks.ENGINEER_RIVET_GUN.get()), 1, "arme de l'Ingénieur donnée");
        player.setData(ModAttachments.HERO, HeroDefinition.ALCHEMIST.ordinal());
        HeroWeapons.ensure(player);
        HeroWeapons.ensure(player);
        helper.assertValueEqual(player.getInventory().countItem(ModBlocks.ENGINEER_RIVET_GUN.get()), 0, "ancienne arme retirée");
        helper.assertValueEqual(player.getInventory().countItem(ModBlocks.ALCHEMIST_FLASKS.get()), 1, "nouvelle arme en un seul exemplaire");

        // Le joueur regarde vers +z (lacet 0), à hauteur d'homme ; les monstres sont devant lui.
        Vec3 stand = helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D));
        player.setPos(stand.x, stand.y, stand.z);
        player.setYRot(0.0F);
        // Le regard d'un être vivant suit la rotation de sa TÊTE, laissée quelconque sur un
        // joueur factice : sans cette ligne le tir part de travers et le test échoue une fois
        // sur deux.
        player.setYHeadRot(0.0F);
        player.setXRot(0.0F);
        BlockPos front = new BlockPos(2, 1, 7);
        // La zone de test est entourée de barrières invisibles, qui arrêteraient le tir comme
        // n'importe quel bloc : on dégage un couloir devant le joueur.
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(1, 1, 0), new BlockPos(4, 3, 16))) {
            helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
        }

        HeroWeaponItem rivetGun = ModBlocks.ENGINEER_RIVET_GUN.get();
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, front);
        flyOut(rivetGun.fire(level, player));
        float baseLoss = target.getMaxHealth() - target.getHealth();
        helper.assertTrue(baseLoss > 0.0F, "le Pistolet à rivets blesse le monstre visé");
        target.discard();

        player.setData(ModAttachments.LEVEL, 5);
        Zombie second = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, front);
        flyOut(rivetGun.fire(level, player));
        float boostedLoss = second.getMaxHealth() - second.getHealth();
        helper.assertTrue(Math.abs(boostedLoss - baseLoss * 1.2F) < 0.01F,
                "niveau 5 : " + boostedLoss + " PV perdus, attendu 1,2 x " + baseLoss);
        second.discard();
        player.setData(ModAttachments.LEVEL, 1);

        Zombie center = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, front);
        Zombie beside = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 8));
        Zombie away = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 14));
        flyOut(ModBlocks.ALCHEMIST_FLASKS.get().fire(level, player));
        helper.assertTrue(center.getHealth() < center.getMaxHealth(), "fiole : monstre visé blessé");
        helper.assertTrue(beside.getHealth() < beside.getMaxHealth(), "fiole : voisin blessé (zone)");
        helper.assertValueEqual(away.getHealth(), away.getMaxHealth(), "fiole : monstre éloigné épargné");
        center.discard();
        beside.discard();
        away.discard();

        Zombie slowed = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, front);
        flyOut(ModBlocks.GEOMANCER_STAFF.get().fire(level, player));
        helper.assertTrue(slowed.getHealth() < slowed.getMaxHealth(), "bâton : monstre blessé");
        helper.assertTrue(slowed.hasEffect(MobEffects.SLOWNESS), "bâton : monstre ralenti");
        slowed.discard();
        helper.succeed();
    }

    /**
     * Caractéristiques : un point par niveau au-delà du premier ; la Vitalité augmente les PV
     * max, la Puissance les dégâts du joueur (en plus du bonus de niveau), la Rapidité la
     * vitesse ; plus de point, plus de dépense ; tout reprendre rend les points et retire les
     * bonus. La Maîtrise des tours du poseur multiplie les dégâts d'une tour, jamais sous x1.
     */
    static void heroStatPoints(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        float baseHealth = player.getMaxHealth();
        double baseSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);

        helper.assertValueEqual(HeroStat.available(player), 0, "aucun point au niveau 1");
        helper.assertFalse(HeroStat.VITALITY.spend(player), "pas de dépense sans point");

        player.setData(ModAttachments.LEVEL, 5);
        helper.assertValueEqual(HeroStat.available(player), 4, "quatre points au niveau 5");
        helper.assertTrue(HeroStat.VITALITY.spend(player), "premier point de Vitalité");
        helper.assertTrue(HeroStat.VITALITY.spend(player), "second point de Vitalité");
        helper.assertTrue(HeroStat.POWER.spend(player), "point de Puissance");
        helper.assertTrue(HeroStat.SWIFTNESS.spend(player), "point de Rapidité");
        helper.assertFalse(HeroStat.TOWER_MASTERY.spend(player), "plus de point à dépenser");
        helper.assertValueEqual(HeroStat.available(player), 0, "tous les points dépensés");

        helper.assertValueEqual(player.getMaxHealth(), baseHealth + 4.0F, "Vitalité : +2 PV max par point");
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - baseSpeed * 1.02D) < 1.0E-9D,
                "Rapidité : +2 % de vitesse par point");
        // Niveau 5 : x1,20 ; un point de Puissance : x1,03.
        helper.assertTrue(Math.abs(ModEvents.playerDamageMultiplier(player) - 1.2D * 1.03D) < 1.0E-9D,
                "Puissance multipliée au bonus de niveau");

        HeroStat.reset(player);
        helper.assertValueEqual(HeroStat.available(player), 4, "points rendus");
        helper.assertValueEqual(player.getMaxHealth(), baseHealth, "PV max revenus à la normale");
        helper.assertTrue(Math.abs(ModEvents.playerDamageMultiplier(player) - 1.2D) < 1.0E-9D, "Puissance retirée");

        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SPIKE_BLOCKADE.get());
        AbstractTowerBlockEntity tower = helper.getBlockEntity(pos, AbstractTowerBlockEntity.class);
        tower.setOwnerBonus(1.3D);
        helper.assertValueEqual(tower.damageMultiplier(), 1.3D, "Maîtrise des tours du poseur");
        tower.setOwnerBonus(0.5D);
        helper.assertValueEqual(tower.damageMultiplier(), 1.0D, "jamais sous x1");
        helper.succeed();
    }

    /**
     * Génération du butin : une arme a toujours un bonus de dégâts et exactement le nombre de
     * bonus secondaires de sa rareté ; elle est plus forte à une vague avancée ; elle survit à
     * l'aller-retour vers un objet au sol ; l'objet posé ne se ramasse pas en marchant dessus.
     */
    static void lootGeneration(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RandomSource random = RandomSource.create(42L);

        for (LootRarity rarity : LootRarity.values()) {
            for (int i = 0; i < 20; i++) {
                LootWeapon weapon = LootGenerator.roll(random, HeroDefinition.ENGINEER, rarity, 1);
                helper.assertTrue(weapon.damage() > 0.0D, "bonus de dégâts présent (" + rarity + ")");
                helper.assertValueEqual(weapon.heroDefinition(), HeroDefinition.ENGINEER, "héros de l'arme");
                helper.assertValueEqual(weapon.lootRarity(), rarity, "rareté de l'arme");
                int secondaries = (weapon.health() > 0.0D ? 1 : 0) + (weapon.speed() > 0.0D ? 1 : 0)
                        + (weapon.towerDamage() > 0.0D ? 1 : 0);
                helper.assertValueEqual(secondaries, rarity.secondaryBonuses(), "bonus secondaires (" + rarity + ")");
                helper.assertValueEqual(weapon.statLines().size(), 1 + rarity.secondaryBonuses(), "lignes affichées");
            }
        }

        // Vague 21 : force doublée (+5 % par vague). Le minimum possible y dépasse le maximum de
        // la vague 1 pour une même rareté (0,04 x 2 = 0,08).
        LootWeapon late = LootGenerator.roll(random, HeroDefinition.SQUIRE, LootRarity.COMMON, 21);
        helper.assertTrue(late.damage() >= 0.08D, "butin plus fort à une vague avancée : " + late.damage());

        LootWeapon original = LootGenerator.roll(random, HeroDefinition.GEOMANCER, LootRarity.LEGENDARY, 3);
        helper.assertValueEqual(LootWeapon.fromStack(original.toStack()).orElse(null), original, "aller-retour par l'objet au sol");
        helper.assertTrue(LootWeapon.fromStack(new ItemStack(ModBlocks.SQUIRE_SWORD.get())).isEmpty(),
                "une arme de base n'est pas du butin");

        ItemEntity dropped = LootGenerator.drop(level, helper.absoluteVec(new Vec3(1.5D, 1.0D, 1.5D)), original);
        helper.assertTrue(dropped.hasPickUpDelay(), "le butin ne se ramasse pas en marchant dessus");
        dropped.discard();
        helper.succeed();
    }

    /**
     * Équipement : ranger et équiper un objet au sol (trop loin : refusé) ; équiper remplace
     * l'arme en place, qui part dans l'inventaire ; l'arme d'un autre héros se range mais ne
     * s'équipe pas ; les bonus s'appliquent (dégâts, PV max, tours) ; chaque héros garde son
     * arme équipée ; échange depuis l'inventaire, déséquiper, détruire ; inventaire plein.
     */
    static void lootEquipment(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 stand = helper.absoluteVec(new Vec3(1.5D, 1.0D, 1.5D));
        player.setPos(stand.x, stand.y, stand.z);
        player.setData(ModAttachments.HERO, HeroDefinition.ALCHEMIST.ordinal());
        float baseHealth = player.getMaxHealth();

        LootWeapon first = new LootWeapon(HeroDefinition.ALCHEMIST.id(), LootRarity.RARE.name(), 0.10D, 4.0D, 0.0D, 0.05D);
        LootWeapon second = new LootWeapon(HeroDefinition.ALCHEMIST.id(), LootRarity.EPIC.name(), 0.20D, 0.0D, 0.0D, 0.0D);
        LootWeapon foreign = new LootWeapon(HeroDefinition.ENGINEER.id(), LootRarity.COMMON.name(), 0.05D, 0.0D, 0.0D, 0.0D);

        ItemEntity far = LootGenerator.drop(level, stand.add(0.0D, 0.0D, 8.0D), first);
        helper.assertFalse(Equipment.pickUp(player, far, false), "objet trop loin : refusé");
        helper.assertTrue(far.isAlive(), "l'objet trop loin reste au sol");
        far.discard();

        ItemEntity onGround = LootGenerator.drop(level, stand, first);
        helper.assertTrue(Equipment.pickUp(player, onGround, true), "équiper directement depuis le sol");
        helper.assertFalse(onGround.isAlive(), "l'objet ramassé disparaît");
        helper.assertValueEqual(Equipment.equipped(player), first, "arme équipée");
        helper.assertValueEqual(Equipment.inventory(player).size(), 0, "rien dans l'inventaire");
        helper.assertValueEqual(Equipment.damageMultiplier(player), 1.10D, "bonus de dégâts de l'arme");
        helper.assertValueEqual(Equipment.towerDamageBonus(player), 0.05D, "bonus aux tours de l'arme");
        helper.assertValueEqual(player.getMaxHealth(), baseHealth + 4.0F, "PV max de l'arme");

        ItemEntity better = LootGenerator.drop(level, stand, second);
        helper.assertTrue(Equipment.pickUp(player, better, true), "équiper une seconde arme");
        helper.assertValueEqual(Equipment.equipped(player), second, "la nouvelle arme remplace l'ancienne");
        helper.assertValueEqual(Equipment.inventory(player), List.of(first), "l'ancienne part dans l'inventaire");
        helper.assertValueEqual(player.getMaxHealth(), baseHealth, "les PV de l'ancienne arme sont retirés");

        ItemEntity other = LootGenerator.drop(level, stand, foreign);
        helper.assertFalse(Equipment.pickUp(player, other, true), "arme d'un autre héros : pas équipable");
        helper.assertTrue(other.isAlive(), "elle reste au sol");
        helper.assertTrue(Equipment.pickUp(player, other, false), "mais elle se range");
        helper.assertValueEqual(Equipment.inventory(player), List.of(first, foreign), "inventaire après rangement");

        // Chaque héros a sa propre arme équipée.
        player.setData(ModAttachments.HERO, HeroDefinition.ENGINEER.ordinal());
        helper.assertTrue(Equipment.equipped(player) == null, "l'Ingénieur n'a pas d'arme de butin");
        helper.assertTrue(Equipment.equipFromInventory(player, 1), "l'Ingénieur équipe la sienne depuis l'inventaire");
        helper.assertValueEqual(Equipment.inventory(player), List.of(first), "la case équipée est retirée de l'inventaire");
        player.setData(ModAttachments.HERO, HeroDefinition.ALCHEMIST.ordinal());
        helper.assertValueEqual(Equipment.equipped(player), second, "l'Alchimiste a gardé la sienne");

        helper.assertTrue(Equipment.equipFromInventory(player, 0), "échange avec une case de l'inventaire");
        helper.assertValueEqual(Equipment.equipped(player), first, "arme échangée");
        helper.assertValueEqual(Equipment.inventory(player), List.of(second), "l'ancienne a pris la case");
        helper.assertTrue(Equipment.unequip(player), "déséquiper");
        helper.assertTrue(Equipment.equipped(player) == null, "plus d'arme de butin");
        helper.assertValueEqual(Equipment.damageMultiplier(player), 1.0D, "plus de bonus");
        helper.assertValueEqual(Equipment.inventory(player).size(), 2, "l'arme déséquipée est rangée");
        helper.assertTrue(Equipment.discard(player, 0), "détruire une case");
        helper.assertFalse(Equipment.discard(player, 5), "case inexistante");
        helper.assertValueEqual(Equipment.inventory(player).size(), 1, "inventaire après destruction");

        while (Equipment.inventory(player).size() < Equipment.CAPACITY) {
            Equipment.store(player, foreign);
        }
        helper.assertFalse(Equipment.store(player, foreign), "inventaire plein : refusé");
        helper.assertValueEqual(Equipment.inventory(player).size(), Equipment.CAPACITY, "capacité respectée");
        helper.succeed();
    }

    private static SpawnerBlockEntity.SpawnEntry parseEntry(GameTestHelper helper, Tag enemy) {
        CompoundTag tag = new CompoundTag();
        tag.put("Enemy", enemy);
        tag.putInt("BaseCount", 5);
        tag.putInt("Spawned", 0);
        tag.putInt("Accumulator", 0);
        tag.putInt("EffectiveTotal", 5);
        return SpawnerBlockEntity.SpawnEntry.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    /**
     * Chaque monstre de l'Atelier chargé (doc/ATELIER.md), sans rien de propre à un monstre : il apparaît en
     * AtelierMonster (donc Monster, ce que visent les tours), avec les PV, dégâts et vitesse de sa fiche, il reçoit l'IA
     * de vague de Dungeon Defenders (celle de son enemy/<id>.json, sinon la mêlée par défaut), et son coup ne tombe
     * qu'à l'impact de son animation « attack », puis pas de second coup avant la fin de l'animation. Sans monstre
     * chargé, rien à vérifier.
     */
    static void atelierMonsters(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<AtelierMonster> spawned = new ArrayList<>();
        Map<AtelierMonster, AtomicInteger> hits = new HashMap<>();
        Map<AtelierMonster, Integer> expectedTicks = new HashMap<>();
        for (Map.Entry<String, FicheMonstre> entry : Atelier.monstres().entrySet()) {
            FicheMonstre fiche = entry.getValue();
            EntityType<AtelierMonster> type = AtelierEntities.types().get(entry.getKey()).get();
            Entity entity = type.spawn(level, helper.absolutePos(new BlockPos(1, 1, 1)), EntitySpawnReason.COMMAND);
            helper.assertTrue(entity instanceof AtelierMonster, fiche.id() + " apparaît en AtelierMonster");
            AtelierMonster monster = (AtelierMonster) entity;
            monster.setNoAi(true);
            helper.assertValueEqual(monster.fiche(), fiche, fiche.id() + " : fiche retrouvée par son type");
            EnemyDefinition definition = EnemyRegistry.definitionFor(type).orElse(null);
            if (definition == null || definition.maxHealth().isEmpty()) {
                helper.assertValueEqual(monster.getMaxHealth(), fiche.stats().pv(), fiche.id() + " : PV de la fiche");
            }
            helper.assertValueEqual((float) monster.getAttributeValue(Attributes.ATTACK_DAMAGE), fiche.stats().degats(),
                    fiche.id() + " : dégâts de la fiche");
            helper.assertTrue(monster.goalSelector.getAvailableGoals().stream().anyMatch(goal ->
                            goal.getGoal() instanceof AttackPriorityTargetGoal || goal.getGoal() instanceof RangedAttackEterniaCrystalGoal),
                    fiche.id() + " : IA de vague reçue");

            AtomicInteger counter = new AtomicInteger();
            helper.assertTrue(monster.beginAttack(counter::incrementAndGet), fiche.id() + " : premier coup lancé");
            helper.assertFalse(monster.beginAttack(counter::incrementAndGet), fiche.id() + " : pas de second coup pendant le premier");
            Optional<String> attack = fiche.animation("attack");
            if (attack.isPresent()) {
                helper.assertValueEqual(counter.get(), 0, fiche.id() + " : les dégâts attendent l'impact");
                expectedTicks.put(monster, 4 + Math.round(Atelier.impact(fiche.id(), attack.get()).secondes() * 20.0F));
            } else {
                helper.assertValueEqual(counter.get(), 1, fiche.id() + " : sans animation, le coup tombe tout de suite");
                expectedTicks.put(monster, 0);
            }
            spawned.add(monster);
            hits.put(monster, counter);

            // Le lien avec les vagues : son enemy/<id>.json (écrit par l'Atelier) le fait apparaître comme le ferait un
            // spawner de map, marqué de son identité d'ennemi (XP, score, total de vague).
            if (definition != null) {
                Identifier enemyId = EnemyRegistry.idFor(type).orElseThrow();
                Entity wave = definition.spawn(level, enemyId, helper.absolutePos(new BlockPos(1, 1, 1)));
                helper.assertTrue(wave instanceof AtelierMonster, fiche.id() + " : apparaît dans une vague");
                helper.assertValueEqual(EnemyRegistry.idFor(wave), Optional.of(enemyId), fiche.id() + " : identité d'ennemi");
                wave.discard();
            }
        }
        int longest = expectedTicks.values().stream().max(Integer::compare).orElse(0);
        helper.runAfterDelay(longest + 2, () -> {
            for (AtelierMonster monster : spawned) {
                helper.assertValueEqual(hits.get(monster).get(), 1, monster.fiche().id() + " : un seul coup, tombé à l'impact");
                monster.discard();
            }
            helper.succeed();
        });
    }
}
