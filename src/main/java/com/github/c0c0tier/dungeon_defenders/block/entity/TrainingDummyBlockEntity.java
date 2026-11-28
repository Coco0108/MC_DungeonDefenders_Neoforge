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

// Entretient le mannequin d'entraînement du bloc : s'il n'y en a pas à sa position, en invoque
// un. Aucun état à persister — contrairement à SpawnerBlockEntity, il n'y a rien à configurer,
// ce block entity n'existait que pour avoir un tick serveur. Depuis le 2026-11-28, il porte aussi le modèle animé
// dd_dummy de l'Atelier (AnimatedBlockEntity) : l'entité lui fait jouer hurt ou hurt_heavy à chaque coup encaissé
// (voir TrainingDummyEntity#hurtServer et onHit).
public class TrainingDummyBlockEntity extends AnimatedBlockEntity {

    /** À partir de ces dégâts en un coup, le mannequin joue {@code hurt_heavy} au lieu de {@code hurt}. */
    public static final float HEAVY_HIT_DAMAGE = 8.0F;

    // Un contrôle par seconde suffit largement : le mannequin ne peut disparaître qu'à un
    // rechargement de monde ou si quelqu'un le supprime à la main. Inutile de balayer les
    // entités 20 fois par seconde pour ça.
    private static final int CHECK_INTERVAL_TICKS = 20;
    // Marge autour de la position du bloc : le mannequin ne bouge pas (setNoAi), mais il fait
    // presque deux blocs de haut et sa boîte englobante dépasse forcément de la case visée.
    private static final double SEARCH_RADIUS = 1.5D;

    public TrainingDummyBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.TRAINING_DUMMY_BE.get(), pos, state, "dd_dummy");
    }

    /** Le mannequin a encaissé {@code damage} : {@code hurt_heavy} pour un gros coup (s'il existe), sinon {@code hurt}. */
    public void onHit(float damage) {
        if (!(damage >= HEAVY_HIT_DAMAGE && jouer("hurt_heavy"))) {
            jouer("hurt");
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TrainingDummyBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel) || level.getGameTime() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        if (!findDummies(serverLevel, pos).isEmpty()) {
            return;
        }

        // EntitySpawnReason.TRIGGERED : invoqué par un mécanisme, pas par le spawn naturel ni
        // par une commande — même famille que ce que fait SpawnerBlockEntity (SPAWNER).
        //
        // À la position du bloc lui-même, pas au-dessus (changé le 2026-09-06, retour en jeu :
        // le mannequin apparaissait flottant un bloc plus haut que l'endroit où le support avait
        // été posé dans la structure). Comme PlayerSpawnBlock, le marqueur EST l'endroit visé,
        // pas la case juste en dessous — cohérent entre les deux marqueurs, et sans conséquence
        // ici puisque le mannequin n'a de toute façon pas besoin de sol sous ses pieds (setNoAi).
        ModEntities.TRAINING_DUMMY.get().spawn(serverLevel, pos, EntitySpawnReason.TRIGGERED);
    }

    /**
     * Supprime le mannequin de ce bloc. Appelé quand le bloc est retiré ({@code
     * TrainingDummyBlock#affectNeighborsAfterRemoval}) : sans ça, casser le support en créatif
     * laisserait une entité orpheline que plus rien ne gère.
     */
    public static void discardDummy(ServerLevel level, BlockPos pos) {
        for (TrainingDummyEntity dummy : findDummies(level, pos)) {
            dummy.discard();
        }
    }

    private static List<TrainingDummyEntity> findDummies(ServerLevel level, BlockPos dummyPos) {
        return level.getEntitiesOfClass(TrainingDummyEntity.class, new AABB(dummyPos).inflate(SEARCH_RADIUS));
    }
}
