package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.List;

// Liste des maps proposées dans l'écran de choix de la taverne (MapSelectionScreen). Ajouter
// une map ici la rend disponible dès qu'elle a sa traduction (dungeon_defenders.map.<id>) et
// son image d'aperçu (assets/dungeon_defenders/textures/gui/maps/<id>.png) — sans texture, le
// jeu affichera la texture "manquante" habituelle plutôt que de planter.
//
// `visible = false` permet d'ajouter une map en cours de conception au mod sans qu'elle
// apparaisse dans le carrousel du joueur : pratique pour la développer par étapes.
public enum GameMap {

    // Entrée de démonstration, avec une image d'aperçu provisoire (un simple aplat de
    // couleur) — à retirer une fois une vraie première map construite et ajoutée ici.
    TEST_MAP("test_map", true);

    private final String id;
    private final boolean visible;

    GameMap(String id, boolean visible) {
        this.id = id;
        this.visible = visible;
    }

    public Component displayName() {
        return Component.translatable("dungeon_defenders.map." + this.id);
    }

    public Identifier previewTexture() {
        return Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "textures/gui/maps/" + this.id + ".png");
    }

    public boolean isVisible() {
        return this.visible;
    }

    /** @return les maps proposées dans l'écran de choix, dans l'ordre de déclaration de l'enum. */
    public static List<GameMap> visibleMaps() {
        return Arrays.stream(values()).filter(GameMap::isVisible).toList();
    }
}
