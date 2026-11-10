# Data-driven, étape 1 : les ennemis

> **Statut : implémenté le 2026-09-29** sur la branche `feature/data-driven`, créée depuis
> `feature/map-ecart-ia` (PR #43), à partir du plan ci-dessous validé avec le joueur le même jour.
> **Pas encore testé en jeu** : procédure dans
> [06-a-tester.md](../06-a-tester.md#ennemis-data-driven-étape-1-enemyregistry-json-dennemis-migration-du-format-des-spawners).
>
> **Contrainte absolue : aucune valeur de gameplay ne change.** Les JSON livrés reprennent
> exactement les valeurs actuelles du code. Après migration, une partie doit se dérouler
> exactement comme avant : mêmes monstres, même XP, mêmes dégâts, même IA. Vérifié par le
> gametest `enemy_shipped_values`, et par comparaison des trois `.nbt` de test régénérés : seul
> le champ `Enemy` diffère, conformément à la table figée.

## Ce qui a changé par rapport au plan

Tout le plan est implémenté tel quel, avec quatre écarts. Aucun ne change une valeur de
gameplay.

1. **Registre des spawners actifs corrigé** (hors plan, mais bloquant pour le test n°1).
   Le gametest `enemy_unknown_excluded_from_wave` trouvait un total de vague à 0.
   - Cause : `LevelChunk#setBlockEntity` appelle `setLevel()` sur le block entity qui arrive,
     **puis** `setRemoved()` sur celui qu'il remplace, à la même position.
     `SpawnerBlockEntity#setRemoved` retirait alors d'`ACTIVE_SPAWNERS` le spawner tout juste
     enregistré.
   - C'est la vraie cause du « total bloqué à 0 » du 2026-09-12, qui n'avait été que contourné.
   - Correctif : le retrait est différé, et n'a lieu que s'il n'y a plus de spawner à cette
     position.
   - Même patron suspect sur `ManaChestBlockEntity` (`ACTIVE_MANA_CHESTS`) : noté dans
     `05-etat-et-problemes-connus.md`, pas corrigé ici.
2. **Version du protocole réseau `"1"` → `"2"`** (`ModNetworking`). Deux paquets changent de
   forme ; un client resté sur l'ancienne version est refusé proprement à la connexion, au lieu
   d'échanger des paquets incompatibles. Conséquence pratique : un client à jour ne peut plus
   rejoindre le serveur du homelab tant que celui-ci tourne l'ancien jar.
3. **« Valider » dans l'écran du spawner conserve une ligne d'ennemi inconnu déjà présente.**
   Le serveur refusait tout identifiant inconnu, ce qui aurait effacé cette ligne (et contredit
   le « jamais de donnée perdue » du §6). Il accepte maintenant un identifiant inconnu s'il était
   déjà dans ce spawner ; un client ne peut toujours pas en introduire un nouveau.
4. **Un `/reload` recalcule le total de vague** des niveaux qui ont des spawners. Ajouter ou
   retirer un ennemi utilisé en pleine vague ne laisse pas un total périmé.

**Vérifié hors jeu :**
- `./gradlew build` ;
- `tools/verifier-dist.py` : aucune classe cliente dans le graphe serveur ;
- les 7 gametests passent (`./gradlew runGameTestServer`), dont 4 nouveaux :
  `enemy_legacy_format`, `enemy_shipped_values`, `enemy_unknown_excluded_from_wave`,
  `enemy_export_conversion` ;
- le serveur de gametest logge bien `2 ennemi(s) chargé(s)` au démarrage.

## Gel de l'enum `SpawnableEnemy` jusqu'à la fusion

> **Sur cette branche, l'enum n'existe plus** : il a été remplacé par les JSON. Le gel reste
> valable **sur toutes les autres branches** tant que celle-ci n'est pas fusionnée.

**Décidé avec le joueur le 2026-09-29.** Jusqu'à la fusion de cette migration, **on ne touche
ni à l'ordre ni au contenu de l'enum `init/SpawnableEnemy.java`, sur aucune branche**. Concrètement,
c'est interdit :

- ajouter un ennemi, même en fin de liste ;
- en retirer ou en renommer un ;
- réordonner les constantes ;
- changer l'`EntityType` associé à une constante.

**Pourquoi.** Le joueur continue de construire la première map de campagne pendant ce temps-là.
Ses spawners sont sauvegardés à l'ancien format, par ordinal. À la conversion, ces nombres seront
traduits par la table figée `LegacyEnemyIds` (§5) : `0 → zombie`, `1 → squelette`. Cette table
décrit l'enum **tel qu'il est aujourd'hui**. Si l'enum bouge avant la fusion, un même nombre
voudra dire deux choses selon la date de sauvegarde du fichier, et la conversion donnera le
mauvais monstre, sans erreur ni message. C'est exactement le problème que cette migration doit
supprimer.

**État vérifié le 2026-09-29 :** le fichier est identique sur toutes les branches du dépôt
(`origin/*`). Il n'y a rien à rattraper, et c'est cet état qui fait foi pour `LegacyEnemyIds`.

**Ce qui reste permis :** modifier les valeurs portées par une constante sans toucher à son rang,
par exemple l'XP (`10`/`15`). Les ordinaux n'en dépendent pas. Mais la règle « aucune valeur de
gameplay ne change » de cette migration s'applique : les JSON reprendront les valeurs du jour de
l'implémentation.

**Après la fusion,** l'enum est supprimé, donc la question ne se pose plus. La table
`LegacyEnemyIds`, elle, reste figée **pour toujours** : c'est elle qui permet de relire les
anciens fichiers.

## Pourquoi

Aujourd'hui, un ennemi est une constante de l'enum fermé `init/SpawnableEnemy.java`. Chaque
spawner le sauvegarde par son **ordinal** (`"Enemy": 0` = zombie, `1` = squelette) :

- dans le NBT des spawners des mondes sauvegardés ;
- dans les `.nbt` de maps, puisqu'une structure embarque le NBT de ses block entities ;
- dans deux paquets réseau (`SpawnerConfigPayload`, `ScoreGainPayload`).

Le jour où l'ordre de l'enum change (un ennemi inséré au milieu, par exemple), toutes les maps
déjà construites se mettent à faire apparaître le mauvais monstre, sans erreur ni message.
Passer à des identifiants (`dungeon_defenders:zombie`) supprime ce risque. Il faut le faire
**avant** que les maps de campagne se multiplient.

## 1. Format JSON d'un ennemi

**Emplacement :** `data/<namespace>/dungeon_defenders/enemy/<nom>.json`. L'identifiant de
l'ennemi est `<namespace>:<nom>`. Les deux ennemis actuels vivent donc dans
`src/main/resources/data/dungeon_defenders/dungeon_defenders/enemy/`. Le double
`dungeon_defenders` est normal : le premier est le namespace du pack, le second le dossier propre
au mod. C'est la convention vanilla (`data/<ns>/<type>/…`), qui évite qu'un autre mod entre en
collision avec un dossier `enemy/` générique.

**Champs :**

| Champ | Type | Obligatoire | Rôle | Remplace |
|---|---|---|---|---|
| `entity_type` | identifiant d'`EntityType` | oui | Le mob réellement créé par le spawner | `SpawnableEnemy#entityType` |
| `icon` | identifiant d'`Item` | oui | Icône dans le renderer du spawner, l'écran de config et le popup de score | `SpawnableEnemy#spawnEggItem` |
| `xp_value` | entier ≥ 0 | oui | XP joueur + score de la carte à chaque mort | `SpawnableEnemy#xpValue` |
| `order` | entier | non (défaut 0) | Ordre dans le bouton « cycler » de l'écran de config (puis identifiant pour départager) | ordre de déclaration de l'enum |
| `behavior` | objet `{ "type": …, …paramètres }` | oui | Le type de comportement IA et ses réglages (voir §3) | le test `instanceof AbstractSkeleton` de `ModEvents#onMonsterSpawn` |

**Nom affiché :** pas de champ dédié. La clé de traduction se déduit de l'identifiant,
`<namespace>.enemy.<nom>`. Ça donne `dungeon_defenders.enemy.zombie`, exactement la clé utilisée
aujourd'hui, donc rien à changer dans `en_us.json`/`fr_fr.json`. Un pack tiers sans traduction
retombe sur l'identifiant brut (`translatableWithFallback`, comme les noms de pack de maps).

**Les deux fichiers livrés, avec les valeurs actuelles exactes :**

`data/dungeon_defenders/dungeon_defenders/enemy/zombie.json`

```json
{
  "entity_type": "minecraft:zombie",
  "icon": "minecraft:zombie_spawn_egg",
  "xp_value": 10,
  "order": 0,
  "behavior": {
    "type": "dungeon_defenders:melee_priority"
  }
}
```

`data/dungeon_defenders/dungeon_defenders/enemy/skeleton.json`

```json
{
  "entity_type": "minecraft:skeleton",
  "icon": "minecraft:skeleton_spawn_egg",
  "xp_value": 15,
  "order": 1,
  "behavior": {
    "type": "dungeon_defenders:ranged_crystal",
    "damage_per_shot": 3,
    "ticks_between_shots": 20,
    "shoot_range": 10.0
  }
}
```

Correspondance avec le code actuel :

- `xp_value` 10/15 : `SpawnableEnemy.ZOMBIE`/`SKELETON`.
- `order` 0/1 : l'ordre de déclaration de l'enum. Le bouton « cycler » et le choix par défaut
  d'une nouvelle ligne (`firstUnusedEnemy()`, qui retombe sur `values()[0]`, le zombie) restent
  identiques. Sans ce champ, un tri alphabétique placerait `skeleton` avant `zombie`.
- `damage_per_shot` 3, `ticks_between_shots` 20, `shoot_range` 10.0 :
  `RangedAttackEterniaCrystalGoal.DEFAULT_*`.

**Volontairement absent de l'étape 1 :** PV, vitesse, dégâts de mêlée. Aujourd'hui aucun ennemi
ne modifie ses attributs : ils gardent les valeurs vanilla (20 PV pour le zombie comme pour le
squelette). Un champ `health` absent voudra toujours dire « ne pas toucher à l'attribut ». Ces
champs pourront être ajoutés plus tard, en optionnel, sans casser les fichiers existants.

## 2. Mécanisme de chargement

### Choix : un reload listener serveur, pas un registre datapack

**Confirmé : ça se recharge avec `/reload`, sans redémarrer.** Vérifié dans les sources
NeoForge 26.1.2.76 et dans le jar du jeu :

- **Le registre datapack (`DataPackRegistryEvent.NewRegistry`) est écarté.** C'est la solution
  « officielle » la plus simple, avec synchro client gratuite. Mais les registres dynamiques ne
  sont chargés qu'au chargement du monde, puis gelés. La javadoc
  d'`AddServerReloadListenersEvent#getRegistryAccess` le dit explicitement : *« All built-in and
  dynamic registries are loaded and frozen by this point »*. Un `/reload` ne les relirait donc
  pas, ce qui contredit le premier intérêt de la démarche (équilibrer sans redémarrer).
- **Retenu :** un `SimpleJsonResourceReloadListener<EnemyDefinition>` (classe vanilla, présente
  dans `minecraft_26.1.2`, constructeur `(Codec<T>, FileToIdConverter)`), ajouté via
  `AddServerReloadListenersEvent#addRetainedListener`. Cet événement est déclenché **à chaque
  reload** : au démarrage du serveur, puis à chaque `/reload`.
- Un JSON invalide est déjà géré par vanilla (`scanDirectory`). L'erreur est loggée
  (`Couldn't parse data file '…' from '…'`) et le fichier est ignoré, les autres se chargent
  normalement. Pas de crash.

**Ce que `/reload` change, et ce qu'il ne change pas :**

- Les **nouveaux** spawns utilisent immédiatement les nouvelles définitions : XP, icône, type et
  paramètres de comportement.
- Les monstres **déjà vivants** gardent l'IA reçue à leur apparition. Les goals sont construits
  à ce moment-là, avec leurs paramètres. C'est acceptable pour un outil d'équilibrage, et ce
  sera documenté.
- Les spawners ne sont pas touchés : ils ne stockent qu'un identifiant, résolu au moment de
  chaque spawn (voir §5).

### Est-ce que ça réutilise la logique des maps ?

**Non pour le mécanisme, oui pour la convention.**

- Les maps ne passent pas par un reload listener. `MapRegistry#discover` interroge le
  `StructureTemplateManager` vanilla (`listTemplates()`) et recalcule la liste à chaque ouverture
  de l'écran de choix. C'est propre aux **structures** `.nbt` et ne sait pas lire des JSON
  arbitraires. Le reprendre pour les ennemis n'aurait pas de sens.
- La convention est partagée : le namespace fait office de pack, et la découverte se fait dans
  tous les namespaces, jar du mod, jars tiers et datapacks confondus. Un pack de maps tiers
  pourra donc aussi livrer ses propres ennemis dans
  `data/<son_namespace>/dungeon_defenders/enemy/`, sans rien déclarer.
- `/dd_export` n'emballera **pas** les JSON d'ennemis : ils ne se créent pas en jeu, contrairement
  aux maps. C'est hors périmètre de l'étape 1.

### Où vivent les données côté serveur

Nouvelle classe `init/EnemyRegistry.java` : une map `Identifier → EnemyDefinition` remplacée en
bloc à chaque reload, plus deux index dérivés.

- `byId(Identifier)` : pour les spawners et le réseau.
- `byEntityType(EntityType<?>)` : pour l'XP et le choix de comportement à l'apparition.
- `sorted()` : trié par `order` puis identifiant, pour l'écran de config.

**Point d'attention, le solo :** en solo, le serveur intégré et le client tournent dans la même
JVM. Les données serveur (`EnemyRegistry`) et la copie client (voir §4) doivent donc être **deux
stockages distincts**. Partager un seul `static` masquerait tout bug de synchro en solo, qui
n'apparaîtrait qu'en multijoueur, sur le serveur dédié.

## 3. « Types de comportement en Java, paramètres en JSON »

Le code définit un petit nombre de **types de comportement**. Chaque JSON choisit le sien et en
règle les paramètres. Aucune logique d'IA n'est écrite en JSON.

**Deux types, qui reproduisent exactement la répartition actuelle** (aujourd'hui décidée par
`instanceof AbstractSkeleton` dans `ModEvents#onMonsterSpawn`) :

| `type` | Goals ajoutés (priorité) | Paramètres JSON | Équivalent actuel |
|---|---|---|---|
| `dungeon_defenders:melee_priority` | `AttackPriorityTargetGoal` (0), `SeekEterniaCrystalGoal` (1) | aucun | branche « tout le reste » |
| `dungeon_defenders:ranged_crystal` | `RangedAttackEterniaCrystalGoal` (1), `SeekEterniaCrystalGoal` (2) | `damage_per_shot`, `ticks_between_shots`, `shoot_range` (tous optionnels, défauts = constantes actuelles) | branche `AbstractSkeleton` |

**Ce qui reste commun à tous les types**, appliqué avant le type et jamais configurable par
ennemi à l'étape 1 :

- le relèvement de `FOLLOW_RANGE` à 128 ;
- la garde anti-doublon (rechargement de chunk) ;
- l'exclusion du mannequin d'entraînement.

**Ce qui reste hors JSON exprès, pour ne rien changer :**

- Les dégâts de mêlée restent lus dans `Config.DAMAGE_PER_HIT` (réglage serveur de
  `dungeon_defenders-common.toml`, 5 par défaut), comme aujourd'hui.
- Les vitesses (1.2, 1.0) et `ACCEPTED_DISTANCE` (2.1) restent des constantes des goals.

Les rendre configurables par ennemi est une décision de gameplay séparée, pour plus tard.

**Mise en œuvre :**

- Une interface scellée `EnemyBehavior` (méthode `applyGoals(PathfinderMob)`) avec un record par
  type.
- Un codec de dispatch sur `type` (`Codec.STRING`/`Identifier` → `MapCodec`), dans une simple
  table Java. Pas de registre NeoForge pour l'instant : on le fera le jour où un addon devra
  ajouter ses propres types.
- Un `type` inconnu fait échouer le parse du fichier, donc log vanilla + fichier ignoré (§6).

**Résolution à l'apparition d'un monstre (`ModEvents#onMonsterSpawn`) :**

1. Définition trouvée via `EnemyRegistry.byEntityType(monster.getType())` → son `behavior`
   ajoute les goals.
2. Aucune définition → **l'ancienne règle est conservée telle quelle en repli**
   (`instanceof AbstractSkeleton` → distance, sinon mêlée). Indispensable pour « aucune valeur
   ne change » : aujourd'hui, **tout** `Monster` reçoit l'IA, y compris un zombie `/summon`, un
   stray ou un wither skeleton (sous-classes d'`AbstractSkeleton`). Sans ce repli, ils perdraient
   leur IA.

**Limite assumée :** la recherche par `EntityType` suppose une définition par type de mob. Deux
JSON avec le même `entity_type` (un futur « zombie élite », par exemple) donneront un
avertissement au chargement : le premier par `order` gagne pour l'IA et l'XP. Le jour où ce cas
existera vraiment, il faudra marquer le mob à sa création avec l'identifiant de son ennemi
(`EntityType#create` + attachment + `addFreshEntity` au lieu d'`EntityType#spawn`, parce que
`EntityJoinLevelEvent` se déclenche *pendant* `spawn`, avant qu'on puisse poser quoi que ce soit
dessus). C'est hors périmètre de l'étape 1, noté pour éviter la surprise.

## 4. Synchronisation client

**Qui a besoin des définitions côté client, aujourd'hui en lisant l'enum directement :**

| Classe (client) | Utilise | Pour |
|---|---|---|
| `SpawnerConfigScreen` | liste ordonnée, icône, nom | bouton « cycler », composition par défaut, ligne ajoutée |
| `SpawnerBlockEntityRenderer` | icône, nom | aperçu de composition au-dessus du spawner |
| `ScoreGainOverlay` (via `DungeonDefendersModClient#handleScoreGain`) | icône | popup « +10 » avec l'œuf du monstre tué |
| `MobHealthBarRenderer` | `entity_type` | filtre en dur `ZOMBIE`/`SKELETON`, sans lien avec l'enum aujourd'hui |

**Mécanisme :**

- Nouveau paquet S2C `EnemyDefinitionsPayload`. Il porte une liste réduite aux champs utiles au
  client (identifiant, `entity_type`, `icon`, `order`) ; le `behavior` reste côté serveur.
- Il est envoyé depuis `OnDatapackSyncEvent`. Vérifié dans les sources NeoForge : cet événement
  se déclenche *« when a player joins the server or when the reload command is ran »*.
  `getRelevantPlayers()` donne le joueur qui arrive, ou tout le monde après un `/reload`. Un
  `/reload` met donc à jour les écrans et icônes de tous les clients connectés, sans reconnexion.
- La copie client vit dans une classe **client-only** (`client/ClientEnemyDefinitions.java`),
  remplacée en bloc à chaque réception. Le handler est enregistré dans `DungeonDefendersModClient`,
  comme `ScoreGainPayload`. Le paquet lui-même ne nomme aucune classe cliente, pour respecter la
  règle du serveur dédié ; `tools/verifier-dist.py` le vérifiera.

**Paquets existants qui changent de forme** (même version de mod des deux côtés, donc aucune
compatibilité réseau à assurer) :

- `SpawnerConfigPayload.Entry(int enemyOrdinal, …)` → `Entry(Identifier enemy, …)`. Le contrôle
  serveur de `ModNetworking#handleSpawnerConfig` passe d'un test de bornes d'ordinal à « cet
  identifiant existe dans `EnemyRegistry` ».
- `ScoreGainPayload(…, int enemyOrdinal)` → `Optional<Identifier> enemy`. `NO_ENEMY = -1`
  disparaît au profit d'`Optional.empty()`.

**`MobHealthBarRenderer` :** le filtre devient « `entity_type` présent dans les définitions
synchronisées ». Avec les deux JSON livrés, c'est exactement {zombie, squelette}, donc rien ne
change visuellement. Un futur ennemi aura automatiquement sa barre de vie.

## 5. Migration ordinal → identifiant

### Principe commun : lecture compatible, écriture au nouveau format, conversion loggée

**Le champ `"Enemy"` du codec `SpawnEntry.CODEC` accepte les deux formes :**

- un **entier** (ancien format), converti via une table figée ;
- une **chaîne** (nouveau format), lue telle quelle comme identifiant.

Techniquement : `Codec.either(Codec.INT, Identifier.CODEC)`.

**Table figée `LegacyEnemyIds`**, écrite en dur en Java :

- `0 → dungeon_defenders:zombie`
- `1 → dungeon_defenders:skeleton`

Cette table ne doit **jamais** changer ni dépendre des JSON. Elle décrit ce que voulaient dire
les anciens fichiers, pas ce qui existe aujourd'hui.

- **Écriture :** toujours la chaîne. Un fichier ne revient jamais à l'ancien format.
- **Ordinal hors table** (ex. `7`, fichier corrompu ou bricolé) : converti en identifiant
  `dungeon_defenders:legacy_unknown_7` et traité comme un ennemi inconnu (§6). Rien n'est perdu
  et ça ne plante pas.
- **Log :** le codec ne connaît pas la position du spawner. `SpawnEntry` garde donc un marqueur
  transitoire « converti depuis l'ordinal N ». `SpawnerBlockEntity#loadAdditional` logge alors
  une ligne `INFO` par entrée convertie, par exemple :
  `Spawner à (10003, 65, -38) : ennemi n°0 (ancien format) converti en dungeon_defenders:zombie`.

### Cas A : spawners des mondes déjà sauvegardés

- Au chargement du chunk, `loadAdditional` lit l'ancien format, convertit et logge.
- Le block entity doit ensuite être marqué modifié, pour que le chunk soit **réécrit au nouveau
  format** à la prochaine sauvegarde. Détail d'implémentation : `setChanged()` n'a aucun effet
  dans `loadAdditional` au chargement d'un chunk, parce que la `Level` n'est pas encore posée.
  Le marqueur est donc consommé dans `setLevel(ServerLevel)`, qui appelle `setChanged()` à ce
  moment-là.
- Résultat : chaque spawner d'un monde existant est converti **une seule fois**, loggé une seule
  fois, puis sauvegardé au nouveau format.

### Cas B : fichiers `.nbt` de maps

Une structure garde le NBT brut de ses blocs. `StructureTemplate` ne crée des block entities
qu'au moment de poser la structure. Poser une map passe donc par le même `loadAdditional` que le
cas A : **la lecture est déjà compatible, rien de plus à coder pour qu'une ancienne map
fonctionne.** Mais le fichier source n'est jamais réécrit. Le log se répéterait donc à **chaque
partie** jouée sur cette map, et une vieille map publiée garderait l'ancien format pour toujours.
Chaque famille de fichiers est donc traitée à la source.

**B1. Maps embarquées dans le jar du mod.** Inventaire vérifié : seuls ces fichiers contiennent
des spawners.

| Fichier | Spawners | Traitement |
|---|---|---|
| `data/dungeon_defenders_test/structure/map/test_arena.nbt` | 1 | Régénéré par `tools/generer-map-de-test.py` |
| `data/dungeon_defenders_test/structure/map/couloir_ecart_ia.nbt` | 1 | Régénéré par `tools/generer-map-ecart-ia.py` |
| `data/dungeon_defenders_test/structure/map/detour_ia.nbt` | 1 | Régénéré par `tools/generer-map-detour-ia.py` |
| `ruins_sanctuary.nbt` (branche `feature/map-ruins-sanctuary`, PR #41, pas encore ici) | 3 | Même correction du générateur **sur sa branche**, à faire au moment de la fusion |
| `tavern.nbt`, `gametest/empty.nbt` | 0 | Rien |

- Les trois générateurs écrivent aujourd'hui `"Enemy": 0` ou `1` : ils écriront la chaîne.
- Vérification : un script relit ancien et nouveau `.nbt` et confirme que **seul** le champ
  `Enemy` diffère (même taille, même palette, mêmes positions, mêmes autres valeurs). C'est la
  preuve qu'aucune valeur de gameplay n'a bougé.

**B2. Maps créées en jeu** (dossier `generated/` de la sauvegarde). Les maps d'un pack tiers
sont corrigées à l'export (procédure au §7).

**Décidé le 2026-09-29 pour la première map de campagne,** celle que le joueur construit en ce
moment :

- Le joueur **continue sans attendre la migration**. Ses spawners sont sauvegardés à l'ancien
  format ; ça fonctionne grâce à la lecture compatible.
- Il la livrera via `map-handoff/`, comme la taverne.
- Je la convertirai à l'intégration dans `src/main/resources/data/dungeon_defenders/`. La
  procédure est détaillée au §7, point 5.
- Condition : **le gel de l'enum** (voir en tête de document) doit être respecté jusque-là.

**B3. Maps déjà exportées et publiées par quelqu'un d'autre.** Ces fichiers sont hors de notre
portée : la lecture compatible les garde jouables indéfiniment. Seul effet : une ligne de log
par spawner à chaque partie. C'est acceptable, et même utile pour signaler à leur auteur qu'un
réexport est conseillé.

### Ce que la migration touche aussi

- La composition par défaut d'un spawner neuf (`SpawnerBlockEntity`, 15 zombies + 5 squelettes)
  et celle de l'écran de config (`SpawnerConfigScreen`, mêmes chiffres) passent aux
  identifiants, avec les mêmes nombres.
- `SpawnableEnemy` est supprimé.
- `DEFAULT_XP_VALUE = 5` (monstre sans définition) est conservé dans `EnemyRegistry`.

## 6. Identifiant d'ennemi inconnu

C'est le cas d'un identifiant qui n'existe plus : pack retiré, JSON supprimé ou cassé, faute de
frappe dans une map. La règle : **log clair, jamais de crash, jamais de donnée perdue, jamais
de vague bloquée.**

| Où | Comportement |
|---|---|
| Entrée de spawner (monde ou `.nbt`) | L'identifiant est **conservé tel quel**. Il est réécrit à la sauvegarde, donc remettre le pack suffit à tout faire revenir. L'entrée ne fait simplement rien spawner. |
| Log correspondant | Un seul `WARN` par couple (spawner, identifiant), pas à chaque tick : `Spawner à (x, y, z) : ennemi inconnu dungeon_defenders:goblin, ignoré (pack manquant ou JSON invalide ?)`. Réarmé à chaque `/reload`, pour signaler à nouveau si le problème persiste. |
| `PhaseTransitions#recomputeWaveEnemiesTotal` | L'entrée inconnue est **exclue du total**. Sinon le total compterait des monstres qui n'apparaîtront jamais et la vague ne se terminerait pas : exactement le symptôme corrigé le 2026-09-12. |
| `SpawnerBlockEntityRenderer` | Icône de repli (barrière vanilla) + l'identifiant brut en texte, pour que le créateur voie le problème en regardant le spawner. |
| `SpawnerConfigScreen` | La ligne affiche l'identifiant brut. Le bouton « cycler » permet de le remplacer par un ennemi connu. |
| `ModNetworking#handleSpawnerConfig` | Un identifiant inconnu envoyé par un client est refusé, comme aujourd'hui un ordinal hors bornes. |
| Chargement des JSON | Fichier invalide (champ manquant, `entity_type`/`icon` inexistant, `type` de comportement inconnu) : log vanilla + fichier ignoré, les autres se chargent. Deux fichiers avec le même `entity_type` : `WARN` explicite (voir la limite du §3). **Aucune** définition chargée : un `ERROR` unique ; les spawners ne font rien apparaître, le serveur tourne. |
| `ScoreGainPayload` côté client | Identifiant absent de la copie client : popup sans icône, comme aujourd'hui avec `NO_ENEMY`. |

## 7. Réexporter les maps existantes avec `/dd_export`

**Constat, vérifié dans `MapExporter#export`.** L'export copie les `.nbt` du dossier
`generated/` de la sauvegarde **octet pour octet** (`Files.readAllBytes`). Il ne prend que les
maps créées en jeu (`fromWorld`) du namespace demandé. Sans rien changer, un export après la
migration republierait donc l'ancien format tel quel.

**Décidé (2026-09-29) : `/dd_export` convertit, et la lecture reste compatible.** Les deux
mécanismes se complètent. La lecture compatible garde jouable tout ancien fichier, y compris
ceux publiés avant la migration. L'export garantit que tout ce qui est publié *après* est au
nouveau format.

`MapExporter` ne recopie plus le fichier brut. Il le décompresse,
convertit les champs `"Enemy"` numériques des blocs `dungeon_defenders:spawner` avec la même
table `LegacyEnemyIds`, puis réécrit le résultat dans le jar. Chaque conversion est loggée
(`Export <ns> : map <id>, spawner (x, y, z) relatif : ennemi n°0 converti en …`). La commande
affiche en plus un compteur dans son message de succès. Le reste du fichier est laissé
intact, octet pour octet dans son contenu NBT.

**Procédure, une fois la branche fusionnée et le mod à jour :**

1. Lancer le monde qui contient les maps à publier (ou le serveur dédié).
2. Pour chaque pack : `/dd_export <namespace>`, par exemple `/dd_export dungeon_defenders` pour
   la campagne.
3. Lire le message de succès : il indique combien d'ennemis ont été convertis. Détail par
   spawner dans `logs/latest.log`.
4. Vérifier le jar produit : relancer avec ce jar à la place des `.nbt` du monde, jouer la map,
   et contrôler que **aucune** ligne « ancien format converti » n'apparaît dans les logs. C'est
   la preuve que le fichier publié est bien au nouveau format.
5. **La campagne livrée dans le jar du mod** (la première map du joueur, pas un pack tiers) ne
   passe pas par `/dd_export`. Elle passe par `map-handoff/`, puis est intégrée dans
   `src/main/resources/data/dungeon_defenders/structure/map/`. Conversion à l'intégration :
   1. Relire le fichier reçu et lister chaque spawner avec ses valeurs `"Enemy"`.
   2. Convertir avec la même table `LegacyEnemyIds`. Une valeur hors table (ni `0` ni `1`)
      signalerait une violation du gel de l'enum : dans ce cas, **arrêt et question au
      joueur**, pas de conversion à l'aveugle.
   3. Vérifier que **seul** le champ `Enemy` diffère entre l'original et le converti, avec le
      même contrôle qu'au cas B1.
   4. Si l'intégration a lieu **avant** la fusion de cette branche, garder le fichier à
      l'ancien format (le code en place ne lit que les nombres) et le convertir dans le cadre
      de la migration, avec les trois maps de test.

**Le fichier source dans `generated/` reste à l'ancien format** tant que la map n'est pas
resauvegardée au bloc de structure. Ce n'est pas grave : il reste lisible, et c'est l'export
qui publie. Le resauvegarder après une partie ne suffirait d'ailleurs pas : la version posée à
`MAP_POS` est une copie, le bloc de structure du créateur pointe vers son lieu de construction.

## Fichiers touchés (prévision)

- **Créés :**
  - `init/EnemyDefinition.java` (record + codec)
  - `init/EnemyBehavior.java` (interface scellée + 2 records + codec de dispatch)
  - `init/EnemyRegistry.java` (reload listener + index)
  - `init/LegacyEnemyIds.java`
  - `network/EnemyDefinitionsPayload.java`
  - `client/ClientEnemyDefinitions.java`
  - les deux JSON du §1
- **Modifiés :**
  - `SpawnerBlockEntity` (codec, défauts, log, marqueur de conversion)
  - `ModEvents` (résolution du comportement + repli, XP)
  - `PhaseTransitions` (exclusion des inconnus)
  - `SpawnerConfigPayload`, `ScoreGainPayload`, `ModNetworking`
  - `SpawnerConfigScreen`, `SpawnerBlockEntityRenderer`, `ScoreGainOverlay`,
    `DungeonDefendersModClient`, `MobHealthBarRenderer`
  - `MapExporter`
  - les trois générateurs Python + les trois `.nbt` régénérés
- **Supprimé :** `init/SpawnableEnemy.java`
- **Docs :** `doc/01`, `02`, `05`, `06` au fil de l'implémentation, selon la règle du projet.

## Vérification prévue

- `./gradlew build -x test` + `tools/verifier-dist.py` : aucune classe cliente dans le graphe
  serveur (le nouveau paquet et le registre sont communs, la copie client ne l'est pas).
- **Gametests** (infra existante, `DungeonDefendersGameTests`), sur de la logique pure :
  - `"Enemy": 0` → `dungeon_defenders:zombie`, marqué converti ;
  - `"Enemy": "dungeon_defenders:skeleton"` → lu tel quel, non marqué ;
  - `"Enemy": 7` → `legacy_unknown_7` ;
  - réencodage → toujours une chaîne ;
  - total de vague qui exclut un identifiant inconnu.
- **Script de comparaison des `.nbt` régénérés :** seul `Enemy` diffère.
- **Checklist en jeu dans `doc/06-a-tester.md` :**
  - un monde existant avec spawners (logs de conversion une seule fois, puis plus rien) ;
  - `/reload` après modification de `xp_value` (nouveau gain au prochain kill, sans
    redémarrage) ;
  - JSON cassé (log, pas de crash) ;
  - map avec ennemi inconnu (vague qui se termine quand même) ;
  - serveur dédié (icônes et écran de config corrects pour un client distant) ;
  - `/dd_export` sur une map créée avant la migration.

## Décisions (2026-09-29)

1. **Emplacement de la doc :** tout ce qui concerne le passage en data-driven vit dans
   `doc/data-driven/`, avec le reste de la documentation du projet. Le premier jet avait été
   créé dans `docs/` par erreur et a été déplacé.
2. **Export et lecture :** `/dd_export` convertit les anciens fichiers au nouveau format (§7), et
   la lecture reste compatible avec l'ancien format partout (§5). L'un ne remplace pas l'autre.
3. **Première map de campagne :** le joueur continue de la construire sans attendre, puis me la
   livre pour conversion à l'intégration (§5 B2, §7 point 5). D'ici la fusion, **gel complet
   de l'enum `SpawnableEnemy`** : ni l'ordre, ni le contenu (voir la section dédiée en tête de
   document).

Plus aucune question ouverte : le plan est prêt à être implémenté, sur feu vert du joueur.
