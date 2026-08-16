package com.github.c0c0tier.dungeon_defenders;

import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.entity.ai.AttackBlockadeGoal;
import com.github.c0c0tier.dungeon_defenders.entity.ai.AttackEterniaCrystalGoal;
import com.github.c0c0tier.dungeon_defenders.entity.ai.RangedAttackEterniaCrystalGoal;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.PhaseTransitions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public class ModEvents {

    // Vie maximale par défaut d'un joueur (vanilla : 20.0). Lue directement par HealthOverlay
    // via player.getMaxHealth(), pas besoin de la partager ailleurs.
    private static final double PLAYER_MAX_HEALTH = 100.0D;

    @SubscribeEvent
    public static void onMonsterSpawn(EntityJoinLevelEvent event) {
        // Généralisé de Zombie à Monster (les deux goals n'exigent qu'un PathfinderMob, que
        // Monster étend) pour couvrir tout futur ennemi sans avoir à énumérer chaque type.
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Monster monster)) {
            return;
        }

        // EntityJoinLevelEvent se déclenche aussi au rechargement d'un chunk ou au
        // changement de dimension : sans ce test, un même monstre cumulerait plusieurs
        // fois le goal et attaquerait le cristal plusieurs fois par seconde.
        boolean alreadyAdded = monster.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof AttackEterniaCrystalGoal
                        || wrapped.getGoal() instanceof RangedAttackEterniaCrystalGoal
                        || wrapped.getGoal() instanceof AttackBlockadeGoal);
        if (alreadyAdded) {
            return;
        }

        // Les squelettes (et tout futur AbstractSkeleton) attaquent à distance avec l'arc
        // déjà équipé par défaut ; les autres, au corps à corps. Seule la mêlée reçoit en plus
        // AttackBlockadeGoal (priorité 0, avant le cristal en priorité 1) : un archer peut
        // tirer par-dessus/à côté d'un Spike Blockade sans avoir besoin de le détruire.
        if (monster instanceof AbstractSkeleton) {
            monster.goalSelector.addGoal(1, new RangedAttackEterniaCrystalGoal(monster));
        } else {
            monster.goalSelector.addGoal(0, new AttackBlockadeGoal(monster));
            monster.goalSelector.addGoal(1, new AttackEterniaCrystalGoal(monster));
        }
    }

    // Générique à TOUTE catégorie de tour (Blockade, Turret, ...) : filtre sur
    // AbstractTowerBlockEntity, pas une catégorie précise, sinon une nouvelle catégorie
    // (catégories sœurs, pas descendantes les unes des autres) échapperait silencieusement à
    // la vérification de mana/phase. Un seul handler, jamais dupliqué par catégorie.
    @SubscribeEvent
    public static void onTowerPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof AbstractTowerBlockEntity tower)) {
            return;
        }

        if (!(event.getLevel() instanceof Level level)) {
            return;
        }

        // Décidé avec le joueur : les tours ne se posent qu'en phase Construction, jamais
        // pendant le Combat — vérifié avant même le mana, pour ne pas laisser croire qu'un
        // refus vient d'un manque de mana alors que c'est la phase qui bloque.
        if (level.getData(ModAttachments.GAME_PHASE) != GamePhase.BUILD.ordinal()) {
            player.sendSystemMessage(Component.translatable("dungeon_defenders.tower.build_phase_only"));
            event.setCanceled(true);
            return;
        }

        int manaCost = tower.getManaCost();
        int currentMana = player.getData(ModAttachments.MANA);

        if (currentMana < manaCost) {
            player.sendSystemMessage(Component.translatable(
                    "dungeon_defenders.tower.not_enough_mana", manaCost, currentMana));
            // Annule le placement : NeoForge restaure le bloc précédent (via le BlockSnapshot)
            // et rend l'item au joueur, comme si la pose n'avait jamais eu lieu.
            event.setCanceled(true);
            return;
        }

        int newMana = currentMana - manaCost;
        player.setData(ModAttachments.MANA, newMana);
        player.syncData(ModAttachments.MANA);
        player.sendSystemMessage(Component.translatable(
                "dungeon_defenders.tower.mana_spent", manaCost, newMana, ModAttachments.MAX_MANA));
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
        // se redéclenche aussi dans ces cas-là, comme pour onMonsterSpawn ci-dessus).
        boolean wasAtPreviousMax = player.getHealth() >= maxHealthAttribute.getValue();
        maxHealthAttribute.setBaseValue(PLAYER_MAX_HEALTH);
        if (wasAtPreviousMax) {
            player.setHealth((float) maxHealthAttribute.getValue());
        }
    }

    @SubscribeEvent
    public static void onMonsterDeath(LivingDeathEvent event) {
        Level level = event.getEntity().level();
        if (level.isClientSide() || !(event.getEntity() instanceof Monster)) {
            return;
        }

        // Ne compte que les morts pendant le combat : un zombie qui traîne encore en phase
        // de construction (rechargement de chunk, etc.) ne doit pas fausser le compteur.
        if (level.getData(ModAttachments.GAME_PHASE) != GamePhase.COMBAT.ordinal()) {
            return;
        }

        int killed = level.getData(ModAttachments.WAVE_ENEMIES_KILLED) + 1;
        level.setData(ModAttachments.WAVE_ENEMIES_KILLED, killed);
        level.syncData(ModAttachments.WAVE_ENEMIES_KILLED);

        // total > 0 : évite un retour en Construction immédiat si aucun spawner actif n'a
        // encore pu contribuer au total (ex. mort d'un monstre resté du combat précédent
        // avant qu'un spawner n'ait retick).
        int total = level.getData(ModAttachments.WAVE_ENEMIES_TOTAL);
        if (total > 0 && killed >= total) {
            // Capturé avant enterBuild/onVictory, qui font tous les deux avancer/réinitialiser
            // current_wave : c'est la vague qu'on vient de nettoyer qui détermine la victoire,
            // pas celle qui suit.
            boolean wasLastWave = level.getData(ModAttachments.CURRENT_WAVE) >= ModAttachments.MAX_WAVE;
            if (wasLastWave) {
                PhaseTransitions.onVictory(level);
            } else {
                PhaseTransitions.enterBuild(level);
                level.players().forEach(player -> player.sendSystemMessage(
                        Component.translatable("dungeon_defenders.spawner.wave_cleared")));
            }
        }
    }
}
