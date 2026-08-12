package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.block.entity.SpawnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

// Logique partagée des deux points d'entrée en phase (le harnais de test manuel de
// SpawnerBlock, et le retour automatique en Construction de ModEvents.onMonsterDeath quand
// une vague est nettoyée) : évite de dupliquer la remise à zéro des compteurs entre les deux.
public final class PhaseTransitions {

    private PhaseTransitions() {
    }

    /** Passe en Combat : nouvelle session (relance la progression de spawn de chaque spawner), compteur de tués à zéro. */
    public static void enterCombat(Level level) {
        level.setData(ModAttachments.GAME_PHASE, GamePhase.COMBAT.ordinal());
        level.syncData(ModAttachments.GAME_PHASE);

        int session = level.getData(ModAttachments.COMBAT_SESSION);
        level.setData(ModAttachments.COMBAT_SESSION, session + 1);

        level.setData(ModAttachments.WAVE_ENEMIES_KILLED, 0);
        level.syncData(ModAttachments.WAVE_ENEMIES_KILLED);
    }

    /** Passe en Construction : recalcule le total de la prochaine vague à partir des spawners actifs. */
    public static void enterBuild(Level level) {
        level.setData(ModAttachments.GAME_PHASE, GamePhase.BUILD.ordinal());
        level.syncData(ModAttachments.GAME_PHASE);

        recomputeWaveEnemiesTotal(level);
    }

    private static void recomputeWaveEnemiesTotal(Level level) {
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
