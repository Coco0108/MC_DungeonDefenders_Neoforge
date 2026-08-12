package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

// Implémente l'algorithme de spawn pondéré de la feuille "Idées" du plan Excel du joueur :
// un accumulateur par type d'ennemi, incrémenté chaque contrôle du poids de ce type ; dès
// qu'il atteint le seuil, un ennemi de ce type spawn et le seuil lui est retiré. Plus un type
// a un poids élevé, plus il sort souvent — c'est le poids qui fait office de "nombre total
// voulu sur la vague".
//
// V1 volontairement simple : une composition fixe (SPAWN_TABLE ci-dessous), pas de GUI de
// configuration (slots d'œufs, multiplicateurs...) — voir 05-etat-et-problemes-connus.md.
public class SpawnerBlockEntity extends BlockEntity {

    /** Un type d'ennemi et son poids dans l'algorithme (voir la classe pour le principe). */
    private record SpawnEntry(EntityType<? extends Monster> type, int weight) {
    }

    // Poids repris tels quels de l'exemple du joueur (15 gobelins / 5 orcs), juste avec les
    // deux ennemis dont on dispose pour l'instant.
    private static final List<SpawnEntry> SPAWN_TABLE = List.of(
            new SpawnEntry(EntityType.ZOMBIE, 15),
            new SpawnEntry(EntityType.SKELETON, 5)
    );
    private static final int SPAWN_THRESHOLD = 20;
    // L'algorithme tourne une fois par seconde plutôt qu'à chaque tick, pour rester lisible.
    private static final int TICKS_BETWEEN_CHECKS = 20;

    // Un accumulateur par entrée de SPAWN_TABLE, même index.
    private final int[] accumulators = new int[SPAWN_TABLE.size()];
    private int ticksSinceLastCheck;

    public SpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.SPAWNER_BE.get(), pos, state);
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

        blockEntity.ticksSinceLastCheck++;
        if (blockEntity.ticksSinceLastCheck < TICKS_BETWEEN_CHECKS) {
            return;
        }
        blockEntity.ticksSinceLastCheck = 0;

        for (int i = 0; i < SPAWN_TABLE.size(); i++) {
            SpawnEntry entry = SPAWN_TABLE.get(i);
            blockEntity.accumulators[i] += entry.weight();

            if (blockEntity.accumulators[i] >= SPAWN_THRESHOLD) {
                entry.type().spawn(serverLevel, pos.above(), EntitySpawnReason.SPAWNER);
                blockEntity.accumulators[i] -= SPAWN_THRESHOLD;
                blockEntity.setChanged();
            }
        }
    }

    // --- PERSISTANCE ---
    // Seuls les accumulateurs sont sauvegardés : ticksSinceLastCheck n'est qu'un minuteur
    // sub-seconde, le perdre au rechargement n'a aucune conséquence visible.

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putIntArray("Accumulators", this.accumulators);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int[] saved = input.getIntArray("Accumulators").orElse(new int[SPAWN_TABLE.size()]);
        // Se protège d'un SPAWN_TABLE agrandi/réduit depuis la dernière sauvegarde plutôt
        // que de planter sur un désalignement de taille.
        System.arraycopy(saved, 0, this.accumulators, 0, Math.min(saved.length, this.accumulators.length));
    }
}
