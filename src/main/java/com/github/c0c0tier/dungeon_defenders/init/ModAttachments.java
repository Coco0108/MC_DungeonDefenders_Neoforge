package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class ModAttachments {
    // Mana maximal par défaut d'un joueur. À externaliser dans Config si des sorts
    // ou upgrades doivent le faire varier.
    public static final int MAX_MANA = 100;

    // Expérience custom maximale par défaut (rien à voir avec l'XP vanilla). Valeur
    // provisoire tant que la façon d'en gagner/monter de niveau n'est pas définie.
    public static final int MAX_EXPERIENCE = 100;

    // Nombre de vagues par défaut. Valeur provisoire tant que le déroulement d'une partie
    // (déclenchement d'une vague, condition de victoire/défaite) n'est pas défini.
    public static final int MAX_WAVE = 5;

    // Nombre d'ennemis par défaut dans une vague. Valeur provisoire tant que la génération
    // des vagues (nombre d'ennemis, difficulté croissante...) n'est pas définie.
    public static final int DEFAULT_WAVE_ENEMIES_TOTAL = 10;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, DungeonDefendersMod.MODID);

    // Mana courant du joueur : plein par défaut, persistant, synchronisé au client
    // pour l'affichage HUD.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> MANA = ATTACHMENT_TYPES.register(
            "mana",
            () -> AttachmentType.builder(() -> MAX_MANA)
                    .serialize(Codec.INT.fieldOf("Mana"))
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    // Expérience custom du joueur : vide par défaut (contrairement au mana/à la vie, elle se
    // gagne au lieu de se dépenser), persistante, synchronisée au client pour le HUD.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> EXPERIENCE = ATTACHMENT_TYPES.register(
            "experience",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT.fieldOf("Experience"))
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    // Vague en cours : commence à 1, persistante, synchronisée au client pour le HUD.
    // Contrairement au mana/à la vie/à l'expérience, c'est un état du monde (Level), pas du
    // joueur : une vague concerne toute la partie, pas un joueur en particulier.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> CURRENT_WAVE = ATTACHMENT_TYPES.register(
            "current_wave",
            () -> AttachmentType.builder(() -> 1)
                    .serialize(Codec.INT.fieldOf("CurrentWave"))
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    // Nombre total d'ennemis de la vague en cours et nombre d'ennemis déjà tués. Même
    // logique que current_wave : état de la Level, pas du joueur. Le total est réinitialisé
    // à sa valeur par défaut au premier chargement du monde ; les tués repartent de 0, comme
    // l'expérience, puisqu'ils s'accumulent au lieu de se dépenser.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> WAVE_ENEMIES_TOTAL = ATTACHMENT_TYPES.register(
            "wave_enemies_total",
            () -> AttachmentType.builder(() -> DEFAULT_WAVE_ENEMIES_TOTAL)
                    .serialize(Codec.INT.fieldOf("WaveEnemiesTotal"))
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> WAVE_ENEMIES_KILLED = ATTACHMENT_TYPES.register(
            "wave_enemies_killed",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT.fieldOf("WaveEnemiesKilled"))
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    // Phase de la partie (construction, combat...) : état de la Level, comme current_wave.
    // En mémoire/réseau, reste l'ordinal de GamePhase (VAR_INT, comme les autres compteurs :
    // pas besoin d'un Codec dédié à l'enum pour la sync, et tout le reste du mod compare
    // toujours des ordinaux). Seule la PERSISTANCE (sauvegarde/chargement du monde) passe par
    // le nom de la constante plutôt que son ordinal : contrairement à l'ordinal, il reste
    // correct même si l'ordre des valeurs de GamePhase change un jour (insertion d'une phase
    // avant COMBAT, par exemple) — sans ça une sauvegarde existante se retrouverait avec la
    // mauvaise phase au chargement. Démarre en phase de construction.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> GAME_PHASE = ATTACHMENT_TYPES.register(
            "game_phase",
            () -> AttachmentType.builder(() -> GamePhase.BUILD.ordinal())
                    .serialize(Codec.STRING.xmap(
                            name -> GamePhase.valueOf(name).ordinal(),
                            ordinal -> GamePhase.values()[ordinal].name()
                    ).fieldOf("GamePhase"))
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    // Score de la carte en cours : conceptuellement l'expérience gagnée pendant cette carte
    // (contrairement à "experience", qui elle est censée persister au-delà d'une carte). État
    // de la Level (comme current_wave) : "notre score", partagé par la partie, pas par
    // joueur. Rien ne l'alimente encore, voir 05-etat-et-problemes-connus.md.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> SCORE = ATTACHMENT_TYPES.register(
            "score",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT.fieldOf("Score"))
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    // Niveau du personnage : état du joueur (contrairement au score), commence à 1,
    // persistant, synchronisé au client pour le HUD. Rien ne le fait encore monter.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> LEVEL = ATTACHMENT_TYPES.register(
            "level",
            () -> AttachmentType.builder(() -> 1)
                    .serialize(Codec.INT.fieldOf("Level"))
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    // Nom de personnage : distinct du pseudo Minecraft, éditable indépendamment (pas encore
    // de commande/écran pour le faire). Par défaut, prend le pseudo Minecraft du joueur au
    // premier accès — juste pour ne pas afficher une chaîne vide tant qu'aucune valeur n'a
    // été choisie, ça reste un champ à part qui peut diverger du compte ensuite.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> CHARACTER_NAME = ATTACHMENT_TYPES.register(
            "character_name",
            () -> AttachmentType.builder(holder -> holder instanceof Player player ? player.getGameProfile().name() : "")
                    .serialize(Codec.STRING.fieldOf("CharacterName"))
                    .sync(ByteBufCodecs.STRING_UTF8)
                    .build());

    // Difficulté de la partie : état de la Level (comme current_wave), censée être choisie
    // au lancement de la map — aucun écran pour le faire n'existe encore, donc démarre en
    // Normal. Stockée comme l'ordinal de GameDifficulty, même approche que game_phase.
    // Consultée par DifficultyScaling pour calculer le multiplicateur des spawners.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> DIFFICULTY = ATTACHMENT_TYPES.register(
            "difficulty",
            () -> AttachmentType.builder(() -> GameDifficulty.NORMAL.ordinal())
                    .serialize(Codec.INT.fieldOf("Difficulty"))
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    // Session de combat en cours : incrémentée à chaque passage en Combat (voir
    // init/PhaseTransitions.java). Sert de déclencheur pour que chaque spawner relance sa
    // propre progression de spawn au début d'une nouvelle session, plutôt que seulement
    // quand current_wave change — sinon rebasculer Combat/Construction/Combat sur la même
    // vague (harnais de test actuel) ne ferait plus rien spawn. Pas besoin de synchronisation
    // client, c'est un détail serveur.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> COMBAT_SESSION = ATTACHMENT_TYPES.register(
            "combat_session",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT.fieldOf("CombatSession"))
                    .build());

    // Registre des spawners actuellement chargés sur cette Level : chaque SpawnerBlockEntity
    // s'y ajoute/retire lui-même (voir SpawnerBlockEntity#setLevel/#setRemoved). Sert à
    // calculer wave_enemies_total en sommant tous les spawners actifs à l'entrée en
    // Construction (voir init/PhaseTransitions.java). Ni persistant ni synchronisé : usage
    // strictement serveur, se reconstruit naturellement au (dé)chargement des chunks — un
    // spawner non chargé ne peut de toute façon pas spawn, l'exclure du total est cohérent.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Set<BlockPos>>> ACTIVE_SPAWNERS = ATTACHMENT_TYPES.register(
            "active_spawners",
            () -> AttachmentType.<Set<BlockPos>>builder((Supplier<Set<BlockPos>>) HashSet::new).build());

    // "Prêt" : état du joueur (clic droit sur le Cristal d'Eternia en phase Construction, voir
    // EterniaCrystalBlock), pas de la Level, comme mana/experience. Faux par défaut, remis à
    // faux pour tout le monde dès que le Combat démarre (voir PhaseTransitions#enterCombat) —
    // pas persistant (se re-décider à chaque Construction, y compris après reconnexion, est
    // le comportement voulu), synchronisé pour un futur indicateur HUD.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> READY = ATTACHMENT_TYPES.register(
            "ready",
            () -> AttachmentType.builder(() -> Boolean.FALSE)
                    .sync(ByteBufCodecs.BOOL)
                    .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
