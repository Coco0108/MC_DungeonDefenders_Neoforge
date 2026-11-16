package com.github.c0c0tier.dungeon_defenders.client;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

// Touches custom pour la roue de sélection des tours (voir TowerPlacementClientEvents,
// TowerWheelScreen) et le mode suppression de tour (voir TowerRemovalClientEvents). Catégorie
// dédiée (pas KeyMapping.Category.GAMEPLAY) : regroupées sous leur propre en-tête "Dungeon
// Defenders" dans Options > Contrôles > Touches, plus simples à retrouver que noyées parmi les
// dizaines de touches vanilla de la catégorie Gameplay.
public class ModKeyMappings {

    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "keys"));

    // Maintenue : ouvre la roue ; relâchée pendant que la roue est ouverte : confirme le
    // secteur survolé (voir TowerWheelScreen.keyReleased).
    public static final KeyMapping TOWER_WHEEL = new KeyMapping(
            "key.dungeon_defenders.tower_wheel",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY);

    // Pressée pendant l'étape "orientation" du mode pose : fait pivoter l'hologramme de 90°.
    // G et non T : T est déjà le raccourci vanilla "Ouvrir le chat" (key.chat, code 84) — un
    // même code de touche pour deux KeyMapping fait que le chat s'ouvre à la place de la
    // rotation (l'écran de chat capte l'entrée clavier en premier).
    public static final KeyMapping ROTATE_TOWER = new KeyMapping(
            "key.dungeon_defenders.rotate_tower",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY);

    // Bascule (pas maintenue) : entre/sort du mode suppression de tour — voir
    // TowerRemovalClientEvents. Un appui ré-active ; le prochain clic gauche sur une tour visée
    // l'envoie au serveur pour destruction + remboursement de mana.
    public static final KeyMapping REMOVE_TOWER_MODE = new KeyMapping(
            "key.dungeon_defenders.remove_tower_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CATEGORY);

    // Pressée en regardant une tour posée : demande son amélioration au serveur (voir
    // TowerInspectClientEvents). U comme "upgrade", libre en vanilla.
    public static final KeyMapping UPGRADE_TOWER = new KeyMapping(
            "key.dungeon_defenders.upgrade_tower",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            CATEGORY);

    // Maintenue : affiche le plan de la zone actuelle (taverne ou map) en plein écran, avec les
    // joueurs dessus — voir MapOverlayClientEvents. Relâchée : disparaît. Premier usage de
    // isDown() sur cette branche (pollée chaque tick client, comme le sera le mécanisme des
    // compétences canalisées sur la branche héros/sorts) plutôt que consumeClick() : un
    // affichage "tant que maintenu", pas une bascule.
    //
    // Tab (2026-09-07, retour du joueur : plus confortable à maintenir que M). Ce code physique
    // était aussi celui de key.playerList (liste des joueurs vanilla) ; comme les deux touches
    // sont lues via isDown() sans exclusivité, les garder toutes les deux sur Tab aurait affiché
    // les deux superposées. Réglé en libérant key.playerList (voir
    // DungeonDefendersModClient#onClientSetup) : le mod n'en a plus l'usage, Tab n'affiche donc
    // plus que ce plan.
    public static final KeyMapping MAP_OVERLAY = new KeyMapping(
            "key.dungeon_defenders.map_overlay",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_TAB,
            CATEGORY);

    // Ouvre l'écran de choix de héros. H libre côté vanilla ; à terme ce choix se fera aussi
    // depuis la taverne, mais une touche évite de dépendre d'un bloc pour un système qui doit
    // être testable tout de suite.
    public static final KeyMapping HERO_SELECTION = new KeyMapping(
            "key.dungeon_defenders.hero_selection",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY);

    // Les quatre emplacements de compétence — touches 1 à 4, l'ordre du HUD (voir
    // AbilitySlotsOverlay) et de init.AbilitySlot : soin, sort 1, sort 2, réparation. Les
    // touches vanilla de hotbar utilisent les mêmes codes physiques, mais la hotbar est déjà
    // neutralisée en survie (voir DungeonDefendersModClient) — rien ne s'y oppose. En créatif,
    // où la hotbar reste active, appuyer sur 1-4 change donc AUSSI le slot sélectionné en plus
    // de déclencher la compétence ; effet de bord mineur, accepté (les compétences n'ont de
    // toute façon d'utilité qu'en jeu, pas en train de construire).
    public static final KeyMapping ABILITY_HEAL = new KeyMapping(
            "key.dungeon_defenders.ability_heal",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_1,
            CATEGORY);

    public static final KeyMapping ABILITY_SPELL_1 = new KeyMapping(
            "key.dungeon_defenders.ability_spell_1",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_2,
            CATEGORY);

    public static final KeyMapping ABILITY_SPELL_2 = new KeyMapping(
            "key.dungeon_defenders.ability_spell_2",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_3,
            CATEGORY);

    public static final KeyMapping ABILITY_REPAIR = new KeyMapping(
            "key.dungeon_defenders.ability_repair",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_4,
            CATEGORY);

    /** Indexé par {@code AbilitySlot.ordinal()} — même ordre que le HUD : soin, sort 1, sort 2, réparation. */
    public static final KeyMapping[] ABILITY_KEYS = {ABILITY_HEAL, ABILITY_SPELL_1, ABILITY_SPELL_2, ABILITY_REPAIR};

    // Déclenche la capture d'aperçu d'une map, une fois armée depuis l'écran de choix (voir
    // MapPreviewCapture). F9 : libre en vanilla, et hors des touches de jeu — c'est un outil de
    // mappeur, utilisé rarement.
    public static final KeyMapping CAPTURE_MAP_PREVIEW = new KeyMapping(
            "key.dungeon_defenders.capture_map_preview",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            CATEGORY);

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(TOWER_WHEEL);
        event.register(ROTATE_TOWER);
        event.register(REMOVE_TOWER_MODE);
        event.register(UPGRADE_TOWER);
        event.register(MAP_OVERLAY);
        event.register(HERO_SELECTION);
        event.register(ABILITY_HEAL);
        event.register(ABILITY_SPELL_1);
        event.register(ABILITY_SPELL_2);
        event.register(ABILITY_REPAIR);
        event.register(CAPTURE_MAP_PREVIEW);
    }
}
