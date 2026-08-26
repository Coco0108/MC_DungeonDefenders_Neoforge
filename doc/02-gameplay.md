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

1. Fixe le point de spawn du monde à **(0, 65, 0)** via `ServerLevel#setRespawnData(...)` —
   remplace la recherche automatique de sol (qui échouerait dans le vide).
2. **(Re)pose le contenu de la taverne** à cet emplacement — pour l'instant une plateforme
   provisoire en dur (9×9, `smooth_stone`, un bloc sous le point de spawn), en attendant une
   vraie structure `.nbt`.

**Pourquoi rejouer l'étape 2 à *chaque* chargement du monde**, plutôt qu'une seule fois : la
taverne suit le même principe que les futures maps (voir 05-etat-et-problemes-connus.md,
"Système de maps/structures") — sa structure sera reposée à cet emplacement fixe à chaque
fois qu'on y "entre", pas construite une fois pour toutes. Sans ça, mettre à jour la structure
de la taverne dans une future version du mod resterait invisible sur une sauvegarde
existante : le joueur garderait la version posée lors de sa toute première connexion, le mod
n'ayant plus aucune raison de la reposer ensuite. Recharger à chaque démarrage du monde est le
déclencheur le plus simple qui garantit que ce qui est affiché correspond toujours à la
version livrée avec le mod installé. Le remplacement de la plateforme provisoire par un vrai
chargement de structure `.nbt` gardera ce même principe (voir le commentaire de
`TavernSpawn#buildPlaceholderPlatform`).

`LevelEvent.Load` se déclenche pour **toute** dimension qui se charge (l'Overworld, mais
aussi le Nether/End vanilla si un joueur y va) — le handler sort immédiatement si ce n'est
pas l'Overworld (`serverLevel.dimension() != Level.OVERWORLD`), pour ne rien changer ailleurs.

## La taverne — choix de map et difficulté

### Le bloc — `block/TavernCrystalBlock.java`

Un cristal, mais **différent** d'`EterniaCrystalBlock` : pas de block entity, pas de PV, pas
de mécanique de combat — juste un point d'interaction dans la taverne. Clic droit ouvre
`MapSelectionScreen`, **entièrement côté client** :

```java
protected InteractionResult useWithoutItem(...) {
    if (level.isClientSide()) {
        Minecraft.getInstance().setScreen(new MapSelectionScreen());
    }
    return InteractionResult.SUCCESS;
}
```

Pas de `player.openMenu(...)` ni de `Menu`/`MenuProvider` comme pour le spawner — cet écran
n'a besoin d'aucune donnée propre à un bloc précis (contrairement au spawner, qui devait
savoir *quel* spawner configurer via son `BlockPos`) : la liste des maps est statique côté
client, et la difficulté actuelle vient d'un attachment de `Level` déjà synchronisé
(`ModAttachments.DIFFICULTY`). Le système de `Menu` sert à transmettre des données du serveur
au client à l'ouverture ; ici il n'y a rien à transmettre, donc pas besoin de ce système.

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

### Le bloc de spawn joueur — `block PLAYER_SPAWN`, `MapInstance#findAndConsumeSpawnMarker`

Décidé avec le joueur (2026-08-26), repris du plan Excel (feuille "Idées" > "CHOIX DE MAP") :
plutôt qu'une téléportation vers `MAP_POS` codée en dur, le créateur d'une map peut poser un
bloc `PLAYER_SPAWN` (bloc plein simple, aucun comportement au clic — `BLOCKS.registerSimpleBlock`,
pas de classe dédiée) à l'endroit exact où les joueurs doivent apparaître.

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

## Le Cristal d'Eternia

### Le bloc — `block/EterniaCrystalBlock.java`

Étend `BaseEntityBlock` (bloc porteur d'un block entity).

| Aspect | Implémentation |
|---|---|
| Codec | `simpleCodec(EterniaCrystalBlock::new)` — obligatoire depuis 1.20.5 |
| Block entity | `newBlockEntity` → `new EterniaCrystalBlockEntity(pos, state)` |
| Render shape | `RenderShape.MODEL` (modèle JSON classique) |
| Collision | `Shapes.box(0, 0, 0, 1, 3, 1)` — 1×3×1, le cristal est infranchissable sur 3 blocs |
| Sélection | même boîte 1×3×1 (contour de visée aligné sur la collision) |

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
   `BlockEntityRenderState` portant un simple `float healthPercent`).
2. **`extractRenderState(...)`** — appelé côté extraction, avec accès au block entity :
   remplit `healthPercent = clamp(getCrystalHealth() / DEFAULT_HEALTH, 0, 1)`.
3. **`submit(state, poseStack, collector, camera)`** — ne voit que l'état :
   - translation à `(0.5, 3.2, 0.5)`, au-dessus de la hitbox de 3 blocs ;
   - billboard via `poseStack.mulPose(camera.orientation)`. Après cette rotation `+X` va vers
     la droite et **`+Y` vers le bas** (même convention que les name tags vanilla), d'où le
     `scale(1, -1, 1)` qui rétablit des coordonnées naturelles ;
   - `collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), ...)` — la
     géométrie est *soumise*, plus dessinée immédiatement ; le lambda reçoit un
     `PoseStack.Pose` et un `VertexConsumer` au moment du rendu réel.

Deux quads sont émis via `addSegment`, **juxtaposés et jamais superposés** : la jauge
colorée occupe la portion pleine (`healthPercent`), le gris (`0.3, 0.3, 0.3`) le reste. Un
segment de largeur nulle n'est pas émis du tout.

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

**Le blocage du passage est gratuit** : un bloc plein (propriété par défaut de n'importe quel
`Block` Minecraft, rien à coder) bloque déjà la marche d'un mob. Toute la logique custom sert
donc uniquement à donner des PV au blockade et à faire en sorte qu'un ennemi choisisse de
l'attaquer plutôt que de rester bloqué bêtement devant.

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

Logique : d'abord la phase — `level.getData(ModAttachments.GAME_PHASE) ==
GamePhase.BUILD.ordinal()`, sinon placement annulé et message dédié
(`dungeon_defenders.tower.build_phase_only`), vérifié **avant** le mana pour ne pas laisser
croire à un problème de mana alors que c'est la phase qui bloque. Puis le mana : si
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
- Pas de remboursement de mana en le cassant.
- Pas d'indicateur visuel de PV restants (barre de vie, changement de texture...) — seul
  `getHealth()` existe côté code, rien ne l'affiche encore.
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
normal (gratuit, comme Blockade), mais ce n'est pas son rôle : elle est pensée "posée en
retrait" (voir la taxonomie du joueur), à l'inverse d'un mur pensé pour être au contact.

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
- Pas d'indicateur visuel de PV restants (même manque que Spike Blockade).
- La convention de rotation de l'hologramme (voir "L'hologramme et le cercle de portée"
  ci-dessous) n'a été vérifiée visuellement que pour `NORTH` (rotation identité, à l'abri de
  toute inversion de sens) — **pas** pour `EAST`/`SOUTH`/`WEST`, jamais lancé en jeu.

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

Deux `KeyMapping` (catégorie vanilla `GAMEPLAY`) : `tower_wheel` (ouvre la roue, `R` par
défaut) et `rotate_tower` (fait pivoter l'hologramme pendant l'étape orientation, `T` par
défaut).

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

### Le mode pose, deux étapes — `client/TowerPlacementState.java`, `client/TowerPlacementClientEvents.java`

Sélectionner une tour dans la roue démarre `TowerPlacementState` (état transitoire, pas
persistant) en étape **AIMING**, puis fait basculer en **ORIENTING** au premier clic droit
valide :

- **AIMING** : chaque tick, un rayon est lancé depuis les yeux du joueur
  (`level.clip(new ClipContext(...))`, portée 20 blocs, `ClipContext.Block.OUTLINE`) ; la
  position juste après le bloc touché (`hit.getBlockPos().relative(hit.getDirection())`)
  devient la cible, valide si `canBeReplaced()`. **Clic gauche** annule tout le mode pose.
  **Clic droit** sur une cible valide verrouille la position et passe en ORIENTING (ignoré sur
  une cible invalide).
- **ORIENTING** : la position ne bouge plus. La touche `rotate_tower` fait pivoter la rotation
  courante (`Direction`, pas de 90°, `getClockWise()`). **Clic droit** envoie
  `PlaceTowerPayload` (tour + position + rotation) au serveur et quitte le mode pose. **Clic
  gauche** annule tout (pas de retour à AIMING).

L'interception des clics passe par `InputEvent.InteractionKeyMappingTriggered`
(`isAttack()`/`isUseItem()`, annulés le temps du mode pose pour ne pas déclencher l'action
vanilla en dessous) — un garde sur `InteractionHand.MAIN_HAND` évite de traiter deux fois le
clic droit (l'event se déclenche une fois par main pour "Use Item").

**Spike Blockade** est un cube symétrique, sans propriété d'orientation dans son `BlockState` —
tourner son hologramme n'a donc aucun effet visuel ni gameplay sur cette tour précise. C'est le
**Harpoon Turret** qui exerce enfin cette étape pour de vrai (`HORIZONTAL_FACING` + cône de
tir orienté, voir plus haut) — l'étape ORIENTING avait été construite génériquement par
anticipation, avant qu'une tour concrète n'en ait besoin.

### L'hologramme et le cercle de portée — rendu

Rendu via le pipeline "submit node" de cette version (pas le rendu immédiat classique) :
`ExtractLevelRenderStateEvent` copie l'état courant de `TowerPlacementState` dans un
`TowerPlacementRenderState`, posé sur le `LevelRenderState` global via un `ContextKey` (pas de
block entity à qui l'accrocher, contrairement aux autres renderers du mod) ;
`SubmitCustomGeometryEvent` le relit et soumet la géométrie :

- **Contour filaire du bloc** (`Shapes.block()`, parcouru arête par arête comme le fait
  `ShapeRenderer.renderShape` vanilla en interne — réimplémenté ici car cette méthode attend un
  `PoseStack` complet, alors que `submitCustomGeometry` ne fournit qu'un `PoseStack.Pose`
  différé) : **vert si la position visée est valide, rouge sinon** — toujours vert en
  ORIENTING, puisque la position y est déjà verrouillée comme valide.
- **Zone de portée** (`renderRangeArea`, `RenderTypes.lines()`), uniquement si
  `TowerDefinition.range() > 0` — **cercle complet** si `coneAngleDegrees >= 360`
  (omnidirectionnel), sinon un **secteur/cône** : l'arc borné à `[-coneAngleDegrees/2,
  +coneAngleDegrees/2]` **plus** deux segments droits vers l'origine, pour lire visuellement un
  cône et pas un arc flottant dans le vide. La rotation de la zone suit `state.rotation`
  **en permanence** (pas seulement en ORIENTING comme pour le contour du bloc, qui ne montre de
  toute façon jamais sa rotation visuellement pour un cube parfait) : le cône reflète déjà
  l'orientation par défaut (`NORTH`) dès l'étape AIMING, et suit la touche de rotation une fois
  en ORIENTING.

  Convention du gabarit local : angle `-90°` (dans le repère `cos`/`sin` déjà utilisé par
  l'ancien cercle complet) correspond à la direction `NORTH`, cohérente avec
  `Direction.NORTH.toYRot() == 0` (rotation identité) — déduite par construction, donc fiable
  pour `NORTH`. **Non vérifiée visuellement pour `EAST`/`SOUTH`/`WEST`** (dépend du sens de
  rotation réel de `Axis.YP.rotationDegrees`, jamais confirmé faute de lancer le client) — à
  contrôler en priorité au premier test du Harpoon Turret, voir `doc/06-a-tester.md`.
  Jamais rien à dessiner pour Spike Blockade (`range = 0.0`) ; le Harpoon Turret
  (`range=12.0`, `coneAngleDegrees=45.0`) est le premier à l'exercer réellement.

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

## Le mana du joueur

Ressource pensée pour alimenter de futurs sorts/capacités (aucun n'existe encore : pour
l'instant le mana ne se dépense ni ne se régénère, il reste affiché plein).

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
  une forme de losange plutôt qu'un rectangle. C'est le losange de **gauche** du groupe (vie à
  droite) ;
- au-dessus du losange, le texte `Mana: X/Y` (clé `dungeon_defenders.hud.mana`), centré
  horizontalement (`guiGraphics.centeredText`) sur son centre.

Lit `player.getData(ModAttachments.MANA)` à chaque frame ; pas d'état côté overlay lui-même.
Voir [05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md) pour ce qu'il reste à
faire (texture, régénération).

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

Modèle provisoire : texture vanilla `minecraft:item/blaze_rod`, pas de modèle dédié.

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
`dungeon_defenders.hud.health`), en rouge, positionné juste à **droite** du losange mana
(centré sur `HudLayout.MARGIN + DIAMOND_RADIUS * 3 + DIAMOND_GAP`), même taille. Lit
directement `player.getHealth()` / `player.getMaxHealth()` à chaque frame — pas besoin
d'attachment, ces valeurs sont déjà tenues à jour et synchronisées par le moteur.

Les cœurs vanilla (`VanillaGuiLayers.PLAYER_HEALTH`) sont masqués dans
`DungeonDefendersModClient.onRegisterGuiLayers` via `event.replaceLayer(..., HIDDEN)` :
avec 100 PV ils s'étaleraient sur plusieurs rangées de cœurs (le rendu vanilla est pensé pour
20 PV, pas 100) et feraient de toute façon doublon avec `HealthOverlay`.

## L'expérience custom du joueur

**Rien à voir avec l'XP vanilla** (`EXPERIENCE_LEVEL`/`getExperienceLevel()`) : c'est une
ressource propre au mod, pensée pour un futur système de progression/niveaux (pas encore
défini — rien ne la fait varier pour l'instant, elle démarre à 0).

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

`MAX_EXPERIENCE = 100` est une valeur provisoire : sans système de niveaux défini, il n'y a
pas encore de vraie notion de "maximum", c'est surtout ce qui donne son échelle à la jauge.

### L'affichage — `client/gui/ExperienceOverlay.java`

Contrairement à `ManaOverlay`/`HealthOverlay`, reste une **barre horizontale** classique
(jauge + texte `Experience: X/Y` à sa droite, clé `dungeon_defenders.hud.experience`), en
vert, tout en bas de l'écran, sous les deux losanges. Comme rien ne fait encore varier
l'attachment, elle s'affiche `0/100` en permanence tant qu'aucun mécanisme n'alimente
`ModAttachments.EXPERIENCE`.

## Le groupe bas-gauche — mana, vie, expérience

`ManaOverlay`, `HealthOverlay` et `ExperienceOverlay` forment un groupe positionné dans le
coin bas-gauche de l'écran :

```
   Mana        Vie
    ◆           ◆     <- losanges, remplissage bas → haut (pointe basse → pointe haute)
   ▓█▓         ▓█▓        mana à gauche, vie à droite
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
.isEmpty()`) ; si aucune des 8 ne convient, replie sur `pos.above()` — la position par défaut
utilisée avant l'ajout du rayon, censée toujours être libre. Pas de vérification qu'il y a un
sol en dessous (un ennemi qui spawn au-dessus d'un trou tombe simplement, ce n'est pas un bug)
— seul l'enlisement dans un bloc plein est évité.

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
  reste" plus bas), phase → `BUILD`, recalcule `WAVE_ENEMIES_TOTAL` à partir du registre pour
  la nouvelle vague.

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
  vient d'être nettoyée. Diffuse `dungeon_defenders.game.victory` (vert, gras) à tous les
  joueurs, puis remet la partie à zéro.
- **`onDefeat(level)`** — appelée par `EterniaCrystalBlockEntity` juste après la destruction
  du bloc à 0 PV (à la suite du message `eternia_crystal.destroyed` déjà existant). Diffuse
  `dungeon_defenders.game.defeat` (rouge, gras), puis remet la partie à zéro.

Les deux passent par le même `resetGameState(level)` privé : `CURRENT_WAVE` → 1, phase →
`BUILD`, `WAVE_ENEMIES_KILLED` → 0, et `WAVE_ENEMIES_TOTAL` recalculé (réutilise
`recomputeWaveEnemiesTotal`, la même méthode privée qu'`enterBuild`) — pour que la partie soit
immédiatement prête à relancer une vague 1 propre, sans qu'un spawner continue à faire
apparaître des ennemis sur une partie déjà gagnée ou perdue.

**Le lien "Retour à la taverne"** : les deux méthodes diffusent aussi, juste après le message
de victoire/défaite, un second message — un simple `Component.translatable(...)` stylé
(`ChatFormatting.AQUA`, souligné) avec un `ClickEvent.RunCommand("/dd_leave")` accroché via
`Style#withClickEvent(...)`. Cliquer dessus revient à taper la commande `/dd_leave`
(`ModCommands`), qui appelle `MapInstance.returnToTavern(level)` — nettoie l'emplacement de
map et téléporte tout le monde. Pas de nouveau paquet réseau : le clic déclenche directement
une commande déjà existante, exactement comme si le joueur l'avait tapée lui-même.

**Ce qui n'est PAS fait ici**, volontairement — voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md) :

- Le Cristal d'Eternia détruit **n'est pas replacé automatiquement** : `resetGameState` remet
  les compteurs à zéro, mais le bloc lui-même reste absent tant que personne n'en repose un à
  la main. Remettre "le cristal" en jeu après une défaite fait partie de la future remise à
  neuf d'une map (structure reposée, tours retirées, PV du cristal restaurés), pas de ce
  morceau-ci.
- Pas d'écran dédié "Victoire"/"Game Over" — juste deux messages système (le résultat, puis le
  lien de retour). L'idée d'un écran avec choix "rejouer/retour à la taverne" existe (vue dans
  le plan Excel du joueur) ; pour l'instant, revenir à la taverne permet déjà de "rejouer" en
  rouvrant `MapSelectionScreen`, donc un vrai écran dédié reste un raffinement visuel, pas un
  vrai manque fonctionnel.
- Rien ne distingue encore une partie "terminée" (victoire/défaite) d'une simple pause entre
  deux vagues : les deux ramènent en phase `BUILD`, vague 1. Un joueur qui n'a pas vu le
  message peut ne pas remarquer que la partie a recommencé à zéro.
- `/dd_leave` reste une commande de harnais, pas un vrai point de sortie posé dans chaque
  map — un joueur pourrait aussi la taper à tout moment, pas seulement après une victoire/
  défaite (pas grave en soi, mais pas le vrai flux prévu à terme).

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
par opposition à `ModAttachments.EXPERIENCE` qui est censée persister au-delà d'une carte.
C'est pourquoi ce n'est pas le même attachment, même si les deux valeurs pourraient un jour
augmenter ensemble (une capacité tuant un ennemi donnerait de l'XP *et* du score, un peu comme
la vue et le score au sens jeu vidéo classique). Comme `current_wave`, c'est un état de la
`Level` : "notre score" est partagé par la partie, pas individuel par joueur. Démarre à `0`,
rien ne l'alimente encore.

Affiché en texte seul (pas de jauge, un score n'a pas de maximum), clé
`dungeon_defenders.hud.score`, centré via `guiGraphics.centeredText(...)`. Expose
`rowY(guiGraphics)`, une méthode package-visible que `CharacterOverlay` utilise pour se
positionner juste au-dessus (même principe que `WaveOverlay.waveText(...)` ou
`ExperienceOverlay.barTop(...)`).

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
  démarre à `1`, persistant, synchronisé. Rien ne le fait encore monter — pas de formule
  d'XP → niveau, pas de notion de "monter de niveau".

## Les emplacements de compétences — `client/gui/AbilitySlotsOverlay.java`

Quatre ronds en bas à **gauche** de l'écran, juste à droite des losanges mana/vie et
au-dessus de la barre d'expérience — dans le prolongement du groupe bas-gauche décrit plus
haut, comme dans le jeu de référence, dans l'ordre gauche → droite :

```
   Mana        Vie
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
