package com.github.c0c0tier.dungeon_defenders;

import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.entity.ManaCrystalEntity;
import com.github.c0c0tier.dungeon_defenders.entity.TrainingDummyEntity;
import com.github.c0c0tier.dungeon_defenders.entity.ai.AttackPriorityTargetGoal;
import com.github.c0c0tier.dungeon_defenders.entity.ai.RangedAttackEterniaCrystalGoal;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ManaCrystalType;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.PhaseTransitions;
import com.github.c0c0tier.dungeon_defenders.init.ScoreSource;
import com.github.c0c0tier.dungeon_defenders.init.SpawnableEnemy;
import com.github.c0c0tier.dungeon_defenders.network.ScoreGainPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public class ModEvents {

    // Vie maximale par défaut d'un joueur (vanilla : 20.0). Lue directement par HealthOverlay
    // via player.getMaxHealth(), pas besoin de la partager ailleurs.
    private static final double PLAYER_MAX_HEALTH = 100.0D;

    // Décidé avec le joueur : pas de mécanique de faim dans ce mod, la barre est déjà masquée
    // (DungeonDefendersModClient) mais rien n'empêchait encore la faim de baisser en arrière-
    // plan (sprint, saut, minage...) — repoussée à son maximum à chaque tick serveur plutôt
    // que d'essayer d'annuler chaque source d'exhaustion une par une.
    private static final int FULL_FOOD_LEVEL = 20;
    private static final float FULL_SATURATION = 20.0F;

    @SubscribeEvent
    public static void onMonsterSpawn(EntityJoinLevelEvent event) {
        // Généralisé de Zombie à Monster (les deux goals n'exigent qu'un PathfinderMob, que
        // Monster étend) pour couvrir tout futur ennemi sans avoir à énumérer chaque type.
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Monster monster)) {
            return;
        }

        // Le mannequin d'entraînement est un Monster (obligatoire pour que les tourelles le
        // ciblent, voir TrainingDummyEntity) mais ne doit surtout pas recevoir de goal : il est
        // censé rester planté là, pas partir attaquer le Cristal d'Eternia.
        if (monster instanceof TrainingDummyEntity) {
            return;
        }

        // EntityJoinLevelEvent se déclenche aussi au rechargement d'un chunk ou au
        // changement de dimension : sans ce test, un même monstre cumulerait plusieurs
        // fois le goal et attaquerait le cristal plusieurs fois par seconde.
        boolean alreadyAdded = monster.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof RangedAttackEterniaCrystalGoal
                        || wrapped.getGoal() instanceof AttackPriorityTargetGoal);
        if (alreadyAdded) {
            return;
        }

        // Les squelettes (et tout futur AbstractSkeleton) attaquent à distance avec l'arc
        // déjà équipé par défaut, et ignorent Blockade/Turret (un archer peut tirer par-dessus/
        // à côté sans avoir besoin de les détruire) ; les autres reçoivent un seul goal qui
        // choisit lui-même la meilleure cible à portée selon les paliers de priorité (voir
        // AiAttackTarget) : Block, puis Corps à corps, puis Cristal, puis Tourelle en dernier
        // recours.
        if (monster instanceof AbstractSkeleton) {
            monster.goalSelector.addGoal(1, new RangedAttackEterniaCrystalGoal(monster));
        } else {
            monster.goalSelector.addGoal(0, new AttackPriorityTargetGoal(monster));
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

    // Décidé avec le joueur (2026-08-26) : aucun bloc ne se casse en jeu, point - pas de
    // minage, pas de récolte, ce n'est pas ce genre de jeu. Générique à tout bloc (pas
    // seulement les tours), donc aucun cas particulier ailleurs dans le mod n'est nécessaire.
    // Réservé aux joueurs non créatifs : le créatif reste le seul moyen de construire/modifier
    // une map, même principe que partout ailleurs dans le mod (spawner, coffre de mana...).
    //
    // BreakBlockEvent (pas BlockEvent.BreakEvent, qui n'existe plus dans cette version) se
    // déclenche indépendamment côté client ET côté serveur (voir sa javadoc) : annulé sans
    // condition de camp pour stopper net la prédiction client autant que la casse réelle
    // côté serveur. Le message n'est envoyé que côté serveur (isClientSide() == false) pour
    // ne pas l'afficher en double (une fois localement côté client, une fois via le paquet
    // serveur).
    @SubscribeEvent
    public static void onBlockBreakAttempt(BreakBlockEvent event) {
        Player player = event.getPlayer();
        if (player.isCreative()) {
            return;
        }

        event.setCanceled(true);
        if (!event.getLevel().isClientSide()) {
            player.sendSystemMessage(Component.translatable("dungeon_defenders.block.break_disabled"));
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
        // se redéclenche aussi dans ces cas-là, comme pour onMonsterSpawn ci-dessus).
        boolean wasAtPreviousMax = player.getHealth() >= maxHealthAttribute.getValue();
        maxHealthAttribute.setBaseValue(PLAYER_MAX_HEALTH);
        if (wasAtPreviousMax) {
            player.setHealth((float) maxHealthAttribute.getValue());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        player.getFoodData().setFoodLevel(FULL_FOOD_LEVEL);
        player.getFoodData().setSaturation(FULL_SATURATION);
    }

    @SubscribeEvent
    public static void onMonsterDeath(LivingDeathEvent event) {
        Level level = event.getEntity().level();
        if (level.isClientSide() || !(event.getEntity() instanceof Monster monster)) {
            return;
        }

        // Décidé avec le joueur : tous les monstres, à chaque mort, quelle que soit la phase
        // (contrairement au comptage de vague juste en dessous, qui reste Combat uniquement).
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.addFreshEntity(new ManaCrystalEntity(
                    serverLevel, monster.getX(), monster.getY(), monster.getZ(), ManaCrystalType.SMALL.value()));
        }

        awardExperienceAndScore(level, monster);

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

    // Score de la carte (Level) + XP de personnage (joueur) pour chaque monstre tué, quelle
    // que soit la phase — même inconditionnel que le drop de cristal de mana ci-dessus. Décidé
    // avec le joueur : partagé entre TOUS les joueurs présents plutôt qu'attribué à celui qui a
    // porté le coup fatal, parce que ce sont surtout les tours qui tuent dans ce mod (aucune
    // notion de "quel joueur a tué quoi" n'existe aujourd'hui) — même logique co-op que le
    // ramassage des cristaux de mana, ouvert à tous.
    private static void awardExperienceAndScore(Level level, Monster monster) {
        int xpValue = SpawnableEnemy.xpValueFor(monster.getType());

        // NO_ENEMY en repli si jamais ce monstre n'est pas dans la liste fermée du Spawner
        // (même repli défensif que SpawnableEnemy.xpValueFor juste au-dessus).
        int enemyOrdinal = SpawnableEnemy.find(monster.getType())
                .map(SpawnableEnemy::ordinal)
                .orElse(ScoreGainPayload.NO_ENEMY);
        grantScore(level, xpValue, ScoreSource.MONSTER_KILLED, enemyOrdinal);

        for (Player player : level.players()) {
            grantExperience(player, xpValue);
        }
    }

    // Centralise tout gain de score : met à jour ModAttachments.SCORE (le total, lu par
    // ScoreOverlay) ET diffuse un ScoreGainPayload à chaque joueur présent (le détail, lu par
    // ScoreGainOverlay côté client — voir ce paquet pour le pourquoi des deux canaux distincts).
    // Toute future source de score (fin de vague, fin de map, multiplicateurs — voir
    // doc/02-gameplay.md) doit passer par ici plutôt que toucher SCORE directement, pour ne pas
    // dupliquer cette double mise à jour ; passer ScoreGainPayload.NO_ENEMY si le gain n'a pas
    // d'ennemi associé (tout ce qui n'est pas un kill).
    private static void grantScore(Level level, int amount, ScoreSource source, int enemyOrdinal) {
        int score = level.getData(ModAttachments.SCORE) + amount;
        level.setData(ModAttachments.SCORE, score);
        level.syncData(ModAttachments.SCORE);

        for (Player player : level.players()) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(
                        new ScoreGainPayload(amount, source.ordinal(), enemyOrdinal).toVanillaClientbound());
            }
        }
    }

    // Boucle plutôt qu'un seul passage : un futur ennemi à forte valeur d'XP pourrait franchir
    // plusieurs paliers de niveau d'un coup. MAX_EXPERIENCE reste un plafond fixe par niveau
    // pour l'instant (pas de barème croissant), comme les autres valeurs de test du mod.
    private static void grantExperience(Player player, int xpValue) {
        int experience = player.getData(ModAttachments.EXPERIENCE) + xpValue;
        int level = player.getData(ModAttachments.LEVEL);

        while (experience >= ModAttachments.MAX_EXPERIENCE) {
            experience -= ModAttachments.MAX_EXPERIENCE;
            level++;
        }

        boolean leveledUp = level != player.getData(ModAttachments.LEVEL);

        player.setData(ModAttachments.EXPERIENCE, experience);
        player.syncData(ModAttachments.EXPERIENCE);
        player.setData(ModAttachments.LEVEL, level);
        player.syncData(ModAttachments.LEVEL);

        if (leveledUp) {
            player.sendSystemMessage(Component.translatable("dungeon_defenders.level.up", level));
        }
    }

    // Les monstres de ce mod n'ont thématiquement aucune raison de donner de la vraie XP
    // Minecraft (le système "experience" du mod est déjà séparé et sans rapport) — annulé ici
    // plutôt que laissé tel quel. Empêche aussi un vrai risque de bug : ExperienceOrb fusionne
    // automatiquement les orbes proches de même valeur (voir ManaCrystalEntity), une vraie
    // orbe d'XP vanilla pourrait sinon fusionner avec un cristal de mana et corrompre le
    // ramassage.
    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof Monster) {
            event.setCanceled(true);
        }
    }

    // SUPPRIMÉ en fusionnant feature/mana-crystals et feature/tower-removal (intégration
    // locale de test, 2026-08-26) : ce handler existait sur feature/mana-crystals pour
    // rembourser du mana au clic-pioche, avant que feature/tower-removal n'introduise la
    // touche dédiée (voir ModNetworking.handleRemoveTower, même TOWER_MANA_REFUND_RATIO) comme
    // unique vraie façon de retirer une tour, ET onBlockBreakAttempt ci-dessus qui annule déjà
    // toute casse de bloc en survie. Les deux mécanismes ensemble auraient été exploitables :
    // onTowerBreak ne vérifiait pas event.isCanceled() avant de créditer le remboursement, donc
    // un joueur non créatif aurait pu obtenir du mana gratuit en "cassant" une tour dont la
    // casse était en fait annulée par onBlockBreakAttempt, sans jamais la détruire pour de vrai.
    // À régler pour de vrai quand ces deux PR seront réconciliées (celle qui merge en second
    // devra retirer ce handler du même coup) — voir la conversation avec le joueur.
}
