package com.github.c0c0tier.dungeon_defenders.client;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.MapPreviews;
import com.github.c0c0tier.dungeon_defenders.network.UploadMapPreviewPayload;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Capture de l'aperçu d'une map créée en jeu. Déroulé validé avec le joueur (2026-09-02) :
//   1. dans l'écran de choix, le bouton « Prendre l'aperçu » ARME la capture pour la map
//      affichée et ferme l'écran ;
//   2. le mappeur se place et cadre ;
//   3. la touche de capture masque le HUD le temps de deux images, lit le rendu, le recadre en
//      16:9, le réduit à 128x72 et l'envoie au serveur (UploadMapPreviewPayload).
// La même touche, accroupi, annule.
@EventBusSubscriber(modid = DungeonDefendersMod.MODID, value = Dist.CLIENT)
public final class MapPreviewCapture {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Nombre de ticks pendant lesquels le HUD reste masqué avant de lire l'image : il faut qu'au
    // moins une image ait été rendue SANS le HUD (et sans ce message) avant la lecture.
    private static final int HIDE_TICKS_BEFORE_GRAB = 3;

    private static @Nullable Identifier armedStructure;
    private static Component armedName = Component.empty();
    private static int ticksUntilGrab = -1;
    private static boolean previousHideGui;

    private MapPreviewCapture() {
    }

    /** Arme la capture pour cette map. Appelé par MapSelectionScreen. */
    public static void arm(Identifier structureId, Component mapName) {
        armedStructure = structureId;
        armedName = mapName;
        ticksUntilGrab = -1;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            // Déconnexion pendant une capture : ne jamais laisser le HUD masqué derrière soi.
            if (ticksUntilGrab >= 0) {
                minecraft.options.hideGui = previousHideGui;
            }
            armedStructure = null;
            ticksUntilGrab = -1;
            return;
        }
        if (armedStructure == null) {
            // Vider les appuis pour qu'un appui hors capture ne déclenche rien plus tard.
            while (ModKeyMappings.CAPTURE_MAP_PREVIEW.consumeClick()) {
                // rien
            }
            return;
        }

        if (ticksUntilGrab > 0) {
            ticksUntilGrab--;
            return;
        }
        if (ticksUntilGrab == 0) {
            ticksUntilGrab = -1;
            grab(minecraft, armedStructure);
            armedStructure = null;
            return;
        }

        // Rappel permanent tant que la capture est armée (barre d'action, au-dessus de la hotbar).
        minecraft.gui.setOverlayMessage(Component.translatable("dungeon_defenders.map_preview.armed",
                armedName, ModKeyMappings.CAPTURE_MAP_PREVIEW.getTranslatedKeyMessage()), false);

        boolean pressed = false;
        while (ModKeyMappings.CAPTURE_MAP_PREVIEW.consumeClick()) {
            pressed = true;
        }
        if (!pressed || minecraft.screen != null) {
            return;
        }
        if (minecraft.player.isShiftKeyDown()) {
            armedStructure = null;
            minecraft.gui.setOverlayMessage(Component.translatable("dungeon_defenders.map_preview.cancelled"), false);
            return;
        }

        previousHideGui = minecraft.options.hideGui;
        minecraft.options.hideGui = true;
        ticksUntilGrab = HIDE_TICKS_BEFORE_GRAB;
    }

    private static void grab(Minecraft minecraft, Identifier structureId) {
        try {
            Screenshot.takeScreenshot(minecraft.getMainRenderTarget(), image -> {
                // Le rappel peut arriver plus tard et sur un autre fil : tout ce qui touche au
                // jeu repasse par la file du client.
                minecraft.execute(() -> {
                    minecraft.options.hideGui = previousHideGui;
                    try (image) {
                        finish(minecraft, structureId, image);
                    } catch (IOException | RuntimeException e) {
                        LOGGER.warn("Capture de l'aperçu de {} impossible", structureId, e);
                        tell(minecraft, Component.translatable("dungeon_defenders.map_preview.failed"));
                    }
                });
            });
        } catch (RuntimeException e) {
            minecraft.options.hideGui = previousHideGui;
            LOGGER.warn("Capture de l'aperçu de {} impossible", structureId, e);
            tell(minecraft, Component.translatable("dungeon_defenders.map_preview.failed"));
        }
    }

    private static void finish(Minecraft minecraft, Identifier structureId, NativeImage screen) throws IOException {
        byte[] png;
        try (NativeImage preview = new NativeImage(MapPreviews.WIDTH, MapPreviews.HEIGHT, false)) {
            // Recadrage centré au format de l'aperçu, puis réduction.
            int[] crop = centeredCrop(screen.getWidth(), screen.getHeight(), MapPreviews.WIDTH, MapPreviews.HEIGHT);
            screen.resizeSubRectTo(crop[0], crop[1], crop[2], crop[3], preview);
            png = encode(preview);
        }

        if (!MapPreviews.isAcceptable(png)) {
            tell(minecraft, Component.translatable("dungeon_defenders.map_preview.too_large"));
            return;
        }

        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null) {
            connection.send(new UploadMapPreviewPayload(structureId, png).toVanillaServerbound());
        }
        // Affichage immédiat chez soi ; les autres joueurs le recevront du serveur.
        ClientMapPreviews.put(structureId, png);
    }

    /**
     * Le plus grand rectangle centré de proportions {@code targetWidth:targetHeight} qui tient
     * dans une image {@code width x height}. @return {x, y, largeur, hauteur}
     */
    static int[] centeredCrop(int width, int height, int targetWidth, int targetHeight) {
        int cropWidth = width;
        int cropHeight = (int) ((long) width * targetHeight / targetWidth);
        if (cropHeight > height) {
            cropHeight = height;
            cropWidth = (int) ((long) height * targetWidth / targetHeight);
        }
        cropWidth = Math.max(1, cropWidth);
        cropHeight = Math.max(1, cropHeight);
        return new int[] {(width - cropWidth) / 2, (height - cropHeight) / 2, cropWidth, cropHeight};
    }

    // NativeImage ne sait écrire que dans un fichier : passage par un fichier temporaire.
    private static byte[] encode(NativeImage image) throws IOException {
        Path temp = Files.createTempFile("dd_map_preview", ".png");
        try {
            image.writeToFile(temp);
            return Files.readAllBytes(temp);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static void tell(Minecraft minecraft, Component message) {
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(message);
        }
    }
}
