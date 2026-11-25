package com.github.c0c0tier.dungeon_defenders.init;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

// Images d'aperçu des maps créées en jeu, rangées dans la sauvegarde :
// <monde>/dungeon_defenders/previews/<namespace>/<id>.png. C'est le client d'un mappeur qui
// capture l'image (voir client/MapPreviewCapture) ; le serveur ne fait que la valider, la
// ranger, et la redistribuer aux clients à l'ouverture de l'écran de choix. /dd_export les
// embarque depuis ce même dossier (voir MapExporter).
public final class MapPreviews {

    /** Dossier des aperçus, relatif à la racine de la sauvegarde. Partagé avec MapExporter. */
    public static final String DIRECTORY = "dungeon_defenders/previews";

    public static final int WIDTH = 128;
    public static final int HEIGHT = 72;

    /**
     * Taille maximale d'une image. Un paquet client vers serveur est plafonné à 32767 octets par
     * le jeu : on garde de la marge pour l'identifiant et l'en-tête. À 128x72 une capture pèse
     * d'ordinaire 5 à 15 Ko.
     */
    public static final int MAX_BYTES = 30_000;

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private MapPreviews() {
    }

    /** @return vrai si ces octets ont la taille et la signature d'un PNG acceptable. */
    public static boolean isAcceptable(byte[] png) {
        if (png == null || png.length < PNG_SIGNATURE.length || png.length > MAX_BYTES) {
            return false;
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (png[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Chemin du fichier d'aperçu d'une map, ou vide si l'identifiant n'est pas celui d'une map
     * ou sortirait du dossier des aperçus (un identifiant peut contenir « .. »).
     */
    public static Optional<Path> file(MinecraftServer server, Identifier structureId) {
        if (!MapDefinition.isMapStructure(structureId)) {
            return Optional.empty();
        }
        Path root = server.getWorldPath(LevelResource.ROOT).resolve(DIRECTORY).normalize();
        String mapId = structureId.getPath().substring(MapDefinition.STRUCTURE_PREFIX.length());
        Path file = root.resolve(structureId.getNamespace()).resolve(mapId + ".png").normalize();
        return file.startsWith(root) ? Optional.of(file) : Optional.empty();
    }

    /** Range l'image. @return faux si elle est refusée ou si l'écriture échoue. */
    public static boolean save(MinecraftServer server, Identifier structureId, byte[] png) {
        Optional<Path> file = file(server, structureId);
        if (file.isEmpty() || !isAcceptable(png)) {
            return false;
        }
        try {
            Files.createDirectories(file.get().getParent());
            Files.write(file.get(), png);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** @return l'image rangée pour cette map, ou vide s'il n'y en a pas (ou si elle est illisible). */
    public static Optional<byte[]> load(MinecraftServer server, Identifier structureId) {
        Optional<Path> file = file(server, structureId);
        if (file.isEmpty() || !Files.isRegularFile(file.get())) {
            return Optional.empty();
        }
        try {
            byte[] png = Files.readAllBytes(file.get());
            return isAcceptable(png) ? Optional.of(png) : Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
