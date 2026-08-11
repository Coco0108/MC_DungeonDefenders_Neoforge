package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// Implémente l'algorithme de spawn pondéré de la feuille "Idées" du plan Excel du joueur :
// un accumulateur par type d'ennemi, incrémenté chaque contrôle du poids de ce type ; dès
// qu'il atteint le seuil, un ennemi de ce type spawn et le seuil lui est retiré. Plus un type
// a un poids élevé, plus il sort souvent — c'est le poids qui fait office de "nombre total
// voulu sur la vague".
//
// V1 volontairement simple : une seule composition fixe (zombies), pas de GUI de
// configuration (slots d'œufs, multiplicateurs...) — voir 05-etat-et-problemes-connus.md.
public class SpawnerBlockEntity extends BlockEntity {

    // Poids du zombie (= nombre "voulu" sur la vague, au sens de l'algorithme) et seuil de
    // déclenchement, repris tels quels de l'exemple du joueur.
    private static final int ZOMBIE_WEIGHT = 15;
    private static final int SPAWN_THRESHOLD = 20;
    // L'algorithme tourne une fois par seconde plutôt qu'à chaque tick, pour rester lisible.
    private static final int TICKS_BETWEEN_CHECKS = 20;

    private int zombieAccumulator;
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

        blockEntity.zombieAccumulator += ZOMBIE_WEIGHT;
        if (blockEntity.zombieAccumulator >= SPAWN_THRESHOLD) {
            EntityType.ZOMBIE.spawn(serverLevel, pos.above(), EntitySpawnReason.SPAWNER);
            blockEntity.zombieAccumulator -= SPAWN_THRESHOLD;
            blockEntity.setChanged();
        }
    }

    // --- PERSISTANCE ---
    // Seul l'accumulateur est sauvegardé : ticksSinceLastCheck n'est qu'un minuteur
    // sub-seconde, le perdre au rechargement n'a aucune conséquence visible.

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("ZombieAccumulator", this.zombieAccumulator);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.zombieAccumulator = input.getIntOr("ZombieAccumulator", 0);
    }
}
