package com.github.c0c0tier.dungeon_defenders;

import com.github.c0c0tier.dungeon_defenders.entity.ai.AttackEterniaCrystalGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onZombieSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Zombie zombie)) {
            return;
        }

        // EntityJoinLevelEvent se déclenche aussi au rechargement d'un chunk ou au
        // changement de dimension : sans ce test, un même zombie cumulerait plusieurs
        // fois le goal et frapperait le cristal plusieurs fois par seconde.
        boolean alreadyAdded = zombie.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof AttackEterniaCrystalGoal);

        if (!alreadyAdded) {
            zombie.goalSelector.addGoal(1, new AttackEterniaCrystalGoal(zombie));
        }
    }
}
