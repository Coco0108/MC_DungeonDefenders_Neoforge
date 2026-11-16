package com.github.c0c0tier.dungeon_defenders.client;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.MapDefinition;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Aperçus des maps créées en jeu, reçus du serveur (MapPreviewPayload) et chargés comme
// textures dynamiques. Une map livrée dans un jar n'a rien ici : son aperçu est une ressource
// ordinaire du pack, voir MapDefinition#previewTexture.
public final class ClientMapPreviews {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Identifier, Identifier> TEXTURES = new HashMap<>();

    private ClientMapPreviews() {
    }

    /** Charge (ou remplace) l'aperçu d'une map. Une image illisible est ignorée, avec un log. */
    public static void put(Identifier structureId, byte[] png) {
        NativeImage image;
        try {
            image = NativeImage.read(png);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Aperçu de la map {} illisible, ignoré : {}", structureId, e.toString());
            return;
        }

        // Identifiant de texture dérivé de celui de la structure : réenregistrer sous le même
        // identifiant remplace (et libère) l'ancienne texture.
        Identifier textureId = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID,
                "dynamic/map_preview/" + structureId.getNamespace() + "/" + structureId.getPath());
        Minecraft.getInstance().getTextureManager().register(textureId,
                new DynamicTexture(() -> "Aperçu de la map " + structureId, image));
        TEXTURES.put(structureId, textureId);
    }

    /** La texture à afficher pour cette map : son aperçu capturé en jeu s'il existe, sinon celui de son pack. */
    public static Identifier textureFor(MapDefinition map) {
        Identifier dynamic = TEXTURES.get(map.structureId());
        return dynamic != null ? dynamic : map.previewTexture();
    }
}
