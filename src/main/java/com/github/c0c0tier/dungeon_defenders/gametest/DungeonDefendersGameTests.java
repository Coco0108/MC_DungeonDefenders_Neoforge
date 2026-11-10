package com.github.c0c0tier.dungeon_defenders.gametest;

import com.github.c0c0tier.dungeon_defenders.CombatCountdown;
import com.github.c0c0tier.dungeon_defenders.Config;
import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.WorldTime;
import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.ManaChestBlock;
import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.ManaChestBlockEntity;
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
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.ModBlocks;
import com.github.c0c0tier.dungeon_defenders.init.PhaseTransitions;
import com.github.c0c0tier.dungeon_defenders.init.ScoreKeeper;
import com.github.c0c0tier.dungeon_defenders.init.ScoreSource;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import com.github.c0c0tier.dungeon_defenders.init.TowerStats;
import com.github.c0c0tier.dungeon_defenders.init.TowerStatsRegistry;
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
import net.minecraft.world.entity.monster.zombie.Zombie;
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
        register(event, environment, "enemy_variants", DungeonDefendersGameTests::enemyVariants);
        register(event, environment, "mana_chest_respawn", DungeonDefendersGameTests::manaChestRespawn);
        register(event, environment, "world_time_frozen", DungeonDefendersGameTests::worldTimeFrozen);
        register(event, environment, "monsters_do_not_burn", DungeonDefendersGameTests::monstersDoNotBurn);
        register(event, environment, "score_bonuses", DungeonDefendersGameTests::scoreBonuses);
        register(event, environment, "combat_countdown", DungeonDefendersGameTests::combatCountdown);
        // Stats des tours en data-driven (doc/data-driven/tours.md).
        register(event, environment, "tower_shipped_values", DungeonDefendersGameTests::towerShippedValues);
        register(event, environment, "tower_stats_validation", DungeonDefendersGameTests::towerStatsValidation);
        // Réglages globaux en data-driven (doc/data-driven/equilibrage.md).
        register(event, environment, "balance_shipped_values", DungeonDefendersGameTests::balanceShippedValues);
        register(event, environment, "balance_validation", DungeonDefendersGameTests::balanceValidation);
    }

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
        spawner.applyConfig(20, 0, 1, 1, List.of(
                new SpawnerBlockEntity.SpawnEntry(LegacyEnemyIds.ZOMBIE, 4),
                new SpawnerBlockEntity.SpawnEntry(Identifier.fromNamespaceAndPath("pack_absent", "goblin"), 6)));

        PhaseTransitions.recomputeWaveEnemiesTotal(level);
        int expected = Math.max(1, (int) Math.round(4 * DifficultyScaling.getMultiplier(level)));
        helper.assertValueEqual(level.getData(ModAttachments.WAVE_ENEMIES_TOTAL), expected, "total sans l'ennemi inconnu");
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
     * Les six JSON de tours livrés sont chargés et reprennent exactement les valeurs par défaut
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

    private static SpawnerBlockEntity.SpawnEntry parseEntry(GameTestHelper helper, Tag enemy) {
        CompoundTag tag = new CompoundTag();
        tag.put("Enemy", enemy);
        tag.putInt("BaseCount", 5);
        tag.putInt("Spawned", 0);
        tag.putInt("Accumulator", 0);
        tag.putInt("EffectiveTotal", 5);
        return SpawnerBlockEntity.SpawnEntry.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }
}
