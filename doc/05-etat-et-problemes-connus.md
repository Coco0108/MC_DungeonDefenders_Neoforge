# 5. État du projet & problèmes connus

État à la version `0.0.1`. Le build (`./gradlew build`) **passe** — c'est aussi ce que
vérifie la CI.

## Ce qui est implémenté

- ✅ Bloc `eternia_crystal` + son item, hitbox 1×3×1, très résistant.
- ✅ Block entity avec PV persistants (100 par défaut) **et synchronisés vers le client**.
- ✅ Destruction du bloc et message « Game Over » à 0 PV.
- ✅ IA : tout `Monster` (zombie, squelette...) qui rejoint le monde reçoit un goal d'attaque
  (`ModEvents.onMonsterSpawn`, généralisé au-delà des zombies) — les archers ne visent que le
  cristal à distance, les autres passent désormais par le **système de priorité IA unifié**
  (voir plus bas et "Système de priorité IA") : Block > Corps à corps > Cristal > Tourelle,
  pas seulement le cristal.
- ✅ Onglet créatif dédié.
- ✅ Renderer de barre de vie 3D au-dessus du cristal (API `submit` de 26.1). Animée entre
  deux paliers de PV (2026-08-24, `EterniaCrystalBlockEntityRenderer.HealthLerp`, 300 ms, même
  principe que `LerpingBossEvent` vanilla — temps réel, pas `partialTicks`, puisque
  `extractRenderState` reçoit un état neuf à chaque frame et ne peut rien retenir lui-même
  d'une frame à l'autre). **Jamais vu en jeu.**
- ✅ Modèle, blockstate, loot table, tags d'outil, traductions `en_us` et `fr_fr`.
- ✅ CI GitHub Actions.
- ✅ `neoforge.mods.toml` renseigné avec les vraies métadonnées (`mod_authors`,
  `mod_description` ajoutés à `replaceProperties` dans `build.gradle`).
- ✅ Bloc `spike_blockade` ("Spike Blockade", premier vrai **tower** du mod — remplace l'ancien
  `spike_trap`, un piège de sol au mécanisme différent, supprimé) : mur avec ses propres PV
  (30 par défaut), bloque le passage, pique tout `Monster` à son contact (2 PV/s,
  `AbstractBlockadeBlockEntity#serverTick`). Hitbox custom de 1,5 bloc de haut (`getShape`/
  `getCollisionShape`, comme un mur/une barrière vanilla) plutôt que le cube plein par défaut —
  un monstre ne peut pas sauter dessus (saut ~1,25 bloc) pour continuer son chemin par-dessus,
  voir "Corrections trouvées lors des tests en jeu du 2026-08-23". Premier
  membre concret de la catégorie de code "Blockade" (voir "Système de tours" plus bas).
  `dealsContactDamage=true` lui donne la priorité IA "corps à corps" (20 — voir "Système de
  priorité IA" plus bas) : un ennemi de mêlée s'y attaque avant le cristal, tant qu'il n'est
  pas détruit — pas les archers, qui peuvent tirer par-dessus/à côté. Coûte
  30 mana à la pose, placement refusé et item rendu si mana insuffisant. Modèle, blockstate,
  loot table, tag `mineable/pickaxe`, traductions `en_us`/`fr_fr`. Détail dans
  [02-gameplay.md](02-gameplay.md#le-spike-blockade--blockspikeblockadeblockjava). **Testé en
  jeu** (2026-08-23).
- ✅ Bloc `harpoon_turret` ("Harpoon Turret") : premier membre de la catégorie "Turret" —
  tour à distance qui scanne et tire toute seule à chaque tick (pas de `Goal` porté par un
  monstre, contrairement à Blockade), dans un **cône** de 45° orienté selon
  `HORIZONTAL_FACING` (angle fixe, la largeur à l'extrémité du cône augmente avec la portée par
  trigonométrie, pas le cône lui-même), portée 12 blocs, 6 dégâts/tir toutes les 1,5 s. Flèche
  visuelle (constructeur `Arrow` sans propriétaire) + dégâts directs, même principe que le
  squelette archer sur le cristal. 20 PV, coût 50 mana — mêmes valeurs de test pas encore
  équilibrées que Spike Blockade. A des PV comme une Blockade (décidé avec le joueur), priorité
  IA la plus basse (40, "Tourelle" — voir "Système de priorité IA" plus bas) : un monstre de
  mêlée ne s'y attaque qu'en dernier recours, si rien de plus prioritaire n'est à portée — la
  toute première fois qu'un turret est réellement ciblable. A motivé l'extraction de
  `block/entity/AbstractTowerBlockEntity.java` (PV/coût mana/persistance/sync, commun aux deux
  catégories désormais) — première vraie duplication entre catégories, généralisée seulement
  maintenant qu'un second exemple concret la prouve. Modèle directionnel (texture furnace
  vanilla, `minecraft:block/orientable`) — premier bloc du mod à avoir une vraie propriété de
  `BlockState` (`HORIZONTAL_FACING`), et premier vrai consommateur de la rotation choisie dans
  la roue (`ModNetworking.handlePlaceTower` l'appliquait déjà par anticipation). Même hitbox
  custom de 1,5 bloc de haut que Spike Blockade (anti-escalade des monstres). Détail dans
  [02-gameplay.md](02-gameplay.md#le-harpoon-turret--blockharpoonturretblockjava). **Testé en
  jeu** (2026-08-23) : trois bugs trouvés et corrigés (voir "Corrections apportées") — le
  cooldown de tir ne se déclenchait jamais (overflow sur `long`), la flèche se figeait dans le
  bloc de la tourelle elle-même, et la rotation à la pose était incorrecte (voir la roue
  ci-dessous).
- ✅ Barre de vie des tours (`block/entity/TowerHealthBarRenderer.java`, 2026-08-24) :
  générique sur `AbstractTowerBlockEntity`, couvre Spike Blockade et Harpoon Turret (et toute
  future catégorie) avec un seul renderer. Cachée à PV pleins et au-delà de 16 blocs de la
  caméra — décidé avec le joueur, pour rester lisible avec potentiellement des dizaines de
  tours posées (contrairement au cristal, toujours affiché, il n'y en a jamais qu'un). Même
  animation 300 ms que le cristal (`HealthLerp`, extraite en classe partagée avec
  `HealthBarRendering` à cette occasion). Détail dans
  [02-gameplay.md](02-gameplay.md#la-barre-de-vie-des-tours--blockentitytowerhealthbarrendererjava).
  **Jamais vu en jeu.**
- ✅ Système de priorité IA unifié (`block/entity/AiAttackTarget.java`,
  `entity/ai/AttackPriorityTargetGoal.java`, voir "Système de priorité IA" plus bas) :
  remplace les deux anciens goals séparés (`AttackBlockadeGoal`/`AttackEterniaCrystalGoal`,
  supprimés, comme le tag `dungeon_defenders:blockades` et `init/ModBlockTags.java`) par un
  seul goal qui choisit lui-même la meilleure cible parmi Block (10) / Corps à corps (20) /
  Cristal (30) / Tourelle (40), palier par palier. Décidé avec le joueur : indices espacés
  pour laisser de la place à une future provocation. Détail dans
  [02-gameplay.md](02-gameplay.md#le-goal-de-mêlée-unifié--entityaiattackprioritytargetgoaljava-blockentityaiattacktargetjava).
  **Testé en jeu** (2026-08-23) via les tests des tours ci-dessus : les monstres priorisent
  bien les tours avant le cristal.
- ✅ Barre de vie des monstres (`entity/MobHealthBarRenderer.java`, 2026-08-24, corrigée le
  2026-08-26) : même principe et mêmes conditions que la barre des tours (endommagé + à portée
  de 16 blocs). La vie n'existant pas nativement sur un `EntityRenderState` vanilla,
  `RegisterRenderStateModifiersEvent` (NeoForge) l'y ajoute via `ContextKey` — cette partie a
  toujours fonctionné. **Bug trouvé en testant en jeu le 2026-08-26** : la première version
  utilisait un `RenderLayer` (branché via `EntityRenderersEvent.AddLayers`), qui s'exécute dans
  le mauvais repère de pose (celui, local et déjà transformé, du modèle de l'entité) pour un
  billboard caméra-face — la barre était bien soumise au rendu, juste mal placée/orientée au
  point d'être invisible en pratique. Remplacé par un handler sur
  `RenderLivingEvent.Post` (bus de jeu), qui se déclenche dans le même repère caméra-relatif
  que le nametag vanilla — voir le détail dans
  [02-gameplay.md](02-gameplay.md#la-barre-de-vie-des-monstres--entitymobhealthbarrendererjava).
  **Le correctif reste à confirmer en jeu.**
- ✅ Roue de sélection des tours (`TowerWheelScreen`, touche `R` par défaut) + mode pose en une
  seule étape (`TowerPlacementState`/`TowerPlacementClientEvents` — position et rotation
  évoluent en parallèle, un seul clic droit pose la tour, simplifié depuis un flux à deux
  étapes/deux clics) avec hologramme vert/rouge et zone de portée (cercle ou **cône**). **Testé
  en jeu** (2026-08-23) : deux bugs de rotation trouvés et corrigés (voir "Corrections
  apportées" plus bas) — cône à l'opposé de la direction de tir, puis Est/Ouest inversés dans
  l'aperçu — et un conflit de touche par défaut (`T`, déjà pris par le chat vanilla) qui
  empêchait toute rotation, désormais `G`. **Unique façon de poser
  une tour**, toute catégorie confondue : le `BlockItem` classique ne pose plus rien
  (`TowerBlockItem#useOn` renvoie systématiquement `PASS`, retiré de l'onglet créatif).
  Pas de filtrage par héros (système inexistant). Le paquet de confirmation
  (`PlaceTowerPayload`) réutilise le hook NeoForge `EventHooks.onBlockPlace` pour déclencher la
  même vérification de mana et de **phase** (Construction uniquement) que
  `ModEvents.onTowerPlace` (renommé depuis `onBlockadePlace`, généralisé aux deux catégories).
  Détail dans
  [02-gameplay.md](02-gameplay.md#la-roue-de-sélection-des-tours-et-la-pose--clientguiscreentowerwheelscreenjava).
- ✅ Cristaux de mana (`entity/ManaCrystalEntity.java`, premier `Entity` custom du mod,
  `extends ExperienceOrb`) : chaque monstre tué (toute phase) lâche un cristal ramassable au
  sol (pas un item d'inventaire), qui donne 5 mana (`ManaCrystalType.SMALL`, un seul palier
  pour l'instant, au moins 6 prévus). Vraies orbes d'XP vanilla désactivées pour tout `Monster`
  (`ModEvents.onExperienceDrop`) pour éviter qu'elles fusionnent avec un cristal de même
  valeur. Rendu = renderer vanilla de l'orbe d'XP réutilisé tel quel (vert/jaune, pas de
  couleur "mana" dédiée). **Casser sa propre tour à la pioche rembourse 50% du coût de pose**
  (`ModEvents.onTowerBreak`, `BreakBlockEvent` — jamais déclenché par une destruction en
  combat). Détail dans
  [02-gameplay.md](02-gameplay.md#les-cristaux-de-mana--entitymanacrystalentityjava-initmanacrystaltypejava).
- ✅ Mana du joueur : data attachment `mana` (persistant, synchronisé), maximum par défaut de
  100, affiché en HUD via `ManaOverlay` — losange en bas à gauche de l'écran, le plus à droite
  du groupe vie/mana (`DiamondGauge`, couleurs plates), très provisoire. Testable en jeu avec
  l'item `mana_test_wand` (clic droit = -10 mana) ou `mana_fill_wand` (clic droit = remplit au
  maximum). **Testé en jeu et confirmé** (2026-08-23) : positionnement (échange avec la vie
  compris), les deux baguettes de test, et la persistance à la reconnexion.
- ✅ Vie du joueur : maximum vanilla porté de 20 à 100 (`ModEvents.onPlayerJoin`), affichée en
  HUD via `HealthOverlay` — losange juste à gauche de celui du mana (vie à gauche, comme le jeu
  de référence). **Testé en jeu et confirmé** (2026-08-23) : positionnement (échange avec le
  mana compris), dégâts qui vident le losange, persistance de la vie perdue à la reconnexion,
  et spawn à `100/100` (pas `20/20`) pour un tout premier join.
- ✅ Expérience custom du joueur : data attachment `experience` (persistant, synchronisé),
  démarre à `0/100` (contrairement au mana/à la vie qui démarrent pleins), affichée en HUD
  via `ExperienceOverlay` — barre horizontale tout en bas, sous les losanges vie/mana. Sans
  rapport avec l'XP vanilla. Le groupe des trois est décrit dans
  [02-gameplay.md](02-gameplay.md#le-groupe-bas-gauche--mana-vie-expérience). **Positionnement
  testé en jeu** (2026-08-23).
- ✅ Vague en cours : data attachment `current_wave` sur la `Level` (persistant, synchronisé,
  démarre à 1), affichée en haut à droite (`Vague X/5`) via `WaveOverlay` — texte seul, pas de
  jauge. Aucun déroulement de vagues n'existe encore. **Positionnement testé en jeu**
  (2026-08-23).
- ✅ Progression de la vague : `wave_enemies_killed`/`wave_enemies_total` (mêmes garanties que
  `current_wave`), affichée en grande barre centrée tout en haut de l'écran via
  `WaveEnemiesOverlay` (jauge orange, texte `Ennemis : X/Y` superposé au centre) — la zone la
  plus visible du HUD, comme dans le jeu de référence. **Positionnement testé en jeu**
  (2026-08-23).
- ✅ Phase de la partie : data attachment `game_phase` sur la `Level` (ordinal de l'enum
  `GamePhase` : `BUILD`/`COMBAT`), démarre en `BUILD`, affichée juste sous la rangée
  vague/ennemis via `PhaseOverlay` (`Phase : Construction`). **Positionnement testé en jeu**
  (2026-08-23).
- ✅ Score de la carte : data attachment `score` sur la `Level` (persistant, synchronisé,
  démarre à 0), affiché tout en bas centre de l'écran via `ScoreOverlay` (`Score : X`, texte
  seul). Censé correspondre à l'expérience gagnée sur la carte en cours, mais distinct de
  `experience` (qui elle persiste au-delà d'une carte) — rien ne l'alimente encore.
  **Positionnement testé en jeu** (2026-08-23).
- ✅ Nom et niveau du personnage : `character_name` (`String`, distinct du pseudo Minecraft
  mais initialisé avec, faute de mieux) et `level` (`Integer`, démarre à 1) — deux data
  attachments sur le joueur, persistants, synchronisés. Affichés juste au-dessus du score via
  `CharacterOverlay` (`Nom - niv X`). Rien ne fait encore varier ni l'un ni l'autre.
  **Positionnement testé en jeu** (2026-08-23).
- ✅ 4 emplacements de compétences (soin sur soi, sort 1, sort 2, réparation de tour) en bas à
  gauche via `AbilitySlotsOverlay`, juste à droite des losanges vie/mana, dans cet ordre —
  fond en rond (`CircleSlot`), purement visuel : pas de clic, pas de cooldown, pas d'icône.
  Voir "Ce qui reste" ci-dessous. **Positionnement testé en jeu** (2026-08-23).
- ✅ HUD vanilla masqué (cœurs, faim, expérience, hotbar) au profit d'une interface custom —
  voir [02-gameplay.md](02-gameplay.md#le-hud-vanilla-masqué). Décidé avec le joueur
  (2026-08-24) : pas de mécanique de faim dans ce mod (`ModEvents.onPlayerTick` la maintient au
  maximum à chaque tick serveur, quelle que soit l'activité du joueur) ; plus de hotbar non
  plus, à terme un seul item par main plutôt que 9 emplacements — les touches 1-9 et la molette
  ne changent donc plus le slot sélectionné (`DungeonDefendersModClient`, `ClientTickEvent.Pre`
  vide les touches avant que Minecraft ne les lise, `InputEvent.MouseScrollingEvent` annulé).
  **Jamais vu en jeu.**
- ✅ Bloc `spawner` : premier vrai morceau de gameplay (pas juste du HUD). Fait apparaître des
  zombies et des squelettes pendant la phase de combat via l'algorithme de spawn pondéré du
  plan Excel du joueur (voir
  [02-gameplay.md](02-gameplay.md#le-spawner--blockspawnerblockjava)). Le nombre de base de
  chaque type sert aussi de plafond pour la vague (une fois atteint, ce type est sauté), et
  est mis à l'échelle par `DifficultyScaling` (difficulté × vague). Configurable par spawner
  (intervalle, rayon de spawn, plage de vagues, nombre de base par type). Shift + clic droit =
  harnais de test qui bascule `BUILD`/`COMBAT`. Incrémente aussi
  `ModAttachments.WAVE_ENEMIES_KILLED` via un nouveau handler `LivingDeathEvent`. **Testé en
  jeu** (2026-08-23) : deux bugs trouvés et corrigés (voir "Corrections apportées") — un
  `StackOverflowError` au chargement/pose d'un spawner (récursion via
  `recomputeWaveEnemiesTotal` appelée depuis `setLevel`), et `WAVE_ENEMIES_TOTAL` qui restait
  bloqué à sa valeur par défaut (`10`) tant qu'aucune vague n'avait encore été nettoyée — se
  recalcule maintenant dès qu'un spawner apparaît/disparaît/est reconfiguré, pas seulement aux
  transitions de phase. Décidé avec le joueur (2026-08-25) : n'est plus jamais un obstacle
  physique — `getCollisionShape` toujours vide (traversable par tout le monde, toute phase),
  `getRenderShape` toujours `INVISIBLE`, `getShape` (ciblage/clic droit) plein uniquement pour
  un joueur créatif. A nécessité de corriger `findSafeSpawnPos` : le repli par défaut spawnait
  au-dessus du bloc du spawner (`pos.above()`), en comptant sur sa solidité comme sol — plus
  valable une fois le spawner intangible, corrigé pour spawn à `pos` directement (le vrai sol
  de la map, sous le marqueur). Détail dans
  [02-gameplay.md](02-gameplay.md#jamais-un-obstacle-physique--getshapegetcollisionshapegetrendershape).
  **Jamais testé en jeu** dans ce nouvel état d'intangibilité.
- ✅ Squelette ajouté comme deuxième ennemi (réutilise `EntityType.SKELETON` vanilla, comme le
  zombie) : cible le cristal comme n'importe quel `Monster`, et sort du spawner.
- ✅ Comportement d'archer pour le squelette (`entity/ai/RangedAttackEterniaCrystalGoal.java`,
  branché dans `ModEvents.onMonsterSpawn` pour tout `AbstractSkeleton`) : s'arrête à distance
  de tir (10 blocs) plutôt que de venir au corps à corps, tend l'arc (pose vanilla), puis tire
  une vraie flèche visuelle sur le cristal — les dégâts (3 PV, contre 5 au corps à corps) sont
  appliqués directement au cristal, même logique "harnais" que `AttackEterniaCrystalGoal`.
  Pensé pour être réutilisable tel quel par un futur ennemi à distance. Détail dans
  [02-gameplay.md](02-gameplay.md#le-goal-à-distance--entityairangedattacketerniacrystalgoaljava).
  **Testé en jeu** (2026-08-23).
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
  **Testé en jeu** (2026-08-23) : un bug trouvé et corrigé (voir "Corrections apportées") — le
  titre et les 4 libellés (Intervalle, Rayon, Vague début/fin) étaient invisibles (couleur
  sans canal alpha), l'écran semblait n'avoir aucune indication sur les champs.
- ✅ Aperçu de composition du spawner en phase Construction (`SpawnerBlockEntityRenderer`) :
  total d'ennemis à venir + détail par type affiché au-dessus du bloc, **visible à travers les
  murs** (`Font.DisplayMode.SEE_THROUGH`), comme dans le jeu de référence. Caché en phase
  Combat ; l'œuf d'invocation vanilla de chaque ennemi s'affiche à côté du texte de sa ligne
  (`ItemStackRenderState`, jamais vu en jeu — voir "Pistes prioritaires" plus bas). Détail dans
  [02-gameplay.md](02-gameplay.md#laperçu-de-composition-en-phase-construction--spawnerblockentityrendererjava).
  **Testé en jeu** (2026-08-23).
- ✅ Une vague se déroule maintenant de bout en bout : le Combat se **déclenche** via un vote
  "prêt" (clic droit sur le Cristal d'Eternia en Construction, data attachment **joueur**
  `ready`, remis à zéro pour tout le monde une fois le combat lancé) plutôt que seulement le
  harnais de test ; `wave_enemies_total` est calculé pour de vrai (registre des spawners
  actifs, `ModAttachments.ACTIVE_SPAWNERS`, sommé à l'entrée en Construction) ; dès que
  `wave_enemies_killed` l'atteint, retour automatique en Construction
  (`ModEvents.onMonsterDeath` → `PhaseTransitions.enterBuild`) **et** `current_wave` avance de
  1 (plafonné à `MAX_WAVE`). Détail dans
  [02-gameplay.md](02-gameplay.md#le-déroulement-dune-vague--initphasetransitionsjava-modeventsonmonsterdeath).
  **Testé en jeu** (2026-08-23) : vote "prêt", dégâts au cristal en combat (mêlée et
  squelette archer), et fin de vague/retour en Construction fonctionnent. Un bug trouvé et
  corrigé au passage : `WAVE_ENEMIES_KILLED` ne se remettait pas à 0 en repassant en
  Construction (voir "Corrections apportées"). Le harnais de test qui infligeait 10 dégâts au
  clic droit en Combat a été retiré (2026-08-24, décidé avec le joueur) : maintenant que les
  monstres endommagent le cristal pour de vrai, il n'a plus lieu d'être — le clic droit ne
  fait plus rien en Combat, `useWithoutItem` ne gère plus que le vote "prêt" en Construction.
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
  [02-gameplay.md](02-gameplay.md#le-monde-et-le-point-de-spawn). **Testé en jeu** (2026-08-23) :
  le monde se crée bien vide, la plateforme de la taverne apparaît et se repose correctement.
- ✅ Cristal de la taverne (`TavernCrystalBlock`, distinct d'`EterniaCrystalBlock` — pas de PV,
  pas de combat) : clic droit ouvre `MapSelectionScreen`, un carrousel de maps
  (`init/GameMap.java`, extensible, chaque entrée peut être masquée du carrousel tant qu'elle
  est en cours de conception) avec image d'aperçu + nom, et un choix de difficulté
  (Facile/Normal/Difficile). Le bouton "Jouer" applique réellement la difficulté choisie
  (`ModAttachments.DIFFICULTY`, via `SetDifficultyPayload`) et téléporte tous les joueurs vers
  `MapInstance` (l'emplacement partagé de "la map en cours", voir plus bas) — le choix de map
  précis, lui, n'a encore aucun effet (une seule map placeholder générique pour l'instant).
  Détail dans [02-gameplay.md](02-gameplay.md#la-taverne--choix-de-map-et-difficulté). **Testé
  en jeu** (2026-08-23), aller-retour taverne/map inclus (`/dd_leave`) : fonctionne.
- ✅ Victoire et défaite (`PhaseTransitions.onVictory/onDefeat`) : la partie se termine
  vraiment maintenant. Nettoyer la dernière vague (`current_wave == MAX_WAVE`) diffuse un
  message de victoire ; la destruction du Cristal d'Eternia diffuse un message de défaite. Les
  deux remettent la partie à zéro (vague 1, phase Construction) et diffusent un lien cliquable
  "Retour à la taverne" (commande `/dd_leave`, `MapInstance.returnToTavern`) qui nettoie
  l'emplacement de map et téléporte tout le monde. Le cristal détruit **n'est pas replacé
  automatiquement** — voir "Ce qui reste" plus bas. Détail dans
  [02-gameplay.md](02-gameplay.md#victoire-et-défaite--phasetransitionsonvictoryondefeat).
  **Testé en jeu** (2026-08-23).
- ✅ Coffre de mana (`block/ManaChestBlock.java`, `ManaChestBlockEntity.java`, 2026-08-24,
  feuille "Idées" du plan Excel du joueur) : meuble de map, comme le Cristal d'Eternia/le
  Spawner — posé par le créateur, pas par un joueur en jeu. Donne une quantité de mana
  configurable par map au clic droit en survie, une fois par vague (comparaison à
  `CURRENT_WAVE`), **quelle que soit la phase** (2026-08-26 : la restriction "Construction
  uniquement" a été retirée, un joueur peut vouloir du mana en pleine Combat). Comme dans le
  jeu de référence, le coffre **disparaît** une fois ouvert (invisible et traversable,
  propriété de blockstate `OPENED`) et **réapparaît** à la vague suivante
  (`ManaChestBlock#respawnAll`, appelé par `PhaseTransitions#enterBuild`, via un registre
  `ACTIVE_MANA_CHESTS` — même principe qu'`ACTIVE_SPAWNERS`) — **signalé cassé en jeu**
  (2026-08-26), revu en détail sans trouver de cause, voir "Ce qui reste" ci-dessous. Clic
  droit en créatif ouvre un écran de configuration (même patron que `SpawnerConfigScreen`, un
  seul champ) — configuration figée hors créatif, comme le spawner. Distribuera aussi des
  armes plus tard, hors scope pour l'instant (voir "Ce qui reste"). Détail dans
  [02-gameplay.md](02-gameplay.md#le-coffre-de-mana--blockmanachestblockjava).
- ✅ Suppression de tour (`client/TowerRemovalState.java`,
  `client/TowerRemovalClientEvents.java`, `network/RemoveTowerPayload.java`) : décidé avec le
  joueur (2026-08-26) sur le modèle du jeu de référence — touche dédiée (`remove_tower_mode`,
  `X` par défaut) pour entrer/sortir d'un mode suppression, puis clic gauche sur une tour visée
  pour la détruire instantanément et récupérer 50% de son coût en mana
  (`TOWER_MANA_REFUND_RATIO`, valeur de test comme les coûts de pose). Reste actif après une
  suppression pour en enchaîner plusieurs. Symétrique à la roue de pose côté serveur (phase
  Construction uniquement, revalidation complète, aucune confiance dans le client). Détail dans
  [02-gameplay.md](02-gameplay.md#la-suppression-de-tour--clienttowerremovalstatejava-clienttowerremovalclienteventsjava-networkremovetowerpayloadjava).
  **Jamais testé en jeu.**
- ✅ Casser un bloc est désactivé pour tout joueur non créatif (`ModEvents.onBlockBreakAttempt`,
  `BreakBlockEvent`) : décidé avec le joueur (2026-08-26), suite directe du point précédent —
  plutôt que de traiter les tours au cas par cas, plus aucun bloc ne se casse en survie, quel
  qu'il soit (terrain, taverne, tours...). Effet de bord voulu : le minage des tours à la
  pioche (qui laissait un item désormais inerte, voir plus haut) est réglé sans rien coder de
  spécifique aux tours — la touche dédiée devient de fait la seule façon de les retirer. Détail
  dans
  [02-gameplay.md](02-gameplay.md#casser-un-bloc-est-désormais-désactivé--modeventsonblockbreakattempt).
  **Jamais testé en jeu.**
- ⚠️ **Fusion locale de test (2026-08-26)** : `feature/mana-crystals` avait sa propre logique de
  remboursement au clic-pioche (`ModEvents.onTowerBreak`, même `TOWER_MANA_REFUND_RATIO`),
  écrite avant que `feature/tower-removal` n'introduise la touche dédiée. Les deux ensemble
  auraient été exploitables (`onTowerBreak` ne vérifiait pas `event.isCanceled()` avant de
  créditer le mana, donc combinable avec `onBlockBreakAttempt` pour du mana gratuit sans
  vraiment casser la tour) — `onTowerBreak` a été retiré dans cette branche de test. **À régler
  pour de vrai** avant/au moment de merger l'une des deux PR dans `main` : celle qui merge en
  second devra retirer ce handler (ou les réconcilier autrement).
- ✅ Bloc de spawn joueur (`ModBlocks.PLAYER_SPAWN`,
  `MapInstance#findAndConsumeSpawnMarker`) : décidé avec le joueur (2026-08-26), repris du plan
  Excel (feuille "Idées" > "CHOIX DE MAP") — le créateur d'une map peut poser ce bloc à
  l'endroit où les joueurs doivent apparaître ; `MapInstance.startGame` le cherche, le retire,
  et téléporte les joueurs sur sa position plutôt que sur `MAP_POS`. Retombe sur `MAP_POS`
  (comportement inchangé) si aucun n'est trouvé — **toujours le cas aujourd'hui**, le
  mécanisme n'a rien à trouver tant que `buildPlaceholderArena()` ne pose qu'un sol générique.
  **Pas concrètement testable avant qu'une vraie structure `.nbt` de map n'existe** — voir
  "Système de maps/structures" plus bas et
  [02-gameplay.md](02-gameplay.md#le-bloc-de-spawn-joueur--bloc-player_spawn-mapinstancefindandconsumespawnmarker).
- ✅ Écran de fin de partie (`client/gui/screen/GameOverScreen.java`,
  `network/GameOverPayload.java`) : décidé avec le joueur (2026-08-26), repris du plan Excel
  ("GUI avec rejouer ou taverne") — s'ouvre automatiquement sur chaque client à la victoire/
  défaite (en plus des messages système existants, pas à leur place), titre vert/rouge + deux
  boutons. "Rejouer" envoie `StartGamePayload`, exactement comme le bouton "Jouer" de
  `MapSelectionScreen` (même limite : ne restaure pas le Cristal d'Eternia s'il a été détruit,
  puisque `buildPlaceholderArena()` n'en pose de toute façon jamais un — voir "Ce qui reste"
  plus bas). "Retour à la taverne" exécute la commande de harnais `/dd_leave`, même effet que
  le lien cliquable déjà existant dans le chat. Premier paquet **clientbound** du mod (tous
  les autres vont du client vers le serveur) — handler enregistré côté client uniquement
  (`DungeonDefendersModClient`), pas dans `ModNetworking` (chargée des deux côtés), pour ne
  jamais charger de classe cliente sur un serveur dédié. **Jamais testé en jeu.** Détail dans
  [02-gameplay.md](02-gameplay.md#lécran-de-fin-de-partie--clientguiscreengameoverscreenjava-networkgameoverpayloadjava).

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

### Corrections trouvées lors des tests en jeu du 2026-08-23

Première vraie session de test en jeu (taverne, cristal en combat, spawners, tours, roue des
tours) — tous les points suivants ont été trouvés en jouant, pas en relisant le code.

| Problème | Correction |
|---|---|
| Tout le texte du HUD (mana/vie/vague/score/...) était invisible | `TEXT_COLOR` sans canal alpha (`0xFFFFFF`, alpha=0) dans 3 écrans + les overlays HUD — `GuiGraphicsExtractor.text()` ignore le rendu si `alpha == 0` ; corrigé en `0xFFFFFFFF` partout |
| La tourelle Harpoon ne tirait jamais | `lastFireTick` initialisé à `Long.MIN_VALUE` : `now - lastFireTick` débordait vers un nombre toujours négatif (overflow sur `long`), le cooldown ne se déclenchait jamais — remplacé par `-attackIntervalTicks` |
| La flèche de la tourelle ne partait jamais (figée sur place) | elle apparaissait au centre du bloc de la tourelle, un cube plein — `AbstractArrow#tick` fige toute flèche dont la position de spawn est déjà dans la géométrie solide du bloc sous elle ; origine décalée de 0.6 bloc devant la face avant |
| Rotation de la roue des tours impossible | touche par défaut `T`, déjà prise par le chat vanilla (`key.chat`) — changée en `G` |
| La tour tirait à l'opposé de la range affichée à la pose | le cône utilisait `Direction.toYRot()`, convention différente de celle du blockstate posé |
| Est/Ouest inversés dans l'aperçu de rotation (après le correctif précédent) | les valeurs `y` du blockstate, correctes pour Minecraft, ne le sont pas pour `Axis.YP.rotationDegrees(...)` (rotation main-droite JOML, sens inverse avec les axes de Minecraft) — vérifié par calcul, Est et Ouest inversés dans `facingYRot()` |
| Le compteur `Ennemis : X/Y` ne se remettait pas à 0 en repassant en Construction | `PhaseTransitions.enterBuild()` oubliait de remettre `WAVE_ENEMIES_KILLED` à 0 |
| `Ennemis : X/10` restait bloqué sur la valeur par défaut avant la première fin de vague | `WAVE_ENEMIES_TOTAL` ne se recalculait qu'aux transitions de phase — recalculé désormais aussi à la pose/casse/reconfiguration d'un spawner |
| `StackOverflowError` au chargement/à la pose d'un spawner | le recalcul ci-dessus, appelé directement depuis `setLevel()`, récursait via `getBlockEntity` (appelé avant l'insertion du block entity dans le chunk) — différé via `serverLevel.getServer().execute(...)` |
| Les monstres montaient sur les tours (Spike Blockade, Harpoon Turret) et continuaient leur chemin par-dessus | hitbox par défaut = cube plein de 1 bloc, sautable par n'importe quel monstre — `getCollisionShape`/`getShape` surchargés dans les deux blocs pour renvoyer une boîte de 1,5 bloc de haut (même principe que les murs/barrières vanilla) |

## Ce qui reste

### Pas de texture dédiée

`models/block/eternia_crystal.json` pointe sur `minecraft:block/diamond_block`. Le bloc est
donc visible et cohérent, mais ressemble à un bloc de diamant. Il faut créer
`textures/block/eternia_crystal.png` et mettre à jour le modèle — idéalement un modèle de
cristal, pas un cube plein, puisque la hitbox fait déjà 3 blocs de haut.

Même situation pour `models/block/spike_blockade.json`, qui pointe sur
`minecraft:block/dripstone_block` en attendant `textures/block/spike_blockade.png`. Même
situation encore pour `models/block/mana_chest.json`, qui pointe sur
`minecraft:block/barrel_top`.

### Les coffres ne réapparaissent pas en jeu — cause non trouvée

Signalé en testant en jeu (2026-08-26). Relu en détail (`ManaChestBlock#respawnAll`,
`PhaseTransitions#enterBuild`/`resetGameState`, `ModAttachments.ACTIVE_MANA_CHESTS`) sans
trouver de bug : les deux points d'entrée en Construction appellent bien `respawnAll`, le
registre suit le même patron qu'`ACTIVE_SPAWNERS` (fonctionnel, confirmé en jeu), et le
changement de blockstate utilise le mécanisme vanilla standard. Pas corrigé faute d'avoir
trouvé la vraie cause — détail dans
[02-gameplay.md](02-gameplay.md#disparition-et-réapparition-visuelles--manachestblockopened-respawnall).
À retester avec un scénario précis (un seul coffre, harnais de test du spawner pour changer de
phase manuellement) pour resserrer le diagnostic la prochaine fois.

### Le coffre ne fait pas tomber l'excédent de mana au sol

Demandé le 2026-08-26 : si le mana du joueur est déjà proche du maximum à l'ouverture, la
partie qui dépasserait 100 devrait tomber au sol sous forme ramassable plutôt que d'être
perdue — actuellement `tryOpen` plafonne juste au maximum (`Math.min(MAX_MANA, ...)`) sans
rien faire du surplus. Pas implémenté : dépend probablement de `ManaCrystalEntity`
(`feature/mana-crystals`, PR #12, pas encore mergée dans cette branche) pour représenter le
mana qui tombe — même dépendance déjà notée plus haut pour la distribution d'armes. À reprendre
une fois #12 mergée.

### Le coffre de mana ne distribue pas encore d'armes

Décidé avec le joueur : construit maintenant pour le mana uniquement (déjà utile tel quel),
les armes suivront une fois qu'il y en aura à distribuer (feuille "Armes" du plan Excel,
actuellement vide — rien à distribuer). `ManaChestBlockEntity` n'a pour l'instant qu'un seul
champ configurable (`manaAmount`) ; ajouter des armes demandera probablement une vraie liste
de loot façon `SpawnerBlockEntity.SpawnEntry`, pas juste un champ de plus.

### Le mana du coffre est donné directement, pas sous forme de cristal ramassable

`ManaChestBlockEntity#tryOpen` fait directement `player.setData(ModAttachments.MANA, ...)` —
contrairement au mana des monstres tués, qui tombe sous forme de `ManaCrystalEntity` ramassable
au sol (voir `feature/mana-crystals`, PR #12). Décidé avec le joueur (2026-08-24) : à revoir
une fois la PR #12 mergée dans `main` — `ManaChestBlock` pourrait alors faire tomber un ou
plusieurs cristaux au lieu de donner le mana instantanément au clic, pour rester cohérent avec
le reste du système de mana. Pas fait maintenant : `ManaChestBlock` a été développé sur une
branche partie de `main`, qui ne contient pas encore `ManaCrystalEntity`.

### Le mana n'a pas encore de vraie capacité/sort qui le consomme

Le mana a désormais une vraie utilité (coût de pose des tours, voir "Ce qui est implémenté" et
[02-gameplay.md](02-gameplay.md#le-mana-du-joueur)) et remonte via les cristaux lâchés par les
monstres (voir "Les cristaux de mana" plus bas, `ManaCrystalEntity`) — **pas de régénération
passive dans le temps**, décidé avec le joueur, uniquement via les cristaux. `ManaTestWandItem`
(retire 10 de mana au clic droit) reste un harnais de test au même titre que le clic droit sur
le cristal, maintenant redondant avec la vraie dépense (pose d'une tour) mais gardé pour tester
rapidement sans avoir à poser quoi que ce soit. Aucun sort/capacité de joueur ne consomme
encore de mana (les emplacements de compétences sont toujours inertes, voir plus bas).

### Les cristaux de mana (drop des monstres, `ManaCrystalEntity`)

Décidé avec le joueur, comme le vrai Dungeon Defenders : chaque monstre tué (toute phase, pas
seulement Combat) lâche un cristal de mana, ramassé en marchant dessus — jamais un item
d'inventaire. Premier vrai `Entity` custom du mod (`extends ExperienceOrb`, réutilise sa
physique/magnétisme/fusion, réécrit seulement `playerTouch` pour donner du mana au lieu d'XP).
Un seul palier pour l'instant (`ManaCrystalType.SMALL`, 5 mana), le joueur en prévoit au moins
6 à terme (couleurs/valeurs différentes) — pas encore construit, juste la structure prête
(enum extensible). Les cristaux dans des coffres entre les vagues sont **explicitement hors
scope** pour l'instant (reporté par le joueur).

**Limite assumée** : le rendu réutilise tel quel le renderer vanilla de l'orbe d'XP — le
cristal de mana a donc l'air d'une orbe d'XP verte/jaune, pas de couleur "mana" (bleue) dédiée.
Les vraies orbes d'XP vanilla sont désactivées pour tout `Monster` du mod
(`ModEvents.onExperienceDrop`, annule `LivingExperienceDropEvent`) — nécessaire pour éviter
qu'une vraie orbe d'XP fusionne avec un cristal de mana de même valeur (`ExperienceOrb` fusionne
les orbes proches en fonction de leur seule valeur numérique, pas de leur type réel), en plus
d'être thématiquement cohérent (ce mod a son propre système `experience`, sans rapport avec
l'XP vanilla).

Détail complet dans
[02-gameplay.md](02-gameplay.md#les-cristaux-de-mana--entitymanacrystalentityjava-initmanacrystaltypejava).

### Hotbar masquée sans remplacement (la faim, elle, est définitivement abandonnée)

`FOOD_LEVEL` est masqué et n'a plus vocation à être remplacé : décidé avec le joueur, ce mod
n'a pas de mécanique de faim du tout (`ModEvents.onPlayerTick` la maintient au maximum en
permanence). `HOTBAR` reste masquée sans équivalent custom pour l'instant : le joueur ne voit
plus l'objet qu'il a en main ni sa barre d'objets, et les touches 1-9/la molette ne
sélectionnent plus rien (voir "Ce qui est implémenté" plus haut) — en attendant le futur
système "un item par main" annoncé par le joueur (pas encore conçu ni implémenté), c'est une
vraie perte d'information en jeu, pas seulement esthétique — à garder en tête en testant (voir
[06-a-tester.md](06-a-tester.md)).

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

### Système de tours (catégories "Blockade" et "Turret" démarrées)

Discuté avec le joueur : les tours ne seront pas toutes construites sur le même patron —
il envisage au moins 5 catégories, chacune avec ses propres règles :

1. **Blocks passifs** et **corps à corps** — **unifiées en une seule catégorie de code,
   "Blockade"** (décision explicite du joueur) : bloquent le passage, avec un booléen optionnel
   pour infliger des dégâts au contact. Un block passif n'est qu'une Blockade avec ce booléen à
   `false`. **Spike Blockade en fait partie (booléen à `true`), et c'est le seul membre concret
   pour l'instant** (voir "Ce qui est implémenté" plus haut et
   [02-gameplay.md](02-gameplay.md#la-catégorie-blockade--blockentityabstractblockadeblockentityjava-tag-dungeon_defendersblockades)).
2. **Tours à distance** — catégorie de code **"Turret"**, démarrée avec le **Harpoon Turret**
   (voir "Ce qui est implémenté" plus haut et
   [02-gameplay.md](02-gameplay.md#le-harpoon-turret--blockharpoonturretblockjava)) : scanne et
   tire elle-même toute seule à chaque tick, dans un cône orienté (angle fixe) — ça, c'est
   toujours la tour qui agit, pas un `Goal` porté par un monstre. Un monstre peut en revanche
   désormais **l'attaquer** en dernier recours (voir "Système de priorité IA" plus bas). Ne
   bloque pas spécialement le passage plus qu'un bloc plein normal, mais ce n'est pas son rôle
   (posée en retrait).
3. **Auras et pièges non attaquables** — pas de PV, pas destructibles par les ennemis ; juste
   une durée ou un taux d'utilisation limité (charges).
4. **Pièges de sol** — un "sur-bloc" (pas un bloc plein) posé sur le sol, non attaquable —
   c'est en fait le mécanisme de l'ancien `SpikeTrapBlock` (dégâts via `stepOn`), retiré au
   profit du vrai Spike Blockade (catégorie 1) mais qui pourrait revenir sous cette catégorie
   plus tard, sous un autre nom du plan Excel (ex. une des trap de la Huntress).
5. D'autres catégories, pas encore réfléchies par le joueur.

**Architecture de code — base commune à toutes les catégories, pas juste Blockade** : le joueur
avait explicitement demandé d'établir une base de code pour "Blockade" avant même un second
exemple de cette catégorie précise. Avec le Harpoon Turret (catégorie sœur "Turret"), une
**vraie duplication entre catégories** est apparue pour de bon (PV, coût mana, persistance,
sync) — extraite maintenant, avec deux exemples concrets pour la constater plutôt que la
deviner :

- `block/entity/AbstractTowerBlockEntity.java` (nouveau) porte ce qui est commun à **toute**
  tour, Blockade ou Turret : PV (`maxHealth`), coût en mana à la pose (`manaCost`, consommé via
  `ModEvents.onTowerPlace`), persistance et sync client. `AbstractBlockadeBlockEntity` et
  `AbstractTurretBlockEntity` en héritent, et n'ajoutent que leur spécifique (dégâts de contact
  pour l'une, portée/cône/tir pour l'autre).
- **Les catégories 3 à 5 restent sans base de code pour l'instant** : chacune sera construite
  en autonomie d'abord (comme prévu à l'origine), sauf nouvelle décision explicite du joueur au
  moment de s'y attaquer — la généralisation systématique dès le départ n'est pas la règle,
  seulement le résultat d'une vraie duplication constatée (comme ici entre Blockade et Turret).

### Système de priorité IA (Block / Corps à corps / Cristal / Tourelle)

Discuté et tranché avec le joueur : au lieu d'une priorité figée en dur entre deux cibles
(Blockade puis cristal, comme avant), un vrai système à **paliers**, commun à toute cible
attaquable — y compris les tourelles, jusque-là totalement ignorées par l'IA :

1. **Block** (mur pur, pas de dégâts actifs) — priorité 10, la plus haute.
2. **Corps à corps** — priorité 20. **Pas une nouvelle catégorie de bloc** : c'est exactement
   `dealsContactDamage=true`, déjà utilisé par Spike Blockade (dégâts périodiques dans un
   petit rayon, sa propre cadence) — confirmé avec le joueur en comparant à des tours comme
   Slice N Dice/Bouncer de son plan Excel, qui suivent le même principe.
3. **Cristal d'Eternia** — priorité 30.
4. **Tourelle** — priorité 40, la plus basse : un monstre de mêlée ne s'y attaque qu'en tout
   dernier recours, si rien de plus prioritaire n'est à portée. Nouveau comportement, jamais
   possible avant (les tourelles étaient ignorées).

Indices **espacés** (10/20/30/40, pas 1/2/3/4), sur demande du joueur, pour laisser de la place
à un futur mécanisme de provocation ou un nouveau type de tour sans décaler les valeurs
existantes.

**Architecture, décidée avec le joueur** : un seul `Goal` compare toutes les cibles à portée
selon ce chiffre, plutôt que d'empiler une classe par palier (l'approche initiale, celle
utilisée par les anciens `AttackBlockadeGoal`/`AttackEterniaCrystalGoal`, tous les deux
**supprimés**) :

- `block/entity/AiAttackTarget.java` (nouvelle interface) : contrat `getAiPriority()` +
  `damage(int)`, implémenté par `AbstractTowerBlockEntity` (donc Blockade et Turret) **et**
  indépendamment par `EterniaCrystalBlockEntity` (aucun lien de code avec les tours, mais le
  même contrat).
- `entity/ai/AttackPriorityTargetGoal.java` (nouveau, remplace les deux goals supprimés) :
  réimplémente la recherche de `MoveToBlockGoal` en **une passe par palier** (10 puis 20 puis
  30 puis 40) — le premier palier qui trouve une cible dans sa propre portée gagne, même si un
  palier suivant a une cible plus proche. Plus besoin du tag `dungeon_defenders:blockades`
  (supprimé avec `init/ModBlockTags.java`) : le filtre se fait directement sur l'interface,
  générique à toute catégorie présente ou future.
- `ModEvents.onMonsterSpawn` : les monstres non-archers reçoivent désormais un seul goal
  (`AttackPriorityTargetGoal`) au lieu de deux. Les archers sont inchangés
  (`RangedAttackEterniaCrystalGoal`, ignorent toujours Blockade/Turret).

Détail complet dans
[02-gameplay.md](02-gameplay.md#le-goal-de-mêlée-unifié--entityaiattackprioritytargetgoaljava-blockentityaiattacktargetjava).

**Reste à faire** : équilibrage réel des coûts en mana (30/50, valeurs de test, pas
réfléchies — le remboursement à la casse existe désormais, voir "Les cristaux de mana" plus
bas), indicateur visuel de PV restants (Blockade et Turret), pas de "block" pur concret pour
exercer le palier 10 (la logique le supporte, aucune tour ne l'utilise encore). Et bien sûr,
les catégories 3 à 5 n'ont aucune implémentation.

**Comment on pose les tours, décidé avec le joueur** : dans le vrai Dungeon Defenders, les
tours ne se posent pas depuis l'inventaire (la liste dépend du héros choisi) — une **roue
radiale** a donc été construite (`TowerWheelScreen`, touche dédiée), avec un mode pose en deux
étapes (viser puis orienter) affichant un hologramme (vert/rouge selon validité) et une zone de
portée (cercle ou cône) générique (voir "Ce qui est implémenté" plus haut et
[02-gameplay.md](02-gameplay.md#la-roue-de-sélection-des-tours-et-la-pose--clientguiscreentowerwheelscreenjava)
pour le détail complet). **Pas de filtrage par héros** pour l'instant (ce système n'existe pas
encore) — la roue liste toutes les tours, prête à être filtrée plus tard.

Décidé également : **la roue est l'unique façon de poser une tour**, toute catégorie confondue.
Le `BlockItem` classique a été neutralisé (`TowerBlockItem#useOn` ne fait plus rien, retiré de
l'onglet créatif) — plus de pose alternative, ni depuis l'inventaire créatif ni depuis un item
récupéré en jeu. Et **les tours ne se posent qu'en phase Construction** :
`ModEvents.onTowerPlace` (renommé depuis `onBlockadePlace`, généralisé à `AbstractTowerBlockEntity`
— point critique : sans ce renommage, un Harpoon Turret aurait échappé à la vérification de
mana/phase, "Turret" étant une catégorie sœur de "Blockade", pas descendante) refuse toute pose
hors de cette phase (message dédié, restauration du bloc précédent, même mécanisme que pour un
mana insuffisant) — vérifié une seconde fois côté client (la roue elle-même refuse de s'ouvrir
en Combat) pour éviter de faire tout le mode pose avant un refus final, mais le serveur reste
la seule autorité réelle.

### La partie se termine, mais sans conclusion visuelle complète

Victoire et défaite existent maintenant (voir "Ce qui est implémenté" plus haut et
[02-gameplay.md](02-gameplay.md#victoire-et-défaite--phasetransitionsonvictoryondefeat)), et
depuis `GameOverScreen` (voir "Ce qui est implémenté" plus haut), plus seulement via un message
système — mais il reste des trous :

- Le Cristal d'Eternia détruit à la défaite **n'est pas replacé automatiquement** —
  `resetGameState` remet les compteurs à zéro, mais le bloc reste absent tant que personne
  n'en repose un à la main. Ça fait partie de la future remise à neuf d'une map (structure
  reposée, tours retirées, PV du cristal restaurés), pas de ce morceau. Cliquer "Rejouer" sur
  `GameOverScreen` ne le corrige pas non plus — voir la note dans sa propre entrée.
- `Échap` ferme `GameOverScreen` sans rien faire, et rien n'empêche de continuer à jouer sur la
  vague 1 fraîchement réinitialisée sans avoir cliqué un bouton — le nouvel écran atténue la
  confusion "partie terminée vs. pause entre deux vagues" (bien plus visible qu'un message
  système), sans l'éliminer complètement.

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
- Le **mécanisme** du bloc de spawn joueur (`PLAYER_SPAWN`,
  `findAndConsumeSpawnMarker`, voir "Ce qui est implémenté" plus haut) : prêt à remplacer le
  repli sur `MAP_POS` dès qu'une vraie structure en pose un, mais rien à trouver tant que
  `buildPlaceholderArena()` ne pose qu'un sol générique.

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

### Gametests : deux premiers, tous deux sur de la logique pure

`./gradlew runGameTestServer` (voir [03-build-et-lancement.md](03-build-et-lancement.md))
exécute maintenant deux tests (`gametest/DungeonDefendersGameTests.java`) — vérifiés
réellement en lançant la commande, pas seulement compilés :

- `eternia_crystal_damage` : le cristal démarre à `Config.DEFAULT_HEALTH` PV, encaisse
  correctement les dégâts, et le bloc disparaît à 0 PV.
- `phase_transitions` : `PhaseTransitions.enterCombat`/`enterBuild` mettent bien à jour
  `GAME_PHASE`, et `enterBuild` remet `WAVE_ENEMIES_KILLED` à 0 (la régression corrigée le
  2026-08-23).

L'API de gametest vanilla a été entièrement refondue dans cette version (système de
"fonctions" de test enregistrées dans un registre `BuiltInRegistries.TEST_FUNCTION` bootstrapé
une seule fois au chargement du jeu, bien avant qu'un mod ait la main — pas de point
d'accroche exploitable) : `ModGameTestInstance` (dans le même package) contourne ce problème
en gardant sa propre table nom → fonction plutôt que de dépendre de ce registre, et s'enregistre
directement en code via `RegisterGameTestsEvent#registerTest` (NeoForge), sans passer par du
JSON de datapack. Structure de test partagée par les deux :
`data/dungeon_defenders/structure/gametest/empty.nbt`, un gabarit 3×3×3 sans le moindre bloc
(les deux tests posent/lisent leurs blocs eux-mêmes, pas besoin de décor).

Volontairement limité à de la logique déterministe et synchrone (pas de mob, pas de minuteur,
pas plusieurs ticks à attendre) : les scénarios les plus simples à rendre fiables. D'autres
tests (dégâts de contact d'une Blockade, tir d'une Turret, spawn effectif d'un monstre...)
demanderaient de gérer le temps qui passe et l'IA, plus sujets aux faux négatifs — pas
tentés pour l'instant.

### Les loot tables de tours sont devenues du code mort... en survie seulement

Réglé indirectement par "Casser un bloc est désormais désactivé" (voir "Ce qui est implémenté"
plus haut) : `BreakBlockEvent` étant annulé avant que le bloc ne soit retiré, un joueur non
créatif ne peut plus jamais faire tomber `data/dungeon_defenders/loot_table/blocks/
spike_blockade.json`/`harpoon_turret.json` en cassant une tour. **Un joueur créatif le peut
toujours** — vérifié dans les sources décompilées
(`ServerPlayerGameMode#destroyBlock`/`destroyAndAck`) : même la casse instantanée créative
passe par `BreakBlockEvent` (`CommonHooks.fireBlockBreak`), donc par le même handler, qui
laisse simplement passer sans l'annuler quand `player.isCreative()` est vrai — la loot table
reste donc atteignable en créatif exactement comme avant. Sans conséquence (un item créatif
n'a pas d'importance), pas supprimé pour l'instant, juste un reliquat qui pourrait être nettoyé
plus tard.

## Reliquats du template

| Fichier | Reliquat |
|---|---|
| `TEMPLATE_LICENSE.txt` | licence MIT du template lui-même (fichiers de gabarit NeoForge MDK), distincte de la licence du mod (`All Rights Reserved`) — conservée volontairement, c'est une vraie mention légale, pas du code mort |

~~`README.md` encore celui du MDK~~, ~~`Config.java` spec d'exemple jamais enregistrée~~,
~~`DungeonDefendersModClient` enregistrait un écran de config pour une config inexistante~~ et
~~`accesstransformer.cfg` élargissait 3 méthodes de `Display` inutilisées depuis le retrait du
code `TextDisplay`~~ — tous résolus le 2026-08-24 : `README.md` renseigné avec les vraies
métadonnées (renvoie vers `doc/`), `Config.java` a une vraie spec (`defaultHealth`,
`damagePerHit`, `searchRange`) enregistrée via `container.registerConfig` dans
`DungeonDefendersMod` (l'écran de config du client affiche donc maintenant du contenu réel), et
`accesstransformer.cfg` supprimé (confirmé inutilisé par recherche globale).

## Pistes prioritaires

1. Créer les textures et vrais modèles du cristal et du Spike Blockade.
2. ~~Renseigner le `README.md` avec les vraies métadonnées~~ — fait (2026-08-24).
3. ~~Externaliser les constantes de gameplay (`DEFAULT_HEALTH`, `DAMAGE_PER_HIT`,
   `SEARCH_RANGE`) dans `Config`, et enregistrer la spec~~ — fait (2026-08-24). `DIFFICULTY`
   (`GameDifficulty`) partage le même genre de constantes en dur ailleurs (`DifficultyScaling`)
   si une prochaine passe veut continuer dans cette direction.
4. Retirer le harnais de test du clic droit quand une autre source de dégâts existera.
5. Donner une vraie utilité au mana côté **sorts/capacités du joueur** (la pose de tours et le
   ramassage de cristaux existent déjà), retirer `ManaTestWandItem`/`ManaFillWandItem`, puis
   habiller
   `ManaOverlay`/`HealthOverlay`/`ExperienceOverlay` de vraies textures (sprites, cadre) une
   fois disponibles — la forme (losange) se rapproche déjà du jeu de référence, il manque la
   matière.
6. ~~Faim~~ tranché (2026-08-24) : pas de mécanique de faim du tout, définitivement masquée.
   Reste la hotbar : concevoir et implémenter le futur système "un item par main" annoncé par
   le joueur, pour remplacer les 9 emplacements masqués (touches 1-9/molette déjà neutralisées,
   voir "Ce qui est implémenté").
7. Définir un vrai système d'expérience/score/niveaux : comment `EXPERIENCE`, `SCORE` et
   `LEVEL` se nourrissent l'un l'autre (aujourd'hui trois compteurs indépendants, tous
   bloqués à leur valeur par défaut, comme le mana avant `ManaTestWandItem`).
8. ~~Définir le déroulement des vagues~~ — fait : vote "prêt" pour déclencher le Combat,
   `WAVE_ENEMIES_TOTAL` juste (registre des spawners actifs), retour automatique en `BUILD`
   dès que `WAVE_ENEMIES_KILLED` l'atteint, `CURRENT_WAVE` qui avance (plafonné à `MAX_WAVE`),
   et maintenant victoire (dernière vague nettoyée) / défaite (cristal détruit) — voir "Ce qui
   est implémenté" et
   [02-gameplay.md](02-gameplay.md#le-déroulement-dune-vague--initphasetransitionsjava-modeventsonmonsterdeath).
   ~~Un vrai écran de fin de partie~~ — fait aussi : `GameOverScreen` (voir "Ce qui est
   implémenté"). Reste ouvert : remettre en jeu le cristal détruit automatiquement — voir la
   section dédiée plus haut.
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
14. ~~Ajouter une icône par type de monstre dans l'aperçu de composition du spawner~~ — fait
    (2026-08-24) : chaque ligne de détail affiche maintenant l'œuf d'invocation vanilla de
    l'ennemi (`SpawnableEnemy#spawnEggItem`), rendu via `ItemStackRenderState`/
    `ItemModelResolver` à côté du texte. **Jamais vu en jeu** (pas d'affichage possible dans cet
    environnement de dev) : taille (`ICON_SIZE`) et décalage (`ICON_GAP`) sont une première
    estimation à ajuster une fois testé — voir la checklist dédiée dans
    [06-a-tester.md](06-a-tester.md). Limite connue : contrairement au texte (`SEE_THROUGH`),
    l'icône est bloquée par les murs — pas d'équivalent "à travers les murs" pour le rendu
    d'item trouvé dans l'API de rendu de cette version.
15. Système de maps/structures : ~~monde vide + point de spawn fixe~~, ~~écran de choix de
    map/difficulté~~ et ~~mécanisme de chargement de map (placeholder)~~ faits (voir "Ce qui
    est implémenté"). Reste : la vraie structure de la taverne (remplacer la plateforme
    provisoire), au moins une vraie map avec sa vraie structure `.nbt` (remplacerait
    `MapInstance.buildPlaceholderArena()`), la réinitialisation tours/PV du cristal entre deux
    tentatives, le force-chargement pendant une partie, une bordure anti-chute dans le vide,
    un vrai point de sortie posé dans chaque map (plutôt que la commande de harnais
    `/dd_leave`). Voir la section dédiée dans "Ce qui reste" ci-dessus — c'est aussi ce qui
    rendra `ACTIVE_SPAWNERS` pleinement fiable (indépendant de la position du joueur).
16. ~~Coffre qui donne du mana entre les vagues~~ (feuille "Idées" du plan Excel) — fait
    (2026-08-24) : `ManaChestBlock`/`ManaChestBlockEntity`, meuble de map configurable,
    voir "Ce qui est implémenté". Reste : distribuer aussi des armes une fois qu'il y en aura
    à distribuer (feuille "Armes" du plan, vide pour l'instant).
