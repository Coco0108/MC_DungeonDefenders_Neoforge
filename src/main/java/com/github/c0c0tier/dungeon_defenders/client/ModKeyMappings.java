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

    // Ouvre l'écran de choix de héros. H libre côté vanilla ; à terme ce choix se fera aussi
    // depuis la taverne, mais une touche évite de dépendre d'un bloc pour un système qui doit
    // être testable tout de suite.
    public static final KeyMapping HERO_SELECTION = new KeyMapping(
            "key.dungeon_defenders.hero_selection",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY);

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(TOWER_WHEEL);
        event.register(ROTATE_TOWER);
        event.register(REMOVE_TOWER_MODE);
        event.register(HERO_SELECTION);
    }
}
