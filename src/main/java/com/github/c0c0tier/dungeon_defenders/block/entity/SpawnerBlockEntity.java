package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.DifficultyScaling;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.PhaseTransitions;
import com.github.c0c0tier.dungeon_defenders.init.SpawnableEnemy;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
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
    // Nombre de positions aléatoires essayées dans le rayon avant de replier sur pos.above()
    // (juste au-dessus du bloc, censé toujours être libre) — évite de faire apparaître un
    // ennemi à l'intérieur d'un bloc plein (mur, terrain irrégulier...) sans boucler indéfiniment.
    private static final int MAX_SPAWN_POSITION_ATTEMPTS = 8;

    /** Un type d'ennemi (parmi la liste fermée SpawnableEnemy), son nombre de base, et sa progression pour la vague en cours. */
    public static final class SpawnEntry {
        public static final Codec<SpawnEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("Enemy").forGetter(e -> e.enemy.ordinal()),
                Codec.INT.fieldOf("BaseCount").forGetter(e -> e.baseCount),
                Codec.INT.fieldOf("Spawned").forGetter(e -> e.spawned),
                Codec.INT.fieldOf("Accumulator").forGetter(e -> e.accumulator),
                Codec.INT.fieldOf("EffectiveTotal").forGetter(e -> e.effectiveTotal)
        ).apply(instance, (enemyOrdinal, baseCount, spawned, accumulator, effectiveTotal) ->
                new SpawnEntry(SpawnableEnemy.values()[enemyOrdinal], baseCount, spawned, accumulator, effectiveTotal)));

        private final SpawnableEnemy enemy;
        private final int baseCount;
        private int spawned;
        private int accumulator;
        private int effectiveTotal;

        public SpawnEntry(SpawnableEnemy enemy, int baseCount) {
            this(enemy, baseCount, 0, 0, baseCount);
        }

        private SpawnEntry(SpawnableEnemy enemy, int baseCount, int spawned, int accumulator, int effectiveTotal) {
            this.enemy = enemy;
            this.baseCount = Math.max(0, baseCount);
            this.spawned = spawned;
            this.accumulator = accumulator;
            this.effectiveTotal = effectiveTotal;
        }

        public SpawnableEnemy enemy() {
            return this.enemy;
        }

        public int baseCount() {
            return this.baseCount;
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

            BlockPos spawnPos = findSafeSpawnPos(level, pos, spawnRadius);
            this.enemy.entityType().spawn(level, spawnPos, EntitySpawnReason.SPAWNER);
            return true;
        }
    }

    /**
     * Choisit une position de spawn dans le rayon configuré, en évitant l'intérieur d'un bloc
     * plein (mur, terrain irrégulier...) : essaie plusieurs offsets aléatoires en vérifiant
     * que la position et celle juste au-dessus (place pour les pieds et la tête) sont toutes
     * les deux traversables, puis replie sur {@code pos} si aucune n'a marché.
     *
     * <p>Le repli est {@code pos} (la cellule du spawner lui-même), pas {@code pos.above()} :
     * depuis que le bloc spawner n'a plus jamais de collision (voir SpawnerBlock, décidé avec
     * le joueur le 2026-08-25 — plus un obstacle physique, comme dans le jeu de référence), il
     * ne peut plus servir de "sol" sous les pieds d'un monstre spawné juste au-dessus de lui.
     * {@code pos} lui-même, en revanche, est censé reposer sur le vrai sol construit par le
     * créateur de la map (le spawner n'est qu'un marqueur posé au niveau du sol, pas une
     * plateforme) — {@code pos.below()} porte donc le monstre, pas le bloc du spawner.
     */
    private static BlockPos findSafeSpawnPos(ServerLevel level, BlockPos pos, int spawnRadius) {
        BlockPos fallback = pos;
        if (spawnRadius <= 0) {
            return fallback;
        }

        for (int attempt = 0; attempt < MAX_SPAWN_POSITION_ATTEMPTS; attempt++) {
            BlockPos candidate = fallback.offset(
                    level.getRandom().nextInt(spawnRadius * 2 + 1) - spawnRadius,
                    0,
                    level.getRandom().nextInt(spawnRadius * 2 + 1) - spawnRadius);
            if (isPassable(level, candidate) && isPassable(level, candidate.above())) {
                return candidate;
            }
        }
        return fallback;
    }

    /** @return true si aucun bloc ne bloque le passage à cette position (pieds ou tête). */
    private static boolean isPassable(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    // Composition par défaut, avec les chiffres exacts de l'exemple du joueur
    // (15 gobelins / 5 orcs -> ici zombie/squelette, faute d'avoir plus d'ennemis).
    private List<SpawnEntry> entries = new ArrayList<>(List.of(
            new SpawnEntry(SpawnableEnemy.ZOMBIE, 15),
            new SpawnEntry(SpawnableEnemy.SKELETON, 5)
    ));

    private int intervalTicks = DEFAULT_INTERVAL_TICKS;
    private int spawnRadius;
    private int waveStart = 1;
    private int waveEnd = ModAttachments.MAX_WAVE;
    // 0 = aucune session de combat traitée pour l'instant ; force un premier resetForWave à
    // l'entrée en combat, puisque COMBAT_SESSION démarre à 0 mais est déjà incrémentée à 1
    // avant que le premier serverTick en combat ne s'exécute (voir PhaseTransitions#enterCombat).
    private int lastCombatSessionHandled;
    private int ticksSinceLastCheck;

    public SpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.SPAWNER_BE.get(), pos, state);
    }

    // --- REGISTRE DES SPAWNERS ACTIFS ---
    // S'enregistre/se désenregistre auprès de la Level (voir ModAttachments.ACTIVE_SPAWNERS)
    // pour que PhaseTransitions puisse sommer tous les spawners actifs à l'entrée en
    // Construction. setLevel(...) est appelé une fois par instance de block entity, à la pose
    // comme au chargement d'un chunk ; setRemoved() à la casse (et peut-être au déchargement
    // d'un chunk selon les cas — sans conséquence : une nouvelle instance se réenregistrera au
    // rechargement du chunk).

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getData(ModAttachments.ACTIVE_SPAWNERS).add(this.worldPosition);
            // Sinon le total affiché au HUD reste bloqué sur la valeur par défaut de
            // l'attachment tant qu'aucune vague n'a encore été nettoyée une première fois
            // (recomputeWaveEnemiesTotal n'était sinon appelée qu'aux transitions de phase).
            //
            // Différé via getServer().execute(...) et PAS appelé directement ici : setLevel()
            // est invoqué par LevelChunk.setBlockEntity() AVANT que ce block entity soit
            // inséré dans la table du chunk. recomputeWaveEnemiesTotal() appelle
            // level.getBlockEntity(pos) pour chaque spawner actif (potentiellement lui-même,
            // pas encore trouvable) — s'il ne le trouve pas, le chunk en recrée un exemplaire à
            // la volée, qui rappelle setLevel(), qui rappelle recomputeWaveEnemiesTotal(), etc.
            // : récursion infinie -> StackOverflowError (planté en jeu, voir
            // 05-etat-et-problemes-connus.md). Exécuter la recompute au tick suivant, une fois
            // l'enregistrement terminé, élimine la réentrance.
            serverLevel.getServer().execute(() -> PhaseTransitions.recomputeWaveEnemiesTotal(serverLevel));
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level instanceof ServerLevel serverLevel) {
            serverLevel.getData(ModAttachments.ACTIVE_SPAWNERS).remove(this.worldPosition);
            // Différé pour la même raison que dans setLevel() ci-dessus (setRemoved() peut lui
            // aussi être appelée pendant l'enregistrement d'un autre block entity, quand
            // LevelChunk.setBlockEntity remplace un exemplaire existant).
            serverLevel.getServer().execute(() -> PhaseTransitions.recomputeWaveEnemiesTotal(serverLevel));
        }
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

    /** @return la composition actuelle du spawner (copie défensive), pour que l'écran de config l'affiche. */
    public List<SpawnEntry> getEntries() {
        return List.copyOf(this.entries);
    }

    // --- ÉCRITURE (appliquée côté serveur par le handler du paquet de config) ---

    /**
     * Remplace entièrement la configuration du spawner (y compris sa composition) et
     * l'applique immédiatement : les plafonds de la vague en cours sont recalculés tout de
     * suite avec le multiplicateur de difficulté actuel, sans attendre le prochain
     * changement de vague détecté par serverTick. C'est un changement volontaire par rapport
     * à la première version (qui attendait la prochaine vague) : reconfigurer un spawner
     * doit se voir tout de suite dans le GUI comme en jeu.
     */
    public void applyConfig(int intervalTicks, int spawnRadius, int waveStart, int waveEnd, List<SpawnEntry> newEntries) {
        this.intervalTicks = Math.max(1, intervalTicks);
        this.spawnRadius = Math.max(0, spawnRadius);
        this.waveStart = Math.max(1, waveStart);
        this.waveEnd = Math.max(this.waveStart, waveEnd);
        if (!newEntries.isEmpty()) {
            this.entries = new ArrayList<>(newEntries);
        }

        if (this.level != null) {
            double multiplier = DifficultyScaling.getMultiplier(this.level);
            for (SpawnEntry entry : this.entries) {
                entry.resetForWave(multiplier);
            }
            this.lastCombatSessionHandled = this.level.getData(ModAttachments.COMBAT_SESSION);
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

        int combatSession = serverLevel.getData(ModAttachments.COMBAT_SESSION);
        if (combatSession != blockEntity.lastCombatSessionHandled) {
            double multiplier = DifficultyScaling.getMultiplier(serverLevel);
            for (SpawnEntry entry : blockEntity.entries) {
                entry.resetForWave(multiplier);
            }
            blockEntity.lastCombatSessionHandled = combatSession;
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
        output.putInt("LastCombatSessionHandled", this.lastCombatSessionHandled);

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
        this.lastCombatSessionHandled = input.getIntOr("LastCombatSessionHandled", 0);

        ValueInput.TypedInputList<SpawnEntry> savedEntries = input.listOrEmpty("Entries", SpawnEntry.CODEC);
        if (!savedEntries.isEmpty()) {
            List<SpawnEntry> loaded = new ArrayList<>();
            savedEntries.forEach(loaded::add);
            this.entries = loaded;
        }
        // Sinon : garde la composition par défaut du champ (spawner tout juste placé, jamais sauvegardé).
    }
}
