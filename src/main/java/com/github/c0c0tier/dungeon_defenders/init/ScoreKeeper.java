package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.network.ScoreGainPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;

// Point de passage UNIQUE de tout gain de score (serveur) : met à jour ModAttachments.SCORE (le
// total, lu par ScoreOverlay) ET diffuse un ScoreGainPayload à chaque joueur présent (le détail,
// lu par ScoreGainOverlay côté client). Toute source de score passe par ici plutôt que de toucher
// SCORE directement.
//
// Feuille de route du score décidée avec le joueur le 2026-08-27 (doc/02-gameplay.md), codée le
// 2026-11-10 : en plus des kills, bonus de fin de vague, bonus de fin de map, multiplicateurs
// "aucun dégât" par vague, multiplicateur de difficulté et multiplicateur de la map. Toutes les
// valeurs sont data-driven (balance/score.json, voir BalanceValue).
public final class ScoreKeeper {

    private ScoreKeeper() {
    }

    /**
     * Crédite un gain de score. {@code baseAmount} est la valeur "brute" (XP d'un monstre, bonus
     * de vague...) ; le montant réellement ajouté tient compte de la difficulté choisie et du
     * multiplicateur de la map (bloc de configuration), les deux valant x1 par défaut en Normal.
     *
     * @return le montant réellement ajouté au score.
     */
    public static int grant(Level level, double baseAmount, ScoreSource source, Optional<Identifier> enemy) {
        int amount = (int) Math.round(baseAmount * globalMultiplier(level));
        if (amount <= 0) {
            return 0;
        }

        level.setData(ModAttachments.SCORE, level.getData(ModAttachments.SCORE) + amount);
        level.syncData(ModAttachments.SCORE);

        for (Player player : level.players()) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ScoreGainPayload(amount, source.ordinal(), enemy).toVanillaClientbound());
            }
        }
        return amount;
    }

    /** Difficulté choisie x multiplicateur de la map en cours. */
    public static double globalMultiplier(Level level) {
        GameDifficulty difficulty = GameDifficulty.values()[level.getData(ModAttachments.DIFFICULTY)];
        return difficulty.scoreMultiplier() * level.getData(ModAttachments.MAP_SCORE_MULTIPLIER);
    }

    /**
     * Bonus de fin de vague, à appeler quand la vague vient d'être nettoyée (avant de quitter la
     * phase Combat). Le bonus de base est multiplié si aucun joueur n'a pris de dégât pendant la
     * vague, encore si le Cristal d'Eternia n'en a pris aucun, et encore si aucune tour n'a été
     * détruite par les monstres — trois conditions distinctes, cumulables. Chaque part est créditée à part, pour que le joueur voie d'où vient son score.
     */
    public static void awardWaveCleared(Level level) {
        double base = GameBalance.get(BalanceValue.SCORE_WAVE_CLEAR_BONUS, false);
        grant(level, base, ScoreSource.WAVE_CLEARED, Optional.empty());

        double running = base;
        if (!level.getData(ModAttachments.WAVE_PLAYER_DAMAGED)) {
            double multiplier = GameBalance.get(BalanceValue.SCORE_NO_PLAYER_DAMAGE_MULTIPLIER, false);
            grant(level, running * (multiplier - 1.0D), ScoreSource.NO_PLAYER_DAMAGE, Optional.empty());
            running *= multiplier;
        }
        if (!level.getData(ModAttachments.WAVE_CRYSTAL_DAMAGED)) {
            double multiplier = GameBalance.get(BalanceValue.SCORE_NO_CRYSTAL_DAMAGE_MULTIPLIER, false);
            grant(level, running * (multiplier - 1.0D), ScoreSource.CRYSTAL_UNTOUCHED, Optional.empty());
            running *= multiplier;
        }
        if (!level.getData(ModAttachments.WAVE_TOWER_LOST)) {
            double multiplier = GameBalance.get(BalanceValue.SCORE_NO_TOWER_LOST_MULTIPLIER, false);
            grant(level, running * (multiplier - 1.0D), ScoreSource.NO_TOWER_LOST, Optional.empty());
        }
    }

    /** Bonus de fin de map (victoire), à appeler avant que le score ne soit remis à zéro. */
    public static void awardMapCompleted(Level level) {
        grant(level, GameBalance.get(BalanceValue.SCORE_VICTORY_BONUS, false), ScoreSource.MAP_COMPLETED, Optional.empty());
    }
}
