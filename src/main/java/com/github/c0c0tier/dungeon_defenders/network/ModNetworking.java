package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.MapInstance;
import com.github.c0c0tier.dungeon_defenders.block.entity.SpawnerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.GameDifficulty;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.SpawnableEnemy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

// Enregistrement des paquets custom du mod. Sans "bus" explicite sur @EventBusSubscriber,
// RegisterPayloadHandlersEvent (qui implémente IModBusEvent) part automatiquement sur le bus
// du mod, comme RegisterGuiLayersEvent dans DungeonDefendersModClient — mais cette classe-ci
// n'est PAS client-only : un serveur dédié doit aussi savoir décoder ce que ses clients lui
// envoient.
@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public class ModNetworking {

    // Distance max (au carré) entre le joueur et le spawner pour accepter le paquet — même
    // ordre de grandeur que la portée d'interaction avec un bloc à conteneur vanilla.
    private static final double MAX_DISTANCE_SQ = 64.0D;

    @SubscribeEvent
    static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                SpawnerConfigPayload.TYPE,
                SpawnerConfigPayload.STREAM_CODEC,
                ModNetworking::handleSpawnerConfig
        );
        registrar.playToServer(
                SetDifficultyPayload.TYPE,
                SetDifficultyPayload.STREAM_CODEC,
                ModNetworking::handleSetDifficulty
        );
        registrar.playToServer(
                StartGamePayload.TYPE,
                StartGamePayload.STREAM_CODEC,
                ModNetworking::handleStartGame
        );
    }

    private static void handleStartGame(StartGamePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }
            MapInstance.startGame(serverLevel);
        });
    }

    private static void handleSetDifficulty(SetDifficultyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (level.isClientSide()) {
                return;
            }

            // Ordinal envoyé par un client : à valider avant indexation, jamais faire
            // confiance à une valeur reçue par le réseau pour indexer un tableau.
            GameDifficulty[] difficulties = GameDifficulty.values();
            if (payload.difficultyOrdinal() < 0 || payload.difficultyOrdinal() >= difficulties.length) {
                return;
            }

            level.setData(ModAttachments.DIFFICULTY, payload.difficultyOrdinal());
            level.syncData(ModAttachments.DIFFICULTY);
        });
    }

    private static void handleSpawnerConfig(SpawnerConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (level.isClientSide()) {
                return;
            }

            BlockPos pos = payload.pos();
            if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)) {
                return;
            }

            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_DISTANCE_SQ) {
                return;
            }

            List<SpawnerBlockEntity.SpawnEntry> entries = new ArrayList<>();
            SpawnableEnemy[] enemies = SpawnableEnemy.values();
            for (SpawnerConfigPayload.Entry entry : payload.entries()) {
                // Ordinal envoyé par un client : à valider avant indexation, jamais faire confiance
                // à une valeur reçue par le réseau pour indexer un tableau.
                if (entry.enemyOrdinal() < 0 || entry.enemyOrdinal() >= enemies.length) {
                    continue;
                }
                entries.add(new SpawnerBlockEntity.SpawnEntry(enemies[entry.enemyOrdinal()], entry.baseCount()));
            }

            spawner.applyConfig(
                    payload.intervalTicks(),
                    payload.spawnRadius(),
                    payload.waveStart(),
                    payload.waveEnd(),
                    entries
            );
        });
    }
}
