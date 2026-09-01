package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.entity.TrainingDummyEntity;
import com.github.c0c0tier.dungeon_defenders.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

// Entretient le mannequin d'entraînement du bloc : s'il n'y en a pas juste au-dessus, en invoque
// un. Aucun état à persister — contrairement à SpawnerBlockEntity, il n'y a rien à configurer,
// ce block entity n'existe que pour avoir un tick serveur.
public class TrainingDummyBlockEntity extends BlockEntity {

    // Un contrôle par seconde suffit largement : le mannequin ne peut disparaître qu'à un
    // rechargement de monde ou si quelqu'un le supprime à la main. Inutile de balayer les
    // entités 20 fois par seconde pour ça.
    private static final int CHECK_INTERVAL_TICKS = 20;
    // Marge autour du bloc du dessus : le mannequin ne bouge pas (setNoAi), mais il fait presque
    // deux blocs de haut et sa boîte englobante dépasse forcément de la case visée.
    private static final double SEARCH_RADIUS = 1.5D;

    public TrainingDummyBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.TRAINING_DUMMY_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TrainingDummyBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel) || level.getGameTime() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        BlockPos dummyPos = pos.above();
        if (!findDummies(serverLevel, dummyPos).isEmpty()) {
            return;
        }

        // EntitySpawnReason.TRIGGERED : invoqué par un mécanisme, pas par le spawn naturel ni
        // par une commande — même famille que ce que fait SpawnerBlockEntity (SPAWNER).
        ModEntities.TRAINING_DUMMY.get().spawn(serverLevel, dummyPos, EntitySpawnReason.TRIGGERED);
    }

    /**
     * Supprime le mannequin de ce bloc. Appelé quand le bloc est retiré ({@code
     * TrainingDummyBlock#affectNeighborsAfterRemoval}) : sans ça, casser le support en créatif
     * laisserait une entité orpheline que plus rien ne gère.
     */
    public static void discardDummy(ServerLevel level, BlockPos pos) {
        for (TrainingDummyEntity dummy : findDummies(level, pos.above())) {
            dummy.discard();
        }
    }

    private static List<TrainingDummyEntity> findDummies(ServerLevel level, BlockPos dummyPos) {
        return level.getEntitiesOfClass(TrainingDummyEntity.class, new AABB(dummyPos).inflate(SEARCH_RADIUS));
    }
}
