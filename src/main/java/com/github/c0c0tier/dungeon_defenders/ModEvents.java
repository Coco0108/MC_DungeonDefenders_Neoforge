package com.github.c0c0tier.dungeon_defenders;

import com.github.c0c0tier.dungeon_defenders.entity.ai.AttackEterniaCrystalGoal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public class ModEvents {

    // Vie maximale par défaut d'un joueur (vanilla : 20.0). Lue directement par HealthOverlay
    // via player.getMaxHealth(), pas besoin de la partager ailleurs.
    private static final double PLAYER_MAX_HEALTH = 100.0D;

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

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Player player)) {
            return;
        }

        AttributeInstance maxHealthAttribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttribute == null) {
            return;
        }

        // Ne complète la vie que si le joueur était déjà à son ancien maximum, pour ne pas
        // effacer des dégâts en cours à chaque rejointe/changement de dimension (l'événement
        // se redéclenche aussi dans ces cas-là, comme pour onZombieSpawn ci-dessus).
        boolean wasAtPreviousMax = player.getHealth() >= maxHealthAttribute.getValue();
        maxHealthAttribute.setBaseValue(PLAYER_MAX_HEALTH);
        if (wasAtPreviousMax) {
            player.setHealth((float) maxHealthAttribute.getValue());
        }
    }
}
