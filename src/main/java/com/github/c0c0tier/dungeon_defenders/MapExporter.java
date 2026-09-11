package com.github.c0c0tier.dungeon_defenders;

import com.github.c0c0tier.dungeon_defenders.init.MapDefinition;
import com.github.c0c0tier.dungeon_defenders.init.MapRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// Emballe les maps d'un pack dans un jar d'extension prêt à publier (CurseForge ou autre).
//
// **Pourquoi cette commande existe** : créer une map se fait entièrement en jeu (voir
// MapRegistry), mais une map créée en jeu vit dans la sauvegarde du monde — elle ne part pas
// avec le mod. La publier revient à recopier des fichiers aux bons endroits d'un jar, avec un
// `neoforge.mods.toml` correct. C'est exactement le genre d'étape qui décourage un auteur, d'où
// une commande qui produit l'objet fini.
//
// Le jar produit ne contient **aucun code** : il est découvert par le même mécanisme que
// n'importe quelle structure (voir MapRegistry), sans inscription ni API.
public final class MapExporter {

    /** Dossier de sortie, à la racine du serveur/jeu — hors de la sauvegarde, facile à retrouver. */
    public static final String EXPORT_DIR = "dungeon_defenders_export";

    /**
     * Emplacement où sont cherchées les images d'aperçu, dans la sauvegarde. Rien ne les y écrit
     * encore (la capture en jeu est la phase suivante) : l'export les embarque si elles existent,
     * pour que cette commande n'ait pas à changer le jour où la capture arrivera.
     */
    private static final String PREVIEW_DIR = "dungeon_defenders/previews";

    private MapExporter() {
    }

    /** Ce qu'a produit un export, pour le message de retour. */
    public record Result(Path jar, int mapCount, int previewCount) {
    }

    /**
     * @throws IllegalArgumentException si le namespace est invalide ou ne contient aucune map
     *                                  exportable ; le message est destiné au joueur.
     */
    public static Result export(ServerLevel level, String namespace) throws IOException {
        if (!isValidModId(namespace)) {
            throw new IllegalArgumentException("namespace_invalid");
        }

        // Seules les maps créées en jeu sont exportables : une map qui vient déjà d'un jar est
        // une ressource en lecture seule, et la ré-emballer n'aurait aucun sens.
        List<MapDefinition> maps = MapRegistry.discover(level).stream()
                .filter(map -> map.packId().equals(namespace))
                .filter(MapDefinition::fromWorld)
                .toList();
        if (maps.isEmpty()) {
            throw new IllegalArgumentException("no_map");
        }

        Path outputDir = level.getServer().getServerDirectory().resolve(EXPORT_DIR);
        Files.createDirectories(outputDir);
        Path jar = outputDir.resolve(namespace + ".jar");

        StructureTemplateManager manager = level.getStructureManager();
        Path worldDir = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize();
        int previews = 0;

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar))) {
            writeEntry(zip, "META-INF/neoforge.mods.toml", modsToml(namespace, maps.size()).getBytes(StandardCharsets.UTF_8));

            for (MapDefinition map : maps) {
                Path structure = MapRegistry.worldFile(manager, map.structureId());
                writeEntry(zip, "data/" + namespace + "/structure/" + map.structureId().getPath() + ".nbt",
                        Files.readAllBytes(structure));

                Path preview = worldDir.resolve(PREVIEW_DIR).resolve(namespace).resolve(map.mapId() + ".png");
                if (Files.isRegularFile(preview)) {
                    writeEntry(zip, "assets/" + namespace + "/textures/gui/maps/" + map.mapId() + ".png",
                            Files.readAllBytes(preview));
                    previews++;
                }
            }

            // Nom du pack : personne ne le connaît (il n'est stocké nulle part), donc on écrit le
            // namespace comme valeur de départ et on invite l'auteur à l'éditer. Mieux vaut un
            // fichier à corriger qu'une clé de traduction crue affichée aux joueurs.
            byte[] lang = packLang(namespace).getBytes(StandardCharsets.UTF_8);
            writeEntry(zip, "assets/" + namespace + "/lang/en_us.json", lang);
            writeEntry(zip, "assets/" + namespace + "/lang/fr_fr.json", lang);
        }

        return new Result(jar, maps.size(), previews);
    }

    private static void writeEntry(ZipOutputStream zip, String path, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(content);
        zip.closeEntry();
    }

    // Un identifiant de mod NeoForge : minuscules, chiffres et underscores, commençant par une
    // lettre. C'est aussi la contrainte d'un namespace d'Identifier, donc une map sauvegardée
    // sous un nom valide donne forcément un modId valide — la vérification est un garde-fou.
    private static boolean isValidModId(String namespace) {
        return namespace.matches("[a-z][a-z0-9_]{1,63}");
    }

    private static String modsToml(String namespace, int mapCount) {
        return """
                # Généré par /dd_export — pack de maps pour Dungeon Defenders.
                # Ce jar ne contient aucun code : ses maps sont découvertes automatiquement.
                modLoader = "lowcodefml"
                loaderVersion = "[1,)"
                license = "All Rights Reserved"

                [[mods]]
                modId = "%s"
                version = "1.0.0"
                displayName = "%s"
                description = "Pack de %d map(s) pour Dungeon Defenders."

                [[dependencies.%s]]
                modId = "%s"
                type = "required"
                versionRange = "[0,)"
                ordering = "NONE"
                side = "BOTH"
                """.formatted(namespace, prettify(namespace), mapCount, namespace, DungeonDefendersMod.MODID);
    }

    private static String packLang(String namespace) {
        return """
                {
                  "dungeon_defenders.map_pack.%s": "%s"
                }
                """.formatted(namespace, prettify(namespace));
    }

    // "cavernes_oubliees" -> "Cavernes Oubliees" : un point de départ lisible, que l'auteur
    // corrigera dans le fichier de langue s'il veut mieux.
    private static String prettify(String namespace) {
        StringBuilder result = new StringBuilder();
        for (String part : namespace.split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.toString();
    }
}
