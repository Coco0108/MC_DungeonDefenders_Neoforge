# 2. Gameplay & mécaniques

Boucle de jeu visée : le joueur pose un **Cristal d'Eternia**, les monstres convergent vers
lui et le frappent, le joueur doit les en empêcher. À 0 PV, partie perdue.

## Le monde et le point de spawn

Le principe retenu (discuté avec le joueur) : la taverne (le hub) et chaque map seront des
**structures** posées à des coordonnées fixes (voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md), "Système de
maps/structures") — rien dans le jeu ne dépend du terrain généré naturellement. Deux
conséquences déjà mises en place, avant même que la vraie structure de la taverne existe :

### L'Overworld est un monde vide — `data/minecraft/dimension/overworld.json`

Un fichier de données (pas de code Java, purement déclaratif) remplace le générateur de
l'Overworld par le préréglage vanilla **"The Void"** — celui utilisé par le menu de création
de monde vanilla (bouton "Personnaliser" → préréglages), copié tel quel :

```json
{
  "type": "minecraft:overworld",
  "generator": {
    "type": "minecraft:flat",
    "settings": {
      "biome": "minecraft:the_void",
      "features": true,
      "lakes": false,
      "layers": [{ "block": "minecraft:air", "height": 1 }],
      "structure_overrides": []
    }
  }
}
```

Placer ce fichier à `data/minecraft/dimension/overworld.json` (namespace **`minecraft`**,
pas `dungeon_defenders`) remplace la définition intégrée de l'Overworld par celle-ci — c'est
le mécanisme standard des datapacks "monde vide" (fonctionne même sans mod, un simple
datapack peut le faire ; ici il est intégré aux ressources du mod pour s'appliquer à toute
partie sans configuration manuelle). Choisi plutôt qu'une dimension séparée : plus simple (pas
de nouvelle dimension à enregistrer, pas de téléportation à gérer), et cohérent avec le fait
que ce mod ne cherche pas à cohabiter avec une survie vanilla classique dans le même monde.

> **Aucun moyen de vérifier ce fichier avant de lancer le jeu** : sa syntaxe n'est validée à
> aucune étape de la compilation (`./gradlew build` ne charge pas les datapacks). Les
> `settings` sont recopiés à l'identique du préréglage `the_void` du jar du jeu (vérifié en
> l'extrayant directement), mais la structure globale du fichier dimension elle-même n'a pas
> pu être testée en jeu — voir 06-a-tester.md.

### Le point de spawn — `TavernSpawn.java`

Un monde vide n'a nulle part où faire apparaître un joueur "normalement" (le jeu cherche
d'habitude un sol solide près de l'origine). `TavernSpawn`, sur `LevelEvent.Load` (une fois
par chargement de l'Overworld) :

1. **(Re)pose la structure de la taverne** à cet emplacement (voir "Le chargement de la
   structure" juste en dessous).
2. Fixe le point de spawn du monde sur la **position d'arrivée réelle** de cette structure via
   `ServerLevel#setRespawnData(...)` — remplace la recherche automatique de sol (qui échouerait
   dans le vide). C'est donc aussi là que réapparaît un joueur mort en pleine map.

**Pourquoi rejouer l'étape 2 à *chaque* chargement du monde**, plutôt qu'une seule fois : la
taverne suit le même principe que les futures maps (voir 05-etat-et-problemes-connus.md,
"Système de maps/structures") — sa structure sera reposée à cet emplacement fixe à chaque
fois qu'on y "entre", pas construite une fois pour toutes. Sans ça, mettre à jour la structure
de la taverne dans une future version du mod resterait invisible sur une sauvegarde
existante : le joueur garderait la version posée lors de sa toute première connexion, le mod
n'ayant plus aucune raison de la reposer ensuite. Recharger à chaque démarrage du monde est le
déclencheur le plus simple qui garantit que ce qui est affiché correspond toujours à la
version livrée avec le mod installé.

`LevelEvent.Load` se déclenche pour **toute** dimension qui se charge (l'Overworld, mais
aussi le Nether/End vanilla si un joueur y va) — le handler sort immédiatement si ce n'est
pas l'Overworld (`serverLevel.dimension() != Level.OVERWORLD`), pour ne rien changer ailleurs.

### Le chargement de la structure — `TavernSpawn#placeTavern`

Depuis le 2026-08-31, la taverne est une vraie structure Minecraft chargée depuis
**`data/dungeon_defenders/structure/tavern.nbt`** (identifiant `dungeon_defenders:tavern`), et
non plus une plateforme codée en dur. C'est le premier vrai chargement de structure du mod — le
même mécanisme servira pour les maps (`MapInstance.buildPlaceholderArena`, pas encore remplacé).

Déroulé de `placeTavern` :

1. `level.getStructureManager().get(TAVERN_STRUCTURE)` — le gestionnaire vanilla, qui lit aussi
   bien les structures livrées dans le jar du mod que celles d'un datapack.
2. **Si le fichier est absent** : message d'avertissement dans les logs et **repli sur l'ancienne
   plateforme** 9×9 en `smooth_stone`. Volontaire : dans un monde vide, un mod sans sol est
   injouable — mieux vaut une plateforme moche qu'une chute infinie.
3. **Nettoyage de la zone** avant de poser (`clearZone`), sur l'emprise exacte de la structure
   plus `CLEAR_MARGIN = 4` de marge dans toutes les directions. Sans ça, une version précédente
   plus grande (ou la plateforme de repli) laisserait ses restes flotter autour de la nouvelle
   taverne. La marge se calcule à partir de `template.getSize()`, pas d'un rayon en dur : la
   taverne peut grandir sans qu'on ait à toucher au code.
4. `template.placeInWorld(...)` avec `Block.UPDATE_CLIENTS` (et **pas** `UPDATE_ALL`) : on ne
   veut pas déclencher une cascade de mises à jour de voisinage sur chaque bloc posé, seulement
   que les clients voient le résultat — même choix que le bloc de structure vanilla.

#### Où la structure est posée

Centrée **horizontalement** sur `SPAWN_POS` (0/0 en X/Z), sa couche la plus basse posée à
`SPAWN_POS.y - 1` (soit Y=64) : le sol de la taverne est donc juste sous le point d'arrivée.

#### La position d'arrivée — le marqueur `player_spawn`, non consommé

`arrivalPos(level)` cherche un bloc `player_spawn` **dans la structure** et renvoie sa position ;
sinon, repli sur `SPAWN_POS`. Le créateur de la taverne peut donc déplacer le point d'arrivée
sans avoir à recaler toute la structure.

Deux différences avec le marqueur d'une map (`MapInstance#findAndConsumeSpawnMarker`) :

- **Il n'est pas consommé.** On revient à la taverne en permanence ; le supprimer à la première
  arrivée casserait toutes les suivantes. C'est sans conséquence visuelle depuis que le bloc est
  invisible et traversable (voir "Le bloc de spawn joueur" plus bas).
- **La recherche se fait dans le template, pas dans le monde** : `StructureTemplate#filterBlocks`
  renvoie directement les positions absolues du bloc demandé, pas besoin de balayer un volume
  bloc par bloc comme le fait `MapInstance`.

`arrivalPos` est recalculée à chaque appel plutôt que mémorisée dans un champ statique : le
gestionnaire de structures garde déjà le template en cache, et elle n'est appelée que rarement
(chargement du monde, retour à la taverne). `MapInstance.returnToTavern` l'utilise aussi, pour
que `/dd_leave` et la fin de partie ramènent au même endroit que le spawn.

#### Les entités aussi sont remises à zéro — `clearZone`

`clearZone` fait deux passes : d'abord tous les blocs de la zone en air, **puis** la suppression
de toutes les entités qui s'y trouvent, joueurs exceptés. C'est ce qui permet à la structure de
contenir des entités (`StructurePlaceSettings` par défaut, pas de `setIgnoreEntities`) sans
qu'elles se dupliquent à chaque rechargement : celles du chargement précédent sont supprimées,
`placeInWorld` repose celles du fichier.

**L'ordre des deux passes est délibéré.** Écrire un bloc force le chargement de son chunk ;
balayer les blocs en premier garantit donc que tous les chunks de la zone sont chargés avant
qu'on interroge leurs entités. Chercher les entités d'abord — pendant `LevelEvent.Load`, avant
que quoi que ce soit ne soit chargé — n'en trouverait aucune, et la structure en poserait un
exemplaire de plus à chaque démarrage du serveur.

Ce que ça emporte, volontairement : les entités décoratives (reposées juste après), les objets
au sol qu'un joueur aurait laissés dans la taverne, et le futur **mannequin d'entraînement** des
tours. Chaque chargement du monde remet la taverne dans l'état livré avec le mod.

> Décidé avec le joueur (2026-08-31) : la suppression d'entités est nécessaire de toute façon,
> puisqu'un mannequin d'entraînement (PV infinis, immobile, cible des tours pour mesurer leurs
> dégâts) viendra vivre dans la taverne et ne doit pas s'accumuler en plusieurs exemplaires.

## La taverne — choix de map et difficulté

### Le bloc — `block/TavernCrystalBlock.java`

Un cristal, mais **différent** d'`EterniaCrystalBlock` : pas de block entity, pas de PV, pas
de mécanique de combat — juste un point d'interaction dans la taverne. Clic droit demande au
client d'ouvrir `MapSelectionScreen`, via un paquet sans champ :

```java
protected InteractionResult useWithoutItem(...) {
    if (player instanceof ServerPlayer serverPlayer) {
        serverPlayer.connection.send(OpenMapSelectionPayload.INSTANCE.toVanillaClientbound());
    }
    return InteractionResult.SUCCESS;
}
```

Pas de `player.openMenu(...)` ni de `Menu`/`MenuProvider` comme pour le spawner — cet écran
n'a besoin d'aucune donnée propre à un bloc précis (contrairement au spawner, qui devait
savoir *quel* spawner configurer via son `BlockPos`) : la liste des maps est statique côté
client, et la difficulté actuelle vient d'un attachment de `Level` déjà synchronisé
(`ModAttachments.DIFFICULTY`). Le système de `Menu` sert à transmettre des données du serveur
au client à l'ouverture ; ici il n'y a rien à transmettre, donc pas besoin de ce système. D'où
un simple paquet clientbound « ouvre cet écran », sur le même modèle que `GameOverPayload`.

> **Pourquoi pas plus simple ?** La version d'origine faisait
> `if (level.isClientSide()) Minecraft.getInstance().setScreen(new MapSelectionScreen());`,
> directement dans le bloc. Ça marche en solo, mais empêche le mod de se charger **du tout**
> sur un serveur dédié : la branche ne s'exécute jamais côté serveur, mais la classe *nomme*
> `MapSelectionScreen`, donc `Screen`, absent d'un serveur. Crash constaté le 2026-08-30, voir
> [01-architecture.md](01-architecture.md#la-règle-clientserveur--nommer-une-classe-cliente-suffit-à-casser-un-serveur-dédié).

### La liste des maps — `init/GameMap.java`

Un enum, sur le même principe que `SpawnableEnemy` : chaque valeur porte un `id` (utilisé
pour la clé de traduction `dungeon_defenders.map.<id>` et le chemin de la texture d'aperçu
`assets/dungeon_defenders/textures/gui/maps/<id>.png`) et un booléen `visible`.

> **Pourquoi `visible`, pas juste retirer l'entrée de l'enum ?** Pour pouvoir ajouter une map
> en cours de conception au mod (la coder, la tester) **sans** qu'elle apparaisse dans le
> carrousel du joueur — demandé explicitement : pouvoir avancer sur une map par étapes sans
> la montrer avant qu'elle soit prête. `GameMap.visibleMaps()` filtre sur ce booléen ; l'écran
> ne voit jamais les entrées masquées.

Une seule entrée pour l'instant, `TEST_MAP` — une image d'aperçu provisoire (un simple aplat
de couleur bleu-gris généré à la main, pas une vraie capture d'écran) le temps qu'une vraie
première map existe. Aucune texture manquante ne fait planter le jeu : si une map ajoutée à
l'enum n'a pas encore son fichier `.png`, le jeu affiche la texture "manquante" habituelle à
sa place.

### L'écran — `client/gui/screen/MapSelectionScreen.java`

Deux zones, comme demandé :

- **Carrousel de maps** (gauche) : boutons `◀`/`▶` qui font tourner un index dans
  `GameMap.visibleMaps()` (bouclant), image d'aperçu (`GuiGraphicsExtractor#blit(Identifier,
  x, y, largeur, hauteur, 0f, 0f, 1f, 1f)` — les quatre derniers paramètres sont les UV en
  fractions 0..1, donc `0,0,1,1` = la texture entière) et nom de la map traduit en dessous.
  Changer de map ne reconstruit **pas** les widgets (contrairement au spawner qui
  ajoute/retire des lignes) : seul l'index change, `extractRenderState` relit `GameMap`
  correspondant à chaque frame.
- **Choix de difficulté** (droite) : trois boutons `GameDifficulty.values()` (Facile/Normal/
  Difficile), un seul "sélectionné" à la fois — pas de vrai composant radio-bouton dans cette
  version, donc simulé en changeant le **texte** du bouton sélectionné (`"> Facile <"` plutôt
  que `"Facile"`, via `AbstractWidget#setMessage(...)`) plutôt qu'en reconstruisant quoi que
  ce soit.

Le bouton **"Jouer"** envoie deux paquets, dans cet ordre (reçus et traités dans le même ordre
côté serveur, même connexion) :

1. `SetDifficultyPayload(difficultyOrdinal)` — validé côté serveur (`ModNetworking`, même
   garde-fou que pour les ordinaux d'ennemis du spawner : jamais indexer un tableau avec une
   valeur reçue du réseau sans la vérifier) puis appliqué à `ModAttachments.DIFFICULTY`.
2. `StartGamePayload` (sans champ) — déclenche `MapInstance.startGame(level)`, voir plus bas.

Le choix de map **précis** dans le carrousel n'a en revanche toujours aucun effet : une seule
map "placeholder" générique existe pour l'instant (voir `MapInstance`), donc "Jouer" lance
toujours la même chose quel que soit l'élément sélectionné — le vrai chargement d'une
structure par map reste à faire, voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md).

### "La map active" — `MapInstance.java`

Puisqu'une seule partie est active à la fois sur tout le serveur (confirmé avec le joueur —
voir [05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md), "Système de
maps/structures"), toutes les maps partagent la **même coordonnée fixe**
(`MAP_POS = (10000, 65, 0)`, loin de la taverne) plutôt que d'avoir chacune la leur — pas
besoin d'une grille de coordonnées puisqu'il n'y en a jamais deux en même temps.

- **`startGame(level)`** (déclenché par `StartGamePayload`) : nettoie la zone (remplace tout
  par de l'air dans un volume autour de `MAP_POS` — plus large que le placeholder lui-même,
  pour rattraper d'éventuelles tours posées autour une fois qu'elles existeront), pose un
  placeholder générique (même technique que `TavernSpawn`, une simple plateforme), cherche et
  consomme un `PLAYER_SPAWN` (voir plus bas), puis téléporte **tous** les joueurs de la `Level`
  — pas seulement celui qui a cliqué "Jouer", puisqu'une seule partie est partagée par tout le
  monde (confirmé explicitement : "de toute façon on devra le faire").
- **`returnToTavern(level)`** : même nettoyage de la zone, puis téléporte tout le monde vers
  `TavernSpawn.SPAWN_POS`. Déclenché par la commande `/dd_leave` (voir `ModCommands` et
  "Victoire et défaite" plus bas) — pas encore par un vrai point de sortie posé dans la map
  elle-même, puisqu'aucune vraie map n'existe.

`MapInstance` est pensé pour que le seul changement nécessaire, une fois de vraies maps
prêtes, soit de remplacer `buildPlaceholderArena()` par un vrai chargement de structure
`.nbt` — même logique que ce qui est prévu pour `TavernSpawn` (voir plus haut).

### Le bloc de spawn joueur — `block/PlayerSpawnBlock.java`, `MapInstance#findAndConsumeSpawnMarker`

Décidé avec le joueur (2026-08-26), repris du plan Excel (feuille "Idées" > "CHOIX DE MAP") :
plutôt qu'une téléportation vers `MAP_POS` codée en dur, le créateur d'une map peut poser un
bloc `PLAYER_SPAWN` (aucun comportement au clic) à l'endroit exact où les joueurs doivent
apparaître.

#### Invisible et traversable, comme le spawner

Demandé en jeu (2026-08-30) : c'est un **marqueur d'édition**, pas un élément de décor — il ne
doit ni se voir ni gêner un déplacement pendant une partie. Traitement identique, méthode par
méthode, à celui de `SpawnerBlock` (voir "Le spawner n'est plus jamais un obstacle physique"
plus bas pour le raisonnement complet, valable tel quel ici) :

| Méthode | Valeur | Effet |
|---|---|---|
| `getRenderShape` | `RenderShape.INVISIBLE` | jamais rendu, pour personne (même limite : pas de contexte joueur dans cette méthode, donc invisible même en créatif) |
| `getCollisionShape` | `Shapes.empty()` | joueur et monstres traversent, quelle que soit la phase ou le mode |
| `getShape` | `Shapes.block()` **si joueur créatif**, sinon `Shapes.empty()` | seul le créatif peut le viser (contour de sélection) — introuvable en survie |

Le bloc était jusque-là enregistré via `BLOCKS.registerSimpleBlock`, sans classe dédiée : cette
classe n'existe que pour ces trois overrides.

**Pourquoi, alors qu'il s'auto-supprime au démarrage ?** Parce que ça ne couvrait qu'un cas :
`findAndConsumeSpawnMarker` ne consomme que le **premier** marqueur trouvé (voir plus bas) —
tous les autres restent posés et visibles — et le marqueur se voit de toute façon tant que la
partie n'a pas démarré.

**Pas besoin de `noOcclusion()` dans ses `Properties`** : l'occlusion est calculée à
l'initialisation du `BlockState` à partir de `getShape` avec un `CollisionContext.empty()`
(vérifié dans `BlockBehaviour$BlockStateBase#initCache` : `occlusionShape = getOcclusionShape(state)`,
lui-même `state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)`). `getEntity()` y est `null`,
donc le repli `Shapes.empty()` s'applique : le marqueur n'occlut rien et ne bloque pas la
lumière, sans réglage supplémentaire. Un bloc invisible qui occlurait laisserait au contraire un
trou noir visible (faces voisines cullées).

Enfin, `getShape` **n'est pas mis en cache** par état (contrairement à `getCollisionShape`,
vérifié dans `BlockStateBase#getShape`, qui délègue directement au bloc à chaque appel) — c'est
ce qui permet à la forme de dépendre du joueur qui regarde.

L'**item**, lui, reste visible normalement dans l'onglet créatif et en main : `RenderShape` ne
concerne que le bloc posé, le rendu de l'item passe par `models/item/player_spawn.json`
(voir "Texture cassée en main" plus bas).

`findAndConsumeSpawnMarker(level)` parcourt le même volume que `clearZone`/
`buildPlaceholderArena` (autour de `MAP_POS`), juste après que l'arène a été (re)posée : le
premier `PLAYER_SPAWN` trouvé est **retiré** (`setBlockAndUpdate(pos, AIR)` — "se supprime pour
ne pas le voir", comme prévu dans le plan Excel) et sa position devient la destination du
téléport ; si aucun n'est trouvé, `startGame` retombe sur `MAP_POS` comme avant — **toujours le
cas aujourd'hui**, puisque `buildPlaceholderArena()` ne pose qu'un sol générique, jamais de
`PLAYER_SPAWN`. Un seul marqueur est attendu par map ; le premier trouvé gagne, pas de gestion
de plusieurs candidats.

**Pas concrètement testable pour l'instant** : le mécanisme ne peut être exercé qu'une fois
qu'une vraie structure `.nbt` de map (contenant un `PLAYER_SPAWN` posé par le créateur) est
chargée à la place du placeholder — voir "Système de maps/structures" dans
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md). Un `PLAYER_SPAWN` posé
manuellement dans le placeholder actuel ne survivrait de toute façon pas à la prochaine
préparation de map (`clearZone` efface tout, y compris un marqueur posé à la main, avant que le
scan n'ait lieu) : ce n'est pas une limite de `findAndConsumeSpawnMarker` en soi, juste
l'absence actuelle de structures réelles à charger.

### Texture cassée en main — `models/item/player_spawn.json`

Signalé en jeu (2026-08-26) : l'item s'affichait avec une texture manquante dans la main/
l'inventaire, alors que le bloc posé s'affichait correctement (il était encore visible à
l'époque — voir "Invisible et traversable" ci-dessus ; le modèle de bloc reste nécessaire, ne
serait-ce que comme parent du modèle d'item). Cause : contrairement au modèle
de **bloc** (`models/block/player_spawn.json`, ajouté dès le départ), aucun modèle d'**item**
n'avait été créé — sans lui, rien n'indique au jeu quel modèle utiliser pour l'icône. Corrigé
en ajoutant `models/item/player_spawn.json` avec `"parent":
"dungeon_defenders:block/player_spawn"` — le même mécanisme standard que les blocs vanilla
simples (ex. `minecraft:item/dirt`, qui référence directement `minecraft:block/dirt`).

> Plusieurs autres blocs du mod (`spawner`, `tavern_crystal`, `spike_blockade`,
> `harpoon_turret`, `eternia_crystal`) n'ont eux non plus jamais eu de `models/item/*.json`
> dédié — probablement le même problème en main/inventaire pour chacun, jamais signalé jusqu'ici
> faute d'avoir été spécifiquement regardé. Pas corrigé ici (hors scope de cette branche/PR,
> chacun vit dans un autre fichier/PR) — à vérifier et corriger au cas par cas.

## Le Cristal d'Eternia

### Le bloc — `block/EterniaCrystalBlock.java`

Étend `BaseEntityBlock` (bloc porteur d'un block entity).

| Aspect | Implémentation |
|---|---|
| Codec | `simpleCodec(EterniaCrystalBlock::new)` — obligatoire depuis 1.20.5 |
| Block entity | `newBlockEntity` → `new EterniaCrystalBlockEntity(pos, state)` |
| Render shape | `RenderShape.MODEL` (modèle JSON classique) |
| Collision | `Shapes.box(0, 0, 0, 1, 3, 1)` — 1×3×1, le cristal est infranchissable sur 3 blocs |
| Sélection | même boîte 1×3×1 — le ciblage reste sur 3 blocs de haut, mais le contour noir n'est **plus dessiné** (voir "Le contour de sélection masqué sur les tours et les cristaux") |

Propriétés définies à l'enregistrement dans `ModBlocks` :

- `destroyTime(50.0F)` — très long à miner à la main
- `explosionResistance(1200.0F)` — quasi immunisé aux explosions (comparable à l'obsidienne)
- `requiresCorrectToolForDrops()` — pas de drop sans le bon outil

### Interaction joueur

`useWithoutItem` (clic droit à main nue) se comporte différemment selon la phase :

- **En Construction** : bascule "prêt" pour le joueur qui clique — voir "Le vote prêt" plus
  bas, c'est le vrai déclencheur du Combat.
- **En Combat** : retire **10 PV** au cristal et envoie un message au joueur — l'ancien
  harnais de test, gardé pour pouvoir déclencher la destruction du cristal sans attendre une
  vraie vague. Dans le jeu final, seuls les monstres devraient endommager le cristal.

Le client renvoie immédiatement `SUCCESS` (prédiction, animation de bras) ; la logique ne
tourne que côté serveur, et `PASS` est renvoyé si le block entity est absent (uniquement
pertinent pour la branche Combat, qui a besoin du block entity — la branche Construction n'en
a pas besoin).

### Le vote "prêt" — déclencheur du Combat

Pour passer de Construction à Combat, il faut que **tous les joueurs présents dans cette
Level** cliquent sur le cristal (pas tout le serveur : une future map/dimension différente
aura ses propres joueurs, voir 05-etat-et-problemes-connus.md). Chaque clic bascule l'état
"prêt" du joueur qui a cliqué (`ModAttachments.READY`, un attachment **joueur**, comme
`mana`/`experience` — pas persistant, se re-décider à chaque Construction est voulu), diffuse
la progression à tout le monde (`Prêt : 2/3`), et dès que tous sont prêts,
`PhaseTransitions.enterCombat(level)` se déclenche — qui remet aussitôt "prêt" à faux pour
tout le monde (voir plus bas), pour repartir propre à la Construction suivante. Un seul joueur
en solo se retrouve donc à devoir cliquer une fois pour lancer le combat (1/1).

Le harnais de test au clic droit du `SpawnerBlock` (shift + clic droit, voir plus bas) reste
disponible en parallèle pour basculer directement de phase sans passer par le vote — pratique
pour les tests, mais il passe aussi par `PhaseTransitions`, donc remet "prêt" à zéro pour tout
le monde comme le vote, pas de comportement divergent entre les deux déclencheurs.

### L'état — `block/entity/EterniaCrystalBlockEntity.java`

```java
public static final int DEFAULT_HEALTH = 100;
private int crystalHealth = DEFAULT_HEALTH;
```

- `getCrystalHealth()` : lecture des PV.
- `damage(int)` : raccourci pour retirer des PV — c'est l'entrée utilisée par l'IA et par
  l'interaction joueur.
- `setCrystalHealth(int)` :
  - clampe la valeur à 0 minimum et sort immédiatement si les PV n'ont pas changé ;
  - appelle `setChanged()` (marque le chunk à sauvegarder) ;
  - sort si `level` est nul ou côté client ;
  - appelle `level.sendBlockUpdated(...)` pour pousser les nouveaux PV vers les clients ;
  - si les PV tombent à ≤ 0 : `level.destroyBlock(worldPosition, false)` — le `false` empêche
    le drop de l'item — puis message `dungeon_defenders.eternia_crystal.destroyed` en rouge
    gras à tous les joueurs, et `PhaseTransitions.onDefeat(level)` (voir plus bas, "Victoire et
    défaite") pour remettre la partie à zéro.

**Persistance.** `saveAdditional` / `loadAdditional` utilisent l'API `ValueOutput` /
`ValueInput` (le remplaçant des `CompoundTag` bruts) :

```java
output.putInt("CrystalHealth", this.crystalHealth);
this.crystalHealth = input.getIntOr("CrystalHealth", DEFAULT_HEALTH);
```

Les PV survivent donc au rechargement du monde.

**Synchronisation client.** Les PV ne sont modifiés que côté serveur, alors que le renderer
les lit côté client. Deux surcharges assurent la propagation :

```java
getUpdatePacket() → ClientboundBlockEntityDataPacket.create(this)
getUpdateTag(registries) → saveWithoutMetadata(registries)   // réutilise saveAdditional
```

`getUpdateTag` couvre l'envoi initial (chargement du chunk), `getUpdatePacket` +
`sendBlockUpdated` couvrent les mises à jour.

## Rendu de la barre de vie — `EterniaCrystalBlockEntityRenderer.java`

Renderer client, enregistré via `EntityRenderersEvent.RegisterRenderers` dans
`DungeonDefendersModClient`.

Depuis 26.1, un `BlockEntityRenderer` ne voit plus le block entity au moment du rendu. Le
cycle est en trois temps :

1. **`createRenderState()`** → un `EterniaCrystalRenderState` (sous-classe de
   `BlockEntityRenderState` portant un simple `float healthPercent`) — **neuf à chaque frame**
   (voir `BlockEntityRenderDispatcher#tryExtractRenderState`), donc incapable de retenir quoi
   que ce soit d'une frame à l'autre tout seul (voir l'animation ci-dessous).
2. **`extractRenderState(...)`** — appelé côté extraction, avec accès au block entity :
   calcule la cible `clamp(getCrystalHealth() / DEFAULT_HEALTH, 0, 1)`, la passe à un
   `HealthLerp` (voir plus bas) et remplit `healthPercent` avec sa valeur **animée**, pas la
   cible brute.
3. **`submit(state, poseStack, collector, camera)`** — ne voit que l'état :
   - translation à `(0.5, 3.2, 0.5)`, au-dessus de la hitbox de 3 blocs ;
   - billboard via `poseStack.mulPose(camera.orientation)`. Après cette rotation `+X` va vers
     la droite et **`+Y` vers le bas** (même convention que les name tags vanilla), d'où le
     `scale(1, -1, 1)` qui rétablit des coordonnées naturelles ;
   - délègue le dessin à `HealthBarRendering.render(...)` (voir plus bas).

### L'animation entre deux paliers de PV — `HealthLerp.java`

Avant (jusqu'au 2026-08-24) : la barre sautait instantanément d'un palier à l'autre à chaque
coup. `HealthLerp` anime la transition sur 300 ms, **en temps réel** (`Util.getMillis()`), pas
sur `partialTicks` — même principe que `LerpingBossEvent` vanilla (barres de boss, 100 ms),
mais `partialTicks` ne convient pas ici : il interpole entre deux valeurs connues à la frontière
d'un tick, alors qu'un `EterniaCrystalRenderState` neuf à chaque frame ne peut stocker ni
l'ancienne valeur ni un point de départ d'animation lui-même. L'objet `HealthLerp` vit donc sur
le **renderer** (une seule instance, réutilisée pour tous les cristaux), dans une
`Map<BlockPos, HealthLerp>` — une entrée par position de cristal vue dans la session, jamais
nettoyée mais négligeable (en pratique une seule à la fois, une seule map active).
`setTarget(...)` repart de la valeur **actuellement affichée** (pas de l'ancienne cible) : un
coup qui arrive pendant que la barre bouge encore redirige l'animation en cours au lieu de la
faire sauter en arrière avant de repartir. Réutilisée telle quelle par `TowerHealthBarRenderer`
(voir plus bas, "La barre de vie des tours") — généralisée dès ce deuxième exemple concret.

### Le dessin du quad — `HealthBarRendering.java`

`collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), ...)` — la géométrie est
*soumise*, plus dessinée immédiatement ; le lambda reçoit un `PoseStack.Pose` et un
`VertexConsumer` au moment du rendu réel. Deux quads sont émis via `addSegment`, **juxtaposés
et jamais superposés** : la jauge colorée occupe la portion pleine (`healthPercent`), le gris
(`0.3, 0.3, 0.3`) le reste. Un segment de largeur nulle n'est pas émis du tout. Extraite dans sa
propre classe (statique, sans état) pour la même raison que `HealthLerp` : réutilisée telle
quelle par `TowerHealthBarRenderer`, seules la taille du quad et la portée de la caméra
diffèrent entre les deux usages.

> C'est volontaire. `debugQuads` est déclaré avec `sortOnUpload()`, un tri de transparence
> par distance à la caméra. Avec un fond pleine largeur recouvert par la jauge, les deux
> quads étaient coplanaires (`z = 0` tous les deux) et l'ordre de tri devenait instable : la
> barre apparaissait entièrement grise par intermittence, selon la position de la caméra.
> Juxtaposer les segments supprime le problème à la racine plutôt que de jouer sur le Z.

Le dégradé **vert → jaune → rouge** vient de `red()` / `green()` (le bleu est toujours 0) :

- au-dessus de 50 % : `red = (1 - p) * 2`, `green = 1` → vert pur à 100 %, jaune à 50 % ;
- en dessous : `red = 1`, `green = p * 2` → jaune à 50 %, rouge pur à 0 %.

> **Pourquoi `debugQuads` ?** C'est le seul render type public qui combine ce dont une barre
> de vie a besoin : quads non texturés (`POSITION_COLOR`), mélange translucide, test de
> profondeur, et surtout `withCull(false)` — donc visible quel que soit le sens du quad, ce
> qui évite tout problème d'orientation après le `scale` négatif.

## IA des ennemis

### La base commune — `entity/ai/AbstractEterniaCrystalAttackGoal.java`

Un seul sous-classeur aujourd'hui : `RangedAttackEterniaCrystalGoal` (les archers, qui ignorent
Blockade/Turret et ne visent que le cristal à distance). Les monstres de mêlée n'en ont plus
besoin depuis `AttackPriorityTargetGoal` (voir plus bas), qui ne cible pas QUE le cristal.
Cette classe abstraite, qui étend `MoveToBlockGoal`, porte le ciblage/déplacement vers le
cristal spécifiquement :

| Membre | Rôle |
|---|---|
| Constructeur `(mob, speedModifier, acceptedDistance, damagePerHit)` | `speedModifier`/`acceptedDistance` passés à `MoveToBlockGoal` ; `damagePerHit` exposé aux sous-classes via `this.damagePerHit` |
| `isValidTarget` | `state.is(ModBlocks.ETERNIA_CRYSTAL)` — ne cible que le cristal |
| `getMoveToTarget` | `this.blockPos` — vise la **base** du cristal, pour éviter que le mob tente de grimper |
| `findCrystal()` | `protected final`, retrouve le `EterniaCrystalBlockEntity` à `this.blockPos` (ou `null` s'il a été cassé) |
| `onReachedTarget(crystal)` | abstrait, appelé chaque tick tant que le mob est à `acceptedDistance()` du cristal |
| `onTargetLost()` | appelé chaque tick tant qu'il ne l'est pas ; no-op par défaut |

`SEARCH_RANGE` (16 blocs) reste une constante interne à cette classe, pas exposée au
constructeur — rien ne justifie encore qu'elle varie d'un ennemi à distance à l'autre.

> **Sous-classer `AbstractEterniaCrystalAttackGoal` directement** n'a d'intérêt que pour un
> **nouveau style d'attaque à distance sur le cristal spécifiquement** (une attaque de zone,
> par exemple) — pour un ennemi à distance qui attaque comme `RangedAttackEterniaCrystalGoal`
> mais avec d'autres chiffres, le constructeur `(mob, damagePerHit, ticksBetweenShots,
> shootRange)` couvre déjà ce cas, pas besoin de sous-classer quoi que ce soit.

### Le goal de mêlée unifié — `entity/ai/AttackPriorityTargetGoal.java`, `block/entity/AiAttackTarget.java`

Remplace ce qui était avant deux goals séparés (`AttackBlockadeGoal` + `AttackEterniaCrystalGoal`,
supprimés) : **un seul** goal, qui choisit lui-même la meilleure cible parmi tout ce qui
implémente l'interface `AiAttackTarget` — Blockade, Turret, Cristal, et toute future catégorie
— selon un système de **paliers de priorité**, discuté et tranché avec le joueur.

`AiAttackTarget` (interface, `block/entity/`) :

```java
public interface AiAttackTarget {
    int PRIORITY_BLOCK = 10;
    int PRIORITY_MELEE_TOWER = 20;
    int PRIORITY_CRYSTAL = 30;
    int PRIORITY_RANGED_TOWER = 40;

    int getAiPriority();
    void damage(int amount);
}
```

Indices **espacés** (10/20/30/40, pas 1/2/3/4) pour laisser de la place à un futur mécanisme
de provocation ou un nouveau type de tour sans décaler les valeurs existantes. Implémentée par
`AbstractTowerBlockEntity` (donc Blockade et Turret, `damage(int)` déjà présent, seul
`getAiPriority()` reste abstrait par catégorie) et par `EterniaCrystalBlockEntity`
indépendamment (pas de lien de code avec les tours, mais le même contrat) :

| Type | `getAiPriority()` |
|---|---|
| `AbstractBlockadeBlockEntity`, `dealsContactDamage=false` (pas de tour concrète encore) | `PRIORITY_BLOCK` (10) |
| `AbstractBlockadeBlockEntity`, `dealsContactDamage=true` (Spike Blockade) | `PRIORITY_MELEE_TOWER` (20) |
| `EterniaCrystalBlockEntity` | `PRIORITY_CRYSTAL` (30) |
| `AbstractTurretBlockEntity` (Harpoon Turret) | `PRIORITY_RANGED_TOWER` (40) |

**Aucun nouveau champ pour "corps à corps"** : c'est exactement le booléen
`dealsContactDamage` déjà utilisé pour les dégâts de contact — une Blockade qui fait mal au
contact (comme Spike Blockade) EST le "corps à corps" du joueur (dégâts périodiques dans un
petit rayon, sa propre cadence), une Blockade passive serait le "block" pur.

`AttackPriorityTargetGoal` (`extends MoveToBlockGoal`) réimplémente `findNearestBlock()`
(`protected` chez vanilla, donc overridable) : au lieu d'une seule recherche, **une passe par
palier, dans l'ordre croissant** (10, 20, 30, 40) — le premier palier qui trouve une cible dans
**sa propre portée** gagne, même si un palier suivant a une cible géométriquement plus proche.
Chaque passe rejoue l'algorithme en spirale de `MoveToBlockGoal.findNearestBlock()` (recopié,
vanilla ne l'expose pas comme méthode réutilisable), avec pour prédicat
`getBlockEntity(pos) instanceof AiAttackTarget target && target.getAiPriority() == tier` — pas
besoin d'un tag de bloc (l'ancien tag `dungeon_defenders:blockades` et `init/ModBlockTags.java`
ont été supprimés) : le filtre porte sur l'interface, générique à toute catégorie présente ou
future.

Portées par palier (reprises telles quelles des deux anciens goals) : Block/Corps à
corps/Tourelle → 8 blocs (comme l'ancien `AttackBlockadeGoal`) ; Cristal → 16 blocs (comme
`AbstractEterniaCrystalAttackGoal`). `DAMAGE_PER_HIT=5`, `TICKS_BETWEEN_HITS=20` (mêmes
valeurs que les deux anciens goals, qui les partageaient déjà) s'appliquent uniformément, quel
que soit le palier de la cible touchée — à l'impact, `getBlockEntity(blockPos) instanceof
AiAttackTarget target` puis `target.damage(...)`, un seul point de frappe pour toutes les
catégories.

`isValidTarget` (encore appelé par `canContinueToUse()` pour vérifier que la cible tenue reste
valide) redevient simple : `getBlockEntity(pos) instanceof AiAttackTarget`, sans revérifier le
palier — si la cible a changé de nature entre-temps, le prochain `canUse()` referait de toute
façon une recherche complète par paliers.

**Nouveau comportement concret, jamais possible avant** : un monstre de mêlée qui n'a **ni**
Blockade **ni** cristal à portée (le cristal étant hors de son rayon de détection de 16 blocs)
mais **a** un Harpoon Turret à portée de 8 blocs va enfin s'attaquer à la tourelle — dernier
recours, mais un recours qui n'existait pas du tout jusqu'ici (les tourelles étaient purement
ignorées par l'IA).

### Le goal à distance — `entity/ai/RangedAttackEterniaCrystalGoal.java`

```java
public RangedAttackEterniaCrystalGoal(PathfinderMob mob)                                                    // 3 dégâts / 20 ticks / 10 blocs (par défaut)
public RangedAttackEterniaCrystalGoal(PathfinderMob mob, int damagePerHit, int ticksBetweenShots, double shootRange)
```

Contrairement au corps à corps, la **portée de tir** (`shootRange`, passée comme
`acceptedDistance` au parent) est elle aussi exposée au constructeur : un futur ennemi à
distance pourrait raisonnablement viser de plus près (un lanceur) ou de plus loin (un
tireur d'élite), pas seulement avoir des dégâts/une cadence différents. `DRAW_TICKS` (20,
temps de tension de l'arc) reste une constante interne : c'est un détail de timing
d'animation, pas un levier d'équilibrage entre archétypes.

Dans `onReachedTarget(crystal)`, une fois à portée : le mob se tourne vers le cristal
(`LookControl#setLookAt`, nécessaire une fois immobile — `MoveToBlockGoal` ne le fait plus
après l'approche), puis alterne tension (`mob.startUsingItem(MAIN_HAND)`, ce qui déclenche la
pose vanilla "arc tendu" puisque le squelette porte déjà un arc par défaut) et tir. Au tir,
`crystal.damage(damagePerHit)` est appliqué **directement** au cristal, sur le même principe
"harnais" que le corps à corps — la flèche réellement lancée (`spawnArrow`, une vraie entité
`Arrow`, avec le calcul d'arc `dy + distance × 0.2` repris du tir vanilla) n'est là que pour
le **visuel** du tir, ce n'est pas sa collision qui inflige les dégâts (le cristal n'étant pas
une entité, une flèche vanilla ne saurait pas le "toucher" toute seule).

### L'attribution — `ModEvents.onMonsterSpawn`

Écoute `EntityJoinLevelEvent` sur le bus de jeu. Pour chaque `Monster` rejoignant un monde
côté serveur, **un seul** goal est ajouté au `goalSelector`, selon le type :

- `AbstractSkeleton` (squelette, et tout futur sous-type) reçoit `RangedAttackEterniaCrystalGoal`
  (priorité 1) — ignore Blockade/Turret, ne vise que le cristal à distance.
- Tout le reste reçoit `AttackPriorityTargetGoal` (priorité 0) — choisit lui-même sa cible
  parmi Block/Corps à corps/Cristal/Tourelle selon leur palier de priorité et leur portée
  respective (voir "Le goal de mêlée unifié" plus haut).

> `Monster` plutôt que `PathfinderMob` : les deux goals n'exigent techniquement qu'un
> `PathfinderMob`, mais cette classe couvre aussi les mobs passifs (animaux, villageois...).
> `Monster` est la bonne frontière sémantique — tout ce qui est hostile, rien de passif.

`EntityJoinLevelEvent` se déclenche aussi au rechargement d'un chunk et au changement de
dimension. Le code vérifie donc d'abord qu'aucun des deux goals n'est déjà présent :

```java
monster.goalSelector.getAvailableGoals().stream()
        .anyMatch(wrapped -> wrapped.getGoal() instanceof RangedAttackEterniaCrystalGoal
                || wrapped.getGoal() instanceof AttackPriorityTargetGoal)
```

Sans ce test, un même monstre cumulerait plusieurs exemplaires du goal et attaquerait le
cristal plusieurs fois par seconde.

## La barre de vie des monstres — `entity/MobHealthBarRenderer.java`

Même mécanisme et mêmes conditions d'affichage que "La barre de vie des tours" (endommagé + à
moins de 16 blocs, animation 300 ms via `HealthLerp`, dessin via `HealthBarRendering`,
2026-08-24) — mais le rendu d'entité vivante fonctionne différemment de celui d'un block
entity, donc une intégration à part plutôt qu'un simple troisième appelant des mêmes classes.

**Le problème (vie absente du render state)** : `EntityRenderState`/`LivingEntityRenderState`
(vanilla) ne portent **aucun** champ de vie — vérifié dans le code source, contrairement à ce
qu'on pourrait attendre par analogie avec `getCrystalHealth()`/`getHealth()` des block entities
de ce mod. **`RegisterRenderStateModifiersEvent`** (NeoForge, bus mod,
`onRegisterRenderStateModifiers` dans `DungeonDefendersModClient`) permet d'exécuter du code
juste après l'extraction vanilla d'un render state, pour y ajouter des données — enregistré sur
`LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>` (capturé via un
`com.google.common.reflect.TypeToken` anonyme — un simple `Class<...>` ne suffit pas ici, le
compilateur ne peut pas vérifier les bornes génériques à partir d'un type brut), donc appliqué
à **toute** entité vivante, coût négligeable (deux `float` + un `int` copiés) même si le rendu
lui-même ne filtre que deux types de mob (voir plus bas). Stockage via `ContextKey<T>`
(`net.minecraft.util.context`, vanilla) : `MobHealthBarRenderer.HEALTH`/`MAX_HEALTH`/
`ENTITY_ID`, écrites par le modificateur (`state.setRenderData(HEALTH, entity.getHealth())`) et
relues côté rendu (`state.getRenderData(HEALTH)`) — le stockage vit sur `BaseRenderState`
(`net.neoforged.neoforge.client.renderstate`), une extension NeoForge que `EntityRenderState`
hérite déjà. **Cette moitié n'a jamais été le bug** (voir plus bas).

**Le vrai bug (trouvé le 2026-08-26, en testant en jeu) — mauvais repère de pose** : la
première version dessinait via un `RenderLayer<S, M>` (mécanisme vanilla pour ajouter du rendu
par-dessus un mob, ex. le collier du loup), branché via `EntityRenderersEvent.AddLayers`. Un
`RenderLayer#submit(...)` s'exécute **à l'intérieur** du repère local du modèle : dans
`LivingEntityRenderer#submit`, la boucle `for (RenderLayer<S,M> layer : this.layers)` a lieu
**entre** un `poseStack.pushPose()` qui applique `scale(-1,-1,1)` (convention de modèle
vanilla) + une rotation selon `state.bodyRot`, et le `poseStack.popPose()` qui referme ce
repère. Essayer d'y superposer sa propre rotation caméra pour un billboard (comme le fait
`HealthBarRendering`/`TowerHealthBarRenderer` dans un repère monde normal) compose deux
transformations incompatibles : la géométrie est bien soumise, mais mal placée/orientée —
invisible en pratique, pas un crash, donc rien dans les logs pour orienter le diagnostic.

**Le nametag vanilla évite exactement ce piège** : `EntityRenderer#submitNameDisplay` est
appelé depuis `EntityRenderer#submit` (la classe de base), lui-même invoqué par
`LivingEntityRenderer#submit` via `super.submit(...)`, **après** le `popPose()` qui referme le
repère du modèle — donc dans le même repère caméra-relatif "brut" qu'un `BlockEntityRenderer`.

**La solution retenue** : abandonner `RenderLayer` au profit de
`net.neoforged.neoforge.client.event.RenderLivingEvent.Post` (bus de jeu, pas bus mod), qui se
déclenche juste après ce `super.submit(...)` — même repère que le nametag, donc valide pour un
billboard caméra-face comme `HealthBarRendering`. Pas de filtre générique par type d'entité sur
cet event (contrairement à `RegisterRenderStateModifiersEvent`) : `onRenderLiving` reçoit
**toute** `LivingEntity` rendue et filtre lui-même sur `state.entityType` (zombie/squelette
uniquement, seuls monstres du mod pour l'instant — pas de liste partagée avec
`SpawnableEnemy`). Sort tôt si vide/PV pleins/trop loin (`state.distanceToCameraSq`, un champ
vanilla), sinon anime via `HealthLerp` (indexé par `ENTITY_ID` plutôt que `BlockPos` — un
monstre bouge, `LERP_BY_ENTITY_ID` static plutôt que porté par une instance de couche
puisqu'il n'y a plus de couche) et dessine via `HealthBarRendering`, exactement comme avant.

**Corrigé le 2026-08-26 suite à un test en jeu, mais le correctif lui-même reste à confirmer en
jeu** — voir [06-a-tester.md](06-a-tester.md).

## Onglet créatif

`dungeon_defenders_tab`, titre `Component.translatable("itemGroup.dungeon_defenders")`,
contient l'item du cristal, celui du Spike Blockade, le spawner, la baguette de mana et le
cristal de la taverne.

## Apparence du Cristal d'Eternia

Le bloc utilise un modèle `cube_all` standard, mais pointe **provisoirement** sur la texture
vanilla `minecraft:block/diamond_block` : il n'y a pas encore de texture dédiée. Voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md).

Le bloc est miné à la pioche en diamant (tags `mineable/pickaxe` et `needs_diamond_tool`) et
se drope lui-même via `data/dungeon_defenders/loot_table/blocks/eternia_crystal.json`.

## Le Spike Blockade — `block/SpikeBlockadeBlock.java`

Premier **tower** du mod (nom repris tel quel du plan Excel du joueur, feuille "Tours" —
Squire), au sens propre du terme cette fois : un mur avec ses propres PV, que les ennemis
doivent détruire pour continuer vers le cristal, plutôt qu'un piège de sol qu'on traverse sans
y prêter attention. Remplace l'ancien `SpikeTrapBlock` (piège au sol via `stepOn`, mécanisme
différent — supprimé, pas juste renommé) : les deux idées ne se recoupent pas assez pour
garder les deux, et le joueur a choisi de repartir sur la vraie mécanique de blocage/PV du
plan Excel plutôt que de garder l'ancien piège en parallèle.

**Le blocage du passage est en grande partie gratuit** : un bloc plein (propriété par défaut de
n'importe quel `Block` Minecraft, rien à coder) bloque déjà la marche d'un mob. Une seule
correction a été nécessaire : la hitbox par défaut fait 1 bloc de haut, hauteur qu'un monstre
peut sauter (~1,25 bloc) pour se retrouver debout dessus et continuer son chemin par-dessus la
tour — testé en jeu le 2026-08-23 (voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md#corrections-trouvées-lors-des-tests-en-jeu-du-2026-08-23)).
`getCollisionShape`/`getShape` sont donc surchargés pour renvoyer une boîte de 1,5 bloc de haut
(`Shapes.box(0, 0, 0, 1, 1.5, 1)`, même principe que les murs/barrières vanilla), même style que
la hitbox 1×3×1 déjà utilisée par `EterniaCrystalBlock`. Le reste de la logique custom sert à
donner des PV au blockade et à faire en sorte qu'un ennemi choisisse de l'attaquer plutôt que de
rester bloqué bêtement devant.

### La catégorie "Blockade" — `block/entity/AbstractBlockadeBlockEntity.java`

Le Spike Blockade est le premier membre concret d'une **catégorie de code** commune à toutes
les futures tours "mur à PV" (voir la taxonomie du joueur dans
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md#système-de-tours-catégorie-blockade-démarrée)) : elle
fusionne ce que la première version de la taxonomie séparait en "block passif" et "corps à
corps" — un blockade passif n'est qu'un blockade avec les dégâts de contact désactivés.

`AbstractBlockadeBlockEntity` étend `AbstractTowerBlockEntity` (voir "Le Harpoon Turret" plus
bas pour le détail de cette base commune, extraite quand une deuxième catégorie concrète —
"Turret" — a fait apparaître une vraie duplication de PV/mana/persistance) et n'ajoute que ce
qui est spécifique au contact :

| Paramètre | Rôle |
|---|---|
| `dealsContactDamage` | booléen : ce blockade pique-t-il au contact, ou est-il purement passif ? |
| `contactDamage` / `contactDamageIntervalTicks` / `contactRange` | ignorés si `dealsContactDamage=false` ; sinon, dégâts et cadence des dégâts de contact |

Pas de stat "portée" sur cette catégorie : une blockade n'a pas de portée d'attaque, seulement
une zone de contact — cohérent avec le fait qu'elle bloque physiquement le passage plutôt que
de tirer dessus.

`serverTick(...)` (défini une fois sur la catégorie, réutilisé tel quel par chaque sous-classe
via `createTickerHelper(type, MON_TYPE.get(), AbstractBlockadeBlockEntity::serverTick)`) scanne
`serverLevel.getEntitiesOfClass(Monster.class, contactArea)` à chaque tick et applique les
dégâts de contact aux monstres dont le cooldown (`WeakHashMap<Monster, Long>` — évite de
retenir des entités mortes/déchargées) est écoulé, **uniquement si** `dealsContactDamage` est
actif — sinon la méthode ne fait rien.

`SpikeBlockadeBlockEntity` n'est donc plus qu'une déclaration de stats : `MAX_HEALTH=30`,
`MANA_COST=30`, `dealsContactDamage=true`, `CONTACT_DAMAGE=2.0F`,
`CONTACT_DAMAGE_INTERVAL_TICKS=20` (1 s), `CONTACT_RANGE=1.0` — toute la logique vit dans la
base ou la catégorie.

`getAiPriority()` (voir "IA des ennemis" plus haut, `AiAttackTarget`) dérive de
`dealsContactDamage` : `true` (Spike Blockade) → `PRIORITY_MELEE_TOWER` (20, le "corps à
corps" du joueur) ; `false` (pas encore de tour concrète) → `PRIORITY_BLOCK` (10, priorité la
plus haute). Aucun champ dédié, aucune duplication.

### Le coût en mana et la restriction de phase — `ModEvents.onTowerPlace`

**Générique à toute catégorie de tour**, pas seulement Blockade (renommé depuis
`onBlockadePlace` en même temps que le Harpoon Turret a introduit une deuxième catégorie —
voir plus bas pourquoi le filtre devait changer). Écoute `BlockEvent.EntityPlaceEvent`
(NeoForge, bus de jeu, annulable), qui se déclenche pour **tout** bloc placé par une entité,
pas seulement les tours — d'où le filtre `getBlockEntity(pos) instanceof
AbstractTowerBlockEntity` en tout début de handler, générique à toutes les catégories sans
avoir besoin d'un tag séparé (le block entity venant d'être créé au moment où l'événement se
déclenche).

Logique : d'abord la phase — `GamePhase.of(level).allowsTowerBuilding()`, vrai en
**Construction** et à la **Taverne**, faux en Combat ; sinon placement annulé et message dédié
(`dungeon_defenders.tower.build_phase_only`), vérifié **avant** le mana pour ne pas laisser
croire à un problème de mana alors que c'est la phase qui bloque. À la Taverne, le handler
s'arrête là : **la pose y est gratuite** (voir "La phase Taverne" plus bas). Puis le mana : si
`player.getData(ModAttachments.MANA) < tower.getManaCost()`, l'événement est annulé
(`event.setCanceled(true)`) — NeoForge restaure alors le `BlockSnapshot` précédent **et** rend
l'item au joueur automatiquement (le bloc n'est jamais réellement resté posé), pas besoin de le
faire à la main. Sinon, le mana est débité et resynchronisé (`setData`/`syncData`, même paire
que `ManaTestWandItem`) et un message confirme la dépense.

Un seul chemin de pose existe aujourd'hui (la roue, voir plus bas), mais ce handler s'applique
à **n'importe quel** déclencheur de `BlockEvent.EntityPlaceEvent`, rien à refaire si un second
apparaît. Doublé côté client (`TowerPlacementClientEvents`) : la roue elle-même refuse de
s'ouvrir hors phase Construction, pour éviter de faire tout le mode pose avant un refus final —
le serveur reste la seule autorité réelle. Aucune exemption pour le mode créatif (même
convention que `ManaTestWandItem`).

### L'item ne pose plus rien — `block/TowerBlockItem.java`

Décidé avec le joueur : la roue est **l'unique façon de poser une tour**, plus d'item posable à
la main, pour n'importe quelle catégorie. `SPIKE_BLOCKADE_ITEM`/`HARPOON_TURRET_ITEM`
(existent toujours pour un éventuel drop à la casse) ne sont plus des `BlockItem` classiques
mais des `TowerBlockItem`, dont `useOn(...)` retourne systématiquement
`InteractionResult.PASS` — clic droit avec en main : rien ne se passe, comme si l'item n'avait
aucune interaction avec le monde. Retirés de l'onglet créatif pour la même raison. Classe
commune à toutes les catégories : une future tour utilise la même classe d'item, pas besoin de
la réécrire.

### La priorité IA

Un monstre ne s'attaque pas naturellement à un bloc plein dans Minecraft (il chercherait
plutôt à le contourner) : il faut donc une IA dédiée pour qu'un ennemi de mêlée choisisse de
détruire une blockade sur son chemin, plutôt que de rester bloqué devant indéfiniment ou de
l'ignorer complètement. C'est désormais un mécanisme générique à toute catégorie de tour, pas
spécifique à Blockade — voir "IA des ennemis" tout en haut de cette page, section "Le goal de
mêlée unifié" (`entity/ai/AttackPriorityTargetGoal.java`, `block/entity/AiAttackTarget.java`)
pour le détail complet (paliers de priorité, algorithme de recherche, etc.). Ce qui concerne
Spike Blockade spécifiquement : `dealsContactDamage=true` lui donne la priorité "corps à
corps" (20), juste après un éventuel "block" pur (10, pas encore de tour concrète) et avant le
cristal (30) et les tourelles (40).

### Apparence

Modèle `cube_all` pointant **provisoirement** sur la texture vanilla
`minecraft:block/dripstone_block` (choisie pour son aspect visuellement "pointu", reprise de
l'ancien `SpikeTrapBlock`). Miné à la pioche (tag `mineable/pickaxe` uniquement, pas de tag de
niveau d'outil) et se drope lui-même via
`data/dungeon_defenders/loot_table/blocks/spike_blockade.json`.

**Ce qui n'est PAS fait**, volontairement — voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md) :

- Coût en mana à la pose branché (30, valeur de test — voir "Le coût en mana et la restriction
  de phase" plus haut), mais **pas encore équilibré** : choisi arbitrairement pour vérifier que
  le mécanisme fonctionne, pas après réflexion sur l'économie de mana globale.
- ~~Pas de remboursement de mana en le cassant~~ — fait, voir `ModEvents.onTowerBreak`
  ("Ce qui est implémenté" dans 05-etat-et-problemes-connus.md).
- ~~Pas d'indicateur visuel de PV restants~~ — fait (2026-08-24) : `TowerHealthBarRenderer`,
  générique à toute catégorie de tour, voir plus bas "La barre de vie des tours".
- Un seul membre concret de la catégorie "Blockade" pour l'instant (Spike Blockade). Une
  deuxième catégorie existe désormais ("Turret", voir plus bas), mais pas encore d'aura/piège
  non attaquable ni de piège de sol (les autres catégories envisagées par le joueur, voir
  05-etat-et-problemes-connus.md) — chacune aura probablement besoin de sa propre base, une
  fois qu'un second exemple concret de chaque existera.

## Le Harpoon Turret — `block/HarpoonTurretBlock.java`

Premier membre de la catégorie "Turret" (nom repris du plan Excel — Squire) : contrairement à
Blockade, ce n'est pas un mur qu'on percute, c'est une **tour à distance** qui scanne et tire
toute seule — construite pour tester l'aperçu de portée de la roue (jamais exercé jusque-là,
aucune tour n'avait `range > 0`). Ne bloque pas spécialement le passage plus qu'un bloc plein
normal, mais ce n'est pas son rôle : elle est pensée "posée en retrait" (voir la taxonomie du
joueur), à l'inverse d'un mur pensé pour être au contact. Même hitbox custom de 1,5 bloc de
haut que Spike Blockade (voir plus haut) : sans ça, un monstre pourrait sauter sur la tourelle
et continuer son chemin par-dessus au lieu de la contourner ou de l'attaquer.

### La base commune à toutes les tours — `block/entity/AbstractTowerBlockEntity.java`

**Refactor motivé par ce chantier** : avant le Harpoon Turret, `AbstractBlockadeBlockEntity`
portait directement PV/coût mana/persistance/sync — dupliquer tout ça pour une deuxième
catégorie aurait été la première vraie duplication concrète entre catégories (pas juste entre
deux tours de la même catégorie, où on ne généralise toujours pas tant qu'un second exemple ne
l'a pas prouvé). `AbstractTowerBlockEntity` absorbe donc ce qui est commun à **toute** tour,
Blockade ou Turret : `maxHealth`, `manaCost`, `getHealth()`/`getMaxHealth()`/`getManaCost()`,
`damage(int)`/`setHealth(int)` (détruit le bloc à 0, sans drop), persistance
(`saveAdditional`/`loadAdditional`) et sync client (`getUpdatePacket`/`getUpdateTag`).
`AbstractBlockadeBlockEntity` et `AbstractTurretBlockEntity` en héritent chacune, et n'ajoutent
que ce qui leur est propre — comportement des deux catégories inchangé, juste moins de code
dupliqué.

### La catégorie "Turret" — `block/entity/AbstractTurretBlockEntity.java`

Contrairement à Blockade (un `Goal` porté par le monstre l'attaque), c'est la tour
**elle-même** qui agit à chaque tick — même principe que `SpawnerBlockEntity`, qui scanne/agit
tout seul sans dépendre d'une IA externe. Stats propres à la catégorie :

| Paramètre | Rôle |
|---|---|
| `range` | distance de détection, en blocs |
| `coneAngleDegrees` | largeur du cône de tir autour de `HORIZONTAL_FACING` ; `>= 360` = omnidirectionnel (pas de filtre d'angle) |
| `damage` / `attackIntervalTicks` | dégâts par tir et cadence |

**Le cône** : angle **fixe** à l'origine (la tour), pas un angle qui s'élargit avec la
distance — la largeur du cône à son extrémité augmente avec `range` par pure trigonométrie
(arc = rayon × angle), le sommet du cône reste toujours la position de la tour.

`serverTick(...)` : si le cooldown (`attackIntervalTicks`) n'est pas écoulé, ne fait rien.
Sinon, lit `HORIZONTAL_FACING` sur le `BlockState` courant (voir plus bas), scanne
`getEntitiesOfClass(Monster.class, AABB(pos).inflate(range))`, filtre par distance horizontale
réelle puis, si `coneAngleDegrees < 360`, par angle (produit scalaire entre le vecteur de
`facing` et le vecteur horizontal vers la cible, `acos` du cosinus donne l'angle en degrés,
rejeté si `> coneAngleDegrees / 2`). Cible retenue = la plus proche valide ; si trouvée, tire :
dégâts appliqués **directement** (`monster.hurt(...)`, même principe que
`RangedAttackEterniaCrystalGoal` sur le cristal — pas de détection de collision) et une flèche
purement visuelle spawnée via le constructeur `Arrow` **sans propriétaire** (`Arrow(Level,
double, double, double, ItemStack, ItemStack)` — pas de `LivingEntity`, c'est un bloc qui tire),
avec le même léger arc vers le haut que `RangedAttackEterniaCrystalGoal` pour compenser la
gravité sur la distance.

`HarpoonTurretBlockEntity` n'est qu'une déclaration de stats : `MAX_HEALTH=20`,
`MANA_COST=50`, `RANGE=12.0`, `CONE_ANGLE_DEGREES=45.0`, `DAMAGE=6.0F`,
`ATTACK_INTERVAL_TICKS=30` (1,5 s) — valeurs de test, pas encore équilibrées, comme pour Spike
Blockade.

Le turret a des PV comme une Blockade, priorité IA la plus basse (`PRIORITY_RANGED_TOWER`, 40
— voir "IA des ennemis" tout en haut de cette page) : un monstre de mêlée ne s'y attaque qu'en
tout dernier recours, quand rien de plus prioritaire (Block, Corps à corps, Cristal) n'est à
portée — nouveau comportement rendu possible par `AttackPriorityTargetGoal`, jamais possible
avant lui (les tourelles étaient purement ignorées par l'IA).

### L'orientation — `HORIZONTAL_FACING`, premier vrai usage de la rotation de la roue

Contrairement à `SpikeBlockadeBlock`, `HarpoonTurretBlock` déclare une vraie propriété de
`BlockState` (`BlockStateProperties.HORIZONTAL_FACING`, via `registerDefaultState` et
`createBlockStateDefinition`). **Aucun changement nécessaire côté réseau** :
`ModNetworking.handlePlaceTower` appliquait déjà la rotation choisie dans la roue
(`state.hasValue(HORIZONTAL_FACING) → state.setValue(...)`) depuis sa construction, en
anticipation — jamais exercé jusqu'ici puisque Spike Blockade n'a pas cette propriété. Le
Harpoon Turret en est le premier vrai consommateur.

### Apparence

Modèle `minecraft:block/orientable` (parent vanilla utilisé par la furnace) avec les textures
`furnace_top`/`furnace_side`/`furnace_front` — placeholder qui a l'avantage d'être
**directionnel** (contrairement au `cube_all` de Spike Blockade), utile pour vérifier
visuellement que la rotation choisie dans la roue correspond bien à la façon dont le bloc est
réellement orienté une fois posé. Blockstate à 4 variantes (`facing=north/east/south/west`,
`y: 0/90/180/270`), même structure que `minecraft:blockstates/furnace.json`. Miné à la pioche,
se drope lui-même via `data/dungeon_defenders/loot_table/blocks/harpoon_turret.json`.

**Ce qui n'est PAS fait**, volontairement :

- Pas de restriction de phase sur le tir lui-même (seule la **pose** est restreinte à la phase
  Construction, comme pour Blockade — même handler généralisé, voir plus haut). Un monstre
  égaré en phase Construction se ferait quand même tirer dessus ; comportement mineur, cohérent
  avec les dégâts de contact de Blockade, jamais phase-gatés non plus.
- Pas de son/particules au tir, juste la flèche visuelle + dégâts directs.
- ~~Pas d'indicateur visuel de PV restants~~ — fait (2026-08-24), voir "La barre de vie des
  tours" plus bas (même renderer générique que Spike Blockade).
- Deux bugs de rotation successifs corrigés en jeu (2026-08-21), tous les deux dans l'aperçu
  (le bloc réellement posé, lui, a toujours été correct — voir plus bas "L'hologramme et le
  cercle de portée" pour le détail des deux causes) :
  1. La rotation utilisait `Direction.toYRot()` (convention `SOUTH=0°/WEST=90°/NORTH=180°/
     EAST=270°`, pensée pour le yaw des entités) — le cône pointait à l'opposé de la vraie
     direction de tir.
  2. Une fois remplacée par les valeurs `y` du blockstate (`NORTH=0°/EAST=90°/SOUTH=180°/
     WEST=270°`, comme `furnace.json`), EST et OUEST restaient inversés : ces valeurs sont
     correctes pour le système de blockstate de Minecraft, mais pas pour
     `Axis.YP.rotationDegrees(...)` (rotation main-droite standard, l'inverse avec les axes de
     Minecraft) utilisé pour dessiner l'aperçu — l'utilisateur choisissait une rotation en se
     fiant au cône, et la tour réellement posée (correcte, mais différente de ce que montrait
     le cône) semblait donc "tournée à l'envers" une fois confirmée.

## La barre de vie des tours — `block/entity/TowerHealthBarRenderer.java`

Un seul renderer, générique sur `AbstractTowerBlockEntity` (`<T extends AbstractTowerBlockEntity>
implements BlockEntityRenderer<T, TowerHealthBarRenderState>`), couvre toute catégorie de tour
existante ou future — enregistré une fois par `BlockEntityType` concret dans
`DungeonDefendersModClient#onRegisterRenderers` (une ligne `event.registerBlockEntityRenderer(...)`
par tour, mais toujours `TowerHealthBarRenderer::new`).

**Conditions d'affichage**, décidées avec le joueur (2026-08-24) : contrairement à la barre du
Cristal d'Eternia (toujours affichée — il n'y en a jamais qu'un), une base peut compter des
dizaines de tours posées. Une barre visible sur toutes en permanence deviendrait illisible, donc
`extractRenderState` la cache tant que :

- la tour n'est **pas endommagée** (`getHealth() >= getMaxHealth()`) ;
- ou la caméra est à plus de 16 blocs (`MAX_DISTANCE_SQ`, même principe que l'aperçu du
  spawner, voir plus bas).

Seules les tours réellement endommagées et à portée de vue affichent donc leur barre.

**Rendu et animation** : même mécanisme que le cristal, extrait dans deux classes partagées
pour éviter la duplication après ce deuxième exemple concret :

- `HealthLerp` — anime le passage d'un ratio de PV à l'autre sur 300 ms, en **temps réel**
  (`Util.getMillis()`), pas sur `partialTicks` : un `BlockEntityRenderState` est recréé à
  chaque frame (`BlockEntityRenderDispatcher#tryExtractRenderState`), impossible d'y retenir
  quoi que ce soit d'une frame à l'autre — l'animation vit donc sur le renderer lui-même, dans
  une `Map<BlockPos, HealthLerp>` (une entrée par position de tour/cristal vue dans la session).
  Un nouveau coup pendant que la barre bouge encore redirige l'animation en cours plutôt que de
  la faire sauter en arrière.
- `HealthBarRendering` — dessine le quad (dégradé vert → jaune → rouge + segment gris restant),
  appelée par les deux renderers une fois le `poseStack` positionné/orienté en billboard.

`TowerHealthBarRenderState` ne porte que `visible` et `healthPercent` — la condition
d'affichage elle-même est calculée dans `extractRenderState`, pas dans `submit`.

## Le reste du roster de l'Écuyer

Décidé avec le joueur (2026-08-29) : les quatre tours suivantes complètent le roster de
l'Écuyer (Spike Blockade et Harpoon Turret existaient déjà). Chaque design a été discuté et
validé un par un avec le joueur avant d'être codé — les questions d'architecture (fallait-il un
nouveau mécanisme, ou les catégories existantes suffisaient-elles ?) ont été tranchées au même
moment, pas devinées après coup.

### Bouncer Blockade — `block/BouncerBlockadeBlock.java`

Deuxième membre de "Blockade" : au contact, inflige des dégâts **et** repousse — décidé avec le
joueur, les deux effets ensemble, pas l'un à la place de l'autre, et seulement les monstres déjà
dans sa portée de contact (même `AABB` que les dégâts, pas un rayon séparé).

`AbstractBlockadeBlockEntity` gagne un nouveau paramètre, `knockbackStrength` (0 pour Spike
Blockade et Slice N Dice Blockade, > 0 pour Bouncer) :

```java
if (blockEntity.knockbackStrength > 0.0F) {
    double dx = (pos.getX() + 0.5D) - monster.getX();
    double dz = (pos.getZ() + 0.5D) - monster.getZ();
    monster.knockback(blockEntity.knockbackStrength, dx, dz);
}
```

**Sens du vecteur, corrigé une fois en jeu (2026-08-29)** : la toute première version passait
"position du monstre moins position de la source" en pensant reproduire la convention de
`LivingEntity#blockedByItem` — au premier test, les monstres étaient **attirés** vers le
blockade au lieu d'être repoussés, signe que la lecture de cette convention vanilla était
inversée. Corrigé à partir du comportement réellement observé (pas re-déduit des sources une
deuxième fois, pour éviter de refaire la même erreur d'interprétation) : `Entity#knockback`
pousse dans le sens **opposé** au vecteur passé (son corps calcule
`deltaMovement - normalize(xd, zd)`), donc pour repousser loin de la source, il faut lui passer
un vecteur qui pointe **vers** elle (source moins monstre), pas l'inverse. Même classe de bug
que la convention de rotation `Axis.YP` de la roue des tours (voir plus haut) : un sens de
vecteur/rotation mal interprété malgré une vérification dans les sources — matérialisé cette
fois par un test en jeu plutôt qu'un rendu visuel.

**"Vitesse d'attaque" (question du joueur, 2026-08-29)** : oui, c'est déjà géré —
`contactDamageIntervalTicks` (10 ticks pour Bouncer, soit 0,5s) est le même cooldown pour les
dégâts **et** la repousse, par monstre (`AbstractBlockadeBlockEntity#serverTick`,
`lastContactDamageTick`). Un monstre au contact ne se fait donc pousser qu'une fois par
intervalle, jamais en continu tant qu'il reste collé au blockade — c'est ce qui empêche déjà la
repousse d'être "trop forte" par répétition ; `knockbackStrength` ne règle que l'intensité d'une
poussée isolée, pas sa fréquence.

25 PV, 25 mana (moins cher que Spike Blockade, l'intérêt n'étant pas les dégâts), 1 PV toutes
les 10 ticks — valeurs de test, pas encore équilibrées. `KNOCKBACK_STRENGTH` a fait un aller-
retour en jeu (2026-08-29) : 0.8F d'abord jugée imperceptible — mais le sens du vecteur était
encore inversé à ce moment-là (voir ci-dessus), donc cette impression n'était pas fiable ;
remontée à 1.6F dans la foulée, puis le sens corrigé séparément. Une fois la repousse
effectivement fonctionnelle dans le bon sens, 1.6F s'est avérée trop forte — **remise à 0.8F**,
sa valeur d'origine.

### Slice N Dice Blockade — `block/SliceNDiceBlockadeBlock.java`

Troisième membre de "Blockade" : confirmé avec le joueur ("tout est correct") — **aucun nouveau
comportement**. `AbstractBlockadeBlockEntity#serverTick` inflige déjà ses dégâts à **tous** les
monstres présents dans `contactRange`, pas seulement le premier ; Spike Blockade ne l'exploite
juste pas vraiment avec son rayon d'1 bloc. Se différencie par une cadence bien plus rapide et
des dégâts plus faibles par coup (DPS continu façon lames tournantes plutôt que coups espacés
façon pics) et un rayon légèrement plus large (1,5 bloc). 35 PV, 40 mana, 1 PV toutes les
5 ticks.

### Bowling Ball Turret — `block/BowlingBallTurretBlock.java`, `entity/BowlingBallEntity.java`

Deuxième membre de "Turret" : demandé explicitement par le joueur — "on veut vraiment que la
boule continue sur une certaine longueur même si elle touche un ennemi, elle continue". Contrairement
à Harpoon Turret (flèche purement visuelle, dégâts appliqués directement par le code appelant à
une seule cible), Bowling Ball Turret lance une vraie `BowlingBallEntity` avec une vraie
collision — c'est **elle** qui applique les dégâts, pas `fireAt`.

**`BowlingBallEntity extends Arrow`** plutôt qu'une nouvelle entité construite de zéro :
`AbstractArrow` a déjà tout ce qu'il faut pour "traverse plusieurs ennemis sans s'arrêter au
premier" — c'est exactement le mécanisme vanilla d'une flèche enchantée **Perforation**
(`piercingIgnoreEntityIds`, qui retient les entités déjà touchées pour ne jamais les re-toucher,
et empêche l'arrêt tant que le nombre de perforations n'est pas dépassé). Problème : le niveau
de perforation se règle via `setPierceLevel(byte)`, **privé** dans `AbstractArrow` — inaccessible
depuis une sous-classe. Le seul point d'entrée public est le constructeur qui accepte un
`firedFromWeapon` réellement enchanté :

```java
private static ItemStack fakePiercingWeapon(ServerLevel level) {
    Holder<Enchantment> piercing = level.registryAccess().getOrThrow(Enchantments.PIERCING);
    ItemStack weapon = new ItemStack(Items.CROSSBOW);
    weapon.enchant(piercing, PIERCE_LEVEL);
    return weapon;
}
```

Cette fausse arme (jamais donnée à personne, jamais visible) est passée au constructeur
`Arrow(level, x, y, z, pickupItemStack, firedFromWeapon)` — en interne, `AbstractArrow` lit
l'enchantement via `EnchantmentHelper.getPiercingCount(...)` et appelle lui-même son propre
`setPierceLevel` privé. Pas de réflexion sur un champ privé vanilla, juste le chemin public
prévu pour ce cas — vérifié dans les sources plutôt que deviné.

Le reste vient gratuitement d'`AbstractArrow#onHitEntity` (déjà appelé par le `tick()` hérité,
rien à réimplémenter) : dégâts (`setBaseDamage`, nuance à noter — proportionnels à la vitesse au
moment de l'impact, comme une vraie flèche vanilla, donc légèrement décroissants avec la
distance déjà parcourue), son au contact, léger recul de la cible. `setNoGravity(true)` : la
boule roule en ligne droite plutôt que de retomber en cloche ; `tick()` est surchargé pour se
`discard()` une fois `MAX_BALL_DISTANCE` parcourue, indépendamment du nombre d'ennemis
traversés en chemin.

**Direction du tir : uniquement horizontale, pas visée sur `target.getEyeY()`** — signalé au
premier test en jeu (2026-08-29) : viser la hauteur des yeux de la cible faisait partir la boule
vers le haut, façon tir à l'arbalète, alors qu'une boule qui roule doit garder une hauteur fixe.
Corrigé dans `BowlingBallTurretBlockEntity.fireAt` (pas dans `BowlingBallEntity` lui-même — le
calcul de direction reste la responsabilité de l'appelant, l'entité ne fait que voler droit une
fois lancée) :

```java
Vec3 direction = new Vec3(
        target.getX() - origin.x,
        0.0D,
        target.getZ() - origin.z
).normalize();
```

Combiné à `setNoGravity(true)`, la boule part maintenant en ligne parfaitement horizontale, à la
hauteur de tir de la tourelle (`muzzlePosition`, mi-hauteur du bloc).

**`extends Arrow` plutôt qu'un nouvel `EntityType` custom** : le constructeur position d'`Arrow`
force `EntityType.ARROW` en interne (voir `Arrow.java`) — la boule prend donc l'apparence d'une
flèche vanilla en vol, pas une vraie boule. Limite assumée, même famille que les autres
placeholders visuels du mod (le cristal de mana a l'air d'une orbe d'XP vanilla, etc.) : évite
d'enregistrer un `EntityType`/renderer dédié pour un MVP. 20 PV, 55 mana, 5 dégâts/tir, cadence
lente (40 ticks).

### Mortar Turret — `block/MortarTurretBlock.java`

Troisième membre de "Turret" : confirmé avec le joueur — contrairement à Bowling Ball Turret,
on veut ici de vrais **dégâts de zone façon explosion** à l'impact (sans dégât de terrain,
décidé explicitement), pas une perforation en ligne.

**Version initiale (2026-08-29) : réutilisait le tir cosmétique de la base (`spawnArrow`,
la même flèche vanilla à gravité que Harpoon Turret).** Signalé au premier test en jeu : la
flèche partait vers le ciel et ne semblait jamais redescendre de façon satisfaisante pour un
"impact" — les dégâts, eux, ont toujours été appliqués instantanément à l'envoi du tir (jamais
liés à l'arrivée réelle d'une flèche), donc la flèche volante n'apportait ni information ni
retour visuel cohérent avec ce qui se passait réellement. **Remplacée par une particule
d'explosion au point d'impact**, jouée au même instant que les dégâts :

```java
@Override
protected void fireAt(ServerLevel level, BlockPos pos, Monster target, Direction facing) {
    Vec3 impact = target.position();
    level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, 4, 0.3D, 0.3D, 0.3D, 0.0D);

    AABB splashArea = new AABB(target.blockPosition()).inflate(SPLASH_RADIUS);
    for (Monster monster : level.getEntitiesOfClass(Monster.class, splashArea)) {
        monster.hurt(level.damageSources().generic(), getDamage());
    }
}
```

**Taille de l'effet, ajustée une fois en jeu (2026-08-29)** : la toute première version
utilisait `ParticleTypes.EXPLOSION_EMITTER` (le grand "poof" dramatique qu'une explosion TNT/
creeper joue une fois en son centre) — jugé trop imposant pour un impact de mortier qui se
répète toutes les 3 secondes. Remplacé par `ParticleTypes.EXPLOSION` (la petite particule
dispersée en nombre dans le rayon d'une vraie explosion vanilla), 4 exemplaires légèrement
éparpillés (`0.3` bloc de rayon) plutôt qu'un seul point — un effet plus modeste, mais toujours
lisible comme une explosion. **Purement visuelle** dans les deux cas : `ServerLevel#sendParticles`
ne fait que diffuser l'information de rendu aux joueurs proches, aucun rapport avec une vraie
`Explosion` vanilla — aucun risque de dégât de terrain, cohérent avec la demande explicite du
joueur. **Pas encore fait** : un vrai visuel de trajectoire avant l'impact (obus qui vole puis
explose) — le joueur a explicitement reporté ce point à plus tard, l'effet actuel est instantané
à l'envoi du tir, comme les dégâts.

Même principe de scan par `AABB` que `AbstractBlockadeBlockEntity#serverTick` pour les dégâts,
mais appliqué **une fois** au point d'impact plutôt qu'en continu autour du bloc. Tour la plus
chère et la plus lente du roster (20 PV, 70 mana, 8 dégâts par ennemi touché, rayon 2 blocs,
cadence 60 ticks) : compense la puissance des dégâts de zone.

### Le refactor commun — `block/entity/AbstractTurretBlockEntity.java`

`fireAt`, `spawnArrow` et le nouveau `muzzlePosition` sont passés de `private` à `protected`
pour permettre à Bowling Ball Turret et Mortar Turret de redéfinir `fireAt` — même principe que
l'extraction d'`AbstractTowerBlockEntity` en son temps (voir plus haut) : généralisé seulement
une fois un vrai deuxième besoin concret constaté (ici, deux tours qui ont chacune besoin d'un
comportement de tir différent de celui de Harpoon), pas une généralisation devinée à l'avance.

**Deux bugs trouvés en touchant cette classe** (donc déjà présents sur Harpoon Turret, pas
seulement les deux nouvelles tours à distance) :

- `lastFireTick` était initialisé à `Long.MIN_VALUE`. `now - lastFireTick` déborde alors vers un
  nombre toujours négatif (overflow de `long`), donc `now - lastFireTick < attackIntervalTicks`
  était **toujours vrai** — le tir ne se déclenchait jamais, pour aucune tourelle. Remplacé par
  `-attackIntervalTicks`, qui permet un premier tir immédiat sans provoquer le même débordement.
- L'origine du tir n'était pas décalée hors du cube plein du bloc — sans effet visible pour
  Harpoon Turret (flèche purement cosmétique, les dégâts sont appliqués directement, la flèche
  peut bien se figer sans que ça ne change rien au gameplay), mais **bloquant** pour
  `BowlingBallEntity`, qui a besoin d'une vraie collision : une flèche qui apparaît dans la
  géométrie de collision du bloc sous elle se fige au premier tick
  (`AbstractArrow#tick`). `muzzlePosition` centralise maintenant ce décalage de 0,6 bloc, la
  même valeur déjà utilisée par `spawnArrow`.

### Assets

Les quatre nouveaux blocs réutilisent des textures vanilla thématiquement proches, en attendant
de vraies textures dédiées (même principe que Spike Blockade/dripstone, Harpoon Turret/furnace) :
Bouncer Blockade → `slime_block` (rebondissant), Slice N Dice Blockade → `iron_block`
(métallique, lames), Bowling Ball Turret → `dispenser`/`furnace` (éjecte quelque chose),
Mortar Turret → `blast_furnace` (arme lourde). Modèle directionnel (`orientable`) pour les deux
tours, `cube_all` pour les deux blockades — mêmes conventions que Harpoon Turret/Spike Blockade
respectivement.

## La roue de sélection des tours et la pose — `client/gui/screen/TowerWheelScreen.java`

**Unique façon de poser une tour** (voir "L'item ne pose plus rien" plus haut) : une **roue
radiale**, pensée pour le futur système de héros (chaque héros n'aura accès qu'à ses propres
tours — pas encore implémenté, donc la roue liste aujourd'hui **toutes** les tours existantes :
Spike Blockade et Harpoon Turret). Toute la logique tourne côté client jusqu'à l'ultime
confirmation ; seul le paquet final touche le serveur.

### Le catalogue — `init/TowerDefinition.java`

Enum commune client/serveur, un membre par tour posable via la roue (id, nom traduit, `Block`
+ `Item` d'icône, portée, `coneAngleDegrees`). **Ne duplique pas le coût en mana** :
`manaCost()` réexpose directement la constante du block entity concerné
(`SpikeBlockadeBlockEntity.MANA_COST`, `HarpoonTurretBlockEntity.MANA_COST`), seule source
d'autorité (déjà lue par `ModEvents.onTowerPlace`). `SPIKE_BLOCKADE` a `range=0.0` (pas de
portée, `coneAngleDegrees` sans effet) ; `HARPOON_TURRET` a `range=12.0`,
`coneAngleDegrees=45.0` — premier membre du catalogue à exercer réellement l'aperçu de portée.

### Les touches — `client/ModKeyMappings.java`

Deux `KeyMapping`, sous une catégorie dédiée `ModKeyMappings.CATEGORY` (pas la catégorie
vanilla `GAMEPLAY`) — enregistrée via `RegisterKeyMappingsEvent#registerCategory`, avec sa
propre clé de lang `key.category.dungeon_defenders.keys` ("Dungeon Defenders") : regroupées
sous leur propre en-tête dans Options > Contrôles > Touches, plus simples à retrouver que
noyées parmi les touches vanilla. `tower_wheel` (ouvre la roue, `R` par défaut) et
`rotate_tower` (fait pivoter l'hologramme pendant l'étape orientation, `G` par défaut — pas
`T`, déjà pris par le chat vanilla).

### La roue — `TowerWheelScreen`

Un secteur par `TowerDefinition`, disposé en cercle (icône = l'item existant de la tour, pas de
nouvel art) autour du centre de l'écran. Le secteur survolé est déterminé par l'angle entre le
centre de l'écran et la souris (`Math.atan2`), avec une zone morte de 20px au centre pour
éviter qu'un simple tremblement de souris sélectionne un secteur au hasard.

Deux façons de confirmer : **cliquer directement** sur un secteur (`mouseClicked`), ou
**maintenir la touche `tower_wheel` en visant**, puis **la relâcher** (`keyReleased`, via
`KeyMapping#matches(KeyEvent)`) — les deux passent par le même calcul de secteur survolé.
`Échap` (comportement par défaut de `Screen`) ferme la roue sans rien sélectionner.

Ouverte directement côté client par `TowerPlacementClientEvents` (sur `ClientTickEvent.Post`,
si la touche est consommée et qu'aucun écran n'est déjà ouvert) — comme `MapSelectionScreen`,
pas de `Menu` ni d'aller-retour serveur nécessaire pour l'ouvrir.

### Le mode pose, une seule étape — `client/TowerPlacementState.java`, `client/TowerPlacementClientEvents.java`

Sélectionner une tour dans la roue démarre `TowerPlacementState` (état transitoire, pas
persistant). Position et rotation évoluent **en parallèle**, dans n'importe quel ordre, tant
que le mode pose reste actif :

- Chaque tick, un rayon est lancé depuis les yeux du joueur (`level.clip(new
  ClipContext(...))`, portée 20 blocs, `ClipContext.Block.OUTLINE`) ; la position juste après
  le bloc touché (`hit.getBlockPos().relative(hit.getDirection())`) devient la cible, valide si
  `canBeReplaced()`.
- La touche `rotate_tower` fait pivoter la rotation courante (`Direction`, pas de 90°,
  `getClockWise()`) à tout moment, y compris en visant encore une position.
- **Clic droit** sur une cible valide pose la tour immédiatement, avec la position et la
  rotation courantes (`PlaceTowerPayload`, tour + position + rotation), et quitte le mode pose
  — ignoré sur une cible invalide. **Clic gauche** annule tout le mode pose à tout moment.

Une seule confirmation, pas deux : jusqu'au 2026-08-2x, le mode pose comptait une étape
AIMING (position mobile) suivie d'une étape ORIENTING (position figée, seule la rotation
changeait, un second clic droit pour confirmer) — simplifié en une seule étape à la demande du
joueur, la rotation n'ayant pas besoin d'une étape dédiée pour un cycle à 4 valeurs.

L'interception des clics passe par `InputEvent.InteractionKeyMappingTriggered`
(`isAttack()`/`isUseItem()`, annulés le temps du mode pose pour ne pas déclencher l'action
vanilla en dessous) — un garde sur `InteractionHand.MAIN_HAND` évite de traiter deux fois le
clic droit (l'event se déclenche une fois par main pour "Use Item").

**Spike Blockade** est un cube symétrique, sans propriété d'orientation dans son `BlockState` —
tourner son hologramme n'a donc aucun effet visuel ni gameplay sur cette tour précise. C'est le
**Harpoon Turret** qui exerce enfin la rotation pour de vrai (`HORIZONTAL_FACING` + cône de tir
orienté, voir plus haut).

### L'hologramme et le cercle de portée — rendu

Rendu via le pipeline "submit node" de cette version (pas le rendu immédiat classique) :
`ExtractLevelRenderStateEvent` copie l'état courant de `TowerPlacementState` dans un
`TowerPlacementRenderState`, posé sur le `LevelRenderState` global via un `ContextKey` (pas de
block entity à qui l'accrocher, contrairement aux autres renderers du mod) ;
`SubmitCustomGeometryEvent` le relit et soumet la géométrie :

- **Contour filaire du bloc** (`Shapes.block()`, parcouru arête par arête comme le fait
  `ShapeRenderer.renderShape` vanilla en interne — réimplémenté ici car cette méthode attend un
  `PoseStack` complet, alors que `submitCustomGeometry` ne fournit qu'un `PoseStack.Pose`
  différé) : **vert si la position visée est valide, rouge sinon**, tourné selon `state.rotation`
  en permanence (voir `facingYRot` plus bas).
- **Zone de portée** (`renderRangeArea`, `RenderTypes.lines()`), uniquement si
  `TowerDefinition.range() > 0` — **cercle complet** si `coneAngleDegrees >= 360`
  (omnidirectionnel), sinon un **secteur/cône** : l'arc borné à `[-coneAngleDegrees/2,
  +coneAngleDegrees/2]` **plus** deux segments droits vers l'origine, pour lire visuellement un
  cône et pas un arc flottant dans le vide. La rotation de la zone suit `state.rotation` en
  permanence elle aussi, dès l'orientation par défaut (`NORTH`).

  Convention du gabarit local : angle `-90°` (dans le repère `cos`/`sin` déjà utilisé par
  l'ancien cercle complet) correspond à la direction `NORTH`, c'est-à-dire au point local
  `(0,0,-radius)` — cohérent avec le vecteur normal vanilla de `Direction.NORTH` ((0,0,-1)).

  La rotation appliquée vient de `facingYRot(Direction)`, une table dédiée avec
  **`NORTH=0°, EAST=270°, SOUTH=180°, WEST=90°`** — ni `Direction.toYRot()` (convention encore
  différente, pensée pour le yaw des entités), ni les valeurs `y` du blockstate posé prises
  telles quelles (`facing=east → y:90` dans `harpoon_turret.json`, correctes pour le système de
  blockstate de Minecraft mais pas pour la rotation appliquée ici). Deux bugs successifs
  corrigés après des tests en jeu : d'abord `toYRot()` (la tour tirait à l'opposé exact du cône
  affiché), puis les valeurs du blockstate telles quelles (EST/OUEST inversés spécifiquement,
  NORD/SUD corrects par symétrie — vérifié par calcul : `Axis.YP.rotationDegrees(...)` applique
  la rotation main-droite standard de JOML, qui avec les axes de Minecraft (+X est, +Z sud) va
  dans le sens inverse de la convention "horaire vu du dessus" du blockstate). Voir "Ce qui
  n'est PAS fait" ci-dessus et `doc/05-etat-et-problemes-connus.md`. Jamais rien à dessiner
  pour Spike Blockade (`range = 0.0`) ; le Harpoon Turret (`range=12.0`,
  `coneAngleDegrees=45.0`) est le premier à
  l'exercer réellement.

### Le paquet final — `network/PlaceTowerPayload.java`, `ModNetworking.handlePlaceTower`

`PlaceTowerPayload(towerOrdinal, pos, directionOrdinal)` — deux ordinaux envoyés par le client,
**jamais indexés sans validation de bornes** côté serveur (même garde-fou que
`SetDifficultyPayload`/`SpawnerConfigPayload`), plus une vérification de distance (même
`MAX_DISTANCE_SQ` que la config du spawner) et de remplaçabilité de la position (le serveur
reste la seule source de vérité, même si le client n'aurait normalement jamais dû laisser
confirmer une position invalide).

Le point important : **la vérification et le débit de mana ne sont PAS réimplémentés ici**.
Le handler pose le bloc (`level.setBlock(...)`, avec la rotation appliquée seulement si le
bloc a `BlockStateProperties.HORIZONTAL_FACING` — vrai pour le Harpoon Turret, sans effet pour
Spike Blockade), puis appelle directement `EventHooks.onBlockPlace(player, snapshot,
direction)` — **le même hook NeoForge qu'utilise en interne la pose par `BlockItem` classique**
pour déclencher `BlockEvent.EntityPlaceEvent` (désormais théorique pour `TowerBlockItem`, dont
`useOn` ne place plus rien lui-même — voir "L'item ne pose plus rien" plus haut — mais le hook
reste le même point d'entrée). Résultat : `ModEvents.onTowerPlace` s'exécute pour la pose via
la roue exactement comme il le ferait pour n'importe quel autre déclencheur de
`BlockEvent.EntityPlaceEvent`, sans aucune duplication — y compris la restriction de phase
(Construction uniquement), pour Blockade **et** Turret puisque le filtre du handler porte sur
`AbstractTowerBlockEntity` (voir "Le coût en mana et la restriction de phase" plus haut). Si
annulé (mana insuffisant ou mauvaise phase), le `BlockSnapshot` capturé avant la pose est
restauré (`before.restore(...)`) — le bloc disparaît comme s'il n'avait jamais été posé.

**Ce qui n'est PAS fait**, volontairement :

- Pas de filtrage par héros — la roue liste toutes les tours, en attendant ce système.
- Pas de remplissage translucide de l'hologramme, juste le contour filaire.

## La suppression de tour — `client/TowerRemovalState.java`, `client/TowerRemovalClientEvents.java`, `network/RemoveTowerPayload.java`

Décidé avec le joueur (2026-08-26), sur le modèle du jeu de référence plutôt que sur le minage
vanilla à la pioche : une **touche dédiée** (`remove_tower_mode`, `X` par défaut,
`client/ModKeyMappings.java`) fait entrer/sortir du **mode suppression** ; pendant ce mode, un
**clic gauche** sur une tour visée la détruit instantanément et rembourse une partie de son coût
en mana. Symétrique à la roue de pose (une seule vraie façon de poser, une seule vraie façon de
retirer), mais l'ancien chemin pioche existe toujours en parallèle — voir "Ce qui n'est PAS
fait" plus bas.

### L'état et la bascule — `TowerRemovalState`

État transitoire client (pas persistant, pas synchronisé), même esprit que
`TowerPlacementState` mais plus simple : juste `active`/`targetPos`/`targetValid`, pas
d'étapes. **Se désactive après chaque suppression** (retour du joueur, 2026-08-26, changé
depuis le comportement d'origine qui restait actif pour enchaîner plusieurs suppressions comme
dans le jeu de référence) — en usage réel, le joueur en retire généralement une seule à la
fois, rester en mode suppression après coup gênait plus qu'il n'aidait. Se désactive aussi sur
un nouvel appui sur la touche (bascule manuelle), ou automatiquement si la phase quitte
Construction, si l'écran ou le niveau/joueur deviennent indisponibles.

`TowerRemovalClientEvents.onClientTick` refuse de basculer le mode si la roue de pose
(`TowerPlacementState.isActive()`) est déjà active ou qu'un écran est ouvert. **Symétriquement**
(bug signalé en jeu et corrigé le 2026-08-26), `TowerPlacementClientEvents.onClientTick` refuse
maintenant d'ouvrir la roue si le mode suppression (`TowerRemovalState.isActive()`) est déjà
actif — l'ancienne version ne vérifiait ce garde-fou que dans un seul sens : rester en mode
suppression pendant tout un mode pose laissait un clic gauche déclencher les deux handlers à la
fois (annulation de la pose **et** suppression de la tour visée).

### Le ciblage — raycast `OUTLINE`, comme la pose

Même mécanisme que `TowerPlacementClientEvents.updateTargetFromRaycast` : rayon de 20 blocs
depuis les yeux du joueur, `ClipContext.Block.OUTLINE`. Une position est une cible valide si
`level.getBlockEntity(pos) instanceof AbstractTowerBlockEntity` — générique à toute catégorie de
tour (Blockade et Turret), aucun cas particulier par type. Rendu d'un contour filaire orange
(`renderBoxOutline`, même technique que le contour vert/rouge de la pose, submit-node pipeline)
autour de la tour visée quand elle est valide — orange choisi délibérément pour ne pas laisser
croire aux deux modes une sémantique commune avec le vert/rouge de validité de pose.

### Le clic et le paquet — `RemoveTowerPayload`, `ModNetworking.handleRemoveTower`

`InputEvent.InteractionKeyMappingTriggered` : pendant le mode, **tout clic gauche est annulé**
(`event.isAttack()`), pour ne jamais casser un bloc ou frapper un monstre par accident en visant
une tour — que la cible soit valide ou non. Si elle est valide, `RemoveTowerPayload(pos)` part
vers le serveur.

Le serveur revalide tout, comme n'importe quel autre paquet du mod : phase Construction
uniquement (même message `tower.build_phase_only` que la pose, symétrique), distance
(`MAX_DISTANCE_SQ`, même constante que la config du spawner), et présence réelle d'une
`AbstractTowerBlockEntity` à la position reçue — le client n'est jamais l'autorité, même s'il a
déjà refusé une cible invalide de son côté.

Remboursement : `Math.round(tower.getManaCost() * TOWER_MANA_REFUND_RATIO)`, avec
`TOWER_MANA_REFUND_RATIO = 0.5F` — valeur de test, pas encore équilibrée, comme les coûts de
pose eux-mêmes (`TowerDefinition`). Plafonné à `MAX_MANA` comme tout gain de mana. La
destruction passe par `serverLevel.destroyBlock(pos, false)` — **pas de drop d'item**, même
convention qu'une tour détruite au combat (`AbstractTowerBlockEntity#setHealth`, `dropBlock =
false`) : la touche dédiée est voulue comme l'unique façon "propre" de retirer une tour.

### Ce qui n'est PAS fait, volontairement

- Pas de confirmation ("es-tu sûr ?") avant suppression — un clic suffit, comme la pose.
- Pas de retour visuel pendant que le mode est actif hors du contour orange sur la cible.

**Le minage à la pioche est réglé par ailleurs** : voir "Casser un bloc est désormais
désactivé" juste en dessous — la touche dédiée n'est plus seulement l'unique façon "propre" de
retirer une tour, c'est désormais **la seule façon tout court**, plus aucun bloc ne se casse à
la pioche en survie, tour ou pas.

## Casser un bloc est désormais désactivé — `ModEvents.onBlockBreakAttempt`

Décidé avec le joueur (2026-08-26), directement en réaction au clutter d'item inerte laissé par
le minage des tours (voir plus haut) : plutôt que de traiter chaque bloc au cas par cas
(rendre les tours incassables, puis le spawner, puis le Cristal d'Eternia...), un seul handler
générique **annule toute tentative de casse de bloc pour un joueur non créatif**, quel que soit
le bloc visé — terrain, structure de la taverne, tours, tout. Ce n'est pas ce genre de jeu : pas
de minage, pas de récolte de ressources.

`BreakBlockEvent` (`net.neoforged.neoforge.event.level.block.BreakBlockEvent` — **pas**
`BlockEvent.BreakEvent`, qui n'existe plus dans cette version de NeoForge, renommé/déplacé) se
déclenche **indépendamment côté client ET côté serveur** (précisé dans sa javadoc). Le handler
annule sans condition de camp — pour stopper net à la fois la prédiction client (le bloc
n'affiche jamais de cassure qui se corrige ensuite) et la casse réelle côté serveur — mais
n'envoie le message système ("Impossible de casser des blocs dans ce monde.") que côté serveur
(`!event.getLevel().isClientSide()`), pour ne pas l'afficher deux fois (une fois localement,
une fois via le paquet réseau).

Réservé aux joueurs non créatifs (`player.isCreative()`), même principe que partout ailleurs
dans le mod : le créatif reste le seul mode où une map se construit/modifie.

**Effet de bord voulu** : comme le handler est générique à tout bloc, il rend aussi le minage
des tours à la pioche impossible sans rien coder de spécifique aux tours — le chemin
"suppression avec remboursement" (voir plus haut) devient de fait la seule façon de retirer une
tour, sans avoir eu besoin de toucher à `strength()`/`getDestroyProgress()` par bloc.
- Aucun retour visuel pendant que le mode est actif hors du contour orange sur la cible (pas de
  changement de curseur, pas d'indicateur HUD permanent) — seul le message système à
  l'activation/désactivation l'indique.

## Le contour de sélection masqué sur les tours et les cristaux — `client/BlockOutlineClientEvents.java`

Demandé en jeu (2026-08-30), pour l'immersion : le contour noir filaire que Minecraft dessine
autour du bloc visé n'apparaît plus sur les tours ni sur les cristaux. Ces blocs ont des modèles
custom rendus par un `BlockEntityRenderer`, et la boîte vanilla — alignée sur `getShape`, donc
**1,5 bloc de haut** pour une tour (hitbox anti-escalade) et **3 blocs** pour le Cristal
d'Eternia — flottait visiblement autour du modèle au lieu de l'épouser.

### Comment — annuler l'extraction du render state, pas toucher aux formes

`ExtractBlockOutlineRenderStateEvent` (`net.neoforged.neoforge.client.event`) est annulable ;
sa javadoc est explicite : annulé, aucun render state de contour n'est soumis, donc rien n'est
dessiné. **`RenderHighlightEvent` des versions précédentes n'existe plus** dans cette version de
NeoForge — c'est cet événement-là (plus `CustomBlockOutlineRenderer` pour un rendu custom, non
utilisé ici) qui l'a remplacé.

```java
@SubscribeEvent
static void onExtractBlockOutline(ExtractBlockOutlineRenderStateEvent event) {
    BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getBlockPos());
    if (hidesOutline(event.getBlockState(), blockEntity)) {
        event.setCanceled(true);
    }
}
```

**Ceci ne touche que le rendu, jamais le ciblage** — c'est la différence essentielle avec
l'approche `getShape` → `Shapes.empty()` utilisée par `SpawnerBlock` (voir plus haut) : là-bas
le but était justement de rendre le bloc introuvable/incliquable en survie, ce qui ici casserait
toute interaction avec les tours et les cristaux. `getShape` reste inchangé, donc viser, le clic
droit (vote prêt sur le Cristal d'Eternia, choix de map sur celui de la taverne), le mode
suppression de tour et la casse en créatif fonctionnent exactement comme avant : le bloc reste
parfaitement cliquable, il n'est simplement plus souligné.

### Quels blocs

Reconnus par leur **block entity** : `AbstractTowerBlockEntity` (couvre Blockade et Turret, donc
automatiquement toute nouvelle tour ajoutée plus tard, même logique générique que le ciblage du
mode suppression) et `EterniaCrystalBlockEntity`. Le cristal de la taverne n'a pas de block
entity (aucun état à stocker) : reconnu à sa classe de bloc, `TavernCrystalBlock`.

Le spawner et le coffre de mana ne sont **pas** concernés (hors demande) — et le spawner ne
dessine de toute façon déjà aucun contour en survie, puisque son `getShape` y est vide.

### L'option de config

`ClientDisplayConfig.SHOW_TOWER_BLOCK_OUTLINE` (`showTowerBlockOutline`), **par défaut `false`**
— contrairement aux autres options d'affichage, activées par défaut : c'est bien l'absence de
contour qui est le comportement voulu, l'option n'existe que pour le remettre. Config de type
CLIENT, donc purement locale : un joueur qui la réactive ne change rien pour les autres.

### Ce qui reste pour repérer une tour

Le contour n'était pas le seul repère visuel : le mode suppression dessine son propre contour
**orange** (`TowerRemovalClientEvents`, inchangé et désormais mieux lisible sans la boîte noire
par-dessus) et les tours affichent leur barre de vie (`TowerHealthBarRenderer`).

## Abandonner un niveau — `client/PauseMenuClientEvents.java`, `network/LeaveMapPayload.java`

Un bouton rouge **« Abandonner le niveau »** ajouté au bas du menu pause vanilla, qui ramène à
la taverne après confirmation.

### Pourquoi le menu pause plutôt qu'un bloc de sortie

Les deux options ont été pesées avec le joueur (2026-09-02). Le bloc à poser dans chaque map
perdait sur trois points : il faut y penser à la construction (une map où le mappeur l'oublie
n'a pas de sortie), il faut aller le chercher physiquement, et ce n'est pas là qu'un joueur
cherche à quitter. Le menu pause marche sur **toutes** les maps sans que le mappeur ait quoi que
ce soit à placer.

La commande `/dd_leave` reste en parallèle : c'est un harnais de test, utilisable à tout moment
y compris pour déboguer.

### Comment le bouton est ajouté

`ScreenEvent.Init.Post` (NeoForge) permet d'ajouter des widgets à un écran vanilla déjà
initialisé, via `addListener`. Le handler filtre sur `PauseScreen`.

**Le bouton n'apparaît que pendant une partie** (`GamePhase#isInGame()`) : proposer d'abandonner
depuis la taverne n'aurait aucun sens.

**Sa position est calculée depuis les widgets déjà présents**, pas depuis la mise en page
vanilla : le bouton se place sous le widget le plus bas de l'écran (`getY() + getHeight()`
maximal), avec un repli et un plafond pour ne jamais déborder. La mise en page du menu pause
change d'une version de Minecraft à l'autre, et d'autres mods peuvent aussi y avoir ajouté des
boutons — se caler sur des coordonnées en dur aurait fini par superposer deux boutons.

Le rouge est celui du **texte** (`ChatFormatting.RED`), pas du fond : un `Button` vanilla ne
sait pas colorer son fond sans widget custom, et ça ne valait pas la peine ici.

### La confirmation

Demandée par le joueur, et justifiée : le menu pause s'ouvre par réflexe, et un clic à côté
ferait perdre la partie en cours sans retour possible. `ConfirmScreen` (vanilla) affiche le
titre et un message qui précise le vrai effet — **tous les joueurs** sont ramenés, pas seulement
celui qui a cliqué. « Non » revient au menu pause (`setScreen` sur la même instance, dont
`init()` est rejoué), pas au jeu.

### Le paquet

`LeaveMapPayload`, C2S sans champ — un simple signal, comme `StartGamePayload` à l'aller. Le
serveur revérifie la phase avant d'agir (le client masque déjà le bouton hors partie, mais il
n'est jamais l'autorité) puis appelle `MapInstance.returnToTavern`, qui nettoie la zone de map,
repasse en phase Taverne et téléporte tout le monde.

## Les zones interdites à la pose — `block/NoBuildZoneBlock.java`

Premier outil de *mappeur* du mod au sens strict (les autres marqueurs servent au fonctionnement
de la map ; celui-ci sert à la régler). Posé par le créateur d'une map pour délimiter où les
joueurs **ne peuvent pas** construire.

### Liste noire, pas liste blanche

Décidé avec le joueur (2026-08-31), à l'inverse du jeu de référence qui définit des zones
*autorisées*. Raison donnée : c'est plus ouvert à la créativité du joueur, et surtout **un oubli
du mappeur autorise une pose en trop plutôt que de rendre un endroit injouable**. Le pire cas
d'une liste noire est bénin ; celui d'une liste blanche ne l'est pas.

Un marqueur = une position interdite. On "peint" donc la zone, typiquement au `/fill` en
créatif ; le format structure les sauvegarde comme n'importe quel bloc.

### Comment il bloque réellement la pose

**Il occupe la case, et ça suffit.** Un bloc normal n'est pas `canBeReplaced()`, et c'est
exactement ce que testent le ciblage côté client (`TowerPlacementClientEvents`) et l'autorité
serveur (`ModNetworking#handlePlaceTower`) pour décider si une position est libre. Aucune
logique de zone, aucun volume à calculer, aucun registre à tenir.

Les deux endroits ajoutent quand même un **test explicite du marqueur**, pour une seule raison :
pouvoir *expliquer* le refus. Sans lui, le joueur voit un hologramme rouge sans cause visible —
le bloc est invisible. Le message `dungeon_defenders.tower.no_build_zone` est envoyé côté client
au moment où il tente de confirmer une pose refusée, et côté serveur s'il refuse de son côté
(le paquet n'arrive normalement jamais, le client filtrant déjà — c'est de la défense en
profondeur, comme partout ailleurs dans le mod).

### Le bloc

Invisible, traversable, ciblable en créatif seulement — même traitement que `SpawnerBlock`,
`PlayerSpawnBlock` et `TrainingDummyBlock` (voir "Le spawner n'est plus jamais un obstacle
physique" pour le raisonnement détaillé, identique ici). Traversable est important : une zone
simplement interdite à la construction ne doit gêner ni un monstre ni un joueur.

### Ce qui n'est PAS fait, volontairement

- **Pas de marqueur de volume** (deux coins définissant une boîte) : un bloc par case, quitte à
  en poser beaucoup. Le `/fill` rend ça indolore, et ça évite toute une gestion de volumes qui
  se chevauchent, de coins orphelins et de sauvegarde de leur appairage.
- **Aucun retour visuel de la zone** en dehors du contour de visée en créatif : pas de
  surbrillance des cases interdites pendant le mode pose. À reconsidérer si ça se révèle
  pénible à l'usage.

## La phase Taverne — `GamePhase.TAVERN`

Décidée avec le joueur (2026-09-01). Le problème de départ : `GAME_PHASE` valait `BUILD` par
défaut et rien ne distinguait le hub d'une map — la roue des tours s'ouvrait donc dans la
taverne, la pose y était acceptée, et le HUD y affichait « Vague 1/5 ». Plutôt que d'y
**interdire** la construction, une troisième phase la rend **délibérée** : la taverne devient
une zone d'essai libre, ce qui va de pair avec le mannequin d'entraînement qui y vit (voir plus
bas).

### Pourquoi une phase globale suffit

`GAME_PHASE` est un attachment de `Level`, donc partagé par tout le monde — a priori mauvais
signe pour une phase censée décrire *où l'on est*. Ça marche quand même parce que
`MapInstance.startGame` et `returnToTavern` téléportent **tous** les joueurs ensemble : le mod
est bâti autour d'une seule session partagée (voir 05-etat-et-problemes-connus.md, "Système de
maps/structures"). « Tout le monde est à la taverne » est donc un vrai état du monde, pas une
approximation.

### Ce que la phase change

| Comportement | Taverne |
|---|---|
| Roue des tours, pose | **autorisée** — `GamePhase#allowsTowerBuilding()`, vrai en `BUILD` et `TAVERN` |
| Coût en mana de la pose | **gratuit** (`ModEvents.onTowerPlace` sort avant le débit) |
| Mode suppression de tour | **autorisé**, même garde — sinon impossible de nettoyer ses essais |
| Remboursement à la suppression | **aucun** (`ModNetworking.handleRemoveTower`) |
| Spawners | inactifs — déjà `COMBAT` uniquement, rien à changer |
| Vote « prêt » du Cristal d'Eternia | inactif — déjà `BUILD` uniquement |
| Comptage de vague, score, XP | inactifs |
| HUD Vague / Ennemis | masqués — `GamePhase#isInGame()` |

**Gratuité et absence de remboursement vont ensemble** : aucun monstre ne meurt dans le hub,
donc aucun mana n'y rentre — faire payer les essais les rendrait impossibles au bout de deux ou
trois tours. Et rembourser une pose gratuite reviendrait à imprimer du mana à volonté.

### Les transitions

| Quand | Appel |
|---|---|
| Chargement du monde | `TavernSpawn.onLevelLoad` → `PhaseTransitions.enterTavern` |
| Clic sur « Jouer » | `MapInstance.startGame` → `PhaseTransitions.startNewGame` (vague 1, `BUILD`) |
| `/dd_leave`, « Retour à la taverne » | `MapInstance.returnToTavern` → `PhaseTransitions.enterTavern` |
| Fin de vague, victoire, défaite | inchangé (`enterBuild`, `onVictory`, `onDefeat`) |

`startNewGame` est volontairement distincte d'`enterBuild` : cette dernière fait **avancer** la
vague (elle sert au retour en Construction entre deux vagues), donc démarrer une partie avec
elle commencerait à la vague 2. Cette remise à zéro était implicite tant que la phase par
défaut valait `BUILD` ; elle ne l'est plus.

**Les tours d'essai sont effacées au lancement d'une partie** (`TavernSpawn.clearTestTowers`,
appelée par `startGame`) : sans ça elles resteraient plantées dans la taverne jusqu'au prochain
chargement du monde. Balayage de la zone bloc par bloc faute de registre de tours (contrairement
aux spawners et aux coffres de mana, qui ont le leur) — coût ponctuel, payé une fois au clic sur
« Jouer ».

### Deux détails d'implémentation

- **`TAVERN` est ajoutée à la fin de l'enum**, pas au début. La phase est persistée par nom
  (donc les sauvegardes existantes s'en fichent), mais la valeur synchronisée vers le client est
  un `ordinal()` — insérer la nouvelle valeur avant les autres les aurait décalées.
- **La valeur par défaut de l'attachment est passée de `BUILD` à `TAVERN`** : c'est là que tout
  joueur apparaît, et `TavernSpawn` force de toute façon cette phase à chaque chargement du
  monde.

> Le harnais de test (maj + clic droit sur un spawner) bascule Construction ↔ Combat. Depuis la
> Taverne, il envoie en Construction — sans intérêt, mais sans dégât non plus.

## Le mannequin d'entraînement — `entity/TrainingDummyEntity.java`, `block/TrainingDummyBlock.java`

Demandé par le joueur (2026-08-31) pour la taverne : une **cible immobile et indestructible**,
attaquée par les tours, pour mesurer leurs dégâts sans avoir à monter une vraie vague.

### L'entité — pourquoi elle hérite de `Zombie`

Deux contraintes de l'existant, pas un choix esthétique :

1. **Les tourelles ne ciblent que des `Monster`** (`AbstractTurretBlockEntity#findTarget` fait un
   `getEntitiesOfClass(Monster.class, ...)`). Le mannequin doit donc en être un, sinon aucune
   tour ne lui tire dessus et il ne sert à rien.
2. Passer par `Zombie` permet de **réutiliser tel quel le `ZombieRenderer` vanilla** (typé sur
   `Zombie`, donc valide pour une sous-classe), sans créer ni modèle ni texture — exactement le
   même procédé que `ManaCrystalEntity extends ExperienceOrb` + `ExperienceOrbRenderer`.

> **Limite assumée** : le mannequin ressemble donc à un zombie planté là, pas à un mannequin de
> paille. Placeholder, comme les textures vanilla réutilisées ailleurs dans le mod.

Tout ce qui fait d'un zombie un zombie est ensuite neutralisé :

| Aspect | Comment |
|---|---|
| Ne bouge pas | `setNoAi(true)` — coupe `isEffectiveAi()`, qui conditionne à la fois le tick des goals et la simulation de déplacement. Il ne subit même pas la gravité, ce qui va bien avec un bloc support invisible et traversable |
| Aucun comportement | `registerGoals()` vide (ni les goals de déplacement/attaque du zombie, ni ses goals de ciblage) |
| Pas de goal du mod | `ModEvents.onMonsterSpawn` ignore explicitement cette classe — sans ça, tout `Monster` qui rejoint le monde reçoit le goal d'attaque du Cristal d'Eternia et le mannequin partirait en promenade |
| Ne brûle pas au soleil | `isSunSensitive()` → `false` (la taverne est à ciel ouvert) |
| Pas repoussé | `KNOCKBACK_RESISTANCE` à `1.0` — annule tout le calcul de `LivingEntity#knockback` (`power *= 1.0 - résistance`), donc le Bouncer Blockade peut le frapper sans le déplacer |
| Ni poussé ni poussant | `isPushable()` → `false`, `doPush(...)` vide |
| Ne disparaît jamais | `removeWhenFarAway(...)` → `false`, plus `setPersistenceRequired()` |
| Vie infinie | `MAX_HEALTH` à **1024**, remise au maximum dans `hurtServer` après le coup, et re-vérifiée à chaque `tick()` |

**Pourquoi 1024 PV et une remise à niveau, plutôt qu'une vraie invulnérabilité** : les dégâts
sont réellement appliqués, donc les événements de dégâts se déclenchent normalement — ce qui
laisse la porte ouverte à un futur compteur de DPS. La marge sert à ce qu'aucun coup unique ne
puisse atteindre 0 avant la remise à niveau ; la source la plus violente du mod en est très
loin.

**Pas de barre de vie** : `MobHealthBarRenderer` filtre sur `EntityType.ZOMBIE` et
`EntityType.SKELETON` exactement. Un type custom ne correspond pas, donc rien n'est dessiné —
c'est bien ce qui est voulu, mais c'est un effet de bord du filtre, pas une exclusion explicite :
si ce filtre est un jour élargi, il faudra penser à en exclure le mannequin.

### Le bloc — un "spawner de mannequin"

`TrainingDummyBlock` n'a aucun comportement propre : son block entity vérifie une fois par
seconde qu'un `TrainingDummyEntity` existe **juste au-dessus** de lui, et en invoque un sinon.
Invisible, traversable, ciblable en créatif seulement — même traitement que `SpawnerBlock` et
`PlayerSpawnBlock`, c'est un marqueur d'édition.

**Pourquoi un bloc plutôt qu'un mannequin posé directement dans le `.nbt` de la taverne**
(décidé avec le joueur) : le bloc fait partie de la structure et se repose donc proprement à
chaque chargement du monde, alors qu'une entité dépend du nettoyage d'entités de la zone
(`TavernSpawn#clearZone`) pour ne pas se dupliquer. Comme le bloc vérifie l'existence du
mannequin avant d'en invoquer un, deux exemplaires ne peuvent pas s'accumuler **même si ce
nettoyage ratait l'ancien**. Bonus : le mannequin se déplace en créatif en déplaçant son bloc,
sans retoucher au fichier de structure.

Retirer le bloc emporte son mannequin (`affectNeighborsAfterRemoval` →
`TrainingDummyBlockEntity#discardDummy`) : sans ça, casser le support en créatif laisserait une
entité orpheline que plus rien ne gère — le nettoyage de zone de la taverne ne passe qu'au
chargement du monde, et une map n'en a pas du tout.

`TrainingDummyBlockEntity` ne persiste **aucun état** — contrairement à `SpawnerBlockEntity`, il
n'y a rien à configurer ; il n'existe que pour avoir un tick serveur.

## Le mana du joueur

Ressource pensée pour alimenter de futurs sorts/capacités (aucun n'existe encore) **et** la
pose de tours (voir `ModEvents.onTowerPlace`, plus haut). Se dépense à la pose, remonte
uniquement en ramassant des cristaux de mana lâchés par les monstres — voir "Les cristaux de
mana" plus bas. **Pas de régénération passive dans le temps** (décidé avec le joueur).

### L'état — `init/ModAttachments.java`

Le mana courant est un **data attachment** NeoForge posé sur l'entité joueur, pas un champ
custom : c'est le mécanisme standard pour attacher un état persistant et synchronisable à une
entité vanilla sans la sous-classer.

```java
public static final int MAX_MANA = 100;

public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> MANA = ATTACHMENT_TYPES.register(
        "mana",
        () -> AttachmentType.builder(() -> MAX_MANA)
                .serialize(Codec.INT.fieldOf("Mana"))
                .sync(ByteBufCodecs.VAR_INT)
                .build());
```

- `serialize(...)` : le mana survit à une sauvegarde/rechargement (comme les PV du cristal).
- `sync(...)` : NeoForge pousse automatiquement la valeur au client propriétaire dès qu'un
  code serveur fait `player.setData(ModAttachments.MANA, valeur)` puis
  `player.syncData(ModAttachments.MANA)` — même logique de synchro explicite que
  `sendBlockUpdated` pour le cristal, mais côté entité.
- `MAX_MANA = 100` est la valeur par défaut : c'est aussi la valeur de retour du
  `defaultValueSupplier`, donc un joueur sans mana explicitement défini est considéré plein.

### L'affichage — `client/gui/ManaOverlay.java`

Couche HUD (`GuiLayer`) enregistrée dans `DungeonDefendersModClient` via
`RegisterGuiLayersEvent#registerAboveAll`. Volontairement minimal, et fait partie du groupe
bas-gauche décrit dans [Le groupe bas-gauche](#le-groupe-bas-gauche--mana-vie-expérience)
ci-dessous :

- un losange (`DiamondGauge`, voir plus bas), en bas à gauche de l'écran : fond gris foncé sur
  toute sa hauteur, recouvert par le bas d'un losange bleu dont la hauteur est proportionnelle
  à `currentMana / maxMana` — la jauge se remplit du bas vers le haut, comme avant, mais dans
  une forme de losange plutôt qu'un rectangle. C'est le losange de **droite** du groupe (vie à
  gauche, comme dans le jeu de référence) ;
- au-dessus du losange, le texte `Mana: X/Y` (clé `dungeon_defenders.hud.mana`), centré
  horizontalement (`guiGraphics.centeredText`) sur son centre.

Lit `player.getData(ModAttachments.MANA)` à chaque frame ; pas d'état côté overlay lui-même.
Voir [05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md) pour ce qu'il reste à
faire (texture).

### Les cristaux de mana — `entity/ManaCrystalEntity.java`, `init/ManaCrystalType.java`

Décidé avec le joueur, comme le vrai Dungeon Defenders : un monstre tué lâche un **cristal de
mana** au sol, ramassé en marchant dessus — jamais un item d'inventaire, un ramassage direct
comme l'expérience vanilla. Premier vrai `Entity` custom du mod (tout ce qui précède était des
`Block`/`BlockEntity`).

`ManaCrystalEntity extends net.minecraft.world.entity.ExperienceOrb` — pas une simple
inspiration, une vraie sous-classe : `ExperienceOrb` a déjà tout ce qu'il faut pour ce
comportement (flotte, gravité, se magnétise vers le joueur le plus proche, fusionne avec les
cristaux voisins, disparaît après un temps), et son point d'entrée de ramassage,
`playerTouch(Player)`, est `public` (pas `final`) — directement surchargeable. Seuls le
constructeur (position/vélocité initiale) et `playerTouch` sont réécrits ; tout le reste
(mouvement, fusion, despawn) vient gratuitement de la classe parente.

```java
@Override
public void playerTouch(Player player) {
    if (!(player instanceof ServerPlayer) || player.takeXpDelay != 0) {
        return;
    }
    player.takeXpDelay = 2;
    player.take(this, 1); // anime le ramassage (son + particule), purement visuel

    int newMana = Math.min(ModAttachments.MAX_MANA, player.getData(ModAttachments.MANA) + this.getValue());
    player.setData(ModAttachments.MANA, newMana);
    player.syncData(ModAttachments.MANA);
    this.discard();
}
```

**Point de sécurité important** : `ExperienceOrb` fusionne automatiquement les orbes proches de
même valeur (`scanForMerges`/`tryMergeToExisting`, `private`, non surchargeables) — sans
précaution, un cristal de mana pourrait fusionner avec une **vraie** orbe d'XP vanilla si elles
ont la même valeur numérique, corrompant le ramassage (l'orbe fusionnée ne se ramasse plus
qu'une fois, avec le comportement de celle qui "survit" à la fusion). Réglé à la racine par
`ModEvents.onExperienceDrop`, qui annule `LivingExperienceDropEvent` (NeoForge) pour tout
`Monster` : les monstres de ce mod n'ont de toute façon thématiquement aucune raison de donner
de la vraie XP Minecraft (le système `experience` custom du mod est déjà séparé et sans
rapport) — plus aucune vraie orbe d'XP ne peut donc exister dans une partie.

`init/ModEntities.java` (nouveau registre, `DeferredRegister.Entities` — API NeoForge dédiée
qui applique déjà la `ResourceKey` correctement, même esprit que `registerBlock` pour les
blocs) enregistre l'`EntityType`. Le rendu réutilise **tel quel** le renderer vanilla de l'orbe
d'XP (`ExperienceOrbRenderer`, pas `final`, paramétré sur `ExperienceOrb` donc valide pour une
sous-classe) — le cristal de mana a donc visuellement l'air d'une orbe d'XP verte/jaune,
**pas** de couleur "mana" dédiée pour l'instant (limite connue, voir
05-etat-et-problemes-connus.md).

`init/ManaCrystalType.java` (enum) porte la valeur du cristal — `SMALL(5)`, un seul membre pour
l'instant (valeur de test), prêt à en accueillir au moins 6 plus tard (le joueur prévoit des
paliers différents, comme le vrai jeu) sans logique de sélection pondérée tant qu'un second
palier concret n'existe pas.

`ModEvents.onMonsterDeath` (existant, voir "Le déroulement d'une vague" plus bas) fait tomber
un cristal à **chaque** mort de `Monster`, **quelle que soit la phase** — contrairement au
comptage de vague juste à côté, qui reste réservé au Combat.

### Le remboursement à la casse — `ModEvents.onTowerBreak`

Casser sa propre tour à la pioche rembourse **50%** du coût de pose en mana (valeur de test,
décidé avec le joueur — "remboursement partiel"). Écoute
`net.neoforged.neoforge.event.level.block.BreakBlockEvent` — **pas** `BlockEvent.BreakEvent`,
qui n'existe plus dans cette version de NeoForge (renommé/déplacé). Ce point est ce qui rend le
mécanisme sûr sans code supplémentaire : `BreakBlockEvent` ne se déclenche que pour une casse
**initiée par un joueur** (son constructeur exige un `Player` non nul), jamais pour
`Level#destroyBlock` déclenché par `AbstractTowerBlockEntity.setHealth()` à 0 PV en combat (qui
n'implique aucun joueur) — le remboursement ne s'applique donc **jamais** à une tour détruite
au combat, cohérent avec "détruite au combat ne se récupère pas" déjà en place pour les PV.

### La baguette de test — `item/ManaTestWandItem.java`

Item simple (pas de bloc) enregistré dans `ModBlocks` via `ITEMS.registerItem(...)` — même
`DeferredRegister.Items` que les `BlockItem`, il n'existe pas encore de classe `ModItems`
séparée. Sert uniquement à vérifier le HUD en jeu, sur le même principe que le harnais de
clic droit du Cristal d'Eternia :

- clic droit : retire 10 de mana (`MANA_COST`) au joueur, via
  `player.setData(ModAttachments.MANA, ...)` puis `player.syncData(...)` pour pousser la
  nouvelle valeur au client ;
- message système confirmant le nouveau mana, ou `dungeon_defenders.mana_test_wand.empty` si
  le mana est déjà à 0 ;
- toute la logique est côté serveur (`level.isClientSide()` en sortie anticipée) ; le client
  reçoit juste `InteractionResult.SUCCESS` pour l'animation de bras.

### La baguette de remplissage — `item/ManaFillWandItem.java`

Symétrique de `ManaTestWandItem` (même enregistrement dans `ModBlocks`, texture vanilla
`glowstone_dust` pour rester visuellement distincte) : clic droit remplit le mana au maximum
(`ModAttachments.MAX_MANA`) plutôt que d'en retirer — pour tester la pose de tours (Spike
Blockade 30, Harpoon Turret 50) sans avoir à farmer des cristaux de mana à chaque essai. Message
`dungeon_defenders.mana_fill_wand.used`, ou `.full` si déjà au maximum. Harnais de test, comme
`ManaTestWandItem` : pas destiné à survivre une fois un vrai système de sorts/capacités en
place.

Modèle provisoire : texture vanilla `minecraft:item/blaze_rod`, pas de modèle dédié.

## Le coffre de mana — `block/ManaChestBlock.java`

Premier item de la feuille "Idées" du plan Excel du joueur à être construit : un coffre qui
donne du mana "entre les vagues", et distribuera aussi des armes plus tard (hors scope tant
qu'il n'y a rien à distribuer, voir 05-etat-et-problemes-connus.md).

**Meuble de map, pas un objet de joueur** : même statut que le Cristal d'Eternia ou le Spawner
— posé par le créateur pendant la construction de la map, jamais par un joueur en jeu (pas
d'item qui le pose via une interaction ni la roue).

### Deux comportements selon le mode — `useWithoutItem`

`ManaChestBlock.useWithoutItem` distingue par `player.isCreative()`, exactement comme
`SpawnerBlock` distingue configuration et harnais de test par `isShiftKeyDown()` — même absence
volontaire d'un vrai système de rôle "créateur de map" vs "joueur" dans ce mod, le mode créatif
sert de proxy :

- **Créatif** → ouvre `ManaChestConfigMenuProvider`/`ManaChestConfigMenu`/
  `ManaChestConfigScreen`, un patron identique à `SpawnerConfigScreen` mais réduit à un seul
  champ scalaire (`manaAmount`), sans les lignes dynamiques de composition. Au clic sur
  "Valider", envoie `ManaChestConfigPayload` au serveur, appliqué par
  `ManaChestBlockEntity#applyConfig` après revérification de portée (`ModNetworking`).
- **Survie** → délègue directement à `ManaChestBlockEntity#tryOpen`. Décidé avec le joueur
  (2026-08-26) : plus de restriction de phase (auparavant `GAME_PHASE != BUILD` refusait
  l'ouverture avec un message) — un joueur peut vouloir aller chercher du mana en pleine
  Combat, le coffre s'ouvre désormais **quelle que soit la phase**.

### L'état et l'ouverture — `block/entity/ManaChestBlockEntity.java`

```java
public boolean tryOpen(Player player, int currentWave) {
    if (this.lastOpenedWave == currentWave) {
        return false;
    }
    this.lastOpenedWave = currentWave;
    ...
    int newMana = Math.min(ModAttachments.MAX_MANA, player.getData(ModAttachments.MANA) + this.manaAmount);
    player.setData(ModAttachments.MANA, newMana);
    player.syncData(ModAttachments.MANA);
    ...
    return true;
}
```

`lastOpenedWave` (0 = jamais ouvert) plutôt qu'un simple booléen "déjà ouvert" : comparé à
`ModAttachments.CURRENT_WAVE`, déjà incrémenté à chaque entrée en Construction par
`PhaseTransitions#enterBuild`. Le coffre "sait" donc tout seul s'il peut redonner du mana pour
la vague en cours, sans qu'aucun code n'ait besoin de remettre ce champ à zéro explicitement.

`manaAmount` (25 par défaut) est **configurable par coffre**, pas une constante globale du mod
— décidé avec le joueur : la bonne quantité dépend de la taille et de la difficulté de chaque
map, pas d'une seule valeur qui conviendrait à toutes.

### Disparition et réapparition visuelles — `ManaChestBlock#OPENED`, `#respawnAll`

Décidé avec le joueur (2026-08-24), comme dans le jeu de référence : un coffre ouvert ne reste
pas visible mais inerte, il **disparaît** jusqu'à la vague suivante. `OPENED` est une vraie
propriété de `BlockState` (`BooleanProperty`, comme `HORIZONTAL_FACING` sur le Harpoon Turret) :

- `getRenderShape` renvoie `RenderShape.INVISIBLE` si `OPENED`, sinon `MODEL`.
- `getShape`/`getCollisionShape` renvoient `Shapes.empty()` si `OPENED` — traversable, et
  surtout **impossible à cibler au clic droit** (le rayon d'interaction du joueur se base sur
  cette même forme) : un coffre déjà ouvert ne peut donc plus jamais être re-cliqué tant qu'il
  n'a pas réapparu, sans avoir besoin d'une vérification explicite côté interaction.

`ManaChestBlockEntity#tryOpen` bascule `OPENED` à `true` (`level.setBlock(pos,
state.setValue(OPENED, true), ...)`) juste après avoir donné le mana — le bloc entity lui-même
n'est ni recréé ni perturbé (même `Block` Java, seule une propriété change ; vanilla ne
recrée un block entity que si le `Block` sous-jacent change, pas une simple propriété).

Pour la **réapparition**, un registre `ModAttachments.ACTIVE_MANA_CHESTS`
(`Set<BlockPos>`, même principe qu'`ACTIVE_SPAWNERS`) est nécessaire : `ManaChestBlockEntity`
s'y ajoute/retire via `setLevel`/`setRemoved`, et `ManaChestBlock.respawnAll(level)` (appelé
par `PhaseTransitions#enterBuild`, à chaque nouvelle Construction) parcourt ce registre et
repasse `OPENED` à `false` pour tout coffre encore ouvert — contrairement à ce qui avait été
envisagé au départ (voir 05-etat-et-problemes-connus.md), un simple champ sur le block entity
ne suffisait pas : rien d'autre ne "réveille" un coffre qui n'est visité par personne, il faut
bien un point d'entrée explicite au changement de phase pour le repasser visible/solide.

> **Signalé en jeu (2026-08-26) : les coffres ne réapparaîtraient pas au retour en
> Construction.** Relu en détail sans trouver de bug : `respawnAll` est bien appelé par les
> deux points d'entrée en Construction (`enterBuild` — retour manuel via le harnais du
> spawner, et automatique via `ModEvents.onMonsterDeath` à la fin d'une vague —, et
> `resetGameState` — victoire/défaite), le registre `ACTIVE_MANA_CHESTS` suit exactement le
> même patron qu'`ACTIVE_SPAWNERS` (confirmé fonctionnel en jeu), et `level.setBlock(pos,
> state.setValue(OPENED, false), Block.UPDATE_ALL)` est le mécanisme vanilla standard pour
> faire réagir le rendu client à un changement de propriété de blockstate. Pas corrigé faute
> d'avoir trouvé la cause réelle — à retester avec un scénario précis (harnais de test vs
> vague automatique, un seul coffre vs plusieurs) pour resserrer le diagnostic.

### Apparence

Modèle provisoire : texture vanilla `minecraft:block/barrel_top` (thème conteneur/stockage,
volontairement distinct des autres placeholders du mod). Pas de PV, pas d'`AiAttackTarget` —
un monstre ne peut ni l'attaquer ni le cibler, comme le Cristal de la Taverne.

**Jamais testé en jeu** — voir [06-a-tester.md](06-a-tester.md).

## La vie du joueur

Contrairement au mana, la vie n'est pas une ressource inventée pour le mod : c'est l'attribut
vanilla `Attributes.MAX_HEALTH` / `LivingEntity.getHealth()`, déjà persistant et déjà
synchronisé par le jeu. Le mod se contente d'en changer le maximum par défaut.

### Le maximum à 100 — `ModEvents.onPlayerJoin`

```java
private static final double PLAYER_MAX_HEALTH = 100.0D;

@SubscribeEvent
public static void onPlayerJoin(EntityJoinLevelEvent event) {
    if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Player player)) {
        return;
    }

    AttributeInstance maxHealthAttribute = player.getAttribute(Attributes.MAX_HEALTH);
    if (maxHealthAttribute == null) {
        return;
    }

    boolean wasAtPreviousMax = player.getHealth() >= maxHealthAttribute.getValue();
    maxHealthAttribute.setBaseValue(PLAYER_MAX_HEALTH);
    if (wasAtPreviousMax) {
        player.setHealth((float) maxHealthAttribute.getValue());
    }
}
```

`EntityJoinLevelEvent` se redéclenche à chaque connexion, respawn et changement de dimension
(même remarque que pour `onMonsterSpawn`). `setBaseValue` est idempotent (poser deux fois la
même valeur ne change rien), donc pas besoin de garde anti-doublon ici. Le seul piège est de
ne pas soigner gratuitement un joueur déjà blessé à chaque relog : le code ne remonte la vie
au nouveau maximum que si le joueur était **déjà** à son ancien maximum (cas du tout premier
join, où le joueur vient de spawn à 20/20 avant que l'attribut ne soit modifié).

### L'affichage — `client/gui/HealthOverlay.java`

Même structure que `ManaOverlay` (losange + texte `Health: X/Y`, clé
`dungeon_defenders.hud.health`), en rouge, positionné le plus à **gauche** du groupe (centré sur
`HudLayout.MARGIN + DIAMOND_RADIUS`, comme dans le jeu de référence — vie à gauche, mana à
droite), même taille. Lit directement `player.getHealth()` / `player.getMaxHealth()` à chaque
frame — pas besoin d'attachment, ces valeurs sont déjà tenues à jour et synchronisées par le
moteur.

Les cœurs vanilla (`VanillaGuiLayers.PLAYER_HEALTH`) sont masqués dans
`DungeonDefendersModClient.onRegisterGuiLayers` via `event.replaceLayer(..., HIDDEN)` :
avec 100 PV ils s'étaleraient sur plusieurs rangées de cœurs (le rendu vanilla est pensé pour
20 PV, pas 100) et feraient de toute façon doublon avec `HealthOverlay`.

## L'expérience custom du joueur

**Rien à voir avec l'XP vanilla** (`EXPERIENCE_LEVEL`/`getExperienceLevel()`) : c'est une
ressource propre au mod. Gagnée en tuant des monstres (voir "Expérience, score et niveau —
`ModEvents.awardExperienceAndScore`" plus bas), elle fait monter `ModAttachments.LEVEL`.

### L'état — `init/ModAttachments.java`

Même mécanisme que le mana : un data attachment, mais qui démarre **vide** plutôt que plein,
puisqu'une expérience se gagne au lieu de se dépenser.

```java
public static final int MAX_EXPERIENCE = 100;

public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> EXPERIENCE = ATTACHMENT_TYPES.register(
        "experience",
        () -> AttachmentType.builder(() -> 0)
                .serialize(Codec.INT.fieldOf("Experience"))
                .sync(ByteBufCodecs.VAR_INT)
                .build());
```

`MAX_EXPERIENCE = 100` est un plafond **fixe par niveau** (pas de barème croissant du type
"niveau N demande N×100") : valeur de test, pas encore équilibrée, comme les coûts de pose des
tours.

### L'affichage — `client/gui/ExperienceOverlay.java`

Contrairement à `ManaOverlay`/`HealthOverlay`, reste une **barre horizontale** classique
(jauge + texte `Experience: X/Y` à sa droite, clé `dungeon_defenders.hud.experience`), en
vert, tout en bas de l'écran, sous les deux losanges.

## Le groupe bas-gauche — mana, vie, expérience

`ManaOverlay`, `HealthOverlay` et `ExperienceOverlay` forment un groupe positionné dans le
coin bas-gauche de l'écran :

```
   Vie         Mana
    ◆           ◆     <- losanges, remplissage bas → haut (pointe basse → pointe haute)
   ▓█▓         ▓█▓        vie à gauche, mana à droite (comme le jeu de référence)
  ▓███▓       ▓███▓
  ░░░░░       ▓▓▓▓▓
    ░           ▓
  [███░░░░░░░░░░░░░░] Experience: 0/100   <- barre horizontale, tout en bas
```

**Pourquoi des losanges et pas des rectangles ?** Le jeu qui a inspiré ce HUD (*Dungeon
Defenders* original) affiche le mana et la vie du joueur dans un widget en forme de
losange/triangle, pas des barres classiques. `DiamondGauge.render(...)` reproduit cette forme
sans texture : pour chaque bande horizontale d'1px de haut, la largeur croît puis décroît
linéairement (maximale au centre, nulle aux deux pointes) — un simple empilement de
`guiGraphics.fill(...)`, façon pixel art, en attendant de vraies textures (voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md)). `ManaOverlay` et
`HealthOverlay` appellent cette même méthode avec des couleurs différentes ; ils ne dessinent
plus rien eux-mêmes directement.

`client/gui/HudLayout.java` centralise les constantes de mise en page partagées par les trois
(marge, rayon/écart des losanges, écart avant la barre d'expérience) : sans ça, garder les
trois classes indépendantes alignées au pixel près demanderait de dupliquer les mêmes valeurs
magiques partout, avec le risque qu'elles divergent au premier ajustement.

Un quatrième overlay, `AbilitySlotsOverlay`, se greffe juste à droite de ce groupe (les 4
emplacements de compétences) — voir
[Les emplacements de compétences](#les-emplacements-de-compétences--clientguiabilityslotsoverlayjava)
plus bas.

`ExperienceOverlay` calcule sa position en premier (ancrée au bord bas de l'écran via
`guiGraphics.guiHeight()`) et expose `barTop(guiGraphics)`, une méthode package-visible que
`ManaOverlay`/`HealthOverlay` appellent pour savoir où s'arrête le bas de leurs colonnes
(`ExperienceOverlay.barTop(guiGraphics) - HudLayout.ROW_GAP`) — même principe de couplage
minimal que `WaveOverlay.waveText(level)` pour le groupe haut-droit.

## La vague en cours

Compteur affiché en haut à droite (`Vague X/Y`), pour un futur déroulement en vagues de
monstres. Aucune mécanique de déclenchement, de victoire ou de défaite n'existe encore : la
valeur ne bouge jamais toute seule.

### L'état — `init/ModAttachments.java`

Contrairement au mana/à la vie/à l'expérience, la vague **n'est pas un état du joueur** :
c'est un état de la partie, donc de la `Level`. `Level`/`ServerLevel` implémentent aussi
`IAttachmentHolder` (comme `Entity`), le même mécanisme de data attachment s'applique donc
directement à l'échelle du monde plutôt que par joueur.

```java
public static final int MAX_WAVE = 5;

public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> CURRENT_WAVE = ATTACHMENT_TYPES.register(
        "current_wave",
        () -> AttachmentType.builder(() -> 1)
                .serialize(Codec.INT.fieldOf("CurrentWave"))
                .sync(ByteBufCodecs.VAR_INT)
                .build());
```

Commence à `1` (pas `0`, une partie démarre à la vague 1). `ServerLevel` expose le même
`syncData(AttachmentType<?>)` qu'`Entity`, poussé à tous les joueurs qui suivent ce monde.

### L'affichage — `client/gui/WaveOverlay.java`

Contrairement aux trois autres overlays, **pas de jauge** : juste le texte `Vague X/Y` (clé
`dungeon_defenders.hud.wave`), en haut à droite de l'écran. La position est calculée avec
`guiGraphics.guiWidth() - MARGIN - font.width(texte)` pour rester collée au bord droit quel
que soit le nombre de chiffres. Lit `Minecraft.getInstance().level.getData(...)` — `level`,
pas `player`, puisque l'état appartient au monde.

### La progression de la vague — `client/gui/WaveEnemiesOverlay.java`

**Centrée tout en haut de l'écran**, indépendante du reste du groupe vague/phase (qui reste
en haut à droite) : c'est l'information la plus visible du jeu de référence (*Dungeon
Defenders* original), une grosse barre en haut-centre, donc elle a sa propre place plutôt que
de se caser à côté du texte `Vague X/Y`.

Même style que `ExperienceOverlay` (jauge + texte `Ennemis : X/Y`, clé
`dungeon_defenders.hud.wave_enemies`), mais plus large (`BAR_WIDTH = 240`) et avec le texte
**superposé au centre de la jauge** plutôt qu'à côté — plus compact malgré la largeur,
également plus proche du rendu du jeu de référence. `X` = ennemis déjà tués
(`ModAttachments.WAVE_ENEMIES_KILLED`, vide par défaut), `Y` = ennemis total de la vague
(`ModAttachments.WAVE_ENEMIES_TOTAL`, `10` par défaut) ; la jauge orange se remplit à mesure
que `X` se rapproche de `Y`. Deux attachments sur la `Level`, même raisonnement que
`current_wave`.

### La phase de la partie — `client/gui/PhaseOverlay.java`

Juste en dessous de la rangée vague/ennemis (`WaveOverlay.ROW_Y + ROW_HEIGHT`), texte seul
comme `WaveOverlay` : `Phase : Construction` ou `Phase : Combat`, clé
`dungeon_defenders.hud.phase`. La partie démarre en `BUILD`. La seule façon de changer de
phase pour l'instant est le harnais de test au clic droit du `SpawnerBlock` (voir plus bas) —
aucun vrai déclencheur de combat n'existe encore.

`init/GamePhase.java` est un enum (`BUILD`, `COMBAT`) plutôt qu'une chaîne libre, pour garder
un ensemble de valeurs fermé et une traduction par valeur
(`dungeon_defenders.phase.build`/`.combat`, via `GamePhase.translationKey()`). L'attachment
`ModAttachments.GAME_PHASE` stocke cependant un simple `Integer` (l'ordinal de l'enum), comme
tous les autres compteurs du mod — pas de `Codec`/`StreamCodec` dédié à l'enum pour
l'instant, ça aurait été de la complexité en plus pour un gain nul tant qu'il n'y a que deux
valeurs. À revoir si la liste des phases grandit ou si l'ordre des constantes doit pouvoir
changer sans casser les sauvegardes existantes (l'ordinal n'est pas stable entre deux
réordonnancements de l'enum).

## Le Spawner — `block/SpawnerBlock.java`

Premier morceau de vraie mécanique de combat du mod (le reste n'était que du HUD affichant
des valeurs par défaut) : un bloc à poser dans la map qui fait apparaître des ennemis pendant
la phase de combat. L'algorithme vient de la feuille "Idées" du plan Excel du joueur, précisé
au fil d'une discussion : un accumulateur par type d'ennemi, incrémenté chaque contrôle de son
"nombre de base" ; dès qu'il atteint un seuil, un ennemi de ce type spawn et le seuil lui est
retiré. Le nombre de base sert **aussi** de plafond pour ce type : une fois atteint, ce type
est sauté (round-robin sur les types restants) jusqu'à ce que tous soient épuisés — c'est
exactement l'exemple du joueur ("on a au total 15 gobelins et 5 orcs", pas juste un ratio).

```java
// SpawnEntry.tickAndMaybeSpawn(...), une fois par entrée de la composition
if (spawned >= effectiveTotal) {
    return false;   // ce type est épuisé pour la vague, on le saute
}
accumulator += effectiveTotal;
if (accumulator < SPAWN_THRESHOLD) {   // 20
    return false;
}
accumulator -= SPAWN_THRESHOLD;
spawned++;
type.spawn(level, spawnPos, EntitySpawnReason.SPAWNER);
```

> Le seuil se déclenche à `>=`, pas `>` : l'exemple du joueur contenait une ligne
> ("Gobelin = 20 → spawn") qui ne fonctionne qu'avec `>=`. Détail d'implémentation, l'idée
> reste identique à ce qu'il avait écrit.

**La position de spawn** (`SpawnerBlockEntity#findSafeSpawnPos`) : avec un rayon supérieur à
0, la position tirée au hasard dans ce rayon peut tomber à l'intérieur d'un bloc plein (mur,
terrain irrégulier autour du spawner). `findSafeSpawnPos` essaie jusqu'à 8 positions
aléatoires, en ne retenant que celles où la position **et** celle juste au-dessus (place pour
les pieds et la tête) sont toutes les deux traversables (`BlockState#getCollisionShape(...)
.isEmpty()`) ; si aucune des 8 ne convient, replie sur `pos` (**pas** `pos.above()`, voir plus
bas). Pas de vérification qu'il y a un sol en dessous (un ennemi qui spawn au-dessus d'un trou
tombe simplement, ce n'est pas un bug) — seul l'enlisement dans un bloc plein est évité.

> **Le repli a changé de `pos.above()` à `pos`** le jour où le spawner est devenu un marqueur
> sans collision (voir plus bas, "Jamais un obstacle physique"). Avant, le bloc du spawner
> lui-même servait de sol sous les pieds d'un monstre spawné juste au-dessus ; ce n'est plus
> possible puisqu'il n'a plus jamais de collision. `pos` lui-même est censé reposer sur le vrai
> sol construit par le créateur de la map (le spawner n'est qu'un marqueur au niveau du sol,
> pas une plateforme) — sans ce changement, tout spawn par défaut (rayon 0, le cas le plus
> courant) aurait fait tomber le monstre indéfiniment dans le vide (voir "Le monde et le point
> de spawn" : ce mod tourne dans une dimension entièrement vide, rien pour rattraper une chute).

### Jamais un obstacle physique — `getShape`/`getCollisionShape`/`getRenderShape`

Décidé avec le joueur (2026-08-25) : dans le jeu de référence, un point de spawn est une zone/
un marqueur, pas un objet physique. Le spawner ne bloque donc plus jamais le passage d'un
monstre ni du joueur, en Construction comme en Combat — avant, c'était un bloc plein classique,
qui pouvait gêner un monstre essayant de se frayer un chemin près de son propre point de spawn.

- **`getCollisionShape`** renvoie toujours `Shapes.empty()` — la forme lue pour la résolution
  physique des déplacements. Traversable pour tout le monde, tout le temps.
- **`getRenderShape`** renvoie toujours `RenderShape.INVISIBLE` — jamais rendu, pour personne.
  Une limite assumée : cette méthode ne reçoit que le `BlockState`, pas de niveau ni de joueur,
  donc impossible de la faire dépendre de qui regarde (contrairement à `getShape` ci-dessous).
  Le repérage en jeu reste possible via l'aperçu de composition (texte à travers les murs en
  Construction, voir plus bas) et, en créatif, via le contour de visée.
- **`getShape`** (forme de ciblage/sélection, **différente** de `getCollisionShape`) renvoie un
  cube plein **seulement si l'entité qui regarde est un joueur en mode créatif**, sinon vide :

  ```java
  protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      if (context instanceof EntityCollisionContext entityContext
              && entityContext.getEntity() instanceof Player player
              && player.isCreative()) {
          return Shapes.block();
      }
      return Shapes.empty();
  }
  ```

  Vérifié dans le code source de cette version (`ClipContext.Block.OUTLINE` → `getShape`,
  utilisé aussi bien pour le rayon de visée du joueur — donc pour savoir quel bloc `useWithoutItem`
  reçoit au clic droit — que pour le contour noir de sélection) : **`getCollisionShape` n'a
  aucun rôle dans le ciblage**, seul `getShape` compte. Résultat : un joueur en survie ne peut
  plus jamais viser/cliquer un spawner (son clic traverse, comme s'il n'était pas là), tandis
  qu'un créatif peut toujours le voir en contour et cliquer dessus pour le configurer — sans
  vérification supplémentaire dans `useWithoutItem` (voir plus bas), le ciblage filtre déjà tout.
  `EntityCollisionContext`/`CollisionContext.empty()` (utilisé par des vérifications sans
  entité précise, génération de terrain, pathfinding...) laisse `getEntity()` à `null` : traité
  comme "pas un joueur créatif", repli sûr sur `Shapes.empty()`.

**Conséquence sur la position de spawn** : `findSafeSpawnPos` (voir plus haut) ne peut plus
compter sur le bloc du spawner comme sol — corrigé au même moment, voir la remarque plus haut.

### L'état — `block/entity/SpawnerBlockEntity.java`

`BaseEntityBlock` + `BlockEntityTicker`, sur le même principe qu'`EterniaCrystalBlockEntity`
(codec, `newBlockEntity`) mais avec un tick serveur en plus — premier bloc du mod à en avoir
un. `serverTick(...)` :

1. Sort immédiatement si `ModAttachments.GAME_PHASE != COMBAT` — le spawner ne tourne qu'en
   combat.
2. Sort aussi si `CURRENT_WAVE` est en dehors de `[waveStart, waveEnd]` — un spawner peut être
   configuré pour ne s'activer que sur une plage de vagues.
3. Si une nouvelle session de combat a commencé depuis le dernier passage
   (`lastCombatSessionHandled` vs `ModAttachments.COMBAT_SESSION`, voir "Le déroulement d'une
   vague" plus bas), recalcule le plafond de chaque type (`resetForWave`, voir plus bas) et
   remet sa progression à zéro : une nouvelle session de combat, une nouvelle chance de spawn
   pour chaque type.
4. Ne fait tourner l'algorithme qu'une fois toutes les `intervalTicks` (20 par défaut, soit
   une seconde), pas à chaque tick, pour rester lisible.
5. Applique l'algorithme ci-dessus, une fois par entrée de la composition.

**La composition** (`List<SpawnEntry>`) est modifiable par spawner — deux entrées par défaut,
zombie (nombre de base 15) et squelette (nombre de base 5), reprenant exactement les chiffres
de l'exemple du joueur. Chaque `SpawnEntry` combine son ennemi (`init/SpawnableEnemy.java`,
plutôt qu'un `EntityType<?>` brut), son nombre de base et sa progression pour la vague en
cours (`spawned`, `accumulator`, `effectiveTotal`), le tout persistant via un `Codec` dédié
(`ValueOutput/ValueInput#list(...)`, la liste ayant une longueur variable contrairement aux
compteurs simples du reste du mod ; l'ennemi est stocké par ordinal, comme `GamePhase`).

`init/SpawnableEnemy.java` est la liste fermée des ennemis choisissables dans un spawner
(`ZOMBIE`, `SKELETON` pour l'instant). Il n'existe pas de tag vanilla générique "tout ce qui
est hostile" dans cette version de Minecraft (vérifié) : cet enum sert à la fois de source de
vérité réseau (transmis par ordinal, voir plus bas) et de liste pour le bouton "cycler le
type" du GUI. Ajouter un ennemi au jeu et vouloir le rendre choisissable dans un spawner se
résume à une ligne dans cet enum — rien d'autre à toucher côté GUI/réseau/persistance.

**Paramètres configurables par spawner**, en plus de la composition : `intervalTicks` (défaut
20), `spawnRadius` (défaut 0 — spawn pile au-dessus du bloc ; au-delà, une position aléatoire
dans ce rayon, pour éviter que tout s'empile sur la même case), `waveStart`/`waveEnd` (défaut
1 à `MAX_WAVE`). Tous persistants. Pas encore de GUI pour les éditer en jeu — en cours, voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md).

### Le multiplicateur de difficulté — `init/DifficultyScaling.java`

`effectiveTotal = round(baseCount × DifficultyScaling.getMultiplier(level))`, recalculé à
chaque nouvelle vague (pas à chaque tick — sinon un type déjà épuisé pourrait redevenir actif
en cours de vague si le multiplicateur changeait entre-temps). Le multiplicateur combine :

- **La difficulté** (`ModAttachments.DIFFICULTY`, enum `GameDifficulty` : `EASY` ×0.75,
  `NORMAL` ×1.0, `HARD` ×1.5) — censée être choisie au lancement de la map, mais aucun écran
  pour le faire n'existe encore : démarre toujours à `NORMAL`.
- **La vague courante** : +10 % par vague au-delà de la première (`1.0 + (wave - 1) × 0.1`).

`DifficultyScaling.getMultiplier(Level)` est le seul point de calcul, pensé pour accueillir
d'autres facteurs plus tard (nombre de joueurs, par exemple) sans changer les appelants.

Comme `effectiveTotal` sert à la fois de fréquence d'apparition (`accumulator +=
effectiveTotal`) et de plafond (`spawned >= effectiveTotal`), une difficulté plus élevée
rend un type d'ennemi **à la fois** plus nombreux et plus fréquent sur la vague — pas
seulement plus nombreux.

### Le harnais de test — clic droit (maj/shift = bascule de phase)

Comme pour le Cristal d'Eternia et `ManaTestWandItem`, un harnais de test plutôt qu'une vraie
mécanique : **shift + clic droit** sur le `SpawnerBlock` bascule `ModAttachments.GAME_PHASE`
entre `BUILD` et `COMBAT` directement, message système confirmant la nouvelle phase — sans
passer par le vote "prêt" du cristal (voir plus haut). Pratique pour tester rapidement sans
réunir tous les joueurs, mais ça fait aussi avancer `CURRENT_WAVE` comme n'importe quelle fin
de combat (voir [Le déroulement d'une
vague](#le-déroulement-dune-vague--initphasetransitionsjava-modeventsonmonsterdeath) plus bas)
— attention à l'utiliser en rafale en test, ça consomme les vagues vite.

Le passage effectif de phase (peu importe le déclencheur : vote "prêt", harnais de test, ou
fin de vague) passe toujours par `init/PhaseTransitions.java`, pas par un
`level.setData(GAME_PHASE, ...)` direct — ça centralise la remise à zéro des compteurs qui va
avec (voir plus bas), pour que les trois déclencheurs se comportent pareil.

Un clic droit **sans shift** ouvre l'écran de configuration (voir plus bas) — **réservé au
mode créatif**, comme un bloc de structure vanilla : la configuration d'un spawner est censée
être figée une fois la map construite (voir "Ce qui n'est PAS dans ce GUI" plus bas).

### L'écran de configuration — `menu/`, `network/`, `client/gui/screen/SpawnerConfigScreen.java`

Premier GUI custom du mod, **pas de slot ni d'item** — les paramètres décidés avec le joueur :
intervalle (ticks), rayon de spawn, vague de début, vague de fin, et une **liste dynamique**
de lignes de composition (une par ennemi choisi). Chaque ligne a un bouton qui affiche le nom
de l'ennemi et le fait cycler vers le suivant au clic (`SpawnableEnemy.next()`, en sautant les
ennemis déjà utilisés par une autre ligne), un champ pour son nombre de base, et un bouton "X"
pour la retirer (cachée s'il ne reste qu'une seule ligne — on garde toujours au moins un
ennemi). Un bouton "+ Ajouter" en bas de la liste, cachée une fois que toutes les valeurs de
`SpawnableEnemy` sont utilisées (la liste d'ennemis possibles est fermée). Pré-rempli avec la
configuration actuelle du spawner ciblé.

**Pourquoi l'état est gardé en mémoire, pas seulement dans les widgets** : ajouter ou retirer
une ligne change le nombre de lignes, donc décale tout ce qui suit (les lignes restantes, le
bouton Ajouter, le bouton Valider) — il faut reconstruire tous les widgets
(`Screen#rebuildWidgets()`). Mais reconstruire détruit les `EditBox`/`Button` existants, donc
leurs valeurs seraient perdues si elles n'étaient pas sauvegardées ailleurs. `SpawnerConfigScreen`
garde donc sa propre copie (`intervalText`, `radiusText`, ..., `rows: List<RowState>`) comme
source de vérité entre deux reconstructions, chargée depuis le `SpawnerBlockEntity` une seule
fois (`loadedFromSpawner`, à la toute première `init()`) pour ne pas écraser les modifications
en cours de l'utilisateur à chaque ajout/retrait. `syncFieldsToState()` recopie les valeurs des
widgets actuels dans cet état juste avant un rebuild. Cycler le type d'une ligne, en revanche,
ne touche **pas** au nombre de lignes : pas besoin de rebuild, juste
`AbstractWidget#setMessage(...)` sur le bouton concerné pour changer son libellé.

**Le trajet complet, dans l'ordre :**

1. **Clic droit** (sans shift) sur un `SpawnerBlock` → `SpawnerBlock.openConfigScreen(...)`
   appelle `player.openMenu(new SpawnerConfigMenuProvider(pos))` côté serveur.
2. **`SpawnerConfigMenuProvider`** (`record MenuProvider`) fournit `createMenu(...)` (le menu
   côté serveur, qui ne porte que le `BlockPos`) et surcharge
   `writeClientSideData(menu, buf)` — l'extension NeoForge de `MenuProvider` qui écrit des
   données supplémentaires dans le paquet d'ouverture. Ici : juste `buf.writeBlockPos(pos)`.
3. Côté client, `IMenuTypeExtension.create(SpawnerConfigMenu::new)` (voir
   `init/ModMenus.java`) reconstruit **le même** `SpawnerConfigMenu` à partir de ce
   `BlockPos` — c'est le rôle du deuxième constructeur de `SpawnerConfigMenu`, qui prend un
   `RegistryFriendlyByteBuf` et lit `extraData.readBlockPos()`.
4. `RegisterMenuScreensEvent` (voir `DungeonDefendersModClient`) associe ce `MenuType` à
   `SpawnerConfigScreen::new`, qui s'ouvre automatiquement.
5. **`SpawnerConfigScreen.init()`** ne lit **pas** le menu pour la configuration (le menu ne
   porte que le `BlockPos`) : il retrouve directement le `SpawnerBlockEntity` **côté client**
   via `Minecraft.getInstance().level.getBlockEntity(pos)` — cette copie cliente est déjà à
   jour grâce à la synchronisation ajoutée à `SpawnerBlockEntity`
   (`getUpdatePacket`/`getUpdateTag`, même mécanisme que pour les PV du cristal). L'état en
   mémoire (`intervalText`, ..., `rows`) est rempli depuis `getIntervalTicks()`,
   `getEntries()`, etc., avec un filtre n'acceptant que des chiffres sur les champs numériques
   (`EditBox#setFilter`).
6. Au clic sur **"Valider"**, le client construit un `SpawnerConfigPayload` (le `BlockPos` +
   4 valeurs scalaires + la liste `entries` (une paire ordinal d'ennemi / nombre de base par
   ligne), lue depuis l'état en mémoire) et l'envoie via
   `Minecraft.getInstance().getConnection().send(payload.toVanillaServerbound())`.
7. **`ModNetworking`** (enregistré via `RegisterPayloadHandlersEvent`, voir plus haut) reçoit
   le paquet côté serveur : revérifie que le bloc à cette position est toujours un
   `SpawnerBlockEntity` et que le joueur est encore à portée (8 blocs), reconstruit la liste
   de `SpawnEntry` en validant chaque ordinal d'ennemi reçu (`0 <= ordinal <
   SpawnableEnemy.values().length` — jamais faire confiance à une valeur reçue par le réseau
   pour indexer un tableau), puis appelle `spawner.applyConfig(...)`.

> **Pourquoi pas de vérification de portée dans `SpawnerConfigMenu#stillValid` ?** Parce que
> ce menu ne contient ni slot ni item : la seule action possible dessus est d'envoyer le
> paquet de config, et ce paquet revérifie déjà tout côté serveur avant d'agir. Une double
> vérification (menu **et** paquet) n'aurait rien apporté ici.

`SpawnerBlockEntity.applyConfig(...)` remplace entièrement la composition (`entries`) et
**applique tout de suite** les nouveaux plafonds : `resetForWave(...)` est appelé
immédiatement avec le multiplicateur de difficulté courant, sans attendre le prochain
changement de vague détecté par `serverTick`. C'est un choix volontaire différent de la toute
première version du spawner (qui attendait la vague suivante) : reconfigurer un spawner doit
se voir tout de suite, comme le reste du GUI. Une conséquence assumée : si on reconfigure en
plein milieu d'une vague, la progression de cette vague (`spawned`) repart de zéro pour les
nouvelles entrées.

**Ce qui n'est PAS dans ce GUI**, volontairement (voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md)) :

- Pas de choix de difficulté ici — c'est un réglage de partie (`ModAttachments.DIFFICULTY`),
  pas du spawner, voir plus haut.
- Pas de défilement (scroll) si la liste d'ennemis grandit au point de dépasser la hauteur de
  l'écran — non géré pour l'instant, acceptable tant que `SpawnableEnemy` ne contient que
  deux valeurs.
- **Pas accessible en survie** (`SpawnerBlock.openConfigScreen` vérifie `player.isCreative()`
  avant d'ouvrir l'écran, message système sinon) : l'idée à terme est que les maps soient des
  structures pré-construites (spawners déjà configurés en créatif, puis sauvegardées) posées
  au lancement d'une partie — pas un réglage que le joueur ferait pendant qu'il joue. Ce
  verrou permet aussi de garder simple le calcul de `wave_enemies_total` (voir plus bas) : pas
  besoin de le recalculer à chaque reconfiguration en pleine partie, puisque ça n'arrive plus.

### L'aperçu de composition en phase Construction — `SpawnerBlockEntityRenderer.java`

Comme dans le jeu de référence : en phase `BUILD`, chaque spawner affiche au-dessus de lui le
total d'ennemis à venir et le détail par type, **visible à travers les murs** — pour planifier
sa défense avant que le combat démarre. Disparaît en phase `COMBAT` (pas d'intérêt une fois la
vague lancée).

Techniquement, c'est un deuxième `BlockEntityRenderer` (même trio `createRenderState` /
`extractRenderState` / `submit` que `EterniaCrystalBlockEntityRenderer`), mais qui dessine du
**texte** plutôt que des quads de couleur — la barre de vie du cristal utilisait
`RenderTypes.debugQuads()` (quads non texturés, non cullés), qui ne convient pas au texte. Le
mécanisme "à travers les murs" ici est `Font.DisplayMode.SEE_THROUGH`, passé à
`SubmitNodeCollector#submitText(...)` : c'est le même render type que celui utilisé par
Minecraft pour les noms des mobs brillants (effet Glowing) au-dessus des blocs.

**Le calcul du total** réutilise exactement la formule de `SpawnEntry.resetForWave(...)`
(`max(1, round(baseCount × DifficultyScaling.getMultiplier(level)))`) pour chaque entrée de la
composition, sommée pour le total — l'aperçu affiché correspond donc exactement à ce que la
prochaine vague fera spawn, tant que la difficulté ne change pas entre-temps.

**Ce qui n'y est pas, volontairement** (voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md)) :

- Pas d'icône par type de monstre, texte seul pour l'instant ("Zombie : 12") — à ajouter plus
  tard si besoin, sans revoir cette classe puisque `SpawnableEnemy` porte déjà tout ce qu'il
  faut (`translationKey()`) pour brancher une icône dessus.
- Portée d'affichage plafonnée à 32 blocs (`MAX_DISTANCE_SQ`) pour éviter d'encombrer l'écran
  si beaucoup de spawners sont proches les uns des autres — valeur arbitraire, à ajuster si
  besoin une fois testée en jeu.

### Le compteur d'ennemis tués — `ModEvents.onMonsterDeath`

`WaveEnemiesOverlay` lisait déjà `ModAttachments.WAVE_ENEMIES_KILLED`, mais rien ne
l'incrémentait. Un handler `LivingDeathEvent` dans `ModEvents` : si l'entité qui meurt est un
`Monster` et que la phase est `COMBAT`, incrémente le compteur et le synchronise. Pas de
filtre sur "a été spawné par un `SpawnerBlockEntity`" — tout monstre mort en combat compte, ce
qui suffit au sens du HUD ("ennemis tués dans la vague").

### Le déroulement d'une vague — `init/PhaseTransitions.java`, `ModEvents.onMonsterDeath`

Ce qui manquait pour qu'une vague se déroule vraiment : un vrai déclencheur pour passer en
Combat (le vote "prêt", voir plus haut), un total juste (`WAVE_ENEMIES_TOTAL` était bloqué à
sa valeur par défaut), un retour automatique en Construction une fois ce total atteint, et
`CURRENT_WAVE` qui avance réellement d'une vague à l'autre.

**Le registre des spawners actifs** (`ModAttachments.ACTIVE_SPAWNERS`, un `Set<BlockPos>` sur
la `Level`) — nécessaire parce que calculer le total demande de connaître **tous** les
spawners de la carte, pas juste "moi-même" comme le fait déjà l'aperçu au-dessus de chaque
bloc (voir plus haut). Chaque `SpawnerBlockEntity` s'y ajoute dans `setLevel(...)` (appelé une
fois par instance, à la pose comme au chargement d'un chunk) et s'en retire dans
`setRemoved()`. Ni persistant ni synchronisé : usage strictement serveur, et un spawner non
chargé ne peut de toute façon pas spawn — l'exclure du registre est donc cohérent, pas un bug.
Ce registre ne reflète que "les spawners actuellement chargés" ; il deviendra pleinement fiable
une fois qu'un système force-chargera toute la zone de jeu pendant une partie (voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md)).

**Le calcul du total** (`PhaseTransitions.enterBuild(...)`) : à chaque entrée en Construction,
somme sur tous les spawners du registre (dont la vague courante tombe dans leur
`[waveStart, waveEnd]`) la même formule que l'aperçu déjà affiché au-dessus de chaque bloc
(`max(1, round(baseCount × multiplier))`). Recalculé **une seule fois**, à l'entrée en
Construction — pas en continu — parce que le GUI de config n'est plus accessible en pleine
partie (voir plus haut, verrou créatif) : rien ne peut changer la composition d'un spawner
une fois la Construction commencée, donc pas besoin de retour en arrière.

**La session de combat** (`ModAttachments.COMBAT_SESSION`, un compteur incrémenté à chaque
entrée en Combat) : chaque spawner relance sa propre progression de spawn (`resetForWave`)
au début de **chaque** session de combat, plutôt que seulement quand `CURRENT_WAVE` change
(remplace l'ancien déclencheur `lastWaveHandled`, désormais `lastCombatSessionHandled`). Cette
distinction reste utile même maintenant que `CURRENT_WAVE` avance : le harnais de test au clic
droit du `SpawnerBlock` (voir plus bas) permet toujours de rebasculer Combat → Construction →
Combat rapidement pour tester, ce qui fait aussi avancer la vague à chaque fois (voir plus
bas) — sans la session de combat comme déclencheur indépendant, un enchaînement de vagues très
rapide pourrait désynchroniser la remise à zéro de la progression de spawn.

**`PhaseTransitions.java`** centralise ces deux transitions (`enterCombat`/`enterBuild`) pour
que le vote "prêt", le harnais de test et le retour automatique de fin de vague passent tous
par le même code, plutôt que de dupliquer la remise à zéro des compteurs à trois endroits :

- `enterCombat(level)` : phase → `COMBAT`, incrémente `COMBAT_SESSION`, remet
  `WAVE_ENEMIES_KILLED` à 0, et remet "prêt" à faux pour tous les joueurs présents (voir plus
  haut, "Le vote prêt").
- `enterBuild(level)` : fait avancer `CURRENT_WAVE` de 1 (plafonné à `MAX_WAVE`, voir "Ce qui
  reste" plus bas), phase → `BUILD`, remet `WAVE_ENEMIES_KILLED` à 0 (corrigé — restait
  auparavant à l'ancienne valeur pendant toute la Construction suivante), recalcule
  `WAVE_ENEMIES_TOTAL` à partir du registre pour la nouvelle vague.

**Le retour automatique** (`ModEvents.onMonsterDeath`) : après avoir incrémenté
`WAVE_ENEMIES_KILLED`, si `killed >= total` (et `total > 0`, pour ne pas basculer
immédiatement si aucun spawner n'a encore pu contribuer), regarde si la vague qu'on vient de
nettoyer était déjà `MAX_WAVE` (capturé **avant** d'appeler `enterBuild`/`onVictory`, puisque
les deux modifient `CURRENT_WAVE`) :

- Si ce n'était **pas** la dernière vague : `enterBuild(...)` comme avant, message
  `dungeon_defenders.spawner.wave_cleared`.
- Si c'**était** la dernière vague : `PhaseTransitions.onVictory(level)` à la place.

### Victoire et défaite — `PhaseTransitions.onVictory/onDefeat`

Deux nouvelles transitions, sur le même principe que `enterCombat`/`enterBuild`
(centralisées dans `PhaseTransitions`, pas dupliquées à chaque appelant) :

- **`onVictory(level)`** — appelée par `ModEvents.onMonsterDeath` quand la dernière vague
  vient d'être nettoyée. Remet la partie à zéro, puis ouvre `GameOverScreen` (victoire) pour
  chaque joueur.
- **`onDefeat(level)`** — appelée par `EterniaCrystalBlockEntity` juste après la destruction
  du bloc à 0 PV (à la suite du message `eternia_crystal.destroyed` déjà existant). Même chose,
  version défaite.

**Changé le 2026-08-26** (retour du joueur) : les deux **n'envoient plus** de message système
ni de lien "Retour à la taverne" dans le chat — devenus redondants une fois `GameOverScreen`
en place (voir plus bas), qui a ses propres boutons "Rejouer"/"Retour à la taverne". `onVictory`
et `onDefeat` ne font donc plus que `resetGameState(level)` puis `sendGameOverScreen(player,
victory)` pour chaque joueur — `ChatFormatting`/`ClickEvent`/`Component`, plus utilisés dans ce
fichier, retirés ; `dungeon_defenders.game.return_to_tavern` (l'ancien lien) retiré des deux
fichiers de langue, devenu une clé morte.

Les deux passent par le même `resetGameState(level)` privé : `CURRENT_WAVE` → 1, phase →
`BUILD`, `WAVE_ENEMIES_KILLED` → 0, et `WAVE_ENEMIES_TOTAL` recalculé (réutilise
`recomputeWaveEnemiesTotal`, aussi appelée par `enterBuild`) — pour que la partie soit
immédiatement prête à relancer une vague 1 propre, sans qu'un spawner continue à faire
apparaître des ennemis sur une partie déjà gagnée ou perdue.

`recomputeWaveEnemiesTotal` est **publique** (pas seulement appelée aux transitions de phase) :
`SpawnerBlockEntity.setLevel`/`setRemoved` et `ModNetworking.handleSpawnerConfig` l'appellent
aussi, pour que `WAVE_ENEMIES_TOTAL` reste juste dès qu'un spawner apparaît, disparaît ou est
reconfiguré — plutôt que de rester bloqué sur la valeur par défaut de l'attachment (`10`)
jusqu'à la toute première transition de phase d'une partie (bug constaté en jeu, corrigé).

**Ce qui n'est PAS fait ici**, volontairement — voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md) :

- Le Cristal d'Eternia détruit **n'est pas replacé automatiquement** : `resetGameState` remet
  les compteurs à zéro, mais le bloc lui-même reste absent tant que personne n'en repose un à
  la main. Remettre "le cristal" en jeu après une défaite fait partie de la future remise à
  neuf d'une map (structure reposée, tours retirées, PV du cristal restaurés), pas de ce
  morceau-ci.
- `GameOverScreen` (voir plus bas) atténue le problème de confusion "partie terminée vs.
  simple pause entre deux vagues" (un écran plein est bien plus visible qu'un message système),
  mais ne le résout pas totalement : `Échap` le ferme sans rien faire, et rien n'empêche de
  continuer à jouer sur la vague 1 fraîchement réinitialisée sans avoir cliqué un bouton.
- `/dd_leave` reste une commande de harnais, pas un vrai point de sortie posé dans chaque
  map — un joueur pourrait aussi la taper à tout moment, pas seulement après une victoire/
  défaite (pas grave en soi, mais pas le vrai flux prévu à terme). `GameOverScreen` s'en sert
  toujours pour son bouton "Retour à la taverne" (`connection.sendCommand(...)`), juste sans
  passer par le chat.

### L'écran de fin de partie — `client/gui/screen/GameOverScreen.java`, `network/GameOverPayload.java`

Décidé avec le joueur (2026-08-26), repris du plan Excel : *"GUI avec rejouer ou taverne"*.
Ouvert automatiquement sur chaque client par `GameOverPayload(victory)`, envoyé depuis
`PhaseTransitions.onVictory/onDefeat` — seule source de retour visuel à la fin d'une partie
désormais, les messages système ont été retirés (voir "Victoire et défaite" plus haut).

**Premier paquet clientbound du mod** — tous les autres (`PlaceTowerPayload`,
`SpawnerConfigPayload`...) vont du client vers le serveur. Enregistré en deux temps, comme
recommandé par la javadoc de `RegisterClientPayloadHandlersEvent` :

- `ModNetworking.onRegisterPayloadHandlers` enregistre seulement le `TYPE`/`STREAM_CODEC` via
  `registrar.playToClient(type, codec)` **sans handler** — cette classe est chargée des deux
  côtés (pas de `Dist.CLIENT`), un serveur dédié doit donc pouvoir décoder ce paquet en théorie,
  mais ne l'exécute jamais lui-même.
- `DungeonDefendersModClient.onRegisterClientPayloadHandlers` (nouveau, classe client-only)
  enregistre le vrai handler, qui appelle `Minecraft.getInstance().setScreen(new
  GameOverScreen(payload.victory()))` sur le thread principal (`context.enqueueWork(...)`).
  Séparer les deux évite de charger une classe cliente (`Minecraft`, `Screen`...) sur un
  serveur dédié en enregistrant juste le handler dans la mauvaise classe.

`PhaseTransitions.sendGameOverScreen(player, victory)` envoie le paquet via
`serverPlayer.connection.send(new GameOverPayload(victory).toVanillaClientbound())` — le
symétrique exact de `.toVanillaServerbound()`, déjà utilisé partout ailleurs côté client.

`GameOverScreen` (simple `Screen`, pas de `Menu`) affiche le titre en vert/rouge
(`dungeon_defenders.game.victory`/`defeat`, réutilisées) et deux boutons :

- **"Rejouer"** envoie `StartGamePayload` — **exactement** le même paquet que le bouton "Jouer"
  de `MapSelectionScreen` : `MapInstance.startGame` nettoie la zone, la repose, et retéléporte.
  Pas de distinction "rejouer la même map" vs "choisir une nouvelle map" pour l'instant, une
  seule map placeholder existe de toute façon (voir 05-etat-et-problemes-connus.md).
- **"Retour à la taverne"** appelle `connection.sendCommand(MapInstance.RETURN_COMMAND)` —
  même commande de harnais qu'utilisait l'ancien lien cliquable du chat (retiré, voir "Victoire
  et défaite" plus haut), juste déclenchée depuis un bouton.

> **Bug trouvé en testant en jeu (2026-08-26) : le titre ne s'affichait jamais, seuls les
> boutons apparaissaient.** Cause : `TITLE_COLOR_VICTORY`/`TITLE_COLOR_DEFEAT` étaient écrites
> comme des littéraux à 6 chiffres (`0x55FF55`/`0xFF5555`), donc avec un octet alpha implicite
> à `0x00` — entièrement transparent. Vérifié dans les sources décompilées :
> `GuiGraphicsExtractor#text(Font, FormattedCharSequence, int, int, int, boolean)` ignore
> silencieusement l'ajout au render state si `ARGB.alpha(color) == 0`, sans la moindre erreur.
> Contrairement à l'ancien `GuiGraphics.drawString` (versions antérieures de Minecraft), cette
> version du pipeline de rendu **ne force plus** l'alpha à `0xFF` par défaut pour une couleur
> écrite sans son octet de poids fort — un piège classique pour tout code qui recopie l'ancienne
> convention. Corrigé en écrivant les deux constantes avec leur alpha explicite
> (`0xFF55FF55`/`0xFFFF5555`). **`MapSelectionScreen.TEXT_COLOR = 0xFFFFFF` a très probablement
> le même défaut** (même convention "RGB nu") — jamais vérifié/signalé, hors scope de cette
> branche/PR (fichier déjà mergé dans `main`), à corriger séparément.

### Apparence

Modèle `cube_all` pointant **provisoirement** sur la texture vanilla
`minecraft:block/spawner` (la cage du spawner vanilla, cohérente thématiquement). Miné à la
pioche (tag `mineable/pickaxe`) et se drope lui-même via
`data/dungeon_defenders/loot_table/blocks/spawner.json`.

## Le score et le personnage — bas centre de l'écran

Deux dernières lignes de texte, centrées horizontalement tout en bas de l'écran (à droite de
la barre d'expérience, qui elle est en bas à gauche) :

```
        toto - niv 15
          Score : 0
```

### Le score — `client/gui/ScoreOverlay.java`

`ModAttachments.SCORE` est conceptuellement l'expérience gagnée **sur la carte en cours**,
par opposition à `ModAttachments.EXPERIENCE` qui persiste au-delà d'une carte. Comme
`current_wave`, c'est un état de la `Level` : "notre score" est partagé par la partie, pas
individuel par joueur. Démarre à `0`, remis à `0` à chaque nouvelle partie
(`PhaseTransitions.resetGameState`, appelé à la victoire/défaite — voir "Victoire et défaite"
plus bas), alimenté par chaque monstre tué (voir "Expérience, score et niveau" ci-dessous).

Affiché en texte seul (pas de jauge, un score n'a pas de maximum), clé
`dungeon_defenders.hud.score`, centré via `guiGraphics.centeredText(...)`. Expose
`rowY(guiGraphics)`, une méthode package-visible que `CharacterOverlay` utilise pour se
positionner juste au-dessus (même principe que `WaveOverlay.waveText(...)` ou
`ExperienceOverlay.barTop(...)`).

### Le gain de score flottant — `client/gui/ScoreGainOverlay.java`, `network/ScoreGainPayload.java`

Décidé avec le joueur (2026-08-27) : un "+X \<source\>" apparaît en bas à **droite** de l'écran
à chaque gain de score (ex. "+10 Ennemi tué"), monte de 20px et s'estompe sur 1,5 seconde,
indépendamment pour chaque popup.

**Pourquoi un paquet dédié plutôt que de lire `ModAttachments.SCORE` ?** Une première version
détectait le gain en comparant le total synchronisé d'une frame à l'autre — ça marchait, mais
`SCORE` n'est qu'un total : impossible d'en déduire la **source** du gain (kill ? fin de vague ?
multiplicateur ?). Comme plusieurs sources de score sont prévues (voir "Feuille de route du
score" plus bas) et que le joueur voulait cette information affichée, `ModEvents.grantScore`
diffuse maintenant un `ScoreGainPayload(amount, sourceOrdinal, enemyOrdinal)` **en plus** de la
sync d'attachment habituelle :

```java
private static void grantScore(Level level, int amount, ScoreSource source, int enemyOrdinal) {
    int score = level.getData(ModAttachments.SCORE) + amount;
    level.setData(ModAttachments.SCORE, score);
    level.syncData(ModAttachments.SCORE);

    for (Player player : level.players()) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(
                    new ScoreGainPayload(amount, source.ordinal(), enemyOrdinal).toVanillaClientbound());
        }
    }
}
```

Toute future source de score (fin de vague, fin de map, multiplicateurs) devra passer par
`grantScore` plutôt que toucher `SCORE` directement, pour ne jamais dupliquer cette double mise
à jour (attachment + paquet), avec `ScoreGainPayload.NO_ENEMY` (`-1`) comme `enemyOrdinal` tant
qu'elle n'a pas d'ennemi précis à associer (voir "L'icône de l'ennemi tué" plus bas).
`init/ScoreSource.java` est l'enum de la source, transmise par ordinal comme le reste des enums
réseau du mod (`GamePhase`, `GameDifficulty`...) — un seul membre pour l'instant
(`MONSTER_KILLED`), les futures sources ajouteront le leur le moment venu.

**Premier paquet clientbound de cette branche** (même principe que celui décrit pour
`GameOverScreen` sur une autre branche, pas encore mergée ici) : le type/codec est enregistré
côté partagé (`ModNetworking.onRegisterPayloadHandlers`, `registrar.playToClient(...)` **sans**
handler), mais le handler lui-même — qui touche `ScoreGainOverlay`, une classe strictement
cliente — n'est enregistré que dans `DungeonDefendersModClient`, via
`RegisterClientPayloadHandlersEvent`. Pas de borne-check sur l'ordinal reçu côté client
(contrairement à `ModNetworking`, qui valide tout ordinal reçu d'un **client**) : ce paquet
vient du serveur, autoritaire dans ce mod co-op — même confiance que les autres ordinaux
synchronisés par attachment (`GamePhase`, `GameDifficulty`), jamais revérifiés côté client non
plus.

**`ScoreGainOverlay` devient un pur récepteur d'événements**, plus une lecture de `SCORE` :

```java
public class ScoreGainOverlay implements GuiLayer {
    public static final ScoreGainOverlay INSTANCE = new ScoreGainOverlay();
    ...
    public void addPopup(int amount, ScoreSource source, SpawnableEnemy enemy) {
        this.popups.add(new Popup(amount, source, enemy, Util.getMillis()));
    }
}
```

Instance unique exposée en statique (constructeur privé) : `RegisterGuiLayersEvent` enregistre
`ScoreGainOverlay.INSTANCE` pour le rendu, et le handler du paquet pousse dans cette même
instance — les deux se rejoignent là plutôt que par un paquet réseau interne au client. Plus
besoin de gérer "premier tick observé" ou "score qui redescend" : sans paquet, pas de popup,
ces cas se résolvent d'eux-mêmes.

**Animation inchangée** : même source de temps que `HealthLerp` (`Util.getMillis()`, temps réel
plutôt que `partialTicks`). Chaque `Popup` (record `amount`/`source`/`spawnTimeMs`) calcule sa
propre progression 0→1 sur `DURATION_MS` (1500 ms), utilisée à la fois pour la montée
(`RISE_PIXELS`) et le fondu (canal alpha du texte, `(alpha << 24) | RGB`) — même vert que
`ExperienceOverlay` (`0x22C55E`). Les popups simultanés se superposent simplement (pas de
logique d'empilement) — volontairement simple.

**Limite assumée, inchangée par ce changement** : si le serveur appelle `grantScore` plusieurs
fois dans le même tick (plusieurs morts simultanées), chaque appel envoie son propre paquet —
contrairement à l'ancienne version basée sur la sync d'attachment, ce n'est **plus** une
limite : chaque kill produit bien son propre popup, même simultané. La seule limite restante est
visuelle (superposition à l'écran, pas de décalage automatique).

#### L'icône de l'ennemi tué

Décidé avec le joueur (2026-08-27), juste après le paquet dédié ci-dessus : l'œuf d'invocation
de l'ennemi apparaît à gauche du texte (ex. œuf de zombie + "+10 Ennemi tué"), même principe que
l'aperçu de composition du Spawner (`SpawnerBlockEntityRenderer`, qui réutilise déjà les œufs
comme icônes reconnaissables — mais celui-là dessine en 3D dans le monde, celui-ci en 2D dans le
HUD).

**Transport** : `ScoreGainPayload` porte un troisième champ, `enemyOrdinal` — l'ordinal du
`SpawnableEnemy` tué, transmis comme `sourceOrdinal`, ou `ScoreGainPayload.NO_ENEMY` (`-1`) si
ce gain n'a pas d'ennemi associé (toute future source hors kill). Pas d'`Optional<Integer>` sur
le réseau : ce mod n'utilise ce patron nulle part ailleurs, une sentinelle entière suffit et
reste lisible. Résolu côté serveur dans `ModEvents.awardExperienceAndScore` via
`SpawnableEnemy.find(EntityType<?>)` (rendue publique à cette occasion — auparavant un détail
privé de `xpValueFor`), avec le même repli `NO_ENEMY` que `DEFAULT_XP_VALUE` si jamais le
monstre tué n'est pas dans la liste fermée du Spawner.

**Rendu** : `guiGraphics.item(ItemStack, x, y)` — la même méthode que la hotbar vanilla, pas une
API spéciale à découvrir. Positionnée à gauche du texte (`ICON_GAP` = 2px d'écart), centrée
verticalement sur la ligne de texte (icône 16px, texte ~9px de haut). **Limite assumée** :
contrairement au texte, l'icône ne s'estompe pas progressivement — `GuiGraphicsExtractor#item`
n'a pas de paramètre de teinte/alpha exploité ici, elle reste pleinement opaque tant que le
popup est affiché puis disparaît d'un coup avec lui, pas de fondu. Coût jugé négligeable :
l'icône vient de l'atlas de textures des items déjà chargé en mémoire (même atlas que la
hotbar/l'inventaire vanilla), aucun nouvel asset, et il n'y a jamais plus qu'une poignée de
popups vivants à la fois (durée de vie 1,5s).

### Le personnage — `client/gui/CharacterOverlay.java`

Affiche `Nom - niv X` (clé `dungeon_defenders.hud.character`), juste au-dessus de
`ScoreOverlay`. Deux points à noter :

- **Le nom** (`ModAttachments.CHARACTER_NAME`) est un attachment `String`, distinct du pseudo
  Minecraft (`GameProfile.name()`) — c'est volontairement un champ à part, pour pouvoir
  diverger du compte du joueur. Sa valeur par défaut reprend le pseudo Minecraft (via
  `AttachmentType.builder(Function<IAttachmentHolder, T>)`, qui donne accès au holder — ici
  le `Player` — pour calculer la valeur initiale), juste pour ne pas afficher une chaîne vide
  tant qu'aucun nom n'a été choisi. Rien ne permet encore de le changer (pas de commande, pas
  d'écran de création de personnage) : voir
  [05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md).
- **Le niveau** (`ModAttachments.LEVEL`) est un attachment joueur (contrairement au score),
  démarre à `1`, persistant, synchronisé. Monte via l'expérience gagnée en tuant des monstres
  (voir ci-dessous).

### Expérience, score et niveau — `ModEvents.awardExperienceAndScore`/`grantExperience`

Décidé avec le joueur (2026-08-27) : tuer un monstre (toute phase, comme le drop de cristal de
mana ci-dessus) donne à la fois du score (carte) et de l'expérience (joueur), branché dans le
même `onMonsterDeath` que le cristal de mana.

**Valeur par monstre** — `init/SpawnableEnemy.xpValue()`, un champ de plus sur l'enum déjà
utilisé par le Spawner (zombie = 10, squelette = 15, valeurs de test pas encore équilibrées,
le squelette rapportant plus car il attaque à distance). `SpawnableEnemy.xpValueFor(EntityType)`
fait la correspondance depuis le monstre tué ; 5 en repli si jamais un monstre hors de cette
liste venait à mourir (défensif, ne devrait pas arriver tant que le Spawner reste l'unique
source de monstres).

**Le score** est incrémenté sans conditions : `level.getData(SCORE) + xpValue`.

**L'expérience est partagée entre tous les joueurs présents**, pas seulement celui qui a porté
le coup fatal — décidé avec le joueur : ce sont surtout les tours qui tuent dans ce mod
(`AbstractTurretBlockEntity`, dégâts directs sans lien avec un joueur), et rien ne capte
aujourd'hui "quel joueur a tué quoi". Même logique co-op que le ramassage des cristaux de mana,
ouvert à n'importe qui plutôt qu'à un seul joueur :

```java
for (Player player : level.players()) {
    grantExperience(player, xpValue);
}
```

`grantExperience` ajoute la valeur à `EXPERIENCE`, puis boucle tant que `experience >=
MAX_EXPERIENCE` (100, plafond fixe par niveau, pas de barème croissant pour l'instant) :
chaque passage décrémente `experience` de `MAX_EXPERIENCE` et incrémente `LEVEL` — une boucle
plutôt qu'un seul `if`, pour rester correct si un futur monstre à forte valeur d'XP fait
franchir plusieurs paliers d'un coup. Un message système (`dungeon_defenders.level.up`) est
envoyé au joueur concerné à chaque passage de niveau — pas redondant avec le HUD, contrairement
aux anciens messages de victoire/défaite retirés (voir "Victoire et défaite" plus bas) : c'est
un événement ponctuel, pas un état déjà affiché en permanence.

`ModAttachments.SCORE` est remis à `0` par `PhaseTransitions.resetGameState` (victoire/défaite,
donc à chaque nouvelle partie) — cohérent avec "score de la carte en cours". `EXPERIENCE`/
`LEVEL`, eux, ne sont jamais remis à zéro : ils persistent au-delà d'une carte, comme prévu dès
l'origine (voir "L'expérience custom du joueur" plus haut).

**Pas encore fait** : aucun bonus de statistique (mana/vie max, etc.) lié au niveau — pour
l'instant purement un compteur affiché, voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md).

### Feuille de route du score (décidée avec le joueur, pas codée pour l'instant)

Le kill est **une seule source de score parmi plusieurs prévues** — confirmé avec le joueur
(2026-08-27) : "le score est officieusement l'XP gagnée sur ce niveau", donc toute future
source de score suit la même logique (Level-scopée, remise à 0 à chaque partie), sans forcément
donner de l'expérience joueur en parallèle (l'un n'implique pas l'autre, seul le kill fait
aujourd'hui les deux à la fois). Prévu, mais **volontairement pas implémenté maintenant** —
seul le kill (ci-dessus) est en place :

- Bonus de score à la fin d'une vague nettoyée.
- Bonus de score à la fin de la map (victoire).
- Multiplicateurs par vague, cumulables (probablement) : aucun dégât pris par un joueur, aucun
  dégât pris par le Cristal d'Eternia, aucune tour détruite par les ennemis — d'autres pourront
  s'ajouter. Demande un suivi de dégâts par vague qui n'existe pas encore (ni pour le joueur, ni
  pour le cristal, ni pour les tours).
- Multiplicateur selon la difficulté choisie (`ModAttachments.DIFFICULTY`, existe déjà — juste
  la table de multiplicateurs à définir).
- Multiplicateur selon la difficulté de la map : un futur paramètre par map (`init/GameMap.java`
  n'a pas encore ce champ), estimé par le créateur de la map au moment de sa conception.

Formules, valeurs et ordre d'implémentation pas encore tranchés — voir
[07-idées-et-backlog.md](07-idées-et-backlog.md) pour le suivi ligne par ligne.

## Les emplacements de compétences — `client/gui/AbilitySlotsOverlay.java`

Quatre ronds en bas à **gauche** de l'écran, juste à droite des losanges vie/mana et
au-dessus de la barre d'expérience — dans le prolongement du groupe bas-gauche décrit plus
haut, comme dans le jeu de référence, dans l'ordre gauche → droite :

```
   Vie         Mana
    ◆           ◆     ( ) ( ) ( ) ( )
   ▓█▓         ▓█▓     soin sort1 sort2 répare
  ▓███▓       ▓███▓
  ░░░░░       ▓▓▓▓▓
    ░           ▓
  [███░░░░░░░░░░░░░░] Experience: 0/100
```

1. Soin sur soi
2. Sort 1 du héros
3. Sort 2 du héros
4. Réparation de tour

**Purement visuel pour l'instant** : aucun clic, aucun cooldown, aucune consommation de mana,
aucune icône (elles arriveront plus tard, une par slot). C'est juste le fond des slots — un
rond avec une fine bordure, dessiné par `CircleSlot` (même philosophie que `DiamondGauge` :
`guiGraphics.fill()` empilés, bande par bande, largeur donnée par le théorème de Pythagore —
pas de texture ni de géométrie custom bas niveau).

Se positionne à partir de `HudLayout.MARGIN + DIAMOND_RADIUS * 4 + DIAMOND_GAP` (le bord droit
du losange vie), plus un petit écart (`GROUP_GAP`) — c'est le seul couplage avec le reste du
groupe bas-gauche, pour rester juste à côté des losanges plutôt que de dupliquer leur calcul
de position en dur.

`AbilitySlotsOverlay.SLOT_NAMES` est un tableau de 4 identifiants (`self_heal`,
`hero_spell_1`, `hero_spell_2`, `repair_tower`) qui ne sert encore à rien à l'exécution : il
documente juste l'ordre attendu, en attendant que chaque slot ait sa propre icône et sa propre
logique. Contrairement aux autres overlays, pas d'attachment ici non plus : il n'y a encore
aucun état à lire (pas de cooldown, pas de "sort débloqué ou non"), voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md).

## Le HUD vanilla masqué

Le mod vise une interface entièrement custom : plusieurs couches du HUD vanilla sont donc
masquées dans `DungeonDefendersModClient.onRegisterGuiLayers`, via
`event.replaceLayer(identifiant, HIDDEN)` où `HIDDEN` est une `GuiLayer` dont `render(...)` ne
fait rien.

| Couche masquée | Identifiant `VanillaGuiLayers` | Remplacée par |
|---|---|---|
| Cœurs de vie | `PLAYER_HEALTH` | `HealthOverlay` |
| Faim | `FOOD_LEVEL` | *(rien pour l'instant)* |
| Expérience vanilla | `EXPERIENCE_LEVEL` | `ExperienceOverlay` (expérience **custom**, sans rapport avec l'XP vanilla) |
| Barre d'inventaire (hotbar) | `HOTBAR` | *(rien pour l'instant)* |

> `replaceLayer` ne fait que vider le contenu d'une couche existante, sans la retirer de la
> liste : l'ordre d'affichage et les couches enregistrées relativement à elle (via
> `registerAbove`/`registerBelow` côté vanilla, par exemple l'armure au-dessus de la hotbar)
> restent inchangés, elles dessinent juste dans le vide.

Faim et hotbar n'ont pas encore d'équivalent custom : tant que ce n'est pas fait, le joueur ne
voit ni sa faim, ni l'objet sélectionné/sa barre d'objets. Voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md).
