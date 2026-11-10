package com.github.c0c0tier.dungeon_defenders;

import com.github.c0c0tier.dungeon_defenders.init.BalanceValue;
import com.github.c0c0tier.dungeon_defenders.init.GameBalance;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.PhaseTransitions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

// Compte à rebours entre le vote "prêt" unanime et le début du Combat (idée du backlog, faite le
// 2026-11-10) : au lieu de démarrer à l'instant où le dernier joueur clique sur le cristal, le
// combat laisse quelques secondes pour finir de se placer. Durée data-driven
// (balance/pacing.json, combat_countdown_seconds, 3 par défaut ; 0 = démarrage immédiat comme
// avant).
//
// Le temps restant vit dans ModAttachments.COMBAT_COUNTDOWN (ticks, synchronisé pour l'affichage
// client, voir client/gui/CombatCountdownOverlay). Annulé si un joueur retire son vote (voir
// EterniaCrystalBlock) ou si la phase n'est plus la Construction.
@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public final class CombatCountdown {

    private static final int TICKS_PER_SECOND = 20;

    private CombatCountdown() {
    }

    /** Tous les joueurs sont prêts : lance le compte à rebours, ou le Combat tout de suite s'il vaut 0. */
    public static void start(Level level) {
        int seconds = GameBalance.getInt(BalanceValue.COMBAT_COUNTDOWN_SECONDS, false);
        if (seconds <= 0) {
            enterCombat(level);
            return;
        }
        set(level, seconds * TICKS_PER_SECOND);
    }

    public static void cancel(Level level) {
        if (level.getData(ModAttachments.COMBAT_COUNTDOWN) > 0) {
            set(level, 0);
        }
    }

    public static boolean isRunning(Level level) {
        return level.getData(ModAttachments.COMBAT_COUNTDOWN) > 0;
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        int remaining = level.getData(ModAttachments.COMBAT_COUNTDOWN);
        if (remaining <= 0) {
            return;
        }

        // La phase a changé pendant le décompte (abandon, harnais de test...) : plus rien à lancer.
        if (level.getData(ModAttachments.GAME_PHASE) != GamePhase.BUILD.ordinal()) {
            set(level, 0);
            return;
        }

        remaining--;
        if (remaining <= 0) {
            set(level, 0);
            enterCombat(level);
            return;
        }

        level.setData(ModAttachments.COMBAT_COUNTDOWN, remaining);
        // Une synchro par seconde suffit : le client n'affiche que des secondes entières.
        if (remaining % TICKS_PER_SECOND == 0) {
            level.syncData(ModAttachments.COMBAT_COUNTDOWN);
        }
    }

    private static void set(Level level, int ticks) {
        level.setData(ModAttachments.COMBAT_COUNTDOWN, ticks);
        level.syncData(ModAttachments.COMBAT_COUNTDOWN);
    }

    private static void enterCombat(Level level) {
        PhaseTransitions.enterCombat(level);
        level.players().forEach(player -> player.sendSystemMessage(Component.translatable(
                "dungeon_defenders.spawner.phase_toggled", Component.translatable(GamePhase.COMBAT.translationKey()))));
    }
}
