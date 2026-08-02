# 3. Build & lancement

## Prérequis

- **JDK 25** — Mojang livre Java 25 aux joueurs en 26.1.2, la toolchain Gradle est fixée à
  `JavaLanguageVersion.of(25)`. Gradle peut le télécharger seul grâce au plugin
  `foojay-resolver-convention` déclaré dans `settings.gradle`.
- Le wrapper Gradle est versionné (`gradlew` / `gradlew.bat`) : aucune install Gradle globale
  n'est nécessaire.
- IDE recommandé : IntelliJ IDEA (le bloc `idea { downloadSources / downloadJavadoc }` est
  configuré) ; une config VS Code existe aussi dans `.vscode/launch.json`.

## Commandes

Sous Windows, remplacer `./gradlew` par `gradlew.bat` (ou `.\gradlew` en PowerShell).

Compiler et packager le jar :

```bash
./gradlew build
```

Lancer le client de développement :

```bash
./gradlew runClient
```

Lancer un serveur dédié de dev (avec `--nogui`) :

```bash
./gradlew runServer
```

Exécuter les gametests puis quitter :

```bash
./gradlew runGameTestServer
```

Générer les ressources (datagen) vers `src/generated/resources/` :

```bash
./gradlew runData
```

Rafraîchir les dépendances si l'IDE ne trouve plus les librairies :

```bash
./gradlew --refresh-dependencies
```

Tout nettoyer (n'affecte pas le code source) :

```bash
./gradlew clean
```

## Configurations de run (`build.gradle`, bloc `neoForge.runs`)

| Run | Type | Particularités |
|---|---|---|
| `client` | client | gametests limités au namespace `dungeon_defenders` |
| `server` | serveur dédié | argument `--nogui` |
| `gameTestServer` | `gameTestServer` | lance tous les gametests puis sort ; **plante s'il n'y en a aucun** |
| `data` | `clientData` | datagen : `--mod dungeon_defenders --all --output src/generated/resources --existing src/main/resources` |

`configureEach` applique à toutes les runs :

- `forge.logging.markers = REGISTRIES` (log du déclenchement des événements de registre) ;
- `logLevel = DEBUG`.

> Il n'y a aucun gametest dans le projet à ce jour : `runGameTestServer` échouera.

## Sorties du build

- Jar : `build/libs/dungeon_defenders-0.0.1.jar` (le nom vient de `base.archivesName = mod_id`).
- Métadonnées générées : `build/generated/sources/modMetadata/META-INF/neoforge.mods.toml`.

`sourceSets.main.resources` inclut aussi `src/generated/resources` (datagen) et exclut des
sorties finales les `**/*.bbmodel` (projets BlockBench) et le cache de datagen.

## Publication Maven

Le plugin `maven-publish` est configuré avec une publication `mavenJava` vers un dépôt local
`file://<projet>/repo` (dossier ignoré par git) :

```bash
./gradlew publish
```

C'est la configuration d'exemple du template — à adapter avant toute vraie diffusion.

## Intégration continue

`.github/workflows/build.yml` — déclenché sur `push` et `pull_request` :

1. `actions/checkout@v4` avec `fetch-depth: 0` et `fetch-tags: true` ;
2. `actions/setup-java@v4`, JDK **25**, distribution Temurin ;
3. `gradle/actions/setup-gradle@v4` (cache Gradle) ;
4. `./gradlew build`.

Aucune étape de publication ou de release n'est configurée.

## Ajouter une dépendance

Le bloc `dependencies` de `build.gradle` est vide mais commenté avec les patterns usuels
(JEI, jar local dans `./libs`, projet frère). Noter la configuration `localRuntime` :

```groovy
configurations { runtimeClasspath.extendsFrom localRuntime }
```

Utiliser `localRuntime` plutôt que `runtimeOnly` pour un mod présent en dev mais qui ne doit
pas devenir une dépendance publiée.
