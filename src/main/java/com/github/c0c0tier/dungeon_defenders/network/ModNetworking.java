package com.github.c0c0tier.dungeon_defenders.network;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.MapInstance;
import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.ManaChestBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.SpawnerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.GameDifficulty;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.block.entity.MapConfigBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.MapDefinition;
import com.github.c0c0tier.dungeon_defenders.init.MapRegistry;
import com.github.c0c0tier.dungeon_defenders.init.ModBlocks;
import com.github.c0c0tier.dungeon_defenders.init.PhaseTransitions;
import com.github.c0c0tier.dungeon_defenders.init.SpawnableEnemy;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
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
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Enregistrement des paquets custom du mod. Sans "bus" explicite sur @EventBusSubscriber,
// RegisterPayloadHandlersEvent (qui implémente IModBusEvent) part automatiquement sur le bus
// du mod, comme RegisterGuiLayersEvent dans DungeonDefendersModClient — mais cette classe-ci
// n'est PAS client-only : un serveur dédié doit aussi savoir décoder ce que ses clients lui
// envoient.
@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public class ModNetworking {

    private static final Logger LOGGER = LogUtils.getLogger();

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
        registrar.playToServer(
                LeaveMapPayload.TYPE,
                LeaveMapPayload.STREAM_CODEC,
                ModNetworking::handleLeaveMap
        );
        registrar.playToServer(
                MapConfigPayload.TYPE,
                MapConfigPayload.STREAM_CODEC,
                ModNetworking::handleMapConfig
        );
        registrar.playToServer(
                DeleteMapPayload.TYPE,
                DeleteMapPayload.STREAM_CODEC,
                ModNetworking::handleDeleteMap
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

            // Le marqueur de zone interdite occupe la case, donc le test canBeReplaced()
            // ci-dessous suffirait déjà à refuser la pose — mais silencieusement. Testé à part
            // pour pouvoir expliquer le refus : le bloc étant invisible, le joueur n'a aucun
            // moyen de deviner pourquoi sa tour ne passe pas.
            if (serverLevel.getBlockState(pos).is(ModBlocks.NO_BUILD_ZONE.get())) {
                player.sendSystemMessage(Component.translatable("dungeon_defenders.tower.no_build_zone"));
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

            // Symétrique à la pose (ModEvents.onTowerPlace) : Construction ou Taverne.
            GamePhase phase = GamePhase.of(level);
            if (!phase.allowsTowerBuilding()) {
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

            // Aucun remboursement à la Taverne : la pose y est déjà gratuite, rembourser
            // reviendrait à imprimer du mana à volonté.
            int refund = phase == GamePhase.TAVERN
                    ? 0
                    : Math.round(tower.getManaCost() * TOWER_MANA_REFUND_RATIO);

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
            // Revérifié côté serveur : le client a listé les maps à l'ouverture de l'écran, mais
            // l'identifiant reçu reste une valeur venue du réseau. Introuvable (map supprimée
            // entre-temps, client bricolé...), on retombe sur l'arène provisoire plutôt que de
            // refuser silencieusement.
            MapInstance.startGame(serverLevel, MapRegistry.find(serverLevel, payload.structureId()).orElse(null));
        });
    }

    // Réglages d'une map, envoyés par MapConfigScreen. Créatif uniquement, comme la config d'un
    // spawner : une map est censée être figée une fois construite.
    private static void handleMapConfig(MapConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (level.isClientSide() || !player.isCreative()) {
                return;
            }

            BlockPos pos = payload.pos();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_DISTANCE_SQ) {
                return;
            }

            if (level.getBlockEntity(pos) instanceof MapConfigBlockEntity mapConfig) {
                mapConfig.applyConfig(payload.displayName(), payload.order(), payload.waveCount(), payload.scoreMultiplier());
            }
        });
    }

    // Suppression d'une map créée en jeu, après confirmation côté client. Ne peut effacer qu'un
    // fichier du dossier `generated/` de la sauvegarde : une map livrée dans un jar (la campagne,
    // un pack tiers) est une ressource en lecture seule, d'où le message d'échec.
    private static void handleDeleteMap(DeleteMapPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (!(level instanceof ServerLevel serverLevel) || !player.isCreative()) {
                return;
            }
            if (!MapDefinition.isMapStructure(payload.structureId())) {
                return;
            }

            StructureTemplateManager manager = serverLevel.getStructureManager();
            boolean deleted;
            try {
                Path file = manager.worldTemplates().createAndValidatePathToStructure(
                        payload.structureId(), StructureTemplateManager.WORLD_STRUCTURE_LISTER);
                deleted = Files.deleteIfExists(file);
            } catch (Exception exception) {
                LOGGER.warn("Suppression de la map {} impossible", payload.structureId(), exception);
                deleted = false;
            }

            // Vide le cache dans tous les cas : sans ça la map supprimée resterait listée
            // jusqu'au prochain rechargement du monde.
            manager.remove(payload.structureId());
            player.sendSystemMessage(Component.translatable(deleted
                    ? "dungeon_defenders.map_config.deleted"
                    : "dungeon_defenders.map_config.delete_failed", payload.structureId().toString()));
        });
    }

    // Bouton "Abandonner le niveau" du menu pause (voir client/PauseMenuClientEvents), après
    // confirmation côté client. Ramène TOUT LE MONDE à la taverne, pas seulement celui qui a
    // cliqué : une seule partie active à la fois sur tout le serveur, les joueurs vont et
    // viennent ensemble (même comportement que /dd_leave et que le bouton de GameOverScreen).
    private static void handleLeaveMap(LeaveMapPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }

            // Revérifié ici comme tout paquet : le client masque déjà le bouton hors partie,
            // mais il n'est jamais l'autorité. Abandonner depuis la taverne n'aurait aucun sens
            // (et relancerait un nettoyage de la zone de map pour rien).
            if (!GamePhase.of(level).isInGame()) {
                return;
            }

            MapInstance.returnToTavern(serverLevel);
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
