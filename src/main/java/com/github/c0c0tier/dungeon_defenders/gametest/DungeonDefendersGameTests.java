package com.github.c0c0tier.dungeon_defenders.gametest;

import com.github.c0c0tier.dungeon_defenders.Config;
import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.SpawnerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.DifficultyScaling;
import com.github.c0c0tier.dungeon_defenders.init.EnemyBehavior;
import com.github.c0c0tier.dungeon_defenders.init.EnemyDefinition;
import com.github.c0c0tier.dungeon_defenders.init.EnemyRegistry;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.LegacyEnemyIds;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.ModBlocks;
import com.github.c0c0tier.dungeon_defenders.init.PhaseTransitions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
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

        helper.assertValueEqual(EnemyRegistry.xpValueFor(EntityType.HUSK), EnemyRegistry.DEFAULT_XP_VALUE, "XP d'un monstre sans JSON");
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
