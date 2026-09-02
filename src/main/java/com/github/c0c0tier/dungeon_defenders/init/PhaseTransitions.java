package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.block.ManaChestBlock;
import com.github.c0c0tier.dungeon_defenders.block.entity.SpawnerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.network.GameOverPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

// Logique partagée des deux points d'entrée en phase (le harnais de test manuel de
// SpawnerBlock, et le retour automatique en Construction de ModEvents.onMonsterDeath quand
// une vague est nettoyée) : évite de dupliquer la remise à zéro des compteurs entre les deux.
public final class PhaseTransitions {

    private PhaseTransitions() {
    }

    /**
     * Passe en Combat : nouvelle session (relance la progression de spawn de chaque
     * spawner), compteur de tués à zéro, et remet "prêt" à faux pour tous les joueurs
     * présents — qu'ils aient déclenché ce combat via le vote (EterniaCrystalBlock) ou via le
     * harnais de test (SpawnerBlock), pour repartir sur une base propre à la prochaine
     * Construction.
     */
    public static void enterCombat(Level level) {
        level.setData(ModAttachments.GAME_PHASE, GamePhase.COMBAT.ordinal());
        level.syncData(ModAttachments.GAME_PHASE);

        int session = level.getData(ModAttachments.COMBAT_SESSION);
        level.setData(ModAttachments.COMBAT_SESSION, session + 1);

        level.setData(ModAttachments.WAVE_ENEMIES_KILLED, 0);
        level.syncData(ModAttachments.WAVE_ENEMIES_KILLED);

        for (Player player : level.players()) {
            player.setData(ModAttachments.READY, false);
            player.syncData(ModAttachments.READY);
        }
    }

    /**
     * Passe en Construction : fait avancer la vague (plafonnée à MAX_WAVE — pas de condition
     * de victoire pour l'instant, voir 05-etat-et-problemes-connus.md), remet le compteur de
     * tués à zéro, recalcule le total de la nouvelle vague à partir des spawners actifs, et
     * "respawn" (redevient visible/solide) tous les coffres de mana ouverts pendant la vague
     * précédente.
     */
    public static void enterBuild(Level level) {
        int nextWave = Math.min(level.getData(ModAttachments.CURRENT_WAVE) + 1, ModAttachments.waveCount(level));
        level.setData(ModAttachments.CURRENT_WAVE, nextWave);
        level.syncData(ModAttachments.CURRENT_WAVE);

        level.setData(ModAttachments.GAME_PHASE, GamePhase.BUILD.ordinal());
        level.syncData(ModAttachments.GAME_PHASE);

        level.setData(ModAttachments.WAVE_ENEMIES_KILLED, 0);
        level.syncData(ModAttachments.WAVE_ENEMIES_KILLED);

        recomputeWaveEnemiesTotal(level);
        ManaChestBlock.respawnAll(level);
    }

    /**
     * La dernière vague (MAX_WAVE) vient d'être nettoyée : ouvre {@code GameOverScreen} pour
     * chaque joueur (voir {@link #sendGameOverScreen}) et remet la partie à zéro (vague 1,
     * phase Construction) pour pouvoir relancer une partie proprement. N'agit pas sur le
     * Cristal d'Eternia lui-même — voir 05-etat-et-problemes-connus.md, la remise à neuf de la
     * map (tours, cristal) reste à faire, liée au futur système de maps/structures.
     *
     * <p>Décidé avec le joueur (2026-08-26) : plus de message système ni de lien "Retour à la
     * taverne" dans le chat, devenus redondants avec {@code GameOverScreen} (qui a ses propres
     * boutons "Rejouer"/"Retour à la taverne") — voir doc/02-gameplay.md.
     */
    public static void onVictory(Level level) {
        resetGameState(level, GamePhase.BUILD);
        for (Player player : level.players()) {
            sendGameOverScreen(player, true);
        }
    }

    /**
     * Le Cristal d'Eternia vient d'être détruit : ouvre {@code GameOverScreen} pour chaque
     * joueur et remet la partie à zéro (vague 1, phase Construction), pour que les spawners
     * arrêtent de faire apparaître des ennemis sur une partie déjà perdue. Le cristal détruit
     * lui-même n'est pas replacé automatiquement — même remarque que pour {@link #onVictory}.
     */
    public static void onDefeat(Level level) {
        resetGameState(level, GamePhase.BUILD);
        for (Player player : level.players()) {
            sendGameOverScreen(player, false);
        }
    }

    /**
     * Entre (ou revient) à la Taverne : remet toute la partie à zéro — vague 1, compteurs,
     * score — et pose la phase {@code TAVERN}. Appelée au chargement du monde
     * ({@code TavernSpawn}) et au retour de map ({@code MapInstance#returnToTavern}).
     *
     * <p>La Taverne n'est pas une phase de partie : aucun spawner ne tourne, aucune vague ne
     * progresse, mais les tours s'y posent librement pour les essais (voir
     * doc/02-gameplay.md).
     */
    public static void enterTavern(Level level) {
        // Le nombre de vagues repart au défaut : la taverne n'est la map de personne, et une
        // valeur héritée de la partie précédente n'aurait aucun sens dans le HUD.
        level.setData(ModAttachments.MAP_WAVE_COUNT, ModAttachments.MAX_WAVE);
        level.syncData(ModAttachments.MAP_WAVE_COUNT);
        resetGameState(level, GamePhase.TAVERN);
    }

    /**
     * Démarre une partie sur une map : vague 1, phase Construction, compteurs et score à zéro.
     * Appelée par {@code MapInstance#startGame}.
     *
     * <p>Volontairement distincte d'{@link #enterBuild} : celle-ci fait <b>avancer</b> la vague
     * (elle sert au retour en Construction entre deux vagues), ce qui démarrerait une partie à
     * la vague 2. Cette remise à zéro explicite était implicite tant que la phase par défaut
     * était {@code BUILD} ; elle ne l'est plus depuis que c'est {@code TAVERN}.
     */
    public static void startNewGame(Level level, int waveCount) {
        level.setData(ModAttachments.MAP_WAVE_COUNT, Math.max(1, waveCount));
        level.syncData(ModAttachments.MAP_WAVE_COUNT);
        resetGameState(level, GamePhase.BUILD);
    }

    // level.players() est statiquement typé Player (Level est commun client/serveur), mais
    // onVictory/onDefeat ne s'exécutent jamais côté client (déclenchés uniquement par de la
    // logique serveur) : le cast est donc toujours valide en pratique, gardé quand même par
    // sécurité plutôt que supposé, même principe que les `instanceof ServerLevel` ailleurs
    // dans le mod.
    private static void sendGameOverScreen(Player player, boolean victory) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new GameOverPayload(victory).toVanillaClientbound());
        }
    }

    private static void resetGameState(Level level, GamePhase phase) {
        level.setData(ModAttachments.CURRENT_WAVE, 1);
        level.syncData(ModAttachments.CURRENT_WAVE);

        level.setData(ModAttachments.GAME_PHASE, phase.ordinal());
        level.syncData(ModAttachments.GAME_PHASE);

        level.setData(ModAttachments.WAVE_ENEMIES_KILLED, 0);
        level.syncData(ModAttachments.WAVE_ENEMIES_KILLED);

        // Le score est censé correspondre à l'expérience gagnée sur LA carte en cours (voir
        // ModAttachments.SCORE) : remis à 0 à chaque nouvelle partie, même point que les
        // compteurs de vague ci-dessus. L'expérience/niveau du joueur, eux, persistent au-delà
        // d'une carte — pas touchés ici.
        level.setData(ModAttachments.SCORE, 0);
        level.syncData(ModAttachments.SCORE);

        recomputeWaveEnemiesTotal(level);
        ManaChestBlock.respawnAll(level);
    }

    /**
     * Recalcule WAVE_ENEMIES_TOTAL à partir des spawners actifs et de la vague courante.
     * Appelée à l'entrée en Construction, mais aussi chaque fois qu'un spawner apparaît,
     * disparaît ou est reconfiguré (voir SpawnerBlockEntity, ModNetworking.handleSpawnerConfig)
     * — pour que le total affiché au joueur ne reste jamais bloqué à la valeur par défaut avant
     * la toute première transition de phase d'une partie.
     */
    public static void recomputeWaveEnemiesTotal(Level level) {
        int currentWave = level.getData(ModAttachments.CURRENT_WAVE);
        double multiplier = DifficultyScaling.getMultiplier(level);

        // Copie défensive : le registre peut changer (chargement/déchargement de chunk)
        // pendant qu'on le parcourt.
        List<BlockPos> activeSpawners = new ArrayList<>(level.getData(ModAttachments.ACTIVE_SPAWNERS));

        int total = 0;
        for (BlockPos pos : activeSpawners) {
            if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)) {
                continue;
            }
            if (currentWave < spawner.getWaveStart() || currentWave > spawner.getWaveEnd()) {
                continue;
            }
            for (SpawnerBlockEntity.SpawnEntry entry : spawner.getEntries()) {
                // Même formule que SpawnEntry.resetForWave(...) / l'aperçu du renderer, pour
                // que ce total corresponde à ce qui sera effectivement spawné.
                total += Math.max(1, (int) Math.round(entry.baseCount() * multiplier));
            }
        }

        level.setData(ModAttachments.WAVE_ENEMIES_TOTAL, total);
        level.syncData(ModAttachments.WAVE_ENEMIES_TOTAL);
    }
}
