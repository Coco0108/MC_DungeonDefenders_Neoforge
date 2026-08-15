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
- ✅ Bloc `spike_blockade` ("Spike Blockade", premier vrai **tower** du mod — remplace l'ancien
  `spike_trap`, un piège de sol au mécanisme différent, supprimé) : mur avec ses propres PV
  (30 par défaut), bloque le passage (gratuit, propriété par défaut d'un bloc plein), pique
  tout `Monster` à son contact (2 PV/s, `AbstractBlockadeBlockEntity#serverTick`). Premier
  membre concret de la catégorie de code "Blockade" (voir "Système de tours" plus bas). Un
  goal d'IA (`AttackBlockadeGoal`, priorité 0, avant le cristal en priorité 1) détourne les
  ennemis de mêlée vers tout bloc du tag `dungeon_defenders:blockades` plutôt que le cristal,
  tant qu'il n'est pas détruit — pas les archers, qui peuvent tirer par-dessus/à côté. Coûte
  30 mana à la pose (`ModEvents.onBlockadePlace`, valeur de test pas encore équilibrée),
  placement refusé et item rendu si mana insuffisant. Modèle, blockstate, loot table, tag
  `mineable/pickaxe`, traductions `en_us`/`fr_fr`, onglet créatif. Détail dans
  [02-gameplay.md](02-gameplay.md#le-spike-blockade--blockspikeblockadeblockjava).
- ✅ Roue de sélection des tours (`TowerWheelScreen`, touche `R` par défaut) + mode pose en
  deux étapes (viser puis orienter, `TowerPlacementState`/`TowerPlacementClientEvents`) avec
  hologramme vert/rouge et cercle de portée (jamais affiché pour l'instant, aucune tour n'a de
  portée > 0). Deuxième façon de poser une tour, en plus du `BlockItem` (gardé). Pas de
  filtrage par héros (système inexistant). Le paquet de confirmation
  (`PlaceTowerPayload`) réutilise le hook NeoForge `EventHooks.onBlockPlace` pour déclencher la
  même vérification de mana que le `BlockItem`, sans duplication. Détail dans
  [02-gameplay.md](02-gameplay.md#la-roue-de-sélection-des-tours-et-la-pose--clientguiscreentowerwheelscreenjava).
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
  zombie) : cible le cristal comme n'importe quel `Monster`, et sort du spawner.
- ✅ Comportement d'archer pour le squelette (`entity/ai/RangedAttackEterniaCrystalGoal.java`,
  branché dans `ModEvents.onMonsterSpawn` pour tout `AbstractSkeleton`) : s'arrête à distance
  de tir (10 blocs) plutôt que de venir au corps à corps, tend l'arc (pose vanilla), puis tire
  une vraie flèche visuelle sur le cristal — les dégâts (3 PV, contre 5 au corps à corps) sont
  appliqués directement au cristal, même logique "harnais" que `AttackEterniaCrystalGoal`.
  Pensé pour être réutilisable tel quel par un futur ennemi à distance. Détail dans
  [02-gameplay.md](02-gameplay.md#le-goal-à-distance--entityairangedattacketerniacrystalgoaljava).
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
  application — appliquée **immédiatement** (pas d'attente de la prochaine vague). **Réservé
  au mode créatif** (`player.isCreative()`) : une vraie partie est censée charger des
  spawners déjà configurés, pas les reconfigurer en jouant. Détail complet dans
  [02-gameplay.md](02-gameplay.md#lécran-de-configuration--menu-network-clientguiscreenspawnerconfigscreenjava).
- ✅ Aperçu de composition du spawner en phase Construction (`SpawnerBlockEntityRenderer`) :
  total d'ennemis à venir + détail par type affiché au-dessus du bloc, **visible à travers les
  murs** (`Font.DisplayMode.SEE_THROUGH`), comme dans le jeu de référence. Caché en phase
  Combat, texte seul pour l'instant (pas d'icône par type). Détail dans
  [02-gameplay.md](02-gameplay.md#laperçu-de-composition-en-phase-construction--spawnerblockentityrendererjava).
- ✅ Une vague se déroule maintenant de bout en bout : le Combat se **déclenche** via un vote
  "prêt" (clic droit sur le Cristal d'Eternia en Construction, data attachment **joueur**
  `ready`, remis à zéro pour tout le monde une fois le combat lancé) plutôt que seulement le
  harnais de test ; `wave_enemies_total` est calculé pour de vrai (registre des spawners
  actifs, `ModAttachments.ACTIVE_SPAWNERS`, sommé à l'entrée en Construction) ; dès que
  `wave_enemies_killed` l'atteint, retour automatique en Construction
  (`ModEvents.onMonsterDeath` → `PhaseTransitions.enterBuild`) **et** `current_wave` avance de
  1 (plafonné à `MAX_WAVE`). Détail dans
  [02-gameplay.md](02-gameplay.md#le-déroulement-dune-vague--initphasetransitionsjava-modeventsonmonsterdeath).
- ✅ Un ennemi ne peut plus spawn à l'intérieur d'un bloc plein (`SpawnerBlockEntity
  #findSafeSpawnPos`) : jusqu'à 8 positions aléatoires essayées dans le rayon de spawn,
  vérifiées traversables (pieds + tête), repli sur `pos.above()` sinon. Pas de vérification
  de sol en dessous — un ennemi qui spawn au-dessus d'un trou tombe simplement, ce n'est pas
  traité comme un problème.
- ✅ L'Overworld est un monde vide (`data/minecraft/dimension/overworld.json`, préréglage
  vanilla "The Void") et le point de spawn est fixé à `(0, 65, 0)` avec une plateforme
  provisoire (`TavernSpawn.java`), **reposée à chaque chargement du monde** plutôt qu'une
  seule fois — pour que le contenu de la taverne reste toujours à jour avec le mod installé,
  même après une mise à jour de sa structure (voir "Système de maps/structures" plus bas pour
  le détail du raisonnement) — premier pas vers le futur système de maps/structures : la
  taverne (hub) et chaque map seront des structures posées à coordonnées fixes, donc rien dans
  le jeu ne doit dépendre du terrain généré naturellement. Détail dans
  [02-gameplay.md](02-gameplay.md#le-monde-et-le-point-de-spawn). **Non vérifié en jeu** — le
  fichier de dimension n'est validé à aucune étape de la compilation, voir 06-a-tester.md.
- ✅ Cristal de la taverne (`TavernCrystalBlock`, distinct d'`EterniaCrystalBlock` — pas de PV,
  pas de combat) : clic droit ouvre `MapSelectionScreen`, un carrousel de maps
  (`init/GameMap.java`, extensible, chaque entrée peut être masquée du carrousel tant qu'elle
  est en cours de conception) avec image d'aperçu + nom, et un choix de difficulté
  (Facile/Normal/Difficile). Le bouton "Jouer" applique réellement la difficulté choisie
  (`ModAttachments.DIFFICULTY`, via `SetDifficultyPayload`) et téléporte tous les joueurs vers
  `MapInstance` (l'emplacement partagé de "la map en cours", voir plus bas) — le choix de map
  précis, lui, n'a encore aucun effet (une seule map placeholder générique pour l'instant).
  Détail dans [02-gameplay.md](02-gameplay.md#la-taverne--choix-de-map-et-difficulté).
- ✅ Victoire et défaite (`PhaseTransitions.onVictory/onDefeat`) : la partie se termine
  vraiment maintenant. Nettoyer la dernière vague (`current_wave == MAX_WAVE`) diffuse un
  message de victoire ; la destruction du Cristal d'Eternia diffuse un message de défaite. Les
  deux remettent la partie à zéro (vague 1, phase Construction) et diffusent un lien cliquable
  "Retour à la taverne" (commande `/dd_leave`, `MapInstance.returnToTavern`) qui nettoie
  l'emplacement de map et téléporte tout le monde. Le cristal détruit **n'est pas replacé
  automatiquement** — voir "Ce qui reste" plus bas. Détail dans
  [02-gameplay.md](02-gameplay.md#victoire-et-défaite--phasetransitionsonvictoryondefeat).

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

Même situation pour `models/block/spike_blockade.json`, qui pointe sur
`minecraft:block/dripstone_block` en attendant `textures/block/spike_blockade.png`.

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

### Système de tours (catégorie "Blockade" démarrée)

Discuté avec le joueur : les tours ne seront pas toutes construites sur le même patron —
il envisage au moins 5 catégories, chacune avec ses propres règles :

1. **Blocks passifs** et **corps à corps** — désormais **unifiées en une seule catégorie de
   code, "Blockade"** (décision explicite du joueur, voir plus bas) : bloquent le passage,
   avec un booléen optionnel pour infliger des dégâts au contact. Un block passif n'est qu'une
   Blockade avec ce booléen à `false`. **Spike Blockade en fait partie (booléen à `true`), et
   c'est le seul membre concret pour l'instant** (voir "Ce qui est implémenté" plus haut et
   [02-gameplay.md](02-gameplay.md#la-catégorie-blockade--blockentityabstractblockadeblockentityjava-tag-dungeon_defendersblockades)).
2. **Tours à distance** — visent et tirent sur les ennemis à portée, ne bloquent probablement
   pas le passage (posées en retrait).
3. **Auras et pièges non attaquables** — pas de PV, pas destructibles par les ennemis ; juste
   une durée ou un taux d'utilisation limité (charges).
4. **Pièges de sol** — un "sur-bloc" (pas un bloc plein) posé sur le sol, non attaquable —
   c'est en fait le mécanisme de l'ancien `SpikeTrapBlock` (dégâts via `stepOn`), retiré au
   profit du vrai Spike Blockade (catégorie 1) mais qui pourrait revenir sous cette catégorie
   plus tard, sous un autre nom du plan Excel (ex. une des trap de la Huntress).
5. D'autres catégories, pas encore réfléchies par le joueur.

**Architecture de code, décidée avec le joueur** : contrairement à la position initiale ("pas
de base commune tant qu'il n'y a qu'un seul exemple concret"), le joueur a explicitement
demandé d'établir dès maintenant une base de code pour la catégorie "Blockade", puisqu'il
connaît déjà son comportement commun avant même d'écrire une deuxième blockade concrète :

- `block/entity/AbstractBlockadeBlockEntity.java` porte les stats communes : PV (`maxHealth`),
  coût en mana à la pose (`manaCost`, **désormais consommé** via `ModEvents.onBlockadePlace`,
  voir plus bas), et un booléen `dealsContactDamage` (+ dégâts/intervalle/portée de contact si
  actif). **Pas de stat "portée d'attaque"** sur cette base : une blockade n'attaque pas à
  distance, seulement au contact.
- **Le coût en mana à la pose est branché** (`ModEvents.onBlockadePlace`, écoute
  `BlockEvent.EntityPlaceEvent`) : générique à toute la catégorie via
  `getBlockEntity(pos) instanceof AbstractBlockadeBlockEntity`, pas seulement Spike Blockade.
  Mana insuffisant → placement annulé, NeoForge restaure le bloc précédent et rend l'item
  automatiquement. Valeur de test sur Spike Blockade : `MANA_COST = 30`, pas encore équilibrée.
  Aucune exemption créative (même convention que `ManaTestWandItem`).
- `entity/ai/AttackBlockadeGoal.java` cible désormais **tout bloc du tag**
  `dungeon_defenders:blockades` (`init/ModBlockTags.java`,
  `data/dungeon_defenders/tags/block/blockades.json`) plutôt que `spike_blockade` en dur —
  ajouter une future blockade au tag JSON suffit pour qu'elle hérite du comportement
  d'attaque, sans toucher au goal.
- **Priorité confirmée avec le joueur** : une blockade à portée de recherche (8 blocs)
  l'emporte toujours sur le cristal, même si elle n'est pas strictement sur le trajet le plus
  direct du monstre (pas de vérification de chemin — un simple scan par rayon). Cette règle
  vaut aussi comme convention par défaut pour les futures catégories de tours : une blockade
  doit rester prioritaire sur le cristal **et** sur les autres types de tours, une fois qu'ils
  existeront.
- `AttackBlockadeGoal` **n'étend toujours pas** `AbstractEterniaCrystalAttackGoal` : cette
  décision-là reste inchangée (le cristal cible un exemplaire unique sur la carte, une
  blockade est cherchée par tag/proximité — formes trop différentes pour forcer une base
  commune maintenant).
- **Les autres catégories (2 à 5) restent sans base de code pour l'instant** : la
  généralisation immédiate n'a été demandée que pour "Blockade" — chacune des autres sera
  construite en autonomie d'abord (comme prévu à l'origine), sauf nouvelle décision explicite
  du joueur au moment de s'y attaquer.

**Reste à faire, même pour Spike Blockade** : équilibrage réel du coût en mana (30 est une
valeur de test, pas réfléchie), remboursement de mana à la casse, indicateur visuel de PV
restants. Et bien sûr, les catégories 2 à 5 n'ont aucune implémentation.

**Comment on pose les tours, décidé avec le joueur** : dans le vrai Dungeon Defenders, les
tours ne se posent pas depuis l'inventaire (la liste dépend du héros choisi) — une **roue
radiale** a donc été construite (`TowerWheelScreen`, touche dédiée), avec un mode pose en deux
étapes (viser puis orienter) affichant un hologramme (vert/rouge selon validité) et un cercle
de portée générique (voir "Ce qui est implémenté" plus haut et
[02-gameplay.md](02-gameplay.md#la-roue-de-sélection-des-tours-et-la-pose--clientguiscreentowerwheelscreenjava)
pour le détail complet). **Pas de filtrage par héros** pour l'instant (ce système n'existe pas
encore) — la roue liste toutes les tours, prête à être filtrée plus tard. Le `BlockItem`
classique (onglet créatif) **n'a pas été retiré**, les deux façons de poser coexistent — à
reconsidérer une fois la roue testée en jeu.

### La partie se termine, mais sans vraie conclusion visuelle

Victoire et défaite existent maintenant (voir "Ce qui est implémenté" plus haut et
[02-gameplay.md](02-gameplay.md#victoire-et-défaite--phasetransitionsonvictoryondefeat)), mais
de façon minimale :

- Un simple message système annonce la victoire/défaite — pas d'écran dédié. L'idée d'un
  écran avec choix "rejouer/retour à la taverne" existe (vue dans le plan Excel du joueur),
  mais dépend du système de maps/structures (savoir concrètement où "rejouer" et "la taverne"
  mènent), pas encore construit.
- Le Cristal d'Eternia détruit à la défaite **n'est pas replacé automatiquement** —
  `resetGameState` remet les compteurs à zéro, mais le bloc reste absent tant que personne
  n'en repose un à la main. Ça fait partie de la future remise à neuf d'une map (structure
  reposée, tours retirées, PV du cristal restaurés), pas de ce morceau.
- Rien ne distingue une partie "terminée" d'une simple pause entre deux vagues : les deux
  ramènent en phase `BUILD`, vague 1 — un joueur qui manque le message système ne verra pas
  forcément que la partie a recommencé à zéro.

Le registre `ModAttachments.ACTIVE_SPAWNERS` ne reflète que les spawners **actuellement
chargés** — fiable en test (le joueur est toujours à proximité), mais deviendra pleinement
correct seulement une fois qu'un système force-chargera toute la zone de jeu pendant une
partie (voir "Système de maps/structures" ci-dessous) plutôt que de compter sur le chargement
naturel autour du joueur.

### Système de maps/structures (démarré : monde vide, taverne, choix de map/difficulté, mécanisme de chargement avec placeholder — les vraies structures restent à faire)

Plan affiné au fil de plusieurs échanges avec le joueur, pas encore tout codé :

- Une map est une **structure** Minecraft (`.nbt`, comme un bloc de structure vanilla)
  construite en créatif — spawners déjà configurés inclus, puisque le format structure
  sauvegarde aussi les données NBT des block entities.
- **Une seule partie active à la fois** sur tout le monde/serveur (comme dans le vrai jeu) —
  pas plusieurs groupes qui jouent des maps différentes en parallèle. Confirmé explicitement :
  ça correspond déjà à ce qui est construit (`GAME_PHASE`, `CURRENT_WAVE`,
  `WAVE_ENEMIES_TOTAL/KILLED`, `COMBAT_SESSION` sont tous des états de la `Level` entière, pas
  par joueur/groupe).
- Conséquence directe : **toutes les maps partagent la même coordonnée fixe** plutôt que
  d'avoir chacune la leur (pas besoin d'une grille de coordonnées puisqu'il n'y en a jamais
  deux en même temps). Au choix d'une map dans `MapSelectionScreen` : téléporter les joueurs à
  cette coordonnée, poser la structure de la map choisie, avec une transition (fondu/écran de
  chargement) **fabriquée par le mod** plutôt qu'un vrai changement de dimension — choisi pour
  éviter un temps de chargement de dimension trop long. Au départ (retour à la taverne, fin de
  partie) : effacer toute la zone occupée (remplacer par de l'air) avant la prochaine map.
- Pas de dégât de terrain en jeu (confirmé par le joueur) : au sein d'une **même visite** d'une
  map, sa structure n'a besoin d'être posée **qu'une seule fois**, pas reposée à chaque
  tentative de vague. Ce qui doit se réinitialiser entre deux tentatives, ce sont les **tours
  posées par le joueur** (à retirer — prévoir un registre de tours similaire à
  `ACTIVE_SPAWNERS` quand elles existeront) et les **PV du cristal** (à remettre au max), pas
  la structure elle-même.
- Toute la zone active serait **force-chargée** (chunk tickets, indépendants de la position du
  joueur) tant qu'un joueur y est, relâchée au retour à la taverne — nécessaire parce que
  Minecraft ne charge/tick normalement que les chunks proches d'un joueur, ce qui ne suffit
  pas pour une arène fixe où plusieurs spawners peuvent être loin les uns des autres (voir
  plus haut, `ACTIVE_SPAWNERS`). Comme une seule map est active à la fois, au maximum une
  seule zone (plus la taverne) est force-chargée simultanément.
- **La taverne suit le même principe que les maps**, à un détail près : chaque **nouvelle
  visite** (choisir une map, quitter puis revenir à la taverne, etc.) repose sa structure
  depuis le fichier — jamais construite une fois pour toutes. Pour la taverne, "chaque visite"
  se traduit par "à chaque chargement du monde" (voir `TavernSpawn.java`), puisque c'est
  l'endroit où le serveur place systématiquement les joueurs par défaut. Sans ce
  rechargement, mettre à jour la structure de la taverne (ou d'une map) dans une future
  version du mod resterait invisible sur une sauvegarde existante — le joueur garderait la
  version posée lors de sa toute première visite, rien ne la reposant ensuite.

**Fait** :
- Monde vide + point de spawn fixe, reposé (pour l'instant une plateforme provisoire) à chaque
  chargement du monde plutôt qu'une seule fois — voir plus haut.
- L'écran de choix de map/difficulté dans la taverne (`TavernCrystalBlock`/
  `MapSelectionScreen`, voir plus haut et
  [02-gameplay.md](02-gameplay.md#la-taverne--choix-de-map-et-difficulté)) — la difficulté
  choisie s'applique réellement.
- Le **mécanisme** de chargement de map (`MapInstance.java`) : un emplacement partagé
  (`MAP_POS`, une seule map active à la fois), nettoyé puis reposé avec un placeholder
  générique au clic sur "Jouer" (`startGame`), tout le monde téléporté ensemble. Retour à la
  taverne via la commande `/dd_leave` (`returnToTavern`), aussi accessible comme lien
  cliquable dans les messages de victoire/défaite (voir "Ce qui est implémenté" plus haut).
  Voir [02-gameplay.md](02-gameplay.md#la-map-active--mapinstancejava).

**Reste à faire** : le choix de map précis dans le carrousel n'a toujours aucun effet (une
seule map placeholder générique pour l'instant, quel que soit l'élément sélectionné) ; la
vraie structure de la taverne (la plateforme actuelle est un placeholder) ; au moins une
vraie map, et le vrai chargement de sa structure `.nbt` (remplacerait
`buildPlaceholderArena()`) ; la réinitialisation tours/PV du cristal entre deux tentatives ;
le force-chargement pendant une partie ; une bordure/barrière anti-chute dans le vide en
dehors des zones bâties ; un vrai point de sortie posé dans chaque map (`/dd_leave` reste une
commande de harnais, utilisable à tout moment, pas seulement après victoire/défaite). C'est
aussi le prérequis pour que le verrou créatif du GUI de config du spawner (voir plus haut) ait
vraiment son plein effet : tant que ce système n'est pas fini, rien n'empêche techniquement de
construire et tester une map "à la main" en créatif.

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

1. Créer les textures et vrais modèles du cristal et du Spike Blockade.
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
8. ~~Définir le déroulement des vagues~~ — fait : vote "prêt" pour déclencher le Combat,
   `WAVE_ENEMIES_TOTAL` juste (registre des spawners actifs), retour automatique en `BUILD`
   dès que `WAVE_ENEMIES_KILLED` l'atteint, `CURRENT_WAVE` qui avance (plafonné à `MAX_WAVE`),
   et maintenant victoire (dernière vague nettoyée) / défaite (cristal détruit) — voir "Ce qui
   est implémenté" et
   [02-gameplay.md](02-gameplay.md#le-déroulement-dune-vague--initphasetransitionsjava-modeventsonmonsterdeath).
   Reste ouvert : un vrai écran de fin de partie (juste un message système pour l'instant), et
   remettre en jeu le cristal détruit automatiquement — voir la section dédiée plus haut.
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
12. ~~Donner au squelette un vrai comportement d'archer~~ — fait :
    `RangedAttackEterniaCrystalGoal` (voir "Ce qui est implémenté" et
    [02-gameplay.md](02-gameplay.md#le-goal-à-distance--entityairangedattacketerniacrystalgoaljava)).
13. ~~Donner un moyen de choisir la difficulté au lancement de la map~~ — fait :
    `MapSelectionScreen` (voir "Ce qui est implémenté" et
    [02-gameplay.md](02-gameplay.md#la-taverne--choix-de-map-et-difficulté)).
14. Ajouter une icône par type de monstre dans l'aperçu de composition du spawner
    (`SpawnerBlockEntityRenderer`, phase Construction) — texte seul pour l'instant. Piste
    envisagée : réutiliser les textures vanilla des œufs d'invocation
    (`zombie_spawn_egg`/`skeleton_spawn_egg`) pour ne pas dépendre d'assets custom.
15. Système de maps/structures : ~~monde vide + point de spawn fixe~~, ~~écran de choix de
    map/difficulté~~ et ~~mécanisme de chargement de map (placeholder)~~ faits (voir "Ce qui
    est implémenté"). Reste : la vraie structure de la taverne (remplacer la plateforme
    provisoire), au moins une vraie map avec sa vraie structure `.nbt` (remplacerait
    `MapInstance.buildPlaceholderArena()`), la réinitialisation tours/PV du cristal entre deux
    tentatives, le force-chargement pendant une partie, une bordure anti-chute dans le vide,
    un vrai point de sortie posé dans chaque map (plutôt que la commande de harnais
    `/dd_leave`). Voir la section dédiée dans "Ce qui reste" ci-dessus — c'est aussi ce qui
    rendra `ACTIVE_SPAWNERS` pleinement fiable (indépendant de la position du joueur).
