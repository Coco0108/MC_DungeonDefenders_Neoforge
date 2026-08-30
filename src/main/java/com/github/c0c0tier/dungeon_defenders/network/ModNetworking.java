package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.MapInstance;
import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.ManaChestBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.SpawnerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.GameDifficulty;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.PhaseTransitions;
import com.github.c0c0tier.dungeon_defenders.init.SpawnableEnemy;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
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

    // Part du coût en mana rendue à la suppression volontaire d'une tour (touche dédiée, voir
    // handleRemoveTower) — valeur de test, pas encore équilibrée, comme les coûts de pose
    // (TowerDefinition). Ne s'applique jamais à une tour détruite au combat (voir
    // AbstractTowerBlockEntity#setHealth, qui ne passe jamais par ce chemin).
    private static final float TOWER_MANA_REFUND_RATIO = 0.5F;

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
        registrar.playToServer(
                PlaceTowerPayload.TYPE,
                PlaceTowerPayload.STREAM_CODEC,
                ModNetworking::handlePlaceTower
        );
        registrar.playToServer(
                ManaChestConfigPayload.TYPE,
                ManaChestConfigPayload.STREAM_CODEC,
                ModNetworking::handleManaChestConfig
        );
        registrar.playToServer(
                RemoveTowerPayload.TYPE,
                RemoveTowerPayload.STREAM_CODEC,
                ModNetworking::handleRemoveTower
        );
        // Sans handler ici : paquets clientbound du mod, le handler vit côté client
        // uniquement (DungeonDefendersModClient#onRegisterClientPayloadHandlers), pour ne
        // jamais charger de classe cliente (Minecraft, Screen...) sur un serveur dédié — cette
        // classe-ci est chargée des deux côtés (voir le commentaire de classe).
        registrar.playToClient(
                GameOverPayload.TYPE,
                GameOverPayload.STREAM_CODEC
        );
        registrar.playToClient(
                ScoreGainPayload.TYPE,
                ScoreGainPayload.STREAM_CODEC
        );
        registrar.playToClient(
                OpenMapSelectionPayload.TYPE,
                OpenMapSelectionPayload.STREAM_CODEC
        );
    }

    private static void handlePlaceTower(PlaceTowerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }

            // Ordinaux envoyés par un client : à valider avant indexation, jamais faire
            // confiance à une valeur reçue par le réseau pour indexer un tableau.
            TowerDefinition[] towers = TowerDefinition.values();
            if (payload.towerOrdinal() < 0 || payload.towerOrdinal() >= towers.length) {
                return;
            }
            Direction[] directions = Direction.values();
            if (payload.directionOrdinal() < 0 || payload.directionOrdinal() >= directions.length) {
                return;
            }

            BlockPos pos = payload.pos();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_DISTANCE_SQ) {
                return;
            }

            if (!serverLevel.getBlockState(pos).canBeReplaced()) {
                return;
            }

            TowerDefinition tower = towers[payload.towerOrdinal()];
            Direction direction = directions[payload.directionOrdinal()];

            BlockState state = tower.block().defaultBlockState();
            // La rotation n'a d'effet que si le bloc a une propriété d'orientation - Spike
            // Blockade (cube symétrique) n'en a pas encore, donc elle est simplement ignorée
            // pour cette tour, sans erreur (voir doc/02-gameplay.md).
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
            }

            BlockSnapshot before = BlockSnapshot.create(serverLevel.dimension(), serverLevel, pos);
            serverLevel.setBlock(pos, state, Block.UPDATE_ALL);

            // Réutilise le même hook NeoForge que la pose par BlockItem (voir CommonHooks côté
            // vanilla) : déclenche BlockEvent.EntityPlaceEvent, donc ModEvents.onBlockadePlace
            // s'applique sans aucune duplication de la vérification/du débit de mana.
            boolean cancelled = EventHooks.onBlockPlace(player, before, Direction.UP);
            if (cancelled) {
                before.restore(Block.UPDATE_ALL);
            }
        });
    }

    // Déclenché par TowerRemovalClientEvents (touche ModKeyMappings.REMOVE_TOWER_MODE puis
    // clic gauche sur une tour visée) : suppression instantanée + remboursement partiel de
    // mana, comme dans le jeu de référence. Tout est revérifié ici (le client a déjà refusé
    // une cible invalide, mais n'est jamais l'autorité) : phase, portée, présence réelle d'une
    // AbstractTowerBlockEntity à cette position.
    private static void handleRemoveTower(RemoveTowerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }

            // Symétrique à la pose (ModEvents.onTowerPlace) : les tours ne se retirent qu'en
            // phase Construction.
            if (level.getData(ModAttachments.GAME_PHASE) != GamePhase.BUILD.ordinal()) {
                player.sendSystemMessage(Component.translatable("dungeon_defenders.tower.build_phase_only"));
                return;
            }

            BlockPos pos = payload.pos();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_DISTANCE_SQ) {
                return;
            }

            if (!(serverLevel.getBlockEntity(pos) instanceof AbstractTowerBlockEntity tower)) {
                return;
            }

            int refund = Math.round(tower.getManaCost() * TOWER_MANA_REFUND_RATIO);

            // false : pas de drop d'item, comme une tour détruite au combat (setHealth) — la
            // touche dédiée est l'unique façon "propre" de retirer une tour, symétrique à la
            // roue pour la pose (voir doc/05-etat-et-problemes-connus.md).
            serverLevel.destroyBlock(pos, false);

            if (refund > 0) {
                int currentMana = player.getData(ModAttachments.MANA);
                int newMana = Math.min(ModAttachments.MAX_MANA, currentMana + refund);
                player.setData(ModAttachments.MANA, newMana);
                player.syncData(ModAttachments.MANA);
                player.sendSystemMessage(Component.translatable(
                        "dungeon_defenders.tower.mana_refunded", refund, newMana, ModAttachments.MAX_MANA));
            }
        });
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

            // applyConfig ne recalcule que les plafonds internes DE ce spawner ; le total
            // affiché au HUD (somme de tous les spawners actifs) doit être recalculé à part.
            PhaseTransitions.recomputeWaveEnemiesTotal(level);
        });
    }

    private static void handleManaChestConfig(ManaChestConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (level.isClientSide()) {
                return;
            }

            BlockPos pos = payload.pos();
            if (!(level.getBlockEntity(pos) instanceof ManaChestBlockEntity chest)) {
                return;
            }

            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_DISTANCE_SQ) {
                return;
            }

            chest.applyConfig(payload.manaAmount());
        });
    }
}
