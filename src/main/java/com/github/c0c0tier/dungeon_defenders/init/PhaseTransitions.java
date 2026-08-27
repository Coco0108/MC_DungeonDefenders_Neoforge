package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.MapInstance;
import com.github.c0c0tier.dungeon_defenders.block.entity.SpawnerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
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
     * tués à zéro, et recalcule le total de la nouvelle vague à partir des spawners actifs.
     */
    public static void enterBuild(Level level) {
        int nextWave = Math.min(level.getData(ModAttachments.CURRENT_WAVE) + 1, ModAttachments.MAX_WAVE);
        level.setData(ModAttachments.CURRENT_WAVE, nextWave);
        level.syncData(ModAttachments.CURRENT_WAVE);

        level.setData(ModAttachments.GAME_PHASE, GamePhase.BUILD.ordinal());
        level.syncData(ModAttachments.GAME_PHASE);

        level.setData(ModAttachments.WAVE_ENEMIES_KILLED, 0);
        level.syncData(ModAttachments.WAVE_ENEMIES_KILLED);

        recomputeWaveEnemiesTotal(level);
    }

    /**
     * La dernière vague (MAX_WAVE) vient d'être nettoyée : diffuse un message de victoire (+
     * un lien de retour à la taverne) et remet la partie à zéro (vague 1, phase Construction)
     * pour pouvoir relancer une partie proprement. N'agit pas sur le Cristal d'Eternia
     * lui-même — voir 05-etat-et-problemes-connus.md, la remise à neuf de la map (tours,
     * cristal) reste à faire, liée au futur système de maps/structures.
     */
    public static void onVictory(Level level) {
        resetGameState(level);
        for (Player player : level.players()) {
            player.sendSystemMessage(Component.translatable("dungeon_defenders.game.victory")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            player.sendSystemMessage(returnToTavernLink());
        }
    }

    /**
     * Le Cristal d'Eternia vient d'être détruit : diffuse un message de défaite (+ un lien de
     * retour à la taverne) et remet la partie à zéro (vague 1, phase Construction), pour que
     * les spawners arrêtent de faire apparaître des ennemis sur une partie déjà perdue. Le
     * cristal détruit lui-même n'est pas replacé automatiquement — même remarque que pour
     * onVictory.
     */
    public static void onDefeat(Level level) {
        resetGameState(level);
        for (Player player : level.players()) {
            player.sendSystemMessage(Component.translatable("dungeon_defenders.game.defeat")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            player.sendSystemMessage(returnToTavernLink());
        }
    }

    // Lien cliquable qui lance la commande de harnais MapInstance.RETURN_COMMAND (voir
    // ModCommands) — en attendant un vrai point de sortie posé dans chaque map.
    private static Component returnToTavernLink() {
        return Component.translatable("dungeon_defenders.game.return_to_tavern")
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.RunCommand("/" + MapInstance.RETURN_COMMAND)));
    }

    private static void resetGameState(Level level) {
        level.setData(ModAttachments.CURRENT_WAVE, 1);
        level.syncData(ModAttachments.CURRENT_WAVE);

        level.setData(ModAttachments.GAME_PHASE, GamePhase.BUILD.ordinal());
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
