package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.DifficultyScaling;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

// Implémente l'algorithme de spawn pondéré de la feuille "Idées" du plan Excel du joueur :
// un accumulateur par type d'ennemi, incrémenté chaque contrôle de son "nombre de base" ; dès
// qu'il atteint le seuil, un ennemi de ce type spawn et le seuil lui est retiré. Le nombre de
// base sert aussi de plafond pour ce type : une fois atteint, le type est sauté (round-robin
// sur les types restants) jusqu'à ce que tous soient épuisés, comme dans l'exemple du joueur
// ("on a au total 15 gobelins et 5 orcs").
//
// Configurable par spawner (intervalle, rayon de spawn, plage de vagues, composition), en
// attendant le GUI qui l'exposera — voir 05-etat-et-problemes-connus.md.
public class SpawnerBlockEntity extends BlockEntity {

    private static final int DEFAULT_INTERVAL_TICKS = 20;
    private static final int SPAWN_THRESHOLD = 20;

    /** Un type d'ennemi, son nombre de base, et sa progression pour la vague en cours. */
    public static final class SpawnEntry {
        public static final Codec<SpawnEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("Type").forGetter(e -> e.type),
                Codec.INT.fieldOf("BaseCount").forGetter(e -> e.baseCount),
                Codec.INT.fieldOf("Spawned").forGetter(e -> e.spawned),
                Codec.INT.fieldOf("Accumulator").forGetter(e -> e.accumulator),
                Codec.INT.fieldOf("EffectiveTotal").forGetter(e -> e.effectiveTotal)
        ).apply(instance, SpawnEntry::new));

        private final EntityType<?> type;
        private int baseCount;
        private int spawned;
        private int accumulator;
        private int effectiveTotal;

        public SpawnEntry(EntityType<?> type, int baseCount) {
            this(type, baseCount, 0, 0, baseCount);
        }

        private SpawnEntry(EntityType<?> type, int baseCount, int spawned, int accumulator, int effectiveTotal) {
            this.type = type;
            this.baseCount = baseCount;
            this.spawned = spawned;
            this.accumulator = accumulator;
            this.effectiveTotal = effectiveTotal;
        }

        public EntityType<?> type() {
            return this.type;
        }

        public int baseCount() {
            return this.baseCount;
        }

        /** Changé par le GUI de config : ne prend effet qu'à la prochaine vague (voir resetForWave). */
        void setBaseCount(int baseCount) {
            this.baseCount = Math.max(0, baseCount);
        }

        /** Recalcule le plafond de la vague à partir du multiplicateur de difficulté et remet la progression à zéro. */
        void resetForWave(double multiplier) {
            this.effectiveTotal = Math.max(1, (int) Math.round(this.baseCount * multiplier));
            this.spawned = 0;
            this.accumulator = 0;
        }

        /** @return true si un spawn a eu lieu (pour savoir s'il faut marquer le block entity comme modifié). */
        boolean tickAndMaybeSpawn(ServerLevel level, BlockPos pos, int spawnRadius) {
            if (this.spawned >= this.effectiveTotal) {
                return false;
            }

            this.accumulator += this.effectiveTotal;
            if (this.accumulator < SPAWN_THRESHOLD) {
                return false;
            }
            this.accumulator -= SPAWN_THRESHOLD;
            this.spawned++;

            BlockPos spawnPos = spawnRadius <= 0
                    ? pos.above()
                    : pos.above().offset(
                            level.getRandom().nextInt(spawnRadius * 2 + 1) - spawnRadius,
                            0,
                            level.getRandom().nextInt(spawnRadius * 2 + 1) - spawnRadius);
            this.type.spawn(level, spawnPos, EntitySpawnReason.SPAWNER);
            return true;
        }
    }

    // Composition par défaut, avec les chiffres exacts de l'exemple du joueur
    // (15 gobelins / 5 orcs -> ici zombie/squelette, faute d'avoir plus d'ennemis).
    private List<SpawnEntry> entries = new ArrayList<>(List.of(
            new SpawnEntry(EntityType.ZOMBIE, 15),
            new SpawnEntry(EntityType.SKELETON, 5)
    ));

    private int intervalTicks = DEFAULT_INTERVAL_TICKS;
    private int spawnRadius;
    private int waveStart = 1;
    private int waveEnd = ModAttachments.MAX_WAVE;
    // 0 = aucune vague traitée pour l'instant ; force un premier resetForWave au démarrage
    // du combat, puisque CURRENT_WAVE démarre à 1 et ne vaudra donc jamais 0.
    private int lastWaveHandled;
    private int ticksSinceLastCheck;

    public SpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.SPAWNER_BE.get(), pos, state);
    }

    // --- LECTURE (GUI de config) ---

    public int getIntervalTicks() {
        return this.intervalTicks;
    }

    public int getSpawnRadius() {
        return this.spawnRadius;
    }

    public int getWaveStart() {
        return this.waveStart;
    }

    public int getWaveEnd() {
        return this.waveEnd;
    }

    /** @return le nombre de base configuré pour ce type, ou 0 s'il n'a pas d'entrée dans ce spawner. */
    public int getBaseCount(EntityType<?> type) {
        return this.entries.stream()
                .filter(entry -> entry.type() == type)
                .findFirst()
                .map(SpawnEntry::baseCount)
                .orElse(0);
    }

    // --- ÉCRITURE (appliquée côté serveur par le handler du paquet de config) ---

    /**
     * Applique la configuration reçue du GUI. Ne modifie pas la progression en cours : la
     * vague en cours (le cas échéant) continue avec les anciens plafonds jusqu'à la
     * prochaine détection de changement de vague, qui recalculera tout avec les nouvelles
     * valeurs (voir serverTick / resetForWave).
     */
    public void applyConfig(int intervalTicks, int spawnRadius, int waveStart, int waveEnd, int zombieBaseCount, int skeletonBaseCount) {
        this.intervalTicks = Math.max(1, intervalTicks);
        this.spawnRadius = Math.max(0, spawnRadius);
        this.waveStart = Math.max(1, waveStart);
        this.waveEnd = Math.max(this.waveStart, waveEnd);

        for (SpawnEntry entry : this.entries) {
            if (entry.type() == EntityType.ZOMBIE) {
                entry.setBaseCount(zombieBaseCount);
            } else if (entry.type() == EntityType.SKELETON) {
                entry.setBaseCount(skeletonBaseCount);
            }
        }

        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpawnerBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Le spawner ne tourne qu'en phase de combat : rien ne fait encore basculer cette
        // phase automatiquement (voir SpawnerBlock, harnais de test au clic droit).
        if (serverLevel.getData(ModAttachments.GAME_PHASE) != GamePhase.COMBAT.ordinal()) {
            return;
        }

        int currentWave = serverLevel.getData(ModAttachments.CURRENT_WAVE);
        if (currentWave < blockEntity.waveStart || currentWave > blockEntity.waveEnd) {
            return;
        }

        if (currentWave != blockEntity.lastWaveHandled) {
            double multiplier = DifficultyScaling.getMultiplier(serverLevel);
            for (SpawnEntry entry : blockEntity.entries) {
                entry.resetForWave(multiplier);
            }
            blockEntity.lastWaveHandled = currentWave;
            blockEntity.setChanged();
        }

        blockEntity.ticksSinceLastCheck++;
        if (blockEntity.ticksSinceLastCheck < blockEntity.intervalTicks) {
            return;
        }
        blockEntity.ticksSinceLastCheck = 0;

        boolean changed = false;
        for (SpawnEntry entry : blockEntity.entries) {
            if (entry.tickAndMaybeSpawn(serverLevel, pos, blockEntity.spawnRadius)) {
                changed = true;
            }
        }
        if (changed) {
            blockEntity.setChanged();
        }
    }

    // --- SYNCHRONISATION CLIENT ---
    // Nécessaire pour que l'écran de config (qui lit la copie cliente du block entity)
    // affiche la configuration à jour après un changement appliqué via applyConfig(...).

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    // --- PERSISTANCE ---

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("IntervalTicks", this.intervalTicks);
        output.putInt("SpawnRadius", this.spawnRadius);
        output.putInt("WaveStart", this.waveStart);
        output.putInt("WaveEnd", this.waveEnd);
        output.putInt("LastWaveHandled", this.lastWaveHandled);

        ValueOutput.TypedOutputList<SpawnEntry> list = output.list("Entries", SpawnEntry.CODEC);
        for (SpawnEntry entry : this.entries) {
            list.add(entry);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.intervalTicks = input.getIntOr("IntervalTicks", DEFAULT_INTERVAL_TICKS);
        this.spawnRadius = input.getIntOr("SpawnRadius", 0);
        this.waveStart = input.getIntOr("WaveStart", 1);
        this.waveEnd = input.getIntOr("WaveEnd", ModAttachments.MAX_WAVE);
        this.lastWaveHandled = input.getIntOr("LastWaveHandled", 0);

        ValueInput.TypedInputList<SpawnEntry> savedEntries = input.listOrEmpty("Entries", SpawnEntry.CODEC);
        if (!savedEntries.isEmpty()) {
            List<SpawnEntry> loaded = new ArrayList<>();
            savedEntries.forEach(loaded::add);
            this.entries = loaded;
        }
        // Sinon : garde la composition par défaut du champ (spawner tout juste placé, jamais sauvegardé).
    }
}
