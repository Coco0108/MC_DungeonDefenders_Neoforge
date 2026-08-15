package com.github.c0c0tier.dungeon_defenders.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

// Touches custom pour la roue de sélection des tours (voir TowerPlacementClientEvents,
// TowerWheelScreen). Catégorie vanilla GAMEPLAY : pas besoin d'une catégorie dédiée pour deux
// touches.
public class ModKeyMappings {

    // Maintenue : ouvre la roue ; relâchée pendant que la roue est ouverte : confirme le
    // secteur survolé (voir TowerWheelScreen.keyReleased).
    public static final KeyMapping TOWER_WHEEL = new KeyMapping(
            "key.dungeon_defenders.tower_wheel",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KeyMapping.Category.GAMEPLAY);

    // Pressée pendant l'étape "orientation" du mode pose : fait pivoter l'hologramme de 90°.
    public static final KeyMapping ROTATE_TOWER = new KeyMapping(
            "key.dungeon_defenders.rotate_tower",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_T,
            KeyMapping.Category.GAMEPLAY);

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOWER_WHEEL);
        event.register(ROTATE_TOWER);
    }
}
