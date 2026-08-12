# 5. État du projet & problèmes connus

État à la version `0.0.1`. Le build (`./gradlew build`) **passe** — c'est aussi ce que
vérifie la CI.

## Ce qui est implémenté

- ✅ Bloc `eternia_crystal` + son item, hitbox 1×3×1, très résistant.
- ✅ Block entity avec PV persistants (100 par défaut) **et synchronisés vers le client**.
- ✅ Destruction du bloc et message « Game Over » à 0 PV.
- ✅ IA : tout `Monster` (zombie, squelette...) qui rejoint le monde converge sur le cristal
  dans un rayon de 16 blocs et lui inflige 5 PV/s (`ModEvents.onMonsterSpawn`, généralisé
  au-delà des zombies).
- ✅ Onglet créatif dédié.
- ✅ Renderer de barre de vie 3D au-dessus du cristal (API `submit` de 26.1).
- ✅ Modèle, blockstate, loot table, tags d'outil, traductions `en_us` et `fr_fr`.
- ✅ CI GitHub Actions.
- ✅ `neoforge.mods.toml` renseigné avec les vraies métadonnées (`mod_authors`,
  `mod_description` ajoutés à `replaceProperties` dans `build.gradle`).
- ✅ Bloc `spike_trap` + son item : 2 PV de dégâts à tout `Monster` qui marche dessus
  (`stepOn`), cooldown de 1 s par entité. Modèle, blockstate, loot table, tag `mineable/pickaxe`,
  traductions `en_us`/`fr_fr`, onglet créatif.
- ✅ Mana du joueur : data attachment `mana` (persistant, synchronisé), maximum par défaut de
  100, affiché en HUD via `ManaOverlay` — losange en bas à gauche de l'écran (`DiamondGauge`,
  couleurs plates), très provisoire. Testable en jeu avec l'item `mana_test_wand` (clic droit
  = -10 mana).
- ✅ Vie du joueur : maximum vanilla porté de 20 à 100 (`ModEvents.onPlayerJoin`), affichée en
  HUD via `HealthOverlay` — losange juste à droite de celui du mana, même style.
- ✅ Expérience custom du joueur : data attachment `experience` (persistant, synchronisé),
  démarre à `0/100` (contrairement au mana/à la vie qui démarrent pleins), affichée en HUD
  via `ExperienceOverlay` — barre horizontale tout en bas, sous les losanges mana/vie. Sans
  rapport avec l'XP vanilla. Le groupe des trois est décrit dans
  [02-gameplay.md](02-gameplay.md#le-groupe-bas-gauche--mana-vie-expérience).
- ✅ Vague en cours : data attachment `current_wave` sur la `Level` (persistant, synchronisé,
  démarre à 1), affichée en haut à droite (`Vague X/5`) via `WaveOverlay` — texte seul, pas de
  jauge. Aucun déroulement de vagues n'existe encore.
- ✅ Progression de la vague : `wave_enemies_killed`/`wave_enemies_total` (mêmes garanties que
  `current_wave`), affichée en grande barre centrée tout en haut de l'écran via
  `WaveEnemiesOverlay` (jauge orange, texte `Ennemis : X/Y` superposé au centre) — la zone la
  plus visible du HUD, comme dans le jeu de référence. Démarre à `0/10`, rien ne fait encore
  varier ni les tués ni le total.
- ✅ Phase de la partie : data attachment `game_phase` sur la `Level` (ordinal de l'enum
  `GamePhase` : `BUILD`/`COMBAT`), démarre en `BUILD`, affichée juste sous la rangée
  vague/ennemis via `PhaseOverlay` (`Phase : Construction`). Aucune transition n'existe
  encore.
- ✅ Score de la carte : data attachment `score` sur la `Level` (persistant, synchronisé,
  démarre à 0), affiché tout en bas centre de l'écran via `ScoreOverlay` (`Score : X`, texte
  seul). Censé correspondre à l'expérience gagnée sur la carte en cours, mais distinct de
  `experience` (qui elle persiste au-delà d'une carte) — rien ne l'alimente encore.
- ✅ Nom et niveau du personnage : `character_name` (`String`, distinct du pseudo Minecraft
  mais initialisé avec, faute de mieux) et `level` (`Integer`, démarre à 1) — deux data
  attachments sur le joueur, persistants, synchronisés. Affichés juste au-dessus du score via
  `CharacterOverlay` (`Nom - niv X`). Rien ne fait encore varier ni l'un ni l'autre.
- ✅ 4 emplacements de compétences (soin sur soi, sort 1, sort 2, réparation de tour) en bas à
  gauche via `AbilitySlotsOverlay`, juste à droite des losanges mana/vie, dans cet ordre —
  fond en rond (`CircleSlot`), purement visuel : pas de clic, pas de cooldown, pas d'icône.
  Voir "Ce qui reste" ci-dessous.
- ✅ HUD vanilla masqué (cœurs, faim, expérience, hotbar) au profit d'une interface custom —
  voir [02-gameplay.md](02-gameplay.md#le-hud-vanilla-masqué).
- ✅ Bloc `spawner` : premier vrai morceau de gameplay (pas juste du HUD). Fait apparaître des
  zombies et des squelettes pendant la phase de combat via l'algorithme de spawn pondéré du
  plan Excel du joueur (voir
  [02-gameplay.md](02-gameplay.md#le-spawner--blockspawnerblockjava)). Le nombre de base de
  chaque type sert aussi de plafond pour la vague (une fois atteint, ce type est sauté), et
  est mis à l'échelle par `DifficultyScaling` (difficulté × vague). Configurable par spawner
  (intervalle, rayon de spawn, plage de vagues, nombre de base par type). Shift + clic droit =
  harnais de test qui bascule `BUILD`/`COMBAT`. Incrémente aussi
  `ModAttachments.WAVE_ENEMIES_KILLED` via un nouveau handler `LivingDeathEvent`.
- ✅ Squelette ajouté comme deuxième ennemi (réutilise `EntityType.SKELETON` vanilla, comme le
  zombie) : cible le cristal comme n'importe quel `Monster`, et sort du spawner. Aucune
  spécificité de comportement (tir à distance, etc.) pour l'instant — voir "Ce qui reste".
- ✅ Difficulté de la partie : data attachment `difficulty` sur la `Level` (ordinal de l'enum
  `GameDifficulty` : `EASY`/`NORMAL`/`HARD`), démarre à `NORMAL` — censée être choisie au
  lancement de la map, mais aucun écran pour le faire n'existe encore.
- ✅ Écran de configuration du spawner (premier GUI custom du mod) : clic droit sans shift sur
  un `SpawnerBlock` ouvre `SpawnerConfigScreen`, sans slot ni item. Intervalle, rayon, vague
  de début/fin, et une **liste dynamique** de composition (ajouter/retirer un ennemi, cycler
  son type parmi `init/SpawnableEnemy.java`, régler son nombre de base) — plus la liste figée
  zombie/squelette de la première version. Réseau custom C2S (`SpawnerConfigPayload`, avec une
  liste de longueur variable via `ByteBufCodecs.collection` + `ModNetworking`), revérifié côté
  serveur (portée, existence du bloc, validité de chaque ordinal d'ennemi reçu) avant
  application — appliquée **immédiatement** (pas d'attente de la prochaine vague). Détail
  complet dans
  [02-gameplay.md](02-gameplay.md#lécran-de-configuration--menu-network-clientguiscreenspawnerconfigscreenjava).

## Corrections apportées

Les points suivants figuraient dans la première version de cette page et sont réglés.

| Problème | Correction |
|---|---|
| Le renderer ne compilait pas : `new EterniaCrystalBlockEntityRenderer()` sans argument | enregistrement via `EntityRenderersEvent.RegisterRenderers`, qui fournit le `Context` |
| L'interface `BlockEntityRenderer` avait changé (`render` → `submit`) | portage complet sur le trio `createRenderState` / `extractRenderState` / `submit` |
| `VertexConsumer.vertex(...).endVertex()` n'existe plus | `addVertex(pose, x, y, z).setColor(...)` via `submitCustomGeometry` |
| `RenderType.gui()` (type 2D) utilisé dans le monde | `RenderTypes.debugQuads()` — quads non texturés, translucides, non cullés |
| Barre de vie figée à 100 % côté client | `getUpdatePacket` + `getUpdateTag` + `sendBlockUpdated` |
| NPE potentiel : `this.level.players()` sans garde | sortie anticipée si `level == null` ou côté client |
| Chat inondé à chaque changement de PV | diffusion supprimée ; il ne reste que le message de destruction |
| PV pouvant descendre sous 0 | `Math.max(0, health)` dans le setter |
| `useWithoutItem` renvoyait toujours `SUCCESS` | `SUCCESS` côté client, `PASS` si le block entity est absent |
| Goals de zombie cumulés à chaque `EntityJoinLevelEvent` | test `anyMatch(... instanceof AttackEterniaCrystalGoal)` avant ajout |
| Cadence de frappe basée sur `mob.tickCount` | cooldown porté par le goal, remis à zéro quand le mob s'éloigne |
| Dégradé de couleur faux : jaune à 100 % de PV | `red = (1 - p) * 2` au-dessus de 50 % → vert pur à pleine vie |
| Aucun modèle, blockstate ni loot table | ajoutés ; le bloc se drope et se mine à la pioche en diamant |
| `en_us.json` ne contenait que les clés `examplemod` | remplacées par les vraies clés, plus un `fr_fr.json` |

Le goal des zombies a par ailleurs été sorti de `ModEvents` (classe anonyme) vers
`entity/ai/AttackEterniaCrystalGoal.java`, ce qui rend le test anti-doublon possible.

## Ce qui reste

### Pas de texture dédiée

`models/block/eternia_crystal.json` pointe sur `minecraft:block/diamond_block`. Le bloc est
donc visible et cohérent, mais ressemble à un bloc de diamant. Il faut créer
`textures/block/eternia_crystal.png` et mettre à jour le modèle — idéalement un modèle de
cristal, pas un cube plein, puisque la hitbox fait déjà 3 blocs de haut.

Même situation pour `models/block/spike_trap.json`, qui pointe sur
`minecraft:block/dripstone_block` en attendant `textures/block/spike_trap.png`.

### Le clic droit endommage encore le cristal

`useWithoutItem` retire 10 PV : c'est le harnais de test qui a servi à développer la
mécanique. Il est conservé volontairement (c'est le seul moyen simple de tester sans faire
spawner un zombie), mais il n'a rien à faire dans une version jouable.

### Le mana n'a pas de vraie utilité de gameplay

`ManaTestWandItem` retire 10 de mana au clic droit, mais c'est un harnais de test au même
titre que le clic droit sur le cristal : aucun sort ni capacité réelle ne consomme de mana,
et il n'y a pas de régénération, donc le mana ne remonte jamais une fois dépensé (à part en
se reconnectant, puisque l'attachment n'est pas remis à `MAX_MANA` ailleurs qu'à sa création).
Prochaines étapes logiques : une vraie capacité qui consomme du mana, une régénération
passive (tick côté serveur, borné à `MAX_MANA`), puis retrait de la baguette de test.

### Faim et hotbar masqués sans remplacement

`FOOD_LEVEL` et `HOTBAR` sont masqués (voir
[02-gameplay.md](02-gameplay.md#le-hud-vanilla-masqué)) mais rien ne les remplace encore : le
joueur ne voit plus sa faim, ni l'objet qu'il a en main/sa barre d'objets. Tant qu'un
équivalent custom n'existe pas, c'est une vraie perte d'information en jeu, pas seulement
esthétique — à garder en tête en testant (voir [06-a-tester.md](06-a-tester.md)).

### L'expérience custom n'a pas de vraie utilité de gameplay

Comme le mana à ses débuts : l'attachment `experience` existe et s'affiche, mais rien ne le
fait varier — pas de source de gain, pas de système de niveaux. Reste `0/100` en permanence
tant que ça n'existe pas.

### Le score et le niveau ne sont reliés à rien

`score` et `level` existent et s'affichent, mais rien ne les fait varier, et surtout **rien
ne les relie entre eux ni à `experience`** : tuer un ennemi ne devrait-il pas donner de
l'expérience *et* du score en même temps ? Le score d'une carte devrait-il remettre `level` à
jour selon un barème ? Aucune de ces questions n'est tranchée — les trois attachments
(`experience`, `score`, `level`) coexistent pour l'instant sans logique commune.

### Pas moyen de changer le nom du personnage

`character_name` est bien un champ distinct du pseudo Minecraft (voir
[02-gameplay.md](02-gameplay.md)), mais aucune commande ni écran ne permet de le modifier :
en pratique, il reste égal au pseudo Minecraft du joueur pour toujours, exactement comme si
l'attachment n'existait pas. L'intérêt de l'avoir séparé du compte ne se concrétise que le
jour où une interface de renommage est ajoutée.

### Les emplacements de compétences ne font rien

`AbilitySlotsOverlay` dessine 4 ronds vides : pas d'icône, pas de clic, pas de cooldown, pas
de coût en mana, pas de lien avec un vrai sort ou une vraie action de réparation (qui
n'existent pas non plus côté gameplay). C'est un pur placeholder visuel, en attendant les
images promises pour chaque slot et la logique derrière.

### Les vagues ne se déroulent toujours pas vraiment

`current_wave` existe et s'affiche (`1/5`), mais rien ne le fait avancer : pas de condition
pour passer à la vague suivante, pas de victoire à la vague 5 ni de défaite si le cristal
tombe avant. Le `spawner` fait maintenant apparaître de vrais ennemis et
`wave_enemies_killed` compte vraiment les morts (voir
[02-gameplay.md](02-gameplay.md#le-spawner--blockspawnerblockjava)), mais :

- `game_phase` ne bascule que via le harnais de test (clic droit sur le `SpawnerBlock`) — pas
  de vrai déclencheur de combat, pas de retour automatique à `BUILD` une fois la vague nettoyée ;
- `wave_enemies_total` reste bloqué à sa valeur par défaut (`10`) : personne ne somme les
  poids des spawners actifs de la carte au démarrage du combat ;
- rien ne fait avancer `current_wave` quand `wave_enemies_killed` atteint `wave_enemies_total`.

### Le GUI du spawner ne choisit que parmi une liste fermée d'ennemis (SpawnableEnemy)

`SpawnerConfigScreen` (voir [02-gameplay.md](02-gameplay.md)) permet maintenant d'ajouter et
retirer des lignes de composition librement, et de cycler le type de chaque ligne — mais
uniquement parmi les valeurs d'`init/SpawnableEnemy.java` (`ZOMBIE`, `SKELETON` pour
l'instant), pas n'importe quel mob du jeu. La feuille "Idées" du plan Excel du joueur
prévoyait à l'origine des **slots d'œufs** pour choisir librement n'importe quel type de mob.
Choix assumé ici : une liste fermée plutôt qu'un `EntityType<?>` arbitraire, parce qu'il
n'existe pas de tag vanilla générique "tout ce qui est hostile" dans cette version de
Minecraft (vérifié) — il faudrait de toute façon une forme de liste blanche pour éviter
qu'un joueur puisse faire spawn n'importe quelle entité (villageois, boss, etc.) depuis ce
GUI. Ajouter un ennemi au jeu et le rendre choisissable ici se résume à une entrée dans
`SpawnableEnemy` (une ligne, une clé de traduction) — pas de nouveau blocage architectural
tant qu'on reste dans cette approche liste-fermée.

Le seuil de déclenchement (`SPAWN_THRESHOLD = 20`) reste une constante globale non exposée
dans le GUI, comme décidé avec le joueur (son effet se règle déjà via l'intervalle et le
nombre de base, l'exposer en plus aurait été redondant).

### `game_phase` stocke un ordinal d'enum, pas un nom stable

`ModAttachments.GAME_PHASE` sérialise `GamePhase.ordinal()` (0 pour `BUILD`, 1 pour
`COMBAT`). Si l'ordre des constantes de `GamePhase` change un jour (insertion d'une phase
avant `COMBAT`, par exemple), les sauvegardes existantes se retrouveront avec la mauvaise
phase au chargement. Pas un problème tant qu'on ajoute des valeurs à la fin de l'enum, mais à
garder en tête — voir [02-gameplay.md](02-gameplay.md#la-phase-de-la-partie--clientguiphaseoverlayjava).

### Le HUD du mana, de la vie et de l'expérience n'a toujours pas de vraie texture

`ManaOverlay`/`HealthOverlay` (via `DiamondGauge`) et `ExperienceOverlay` dessinent des formes
en couleurs plates (`guiGraphics.fill` empilés), sans texture ni alignement avec le reste du
HUD (hotbar, XP, faim…). Le passage de rectangles à losanges (`DiamondGauge`) est une première
étape pour se rapprocher du jeu de référence (*Dungeon Defenders* original, voir
[02-gameplay.md](02-gameplay.md#le-groupe-bas-gauche--mana-vie-expérience)) au niveau de la
**forme**, mais ça reste un placeholder assumé côté **matière** : pas de sprite, pas de cadre
métallique, pas d'icône. Ils sont aussi positionnés en `registerAboveAll` à des coordonnées
fixes (bas gauche, via les constantes de `HudLayout`), sans tenir compte de
`Gui.leftHeight`/`rightHeight` comme le fait le HUD vanilla pour empiler les barres sans se
chevaucher.

### Le rendu n'est pas interpolé

`extractRenderState` reçoit `partialTicks` mais ne s'en sert pas : la barre saute d'un palier
à l'autre à chaque coup encaissé. Une interpolation entre l'ancienne et la nouvelle valeur
rendrait l'effet plus lisible.

### Aucun gametest

Le run `gameTestServer` est configuré dans `build.gradle` mais aucun gametest n'existe : il
plantera au lancement. La CI ne l'exécute pas (`./gradlew build` seulement).

## Reliquats du template

| Fichier | Reliquat |
|---|---|
| `README.md` | encore le README du MDK NeoForge, ne parle pas du mod |
| `Config.java` | spec d'exemple (`logDirtBlock`, `magicNumber`…) jamais enregistrée via `registerConfig` |
| `DungeonDefendersModClient` | enregistre un `IConfigScreenFactory` pour une config inexistante au runtime |
| `TEMPLATE_LICENSE.txt` | licence du template, à ne pas confondre avec la licence du mod (`All Rights Reserved`) |
| `accesstransformer.cfg` | élargit trois méthodes de `Display` que plus aucune classe n'utilise depuis le retrait du code `TextDisplay` |

## Pistes prioritaires

1. Créer les textures et vrais modèles du cristal et du piège à pics.
2. Renseigner le `README.md` avec les vraies métadonnées (le `neoforge.mods.toml` est fait).
3. Externaliser les constantes de gameplay (`DEFAULT_HEALTH`, `DAMAGE_PER_HIT`,
   `SEARCH_RANGE`) dans `Config`, et enregistrer la spec.
4. Retirer le harnais de test du clic droit quand une autre source de dégâts existera.
5. Donner une vraie utilité au mana (un sort/une capacité qui le consomme, une régénération
   passive), retirer `ManaTestWandItem`, puis habiller `ManaOverlay`/`HealthOverlay`/
   `ExperienceOverlay` de vraies textures (sprites, cadre) une fois disponibles — la forme
   (losange) se rapproche déjà du jeu de référence, il manque la matière.
6. Concevoir et implémenter les remplacements custom de la faim et de la hotbar (masquées
   mais vides pour l'instant).
7. Définir un vrai système d'expérience/score/niveaux : comment `EXPERIENCE`, `SCORE` et
   `LEVEL` se nourrissent l'un l'autre (aujourd'hui trois compteurs indépendants, tous
   bloqués à leur valeur par défaut, comme le mana avant `ManaTestWandItem`).
8. Définir le déroulement des vagues : un vrai déclencheur pour passer en `COMBAT` (à la
   place du harnais de test au clic droit), sommer les poids des spawners actifs dans
   `WAVE_ENEMIES_TOTAL` au démarrage, faire avancer `CURRENT_WAVE` et repasser en `BUILD`
   quand `WAVE_ENEMIES_KILLED` atteint `WAVE_ENEMIES_TOTAL`, victoire à la dernière vague,
   défaite si le cristal tombe avant.
9. Donner un moyen de choisir/changer `ModAttachments.CHARACTER_NAME` (commande, écran de
   création de personnage...) — sans ça, il reste égal au pseudo Minecraft en permanence.
10. Une fois les images des 4 compétences fournies : les afficher dans `AbilitySlotsOverlay`
    (probablement via `blitSprite`, une texture par `SLOT_NAMES`), puis brancher le clic, un
    cooldown, et enfin le vrai effet de chaque compétence (soin, sorts, réparation de tour —
    aucun n'existe encore côté gameplay).
11. ~~Étendre `SpawnerConfigScreen`/`SpawnerConfigPayload` d'une composition figée à une vraie
    liste~~ — fait : liste dynamique (ajouter/retirer/cycler), voir
    [02-gameplay.md](02-gameplay.md#lécran-de-configuration--menu-network-clientguiscreenspawnerconfigscreenjava).
    Reste ouvert : gérer le défilement si `SpawnableEnemy` grandit au point de dépasser la
    hauteur de l'écran (non géré pour l'instant, deux valeurs seulement).
12. Donner au squelette (et à tout futur ennemi à distance) un vrai comportement d'archer —
    il utilise pour l'instant le même goal de mêlée que le zombie (`AttackEterniaCrystalGoal`
    le fait juste taper le cristal au corps à corps), pas d'attaque à l'arc.
13. Donner un moyen de choisir la difficulté au lancement de la map
    (`ModAttachments.DIFFICULTY`) — reste bloquée à `NORMAL`, aucun écran de lancement de
    partie n'existe encore.
