package com.github.c0c0tier.dungeon_defenders.atelier;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.jarcontents.JarContents;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Lit et vérifie au démarrage tout ce que l'Atelier a déployé dans le jar du mod (doc/ATELIER.md). Adapté de
 * {@code fiche/FicheLoader.java} de l'Atelier, sans les capacités, effets, projectiles, apparition ni monnaie vivante.
 *
 * <p>Le chargement a lieu à la CONSTRUCTION du mod, avant le gel des registres, parce que chaque fiche devient son propre
 * type d'entité (et chaque son, son événement) : on ne peut pas en ajouter à chaud, donc pas de reload listener de
 * datapack. Les fiches et leurs fichiers doivent donc être dans le jar. Chaque problème d'une fiche est loggé, et une
 * fiche invalide est ignorée : elle ne fait jamais planter le jeu.
 */
public final class AtelierLoader {
    public static final String MONSTRES = "data/" + DungeonDefendersMod.MODID + "/atelier_monstres/";
    /** Tous les sons du mod, enregistrés comme événements au démarrage ({@link AtelierSounds}). */
    /** Fiches d'objets ({@link FicheObjet}) et de blocs ({@link FicheBloc}), formes des blocs {@code modele}. */
    public static final String OBJETS = "data/" + DungeonDefendersMod.MODID + "/atelier_objets/";
    public static final String BLOCS = "data/" + DungeonDefendersMod.MODID + "/atelier_blocs/";
    public static final String SONS = "assets/" + DungeonDefendersMod.MODID + "/sounds.json";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");
    // GeckoLib 5 ne charge que ces deux dossiers d'assets (GeckoLibResources.MODELS_PATH / ANIMATIONS_PATH).
    private static final String MODELES = "geckolib/models/";
    private static final String ANIMATIONS = "geckolib/animations/";
    private static final String GLOWMASK = "_glowmask";

    private final JarContents jar;
    private final Set<String> idsPris;
    private final Set<String> objetsPris;
    private Set<String> sons = Set.of();
    private final Set<Identifier> glowmasks = new HashSet<>();

    private AtelierLoader(JarContents jar, Set<String> idsPris, Set<String> objetsPris) {
        this.jar = jar;
        this.idsPris = idsPris;
        this.objetsPris = objetsPris;
    }

    /**
     * Charge toutes les fiches de {@code jar}. {@code idsPris} : les ids d'entité déjà utilisés par Dungeon Defenders
     * lui-même (un monstre de l'Atelier ne peut pas en prendre un) ; {@code objetsPris} : ses ids d'objet et de bloc.
     */
    public static void load(JarContents jar, Set<String> idsPris, Set<String> objetsPris) {
        AtelierLoader loader = new AtelierLoader(jar, idsPris, objetsPris);
        loader.sons = loader.lireSons();
        Map<String, FicheMonstre> valides = new TreeMap<>();
        Map<String, List<String>> noms = new TreeMap<>();
        Map<String, Map<String, Impact>> impacts = new TreeMap<>();
        Map<String, List<String>> rejetees = new TreeMap<>();
        loader.lister(MONSTRES).forEach((fichier, chemin) -> {
            List<String> erreurs = new ArrayList<>();
            Map<String, Impact> instants = new TreeMap<>();
            FicheMonstre fiche = loader.lire(fichier, chemin, erreurs, instants);
            if (fiche != null && erreurs.isEmpty()) {
                valides.put(fiche.id(), fiche);
                noms.put(fiche.id(), List.copyOf(instants.keySet()));
                impacts.put(fiche.id(), Map.copyOf(instants));
            } else {
                rejetees.put(fichier, List.copyOf(erreurs));
                LOGGER.error("Fiche {} rejetée ({} problème(s)) :\n  - {}", chemin, erreurs.size(), String.join("\n  - ", erreurs));
            }
        });
        Atelier.initMonstres(valides, noms, impacts, loader.sons, loader.glowmasks, rejetees);
        LOGGER.info("Atelier : {} monstre(s) chargé(s) {}, {} fiche(s) rejetée(s) {}",
                valides.size(), valides.keySet(), rejetees.size(), rejetees.keySet());

        // Objets et blocs : tous dans le même registre d'objets, avec les œufs des monstres chargés.
        Set<String> oeufs = new HashSet<>();
        valides.keySet().forEach(id -> oeufs.add(id + "_spawn_egg"));
        Map<String, FicheObjet> objets = loader.lireDossier(OBJETS, "Objet", FicheObjet.CODEC, FicheObjet::id,
                Set.of("id", "nom", "nom_en", "pile", "rarete", "brillant", "nourriture", "destinations", "origine"), Set.of(),
                (objet, erreurs) -> loader.verifierObjet(objet, oeufs, erreurs));
        Map<String, List<double[]>> formes = new TreeMap<>();
        Map<String, FicheBloc> blocs = loader.lireDossier(BLOCS, "Bloc", FicheBloc.CODEC, FicheBloc::id,
                Set.of("id", "nom", "nom_en", "durete", "resistance", "outil", "niveau", "lumiere", "son", "modele", "transparent",
                        "orientation", "type", "variantes", "destinations", "origine"), Set.of("cache"),
                (bloc, erreurs) -> loader.verifierBloc(bloc, oeufs, objets, formes, erreurs));
        formes.keySet().retainAll(blocs.keySet());
        Atelier.initObjets(objets, blocs, formes);
        LOGGER.info("Atelier : {} objet(s) {}, {} bloc(s) {}", objets.size(), objets.keySet(), blocs.size(), blocs.keySet());
    }

    /**
     * Lit chaque fichier d'un dossier d'objets ou de blocs, avec un message par problème ; les valides sont renvoyés par
     * id. Mêmes règles que les monstres : le nom du fichier est l'id, un fichier invalide est ignoré sans arrêter le jeu.
     * {@code ignores} : sections reconnues mais pas lues ici (avertissement) ; tout champ hors de {@code connus} aussi.
     */
    private <T> Map<String, T> lireDossier(String dossier, String quoi, com.mojang.serialization.Codec<T> codec,
            java.util.function.Function<T, String> idDe, Set<String> connus, Set<String> ignores,
            java.util.function.BiConsumer<T, List<String>> verifier) {
        Map<String, T> valides = new TreeMap<>();
        lister(dossier).forEach((fichier, chemin) -> {
            List<String> erreurs = new ArrayList<>();
            List<String> avertissements = new ArrayList<>();
            T valeur = null;
            if (!ID.matcher(fichier).matches()) {
                erreurs.add("nom de fichier invalide : minuscules, chiffres et _ uniquement (pas d'accent, d'espace ni de tiret)");
            } else if (lireJson(chemin, erreurs) instanceof JsonObject json) {
                valeur = codec.parse(JsonOps.INSTANCE, json)
                        .ifError(error -> erreurs.addAll(List.of(error.message().split("; "))))
                        .result().orElse(null);
                for (String champ : json.keySet()) {
                    if (ignores.contains(champ)) {
                        avertissements.add("`" + champ + "` n'est pas lu par Dungeon Defenders (voir doc/ATELIER.md) : ignoré");
                    } else if (!connus.contains(champ)) {
                        avertissements.add("champ inconnu `" + champ + "` : ignoré");
                    }
                }
            } else if (erreurs.isEmpty()) {
                erreurs.add(chemin + " n'est pas un objet JSON");
            }
            if (valeur != null) {
                if (!idDe.apply(valeur).equals(fichier)) {
                    erreurs.add("`id` vaut « " + idDe.apply(valeur) + " » mais le fichier s'appelle " + fichier
                            + ".json : ils doivent être identiques");
                }
                verifier.accept(valeur, erreurs);
            }
            if (valeur != null && erreurs.isEmpty()) {
                valides.put(fichier, valeur);
                avertissements.forEach(avertissement -> LOGGER.warn("{} {} : {}", quoi, chemin, avertissement));
            } else {
                LOGGER.error("{} {} rejeté ({} problème(s)) :\n  - {}", quoi, chemin, erreurs.size(), String.join("\n  - ", erreurs));
            }
        });
        return valides;
    }

    /** Sa texture doit exister ; son id ne doit être ni un objet du mod, ni un œuf. */
    private void verifierObjet(FicheObjet objet, Set<String> oeufs, List<String> erreurs) {
        verifierIdObjet(objet.id(), oeufs, erreurs);
        if (!jar.containsFile(objet.texturePath())) {
            erreurs.add("texture introuvable : " + objet.texturePath() + " (une image 16 × 16)");
        }
    }

    /**
     * Sa texture doit exister, son id être libre (objets et blocs partagent le registre des objets), son niveau aller avec
     * un outil ; un bloc {@code modele} doit avoir son modèle converti et une forme valide (une menuiserie : ses modèles
     * d'état), une orientation aller avec sa sorte (horizontale : un modèle ; axe : un cube ; aucune pour une menuiserie).
     */
    private void verifierBloc(FicheBloc bloc, Set<String> oeufs, Map<String, FicheObjet> objets,
            Map<String, List<double[]>> formes, List<String> erreurs) {
        verifierIdObjet(bloc.id(), oeufs, erreurs);
        if (objets.containsKey(bloc.id())) {
            erreurs.add("`id` : « " + bloc.id() + " » est déjà un objet (" + OBJETS + bloc.id() + ".json) : un bloc est aussi un objet,"
                    + " ils ne peuvent pas avoir le même id");
        }
        if (!jar.containsFile(bloc.texturePath())) {
            erreurs.add("texture introuvable : " + bloc.texturePath());
        }
        if (bloc.niveau() != FicheBloc.Niveau.AUCUN && bloc.outil() == FicheBloc.Outil.AUCUN) {
            erreurs.add("`niveau` « " + bloc.niveau().getSerializedName() + " » : il faut aussi l'`outil` qui le casse");
        }
        if (bloc.type() != FicheBloc.Type.SIMPLE) {
            if (bloc.orientation() != FicheBloc.Orientation.AUCUNE) {
                erreurs.add("`type` « " + bloc.type().getSerializedName() + " » : pas d'`orientation`, il s'oriente et s'ouvre"
                        + " comme son équivalent vanilla");
            }
            for (String modele : bloc.modelesEtats()) {
                if (!jar.containsFile(modele)) {
                    erreurs.add("modèle introuvable : " + modele + " (un modèle par état, déployé par l'Atelier)");
                }
            }
        } else if (bloc.modele()) {
            verifierForme(bloc, formes, erreurs);
        }
        if (bloc.orientation() == FicheBloc.Orientation.HORIZONTALE && !bloc.modele()) {
            erreurs.add("`orientation` « horizontale » : seulement pour un bloc `modele` (un cube n'a pas de devant)");
        }
        if (bloc.orientation() == FicheBloc.Orientation.AXE && bloc.modele()) {
            erreurs.add("`orientation` « axe » : seulement pour un cube (comme une bûche), pas pour un bloc `modele`");
        }
    }

    private void verifierIdObjet(String id, Set<String> oeufs, List<String> erreurs) {
        if (id.endsWith("_spawn_egg")) {
            erreurs.add("`id` : « _spawn_egg » est réservé aux œufs d'apparition des monstres");
        } else if (objetsPris.contains(id) || oeufs.contains(id)) {
            erreurs.add("`id` : « " + id + " » est déjà un objet ou un bloc de Dungeon Defenders (" + DungeonDefendersMod.MODID + ":" + id + ")");
        }
    }

    /**
     * Un bloc {@code modele} : son modèle converti, et sa forme, des boîtes de 6 nombres chacune, dans les limites d'un
     * modèle de bloc (-16 à 32 pixels). Sans boîte, il ne pourrait même pas être sélectionné.
     */
    private void verifierForme(FicheBloc bloc, Map<String, List<double[]>> formes, List<String> erreurs) {
        if (!jar.containsFile(bloc.modelPath())) {
            erreurs.add("modèle introuvable : " + bloc.modelPath() + " (le modèle converti, déployé par l'Atelier)");
        }
        if (!jar.containsFile(bloc.formePath())) {
            erreurs.add("forme introuvable : " + bloc.formePath() + " (déployée par l'Atelier)");
            return;
        }
        JsonElement json = lireJson(bloc.formePath(), erreurs);
        JsonElement boites = json != null && json.isJsonObject() ? json.getAsJsonObject().get("boites_px") : null;
        if (boites == null || !boites.isJsonArray() || boites.getAsJsonArray().isEmpty()) {
            erreurs.add(bloc.formePath() + " : il faut une liste « boites_px » non vide");
            return;
        }
        List<double[]> forme = new ArrayList<>();
        int index = 0;
        for (JsonElement boite : boites.getAsJsonArray()) {
            index++;
            double[] valeurs = boite.isJsonArray() && boite.getAsJsonArray().size() == 6
                    ? boite.getAsJsonArray().asList().stream().mapToDouble(v -> v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()
                            ? v.getAsDouble() : Double.NaN).toArray()
                    : null;
            if (valeurs == null || java.util.Arrays.stream(valeurs).anyMatch(v -> Double.isNaN(v) || v < -16 || v > 32)
                    || valeurs[0] >= valeurs[3] || valeurs[1] >= valeurs[4] || valeurs[2] >= valeurs[5]) {
                erreurs.add(bloc.formePath() + " : boîte " + index + " invalide (6 nombres x0, y0, z0, x1, y1, z1 en pixels, de -16 à 32,"
                        + " chaque début avant sa fin)");
                return;
            }
            forme.add(valeurs);
        }
        formes.put(bloc.id(), List.copyOf(forme));
    }

    /** Les fichiers JSON directement dans {@code dossier}, par nom sans extension, triés. */
    private Map<String, String> lister(String dossier) {
        Map<String, String> fichiers = new TreeMap<>();
        jar.visitContent(dossier, (chemin, resource) -> {
            String nom = chemin.substring(dossier.length());
            if (nom.endsWith(".json") && !nom.contains("/")) {
                fichiers.put(nom.substring(0, nom.length() - ".json".length()), chemin);
            }
        });
        return fichiers;
    }

    /** Décode et vérifie une fiche. Renvoie null, avec {@code erreurs} rempli, si elle est inutilisable. */
    private @Nullable FicheMonstre lire(String fichier, String chemin, List<String> erreurs, Map<String, Impact> impacts) {
        if (!ID.matcher(fichier).matches()) {
            erreurs.add("nom de fichier invalide : minuscules, chiffres et _ uniquement (pas d'accent, d'espace ni de tiret)");
            return null;
        }
        JsonElement json = lireJson(chemin, erreurs);
        if (json == null) {
            return null;
        }
        if (!json.isJsonObject()) {
            erreurs.add(chemin + " n'est pas un objet JSON");
            return null;
        }
        FicheMonstre fiche = FicheMonstre.CODEC.parse(JsonOps.INSTANCE, json)
                // Le codec joint les erreurs d'un même objet par « ; » : une ligne par problème se lit mieux.
                .ifError(error -> erreurs.addAll(List.of(error.message().split("; "))))
                .result().orElse(null);
        if (fiche == null) {
            return null;
        }
        List<String> avertissements = new ArrayList<>();
        champsNonLus(json.getAsJsonObject(), avertissements);
        verifier(fichier, fiche, erreurs, avertissements, impacts);
        if (erreurs.isEmpty()) {
            avertissements.forEach(avertissement -> LOGGER.warn("Fiche {} : {}", chemin, avertissement));
        }
        return fiche;
    }

    /** Les sections que Dungeon Defenders ne lit pas encore, et les champs inconnus (faute de frappe ?). */
    private static void champsNonLus(JsonObject json, List<String> avertissements) {
        for (String champ : json.keySet()) {
            if (FicheMonstre.IGNOREES.contains(champ)) {
                avertissements.add("`" + champ + "` n'est pas lu par Dungeon Defenders (voir doc/ATELIER.md) : ignoré");
            } else if (!FicheMonstre.CONNUES.contains(champ)) {
                avertissements.add("champ inconnu `" + champ + "` : ignoré");
            }
        }
    }

    /** Confronte une fiche décodée aux fichiers reçus : ids, chemins, animations, textures, sons. */
    private void verifier(String fichier, FicheMonstre fiche, List<String> erreurs, List<String> avertissements,
            Map<String, Impact> impacts) {
        if (!fiche.id().equals(fichier)) {
            erreurs.add("`id` vaut « " + fiche.id() + " » mais le fichier s'appelle " + fichier + ".json : ils doivent être identiques");
        }
        if (idsPris.contains(fiche.id())) {
            erreurs.add("`id` : « " + fiche.id() + " » est déjà une entité de Dungeon Defenders ("
                    + DungeonDefendersMod.MODID + ":" + fiche.id() + ")");
        }

        JsonObject geo = verifierAsset("modele", fiche.modele(), MODELES, ".geo.json", erreurs)
                ? lireAssetJson("modele", fiche.modele(), erreurs) : null;
        if (geo != null && !geo.has("minecraft:geometry")) {
            erreurs.add("`modele` : " + fiche.modele() + " ne contient pas de « minecraft:geometry » (export GeckoLib attendu)");
        }

        JsonObject animations = verifierAsset("animations", fiche.animations(), ANIMATIONS, ".animation.json", erreurs)
                ? lireAssetJson("animations", fiche.animations(), erreurs) : null;
        if (animations != null) {
            if (animations.get("animations") instanceof JsonObject toutes) {
                List<String> erreursImpact = new ArrayList<>();
                List<String> avertissementsImpact = new ArrayList<>();
                toutes.asMap().forEach((nom, animation) -> impacts.put(nom, animation instanceof JsonObject objet
                        ? Impact.read(nom, objet, erreursImpact, avertissementsImpact) : new Impact(0, false, 0)));
                toutes.asMap().forEach((nom, animation) -> {
                    if (animation instanceof JsonObject objet && objet.get("sound_effects") instanceof JsonObject effets) {
                        effets.asMap().values().forEach(effet -> verifierSonAnimation(nom, effet, avertissementsImpact));
                    }
                });
                erreursImpact.forEach(erreur -> erreurs.add("`animations` : " + erreur));
                avertissementsImpact.forEach(avertissement -> avertissements.add("`animations` : " + avertissement));
            } else {
                erreurs.add("`animations` : " + fiche.animations() + " ne contient pas d'objet « animations »");
            }
        }

        for (String role : FicheMonstre.ROLES_OBLIGATOIRES) {
            if (!fiche.rolesAnimation().containsKey(role)) {
                erreurs.add("`roles_animation` : le rôle obligatoire « " + role + " » n'a pas d'animation");
            }
        }
        if (animations != null) {
            fiche.rolesAnimation().forEach((role, nom) -> {
                if (!impacts.containsKey(nom)) {
                    erreurs.add("`roles_animation." + role + "` : l'animation « " + nom + " » n'existe pas dans " + fiche.animations()
                            + " (animations trouvées : " + String.join(", ", impacts.keySet()) + ")");
                }
            });
        }
        if (fiche.comportement().combat()) {
            fiche.animation("attack").ifPresentOrElse(nom -> {
                Impact impact = impacts.get(nom);
                if (impact != null && !impact.marqueur()) {
                    avertissements.add("`roles_animation.attack` : « " + nom + " » n'a pas de marqueur « impact », les dégâts tombent"
                            + " à la moitié de l'animation (" + impact.secondes() + " s)");
                }
            }, () -> avertissements.add("comportement « " + fiche.comportement().getSerializedName()
                    + " » sans rôle `attack` : le monstre frappera sans animation"));
        }
        if (fiche.comportement() == FicheMonstre.Comportement.FUYARD || fiche.comportement() == FicheMonstre.Comportement.MARCHAND) {
            avertissements.add("comportement « " + fiche.comportement().getSerializedName()
                    + " » : la monnaie vivante n'est pas lue par Dungeon Defenders, il se comporte en « passif »");
        }

        List<String> sonsDuMonstre = sonsDe(fiche.id());
        fiche.sons().forEach((role, nom) -> {
            if (!sonsDuMonstre.contains(nom)) {
                erreurs.add("`sons." + role + "` : le son « " + nom + " » n'existe pas pour " + fiche.id() + " (sons trouvés : "
                        + (sonsDuMonstre.isEmpty() ? "aucun" : String.join(", ", sonsDuMonstre)) + ") ; un son est une entrée entity."
                        + fiche.id() + ".<nom> de " + SONS);
            } else if (!FicheMonstre.ROLES_SON.contains(role)) {
                avertissements.add("`sons." + role + "` : seuls " + String.join(", ", FicheMonstre.ROLES_SON)
                        + " sont joués automatiquement, ce son ne servira qu'aux keyframes des animations");
            }
        });

        verifierTexture("texture", fiche.texture(), erreurs);
        Set<String> variantes = new HashSet<>();
        for (FicheMonstre.Variante variante : fiche.variantes()) {
            String champ = "variantes." + variante.id();
            if (!ID.matcher(variante.id()).matches()) {
                erreurs.add("`" + champ + "` : id de variante invalide (minuscules, chiffres et _ uniquement)");
            }
            if (!variantes.add(variante.id())) {
                erreurs.add("`" + champ + "` : variante déclarée deux fois");
            }
            verifierTexture(champ + ".texture", variante.texture(), erreurs);
        }
    }

    /** Une texture de monstre, et son calque lumineux {@code <texture>_glowmask.png} s'il existe. */
    private void verifierTexture(String champ, Identifier texture, List<String> erreurs) {
        if (verifierAsset(champ, texture, "textures/", ".png", erreurs)) {
            String chemin = texture.getPath();
            Identifier glowmask = texture.withPath(chemin.substring(0, chemin.length() - ".png".length()) + GLOWMASK + ".png");
            if (jar.containsFile(cheminAsset(glowmask))) {
                glowmasks.add(texture);
            }
        }
    }

    /** Les noms des sons d'un monstre : les événements {@code entity.<id>.<nom>} du sounds.json, triés. */
    private List<String> sonsDe(String id) {
        String prefixe = "entity." + id + ".";
        return sons.stream().filter(e -> e.startsWith(prefixe)).map(e -> e.substring(prefixe.length())).toList();
    }

    /**
     * Une keyframe de son d'une animation ({@code "sound_effects": {"0.58": {"effect": "dungeon_defenders:entity.golem.step"}}}).
     * GeckoLib ne la joue que si l'événement est enregistré : une keyframe vers un son non déclaré resterait muette.
     */
    private void verifierSonAnimation(String animation, JsonElement effet, List<String> avertissements) {
        String son = effet instanceof JsonObject objet && objet.get("effect") instanceof JsonElement e && e.isJsonPrimitive()
                ? e.getAsString().split("\\|")[0] : null;
        Identifier id = son == null ? null : Identifier.tryParse(son);
        if (id == null) {
            avertissements.add("animation « " + animation + " » : keyframe de son sans « effect » lisible, elle sera muette");
        } else if (!id.getNamespace().equals("minecraft")
                && !(id.getNamespace().equals(DungeonDefendersMod.MODID) && sons.contains(id.getPath()))) {
            avertissements.add("animation « " + animation + " » : le son « " + id + " » n'est pas déclaré dans " + SONS
                    + ", il sera muet");
        }
    }

    /** Les événements déclarés dans {@link #SONS} ; aucun si le fichier manque ou est illisible (loggé). */
    private Set<String> lireSons() {
        if (!jar.containsFile(SONS)) {
            return Set.of();
        }
        List<String> erreurs = new ArrayList<>();
        JsonElement json = lireJson(SONS, erreurs);
        if (json == null || !json.isJsonObject()) {
            LOGGER.error("{} ignoré, aucun son de l'Atelier ne sera joué : {}", SONS,
                    erreurs.isEmpty() ? "ce n'est pas un objet JSON" : String.join(" ; ", erreurs));
            return Set.of();
        }
        Set<String> evenements = new TreeSet<>();
        json.getAsJsonObject().keySet().forEach(cle -> {
            if (Identifier.isValidPath(cle)) {
                evenements.add(cle);
            } else {
                LOGGER.error("{} : nom d'événement « {} » invalide (minuscules, chiffres, _ . - /), ignoré", SONS, cle);
            }
        });
        return evenements;
    }

    /** Vérifie qu'un asset désigne un fichier du jar, dans le dossier d'où le jeu le lit. */
    private boolean verifierAsset(String champ, Identifier asset, String dossier, String extension, List<String> erreurs) {
        if (!asset.getNamespace().equals(DungeonDefendersMod.MODID)) {
            erreurs.add("`" + champ + "` : " + asset + " doit être dans le namespace « " + DungeonDefendersMod.MODID + " »");
        } else if (!asset.getPath().startsWith(dossier) || !asset.getPath().endsWith(extension)) {
            erreurs.add("`" + champ + "` : " + asset + " doit être de la forme " + DungeonDefendersMod.MODID + ":" + dossier + "…" + extension);
        } else if (!jar.containsFile(cheminAsset(asset))) {
            erreurs.add("`" + champ + "` : fichier introuvable : " + cheminAsset(asset));
        } else {
            return true;
        }
        return false;
    }

    private @Nullable JsonObject lireAssetJson(String champ, Identifier asset, List<String> erreurs) {
        List<String> erreursLecture = new ArrayList<>();
        JsonElement json = lireJson(cheminAsset(asset), erreursLecture);
        erreursLecture.forEach(erreur -> erreurs.add("`" + champ + "` : " + erreur));
        if (json != null && !json.isJsonObject()) {
            erreurs.add("`" + champ + "` : " + cheminAsset(asset) + " n'est pas un objet JSON");
            return null;
        }
        return json == null ? null : json.getAsJsonObject();
    }

    private @Nullable JsonElement lireJson(String chemin, List<String> erreurs) {
        try (InputStream in = jar.openFile(chemin); Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (IOException | RuntimeException e) {
            erreurs.add(chemin + " illisible (JSON mal formé ?) : " + e.getMessage());
            return null;
        }
    }

    private static String cheminAsset(Identifier asset) {
        return "assets/" + asset.getNamespace() + "/" + asset.getPath();
    }
}
