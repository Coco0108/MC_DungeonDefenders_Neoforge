package com.github.c0c0tier.dungeon_defenders;

import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.ModBlocks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onZombieSpawn(EntityJoinLevelEvent event) {
        // Dès qu'un Zombie apparaît dans le monde (via un œuf ou naturellement)
        if (event.getEntity() instanceof Zombie zombie && !event.getLevel().isClientSide()) {

            // On lui ajoute une tâche prioritaire : foncer sur le cristal et le frapper
            zombie.goalSelector.addGoal(1, new MoveToBlockGoal(zombie, 1.2D, 16) {

                @Override
                protected boolean isValidTarget(LevelReader level, BlockPos pos) {
                    // Le zombie cherche le bloc de ton cristal
                    return level.getBlockState(pos).is(ModBlocks.ETERNIA_CRYSTAL.get());
                }

                @Override
                protected BlockPos getMoveToTarget() {
                    // On vise la BASE du cristal (et non le dessus) : le zombie attaque
                    // depuis le sol adjacent au lieu d'essayer de grimper dessus.
                    return this.blockPos;
                }

                @Override
                public double acceptedDistance() {
                    // Distance suffisante pour qu'un zombie collé au cristal (boîte de 3 de haut)
                    // soit considéré "arrivé" depuis le sol.
                    return 2.1D;
                }

                @Override
                public void tick() {
                    super.tick();
                    // Si le zombie est assez proche du cristal
                    if (this.isReachedTarget()) {
                        BlockPos crystalPos = this.blockPos;
                        var level = mob.level();

                        // Toutes les 20 vécus (1 seconde), il donne un coup
                        if (mob.tickCount % 20 == 0) {
                            if (level.getBlockEntity(crystalPos) instanceof EterniaCrystalBlockEntity crystal) {
                                int currentHealth = crystal.getCrystalHealth();
                                crystal.setCrystalHealth(currentHealth - 5); // Il retire 5 PV

                                // On fait jouer l'animation de bras au zombie
                                mob.swing(InteractionHand.MAIN_HAND);
                            }
                        }
                    }
                }
            });
        }
    }
}