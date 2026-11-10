package com.github.c0c0tier.dungeon_defenders;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

import java.util.Optional;

// L'heure du monde est FIGÉE (idées du joueur, issues #44 et #45, 2026-11-10) : plus de cycle
// jour/nuit, la taverne et chaque map ont une heure fixe. Un jeu de défense par vagues n'a rien à
// gagner à voir la nuit tomber au milieu d'une partie, et l'ambiance d'une map (plein jour,
// crépuscule, nuit) devient un choix de son créateur — voir MapConfigBlockEntity, champ
// "TimeOfDay".
//
// Depuis Minecraft 26.1 l'heure passe par des "horloges" (ServerClockManager) : on met celle de
// l'Overworld en pause et on la règle directement, ce qui diffuse aussi la nouvelle heure aux
// clients. La météo est arrêtée de la même façon (pluie sans intérêt sur un monde vide).
@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public final class WorldTime {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int TICKS_PER_DAY = 24000;
    /** Valeur du champ d'heure d'une map qui n'en fixe pas : l'heure par défaut de la config. */
    public static final int USE_DEFAULT = -1;

    private WorldTime() {
    }

    // À ServerStartedEvent et pas au chargement du niveau (comme TavernSpawn) : le gestionnaire
    // d'horloges est une donnée du serveur entier, initialisée après les niveaux.
    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        ServerLevel overworld = event.getServer().overworld();
        overworld.getGameRules().set(GameRules.ADVANCE_WEATHER, false, event.getServer());
        overworld.resetWeatherCycle();
        freezeAt(overworld, USE_DEFAULT);
    }

    /**
     * Fige l'heure du monde. {@code timeOfDay} en ticks vanilla (0 = lever du soleil, 6000 = midi,
     * 12000 = coucher, 18000 = minuit) ; {@link #USE_DEFAULT} ou toute valeur hors de
     * [0, 24000[ donne l'heure par défaut de la config (`defaultTimeOfDay`).
     */
    public static void freezeAt(ServerLevel level, int timeOfDay) {
        int resolved = isValid(timeOfDay) ? timeOfDay : Config.DEFAULT_TIME_OF_DAY.get();

        Optional<Holder.Reference<WorldClock>> clock =
                level.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).get(WorldClocks.OVERWORLD);
        if (clock.isEmpty()) {
            LOGGER.warn("Horloge de l'Overworld introuvable : l'heure du monde n'est pas figée.");
            return;
        }

        ServerClockManager clocks = level.clockManager();
        clocks.setPaused(clock.get(), true);
        clocks.setTotalTicks(clock.get(), resolved);
    }

    public static boolean isValid(int timeOfDay) {
        return timeOfDay >= 0 && timeOfDay < TICKS_PER_DAY;
    }
}
