package com.github.c0c0tier.dungeon_defenders.gametest;

import com.github.c0c0tier.dungeon_defenders.Config;
import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.ModBlocks;
import com.github.c0c0tier.dungeon_defenders.init.PhaseTransitions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

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
}
