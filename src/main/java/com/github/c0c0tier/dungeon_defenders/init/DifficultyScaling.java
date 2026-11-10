package com.github.c0c0tier.dungeon_defenders.init;

import net.minecraft.world.level.Level;

// Point de calcul unique du multiplicateur appliqué aux compositions de spawner
// (nombre_effectif = nombre_de_base * getMultiplier(level)), pour ne pas éparpiller cette
// logique. Pour l'instant : difficulté choisie (voir GameDifficulty) x un facteur qui
// augmente de 10 % par vague au-delà de la première. D'autres facteurs (nombre de joueurs,
// par exemple) pourront s'ajouter ici plus tard sans changer l'appelant.
public final class DifficultyScaling {

    private DifficultyScaling() {
    }

    public static double getMultiplier(Level level) {
        GameDifficulty difficulty = GameDifficulty.values()[level.getData(ModAttachments.DIFFICULTY)];
        int wave = level.getData(ModAttachments.CURRENT_WAVE);

        // Les deux facteurs sont data-driven (balance/difficulty.json, voir GameBalance) ; côté
        // client (aperçu du spawner), c'est la copie reçue du serveur qui est lue.
        boolean clientSide = level.isClientSide();
        double waveFactor = 1.0D + Math.max(0, wave - 1) * GameBalance.get(BalanceValue.PER_WAVE_INCREASE, clientSide);
        return difficulty.multiplier(clientSide) * waveFactor;
    }
}
