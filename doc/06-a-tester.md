# 6. À tester en jeu

Ce fichier liste ce qui a été écrit et compile, mais **jamais lancé en jeu** : le
développement s'est fait dans un environnement sans affichage (`./gradlew compileJava`
passe, mais `./gradlew runClient` n'a pas pu être vérifié visuellement). Coche au fur et à
mesure, et signale ici ce qui casse pour que ça reste une référence à jour.

Lancer le client de dev :

```bash
./gradlew runClient
```

## Le monde vide et le point de spawn (`data/minecraft/dimension/overworld.json`, `TavernSpawn.java`)

**Le point le plus à risque de tout le projet jusqu'ici** : c'est la première fois qu'un
fichier de données (pas de code Java) contrôle le comportement du jeu — `./gradlew build` ne
charge ni ne valide les datapacks, donc une erreur de syntaxe ou de structure dans
`overworld.json` ne se verrait **qu'au lancement**, probablement par un monde qui ne se crée
pas du tout ou une erreur au chargement. Impossible à vérifier sans lancer le client.

- [ ] **Créer un nouveau monde** avec le mod actif : la création ne doit **pas planter**. Si
      elle échoue, vérifier `run/logs/latest.log` pour une erreur de parsing autour de
      `overworld.json` (chemin exact, indentation JSON, valeur de `type`/`generator`).
- [ ] Une fois dans le monde : l'environnement doit être **entièrement vide** (pas de sol, pas
      de biome visible, ciel présent) — hormis la plateforme provisoire (voir plus bas). Si
      un sol "normal" apparaît (herbe, pierre generée), le remplacement du générateur n'a pas
      pris effet — vérifier que le fichier est bien à `data/minecraft/dimension/overworld.json`
      (namespace **`minecraft`**, pas `dungeon_defenders`).
- [ ] Le joueur doit apparaître **debout sur une plateforme carrée** (`smooth_stone`, environ
      9×9) centrée en `(0, 64, 0)`, pas en train de tomber dans le vide.
- [ ] Se déplacer hors de la plateforme : confirme qu'il n'y a **rien d'autre** autour (vide
      total), pas de terrain résiduel.
- [ ] Quitter le monde et le recharger : le joueur doit réapparaître **au même endroit**, sur
      la même plateforme (pas de nouvelle position aléatoire, pas de nouvelle plateforme
      décalée).
- [ ] **Casser un bloc de la plateforme** (pioche), puis quitter le monde et le recharger : le
      bloc cassé doit être **réapparu**, la plateforme entièrement reconstituée — vérifie que
      le contenu de la taverne est bien reposé à chaque chargement (volontaire, voir
      02-gameplay.md), pas juste construit une fois à la création du monde.
- [ ] Mourir (`/kill @s` ou tomber du bord) sans lit ni ancre de réapparition posés : doit
      réapparaître sur la plateforme, pas à un autre endroit du vide.
- [ ] Aller dans le Nether ou l'End (si accessible) : ces dimensions doivent rester **normales**
      (générées comme d'habitude) — seul l'Overworld doit être affecté.
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `TavernSpawn`,
      `LevelEvent.Load`, ou au chargement de la dimension `overworld`.

## Le cristal de la taverne et l'écran de choix de map (`TavernCrystalBlock`, `MapSelectionScreen`)

Premier écran du mod qui affiche une **image** (pas juste du texte/des widgets) — le calcul
d'UV du `blit` et le chargement de la texture `test_map.png` n'ont jamais pu être vérifiés
visuellement.

- [ ] Prendre `tavern_crystal` dans l'onglet créatif (texture provisoire : bloc d'améthyste,
      **différente** de celle du Cristal d'Eternia — diamant), le poser.
- [ ] Clic droit dessus : ouvre `MapSelectionScreen`, sans crash ni écran noir. **Aucune**
      interaction avec les PV/le combat ne doit se produire (contrairement au Cristal
      d'Eternia, ce bloc ne doit jamais être visé par un ennemi ni afficher de dégâts).
- [ ] L'écran affiche : à gauche une image (aplat de couleur bleu-gris, `test_map.png`) avec
      le nom "Map de test" en dessous et des flèches `◀`/`▶` de part et d'autre ; à droite
      trois boutons Facile/Normal/Difficile ; un bouton "Jouer" en bas. Rien ne doit se
      chevaucher ni sortir de l'écran.
- [ ] L'image doit s'afficher **entièrement remplie** de la couleur bleu-gris unie, sans
      déformation ni répétition/troncature visible — si l'image apparaît coupée, étirée de
      travers ou totalement absente (juste un cadre vide), c'est un signe que les UV du
      `blit` (`0,0,1,1`) ou le chemin de la texture sont faux.
- [ ] Cliquer sur `▶` puis `◀` : avec une seule map disponible (`TEST_MAP`), l'affichage doit
      **rester identique** à chaque clic (bouclage sur une liste à un seul élément) — pas de
      crash, pas d'image qui disparaît.
- [ ] Cliquer sur chacun des 3 boutons de difficulté : le bouton cliqué doit afficher un
      **marqueur de sélection** (`> Facile <` plutôt que `Facile`), et les deux autres revenir
      à leur libellé normal — un seul sélectionné à la fois.
- [ ] À l'ouverture, le bouton de difficulté déjà **sélectionné** doit correspondre à la
      difficulté actuelle de la partie (Normal par défaut, tant que rien ne l'a changée).
- [ ] Cliquer sur "Jouer" après avoir choisi "Difficile" : l'écran doit se fermer. Rouvrir
      l'écran (reclic sur le bloc) : la difficulté doit maintenant s'afficher comme
      **Difficile** déjà sélectionnée — vérifie que le choix a bien été appliqué côté serveur
      et resynchronisé (test le plus important de cette section).
- [ ] Cliquer sur "Jouer" (n'importe quelle map/difficulté) : doit **téléporter** vers un
      nouvel emplacement, loin de la taverne — une petite plateforme provisoire similaire à
      celle de la taverne (voir section dédiée ci-dessous pour le détail).
- [ ] Le choix de map dans le carrousel (flèches `◀`/`▶`) n'a **aucun effet** sur ce qui se
      passe au clic sur "Jouer" — normal, une seule map placeholder générique existe pour
      l'instant (voir 05-etat-et-problemes-connus.md), pas un bug.
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `TavernCrystalBlock`,
      `MapSelectionScreen`, `GameMap`, `SetDifficultyPayload`, `StartGamePayload`, ou au
      chargement de la texture `textures/gui/maps/test_map.png` (identifiant de texture
      introuvable, etc.).

## "Jouer" et le retour à la taverne (`MapInstance.java`, `/dd_leave`)

- [ ] Depuis la taverne, cliquer "Jouer" : tous les joueurs présents (pas juste celui qui a
      cliqué, si plusieurs) doivent être téléportés **ensemble** vers le même nouvel
      emplacement, loin des coordonnées de la taverne (`~10000, 65, 0`).
- [ ] À l'arrivée : une plateforme carrée (`smooth_stone`, ~17×17) doit être là, le joueur
      doit apparaître dessus, pas en train de tomber dans le vide.
- [ ] Taper `/dd_leave` : doit nettoyer la plateforme de map (tout redevient du vide à cet
      emplacement) et téléporter tous les joueurs présents vers la taverne — sur la
      plateforme de la taverne, pas ailleurs dans le vide.
- [ ] Retourner sur "Jouer" une seconde fois après un premier aller-retour : la plateforme de
      map doit être reposée normalement (pas de blocs résiduels d'un tour précédent, pas de
      trous), même chose pour la taverne après un `/dd_leave`.
- [ ] Casser un bloc de la plateforme de map, revenir à la taverne (`/dd_leave`), puis
      recliquer "Jouer" : la plateforme doit être entièrement reconstituée (le nettoyage +
      repose s'exécute à chaque "Jouer", pas juste la première fois).
- [ ] Après une **victoire** ou une **défaite** (voir section dédiée plus bas) : un message
      cliquable **"[Retour à la taverne]"** (bleu clair, souligné) doit apparaître juste après
      le message de victoire/défaite. Cliquer dessus doit avoir le même effet que taper
      `/dd_leave` — retour à la taverne.
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `MapInstance`, `ModCommands`,
      ou `StartGamePayload`.

## HUD — groupe bas-gauche (mana, vie, expérience)

- [ ] En bas à gauche de l'écran : deux **losanges** côte à côte (mana en bleu à gauche, vie
      en rouge à droite — pointes en haut/bas/gauche/droite, pas des rectangles), avec une
      barre horizontale verte (expérience) tout en bas, sous les deux losanges. Rien ne doit
      se chevaucher.
- [ ] Les losanges sont bien des losanges reconnaissables (largeur qui augmente puis diminue
      du bas vers le haut), pas des rectangles ni des formes crénelées illisibles — c'est un
      rendu "pixel art" (empilement de bandes de 1px), un léger crénelage sur les bords
      obliques est normal et attendu, ce n'est pas un bug.
- [ ] Le texte `Mana: 100/100` apparaît **au-dessus** de la pointe haute du losange mana,
      centré. Idem pour `Vie : 100/100` au-dessus du losange vie.
- [ ] Le texte `Expérience : 0/100` apparaît à droite de la barre verte (comme avant, ça n'a
      pas changé pour l'expérience).
- [ ] Les losanges mana/vie sont pleins (bleu/rouge sur toute leur hauteur) à `100/100` ; la
      barre d'expérience est vide (fond gris) à `0/100`.
- [ ] Les cœurs vanilla (habituellement en bas à gauche, au-dessus de la barre de faim) sont
      **absents** — remplacés par le losange rouge.
- [ ] Vérifier que la barre d'XP vanilla est bien invisible (`EXPERIENCE_LEVEL` masqué) et ne
      se confond pas avec la barre verte du mod.
- [ ] Prendre `mana_test_wand` dans l'onglet créatif Dungeon Defenders (icône bâton de
      blaze, pas de texture dédiée). Clic droit : le losange mana perd 10 % de sa hauteur
      **par le haut** (il se vide du haut vers le bas puisqu'il se remplit du bas vers le
      haut — la pointe haute disparaît en premier), le texte passe à `90/100`, un message
      système confirme `-10 mana (90/100)`.
- [ ] Répéter jusqu'à `0/100` : le losange mana doit être entièrement vide (fond gris), et un
      nouveau clic droit affiche le message « Pas assez de mana ! » sans repasser en négatif.
- [ ] Quitter le monde et y revenir (ou `/reload` + relog) : le mana affiché doit être celui
      d'avant la déconnexion, pas remis à 100 (l'attachment est censé persister).
- [ ] Se prendre des dégâts (chute, mob, `/damage`) : le losange vie diminue par le haut, en
      cohérence avec les dégâts subis.
- [ ] Se reconnecter (relog) après avoir perdu de la vie : la vie perdue doit être conservée,
      pas remise à 100/100 (seul un joueur qui était déjà à son maximum doit se retrouver à
      100/100 après coup).
- [ ] Un nouveau joueur (jamais connecté à ce monde) doit spawn à 100/100, pas à 20/20.
- [ ] Redimensionner la fenêtre : le groupe entier reste collé au bord bas-gauche, losanges et
      barre toujours alignés entre eux.

## HUD — vague

- [ ] Le texte `Vague 1/5` apparaît en haut à droite de l'écran, collé au bord droit (pas de
      jauge, juste du texte).
- [ ] Il reste stable (`1/5`) quoi qu'il se passe en jeu — rien ne le fait encore varier,
      c'est attendu.
- [ ] Redimensionner la fenêtre de jeu : le texte doit rester collé au bord droit (position
      recalculée à chaque frame via `guiWidth()`), pas figé à une position absolue de l'écran
      d'origine.

## HUD — progression de la vague

- [ ] Une grande jauge orange (240px de large) apparaît **centrée tout en haut de l'écran**,
      bien séparée du texte `Vague X/Y` (qui reste en haut à droite).
- [ ] Le texte `Ennemis : 0/10` est **superposé au centre de la jauge** (pas à côté), lisible
      par-dessus le fond gris.
- [ ] La jauge est vide (fond gris) puisque `0/10` : rien ne doit apparaître en orange tant
      que rien ne tue d'ennemis, c'est attendu.
- [ ] En redimensionnant la fenêtre, la jauge reste centrée horizontalement et collée au bord
      haut de l'écran, indépendamment de la position de `Vague X/Y`.

## HUD — phase

- [ ] Le texte `Phase : Construction` apparaît juste en dessous de la rangée `Vague X/Y` /
      `Ennemis : X/Y`, collé au bord droit.
- [ ] Il reste stable sur `Construction` — rien ne le fait encore changer, c'est attendu.
- [ ] Les trois lignes en haut à droite (vague+ennemis, phase) sont bien empilées sans se
      chevaucher ni se toucher.

## HUD — score et personnage (bas centre)

- [ ] Tout en bas de l'écran, centré horizontalement (pas collé à gauche ni à droite) :
      `Score : 0`, sans jauge, juste du texte.
- [ ] Juste au-dessus, toujours centré : `<pseudo Minecraft> - niv 1` — le nom affiché est
      `character_name`, un champ distinct du pseudo, mais qui vaut le pseudo Minecraft par
      défaut : à `niv 1`, il doit donc afficher exactement le pseudo du compte utilisé pour
      se connecter (aucun moyen de le changer pour l'instant, c'est attendu).
- [ ] Les deux lignes restent stables (`Score : 0`, `niv 1`) — rien ne les fait encore
      varier, c'est attendu.
- [ ] Le bloc score/personnage ne chevauche pas la barre d'expérience (bas gauche) : il doit
      y avoir un espace visible entre les deux, la barre d'expérience étant plus à gauche.
- [ ] Redimensionner la fenêtre : le bloc reste centré horizontalement et collé au bord bas.

## HUD — emplacements de compétences (bas gauche)

- [ ] 4 ronds apparaissent en bas à gauche de l'écran, **juste à droite du losange vie**,
      alignés horizontalement sur la même ligne que les losanges mana/vie, au-dessus de la
      barre d'expérience (dans l'ordre gauche → droite : soin, sort 1, sort 2, réparation —
      impossible à distinguer visuellement pour l'instant, pas d'icône, c'est attendu).
- [ ] Chaque rond a une fine bordure sombre autour d'un fond gris — vérifier que ce sont bien
      des ronds (pas des carrés), même remarque que pour les losanges : un léger crénelage sur
      le contour est normal avec cette technique de rendu.
- [ ] Rien ne se passe au clic dessus (ce ne sont pas des boutons pour l'instant, c'est
      attendu) et il n'y a pas d'erreur dans les logs.
- [ ] Ne chevauche ni les losanges mana/vie ni la barre d'expérience — il doit y avoir un
      petit espace visible entre le losange vie et le premier rond (soin).
- [ ] Redimensionner la fenêtre : le groupe entier (losanges + ronds + barre d'expérience)
      reste collé au coin bas-gauche et aligné.

## Le vote « prêt » (déclencheur du Combat) — `EterniaCrystalBlock`

Nouveau vrai déclencheur du Combat, à tester **avant** la section Spawner ci-dessous
puisqu'elle en dépend pour entrer en Combat autrement qu'avec le harnais de test.

- [ ] **En solo**, en phase Construction, clic droit à main nue sur le Cristal d'Eternia : un
      message "Prêt : 1/1" s'affiche, et la phase bascule **immédiatement** en Combat (un seul
      joueur = tout le monde est prêt dès son propre clic).
- [ ] **À plusieurs** (2+ joueurs), un seul clique : message "Prêt : 1/2" pour tout le monde,
      la phase **ne bascule pas encore**. Le second joueur clique à son tour : "Prêt : 2/2",
      puis bascule en Combat pour tout le monde.
- [ ] Re-cliquer sur le cristal **avant** que tout le monde soit prêt annule son propre "prêt"
      (redevient "Prêt : 0/2" si on était seul à avoir cliqué) — vérifie que c'est bien une
      bascule (toggle), pas juste un "je suis prêt" à sens unique.
- [ ] Une fois en Combat, clic droit à main nue sur le cristal inflige maintenant **10 PV de
      dégâts** (`dungeon_defenders.eternia_crystal.damaged`) — c'est l'ancien harnais de test,
      gardé mais désormais réservé à la phase Combat (plus de vote "prêt" pendant le combat).
- [ ] Après un Combat lancé via le vote, tuer tous les ennemis pour revenir en Construction
      (voir section Spawner) : re-cliquer sur le cristal doit à nouveau proposer le vote
      "prêt" (repart de 0/N, pas de résidu de l'ancien vote).
- [ ] Un joueur qui se déconnecte puis se reconnecte pendant que d'autres sont déjà prêts ne
      doit pas bloquer indéfiniment le décompte : il doit apparaître comme non-prêt (0 par
      défaut) et devoir cliquer comme les autres.

## Le Spawner (premier vrai gameplay, pas juste du HUD)

- [ ] Prendre `spawner` dans l'onglet créatif Dungeon Defenders (texture : cage de spawner
      vanilla), le poser.
- [ ] **Shift + clic droit** dessus : un message système confirme le changement de phase
      (`Phase changée : Combat` / `Phase changée : Construction`), et le texte `Phase : ...`
      en haut à droite du HUD change en conséquence — raccourci de test qui contourne le vote
      "prêt" ci-dessus (utile seul, sans avoir à se re-cliquer soi-même dessus).
- [ ] **Clic droit sans shift, en créatif** : ouvre l'écran de configuration (voir section
      dédiée ci-dessous). **En survie**, le même clic droit sans shift doit plutôt afficher le
      message "Les spawners ne sont configurables qu'en mode créatif" et ne rien ouvrir.
- [ ] **Avant même de passer en Combat une première fois** : `Ennemis : X/10` peut encore
      afficher l'ancienne valeur par défaut (`/10`) — normal, le total ne se calcule qu'à
      l'entrée en Construction, qui n'a encore jamais eu lieu explicitement au tout premier
      chargement du monde (limite connue, voir 05-etat-et-problemes-connus.md).
- [ ] Une fois en `Combat`, attendre : des **zombies et des squelettes** doivent apparaître
      au-dessus du bloc, les zombies plus fréquemment que les squelettes (nombre de base 15
      contre 5 par défaut — environ 3x plus de zombies sur la durée, pas un ratio exact vague
      par vague).
- [ ] Laisser tourner un moment : **chaque type doit finir par s'arrêter** une fois son
      plafond atteint (15 zombies et 5 squelettes par défaut, sur la vague 1 avec la
      difficulté Normal) — le spawner ne doit **pas** spawn indéfiniment tant qu'on reste en
      combat.
- [ ] Le squelette apparu **s'arrête à distance** du cristal (ne vient plus au corps à
      corps), lève son arc (pose vanilla "arc tendu"), puis tire une flèche visible vers le
      cristal — un message de dégâts doit apparaître côté serveur (`crystal.damage(3)`), même
      si la flèche elle-même ne "touche" pas physiquement le cristal (voir
      02-gameplay.md pour pourquoi). Nouveau comportement à vérifier en priorité dans cette
      section — voir aussi la section dédiée ci-dessous.
- [ ] Tuer un zombie ou un squelette apparu ainsi (ou n'importe quel autre monstre)
      **pendant que la phase est Combat** : le texte `Ennemis : X/20` (total = 15+5 par défaut,
      sur un seul spawner) doit voir son `X` s'incrémenter, et la jauge orange se remplir.
- [ ] **Tuer tous les ennemis de la vague** (les 20, avec un seul spawner par défaut) : la
      phase doit **repasser automatiquement en Construction**, avec un message système "Vague
      terminée ! Retour à la Construction." diffusé à tous les joueurs, **et** le texte
      `Vague X/5` du HUD doit passer de `1/5` à `2/5` — c'est le nouveau comportement
      principal à vérifier dans cette section.
- [ ] Sur la vague 2 (et suivantes), le total `Ennemis : X/Y` doit être **légèrement plus
      élevé** que 20 (multiplicateur de difficulté +10 %/vague — voir `DifficultyScaling`),
      pas figé à `/20` d'une vague à l'autre.
- [ ] Répéter jusqu'à la vague 5 : `Vague 5/5` atteint, la vague ne doit **pas** dépasser
      `5/5` même en nettoyant encore une vague après (plafonné à `MAX_WAVE`, pas de `6/5`) —
      pas de message de victoire pour l'instant, c'est un manque connu.
- [ ] Tuer un monstre **pendant que la phase est Construction** : `Ennemis : X/Y` ne doit
      **pas** bouger (le compteur ne compte que les morts en combat, c'est attendu).
- [ ] Rebasculer manuellement en Combat (shift + clic droit) **sans avoir cliqué sur le
      cristal** : le spawner doit **recommencer à spawn** normalement (vérifie que
      `COMBAT_SESSION` relance bien la progression de chaque spawner) — et la vague avance
      quand même au retour en Construction, comme n'importe quelle fin de combat.
- [ ] Poser **2 spawners** avec des compositions différentes : à l'entrée en Construction,
      `Ennemis : X/Y` doit afficher la **somme des deux** (`Y` = total du premier + total du
      second) — vérifie le registre `ACTIVE_SPAWNERS`.
- [ ] Poser un spawner **collé contre un mur/dans un renfoncement irrégulier**, mettre un
      **rayon de spawn de 3-4** (via l'écran de config), passer en Combat : aucun ennemi ne
      doit apparaître **à moitié ou totalement enlisé dans un bloc** — ils doivent tous
      apparaître dans un espace libre, quitte à être repliés sur juste au-dessus du bloc
      spawner si le rayon ne trouve rien de valide (`findSafeSpawnPos`).
- [ ] Casser le spawner à la pioche : il se drope (comme spike_trap), et l'accumulateur
      redémarre à 0 si on le repose (comportement attendu, pas de sauvegarde de position
      liée au bloc en tant que tel).
- [ ] Aucune erreur dans les logs au placement, au tick, ou à la casse du bloc — en particulier
      liée à `PhaseTransitions`, `ModAttachments.ACTIVE_SPAWNERS` ou `COMBAT_SESSION`.

## Victoire et défaite (`PhaseTransitions.onVictory/onDefeat`)

- [ ] Jouer jusqu'à la vague `5/5` (la valeur actuelle de `MAX_WAVE`) et la nettoyer
      entièrement (tuer tous les ennemis) : message **"Victoire ! Toutes les vagues sont
      nettoyées."** (vert, gras) diffusé à tous les joueurs — pas le message habituel "Vague
      terminée !".
- [ ] Après la victoire : `Vague X/5` doit être revenu à `1/5`, et `Phase : ...` doit être
      repassé à `Construction` — la partie doit être immédiatement rejouable depuis le début
      (pas besoin de relancer le monde).
- [ ] Sur une vague **avant** la dernière (ex. vague 2/5), la nettoyer : doit toujours afficher
      le message habituel **"Vague terminée !"**, pas le message de victoire — vérifie que la
      distinction "était-ce la dernière vague" fonctionne dans les deux sens.
- [ ] Après la victoire, un message **"[Retour à la taverne]"** (bleu clair, souligné) doit
      apparaître juste en dessous — cliquable, voir la section dédiée plus haut pour le détail.
- [ ] Détruire le Cristal d'Eternia (attaques répétées, ou clic droit dessus en Combat) :
      message **"Défaite ! Le Cristal d'Eternia est tombé."** (rouge, gras), en plus du
      message habituel de destruction du cristal — les deux doivent apparaître, suivis eux
      aussi du lien **"[Retour à la taverne]"**.
- [ ] Après la défaite : `Vague X/5` doit être revenu à `1/5`, `Phase : ...` repassé à
      `Construction` — mais le **bloc du cristal reste absent** (pas replacé automatiquement,
      comportement attendu pour l'instant, voir 05-etat-et-problemes-connus.md). Il faut le
      reposer à la main pour continuer à tester.
- [ ] Provoquer une défaite **en pleine vague 3** (par exemple) : vérifie que ça fonctionne à
      n'importe quel moment de la partie, pas seulement en fin de vague — la défaite doit
      pouvoir interrompre une vague en cours.
- [ ] Poser un spawner avec une plage de vagues incluant la vague 1 (ex. `waveStart=1`) et
      relancer une partie après victoire/défaite : les spawners doivent redémarrer
      normalement sur la nouvelle vague 1, pas rester bloqués sur l'ancien état de la partie
      précédente.
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `PhaseTransitions.onVictory`,
      `onDefeat`, ou `resetGameState`.

## Le squelette archer (`RangedAttackEterniaCrystalGoal`)

Premier ennemi à distance du mod, et première flèche jamais lancée par le mod — à vérifier
avec attention, aucun test visuel possible pendant le développement.

- [ ] Faire spawn un squelette (spawner ou `/summon minecraft:skeleton`) à plus de 10 blocs du
      cristal : il doit **courir vers le cristal**, puis **s'arrêter** avant d'être collé
      dessus (autour de 10 blocs, pas au corps à corps comme le zombie).
- [ ] Une fois arrêté : le squelette doit **lever son arc** (l'animation vanilla "arc tendu",
      la même que quand il vise un joueur) pendant environ 1 seconde, puis **tirer une
      flèche visible** en direction du cristal.
- [ ] La flèche tirée doit **voler vers le cristal** (trajectoire à peu près dans la bonne
      direction, avec un peu d'arc vers le haut) — elle peut manquer visuellement ou passer à
      travers/à côté du cristal, **ce n'est pas un bug** : les dégâts sont appliqués
      directement au cristal au moment du tir, indépendamment de la trajectoire réelle de la
      flèche (voir 02-gameplay.md).
- [ ] Le cristal doit perdre **3 PV** à chaque tir (message `dungeon_defenders.eternia_crystal
      .damaged`), au rythme d'environ un tir toutes les 2 secondes (tension + pause).
- [ ] Faire venir un squelette très près du cristal (le pousser, ou le faire spawn juste à
      côté) : il doit quand même tirer (pas de recul particulier attendu pour l'instant), pas
      de crash ni de comportement bizarre à bout portant.
- [ ] Interrompre le squelette en pleine tension (le pousser hors de portée juste après qu'il
      commence à lever l'arc) : il doit **annuler proprement** la tension (l'animation d'arc
      tendu doit s'arrêter) et reprendre l'approche, pas rester bloqué en position de tir.
- [ ] Tuer le squelette pendant qu'il tire : pas d'exception dans les logs.
- [ ] Un zombie à proximité doit continuer à se comporter normalement (corps à corps) — les
      deux comportements ne doivent pas se mélanger entre types d'ennemis.
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `RangedAttackEterniaCrystalGoal`
      ou à la création de l'entité `Arrow`.

## L'écran de configuration du spawner (GUI custom, liste dynamique, avec réseau)

C'est le morceau le plus à risque de cette session (aucun test visuel possible pendant le
développement, ni pour la reconstruction dynamique des widgets à l'ajout/retrait de lignes) —
à vérifier avec le plus d'attention.

- [ ] **En mode créatif**, clic droit (sans shift) sur un spawner ouvre bien un écran, sans
      crash ni écran noir (voir aussi la section "Le Spawner" ci-dessus pour le test du
      verrou créatif en survie).
- [ ] L'écran affiche 4 champs numériques (Intervalle, Rayon de spawn, Première/Dernière vague
      active) avec leurs libellés, **2 lignes de composition** (un bouton avec le nom de
      l'ennemi + un champ nombre + un bouton "X" chacune), un bouton "+ Ajouter un ennemi" et
      un bouton "Valider" — rien ne doit se chevaucher ni sortir de l'écran.
- [ ] Les champs sont **pré-remplis** avec les valeurs par défaut du spawner tout juste posé
      (`20`, `0`, `1`, `5`, puis "Zombie"/`15` et "Squelette"/`5` sur les deux lignes).
- [ ] Taper des lettres dans un champ numérique : rien ne s'affiche (filtre chiffres uniquement).
- [ ] **Cliquer sur le bouton d'une ligne** (nom de l'ennemi) : son libellé change pour
      l'ennemi suivant (Zombie → Squelette → Zombie...), sans reconstruire tout l'écran (pas
      de clignotement des autres champs). Comme il n'y a que 2 `SpawnableEnemy` pour l'instant,
      cycler doit systématiquement échanger avec l'autre ligne si elle utilise déjà l'autre
      type (jamais deux lignes sur le même ennemi).
- [ ] **Bouton "X"** : le retire, l'écran se reconstruit (les lignes/boutons suivants se
      replacent correctement) — sauf s'il ne reste qu'une ligne, où le bouton "X" doit être
      **absent** (toujours garder au moins un ennemi).
- [ ] **Bouton "+ Ajouter un ennemi"** : ajoute une ligne (nombre de base `0`), l'écran se
      reconstruit. Avec seulement 2 `SpawnableEnemy` disponibles, le bouton doit **disparaître**
      dès que les 2 lignes sont présentes (liste fermée, rien à ajouter de plus pour l'instant).
- [ ] Modifier une valeur dans un champ, ajouter/retirer une ligne, **puis** modifier une
      autre valeur : vérifier que les valeurs saisies avant le rebuild n'ont pas été perdues
      (elles sont recopiées dans l'état en mémoire de l'écran avant chaque reconstruction —
      voir `SpawnerConfigScreen.syncFieldsToState()`).
- [ ] Modifier la composition (ex : mettre `30` sur la ligne Zombie), cliquer "Valider" :
      l'écran se ferme, aucune erreur dans les logs.
- [ ] **Rouvrir l'écran du même spawner** (clic droit à nouveau) : la nouvelle valeur (`30`) et
      la composition (nombre de lignes, type de chaque ligne) doivent apparaître telles que
      validées — pas les anciennes. C'est le test le plus important : il vérifie que la config
      a bien été appliquée côté serveur **et** resynchronisée vers le client.
- [ ] Avec le spawner en Combat au moment de la modification : contrairement à la première
      version de ce GUI, les nouveaux plafonds/composition s'appliquent **immédiatement** (pas
      d'attente de la vague suivante) — après "Valider", les prochains spawns doivent déjà
      suivre la nouvelle configuration.
- [ ] Poser 2 spawners, ouvrir l'écran de l'un, le fermer sans "Valider" (touche Échap) :
      l'autre spawner ne doit pas avoir été affecté.
- [ ] Vérifier `run/logs/latest.log` après une session de test avec cet écran : aucune
      exception liée à `SpawnerConfigMenu`, `SpawnerConfigScreen`, `SpawnerConfigPayload` ou
      `ModNetworking`.

## L'aperçu de composition du spawner (texte à travers les murs, phase Construction)

Deuxième morceau à risque de cette session : premier texte du mod rendu "à travers les murs"
(`Font.DisplayMode.SEE_THROUGH`), jamais vérifié visuellement pendant le développement.

- [ ] En phase **Construction**, poser un spawner : un texte flotte au-dessus
      ("Total : 20", puis "Zombie : 15" et "Squelette : 5" sur les valeurs par défaut),
      centré horizontalement, sans chevauchement entre les lignes.
- [ ] **Se placer derrière un mur** en gardant le spawner à moins de ~32 blocs : le texte doit
      rester visible à travers le mur (c'est le point central de cette fonctionnalité).
- [ ] S'éloigner au-delà d'environ 32 blocs (`MAX_DISTANCE_SQ`) : le texte doit disparaître.
- [ ] Modifier la composition du spawner via l'écran de config (ex : mettre `30` sur Zombie),
      valider : l'aperçu au-dessus du bloc doit refléter le nouveau nombre immédiatement,
      sans avoir besoin de rouvrir le monde.
- [ ] Changer la difficulté n'est pas encore possible en jeu (`DIFFICULTY` reste à `NORMAL`,
      voir 05-etat-et-problemes-connus.md) — impossible de tester la mise à l'échelle du total
      par la difficulté pour l'instant, seulement par le nombre de base et la vague.
- [ ] **Basculer en phase Combat** (shift + clic droit sur le spawner) : l'aperçu doit
      **disparaître** immédiatement. Rebasculer en Construction : il doit réapparaître.
- [ ] Poser 2-3 spawners avec des compositions différentes proches les uns des autres :
      chaque aperçu doit afficher les bons chiffres pour **son propre** spawner, pas ceux
      d'un autre.
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `SpawnerBlockEntityRenderer`
      ou `SpawnerRenderState` (en particulier au chargement du monde ou à la casse du bloc).

## Le Spike Blockade (`SpikeBlockadeBlock`, `AbstractBlockadeBlockEntity`, `AttackPriorityTargetGoal`)

Premier membre concret de la catégorie de tours "Blockade" (voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md#système-de-tours-catégorie-blockade-démarrée)) :
remplace l'ancien `spike_trap` (piège de sol au `stepOn`) par un vrai bloc à PV, bloquant le
passage, que les monstres doivent maintenant activement attaquer pour détruire — premier goal
d'IA du mod qui cible un bloc plutôt qu'une entité, jamais vérifié visuellement. Le ciblage se
fait via `AiAttackTarget`/`getAiPriority()` (interface, pas un tag de bloc) : ce test valide
donc aussi que ce mécanisme fonctionne correctement, pas seulement le Spike Blockade en
particulier — voir la section dédiée "Système de priorité IA" plus bas pour les tests
multi-cibles (Block vs Cristal vs Tourelle).

- [ ] Prendre `spike_blockade` en créatif via `/give` (ex. `/give @s dungeon_defenders:spike_blockade`) —
      **il n'apparaît plus dans l'onglet créatif** du mod, c'est attendu. Clic droit avec en
      main, viser un bloc : **rien ne doit se passer** (pas de pose, pas d'interaction) — vérifie
      que `TowerBlockItem#useOn` ne fait plus rien, la roue étant l'unique façon de poser
      (voir section dédiée plus bas pour les tests de pose/mana/mana insuffisant/phase, qui ne
      passent maintenant que par la roue).
- [ ] Poser un Spike Blockade via la roue (voir section dédiée plus bas) pour les tests
      suivants, qui portent sur son comportement une fois posé — pas sur la façon dont il a été
      posé. Doit **bloquer le passage** comme n'importe quel bloc plein (pas de hitbox custom,
      collision vanilla normale).
- [ ] Faire spawn un zombie à proximité (spawner ou `/summon minecraft:zombie`) avec un Spike
      Blockade posé entre lui et le Cristal d'Eternia, sur son chemin le plus direct : le
      zombie doit se diriger vers le **Spike Blockade en premier** (pas continuer tout droit
      vers le cristal en l'ignorant), s'arrêter à son contact, puis lui donner des coups (les
      mêmes animations que taper une entité) — vérifie que la priorité "Corps à corps" (20)
      l'emporte bien sur "Cristal" (30) dans `AttackPriorityTargetGoal`.
- [ ] Après quelques coups (`DAMAGE_PER_HIT=5` toutes les `TICKS_BETWEEN_HITS=20` ticks), le
      bloc doit finir par se **casser** (PV par défaut : 30, donc 6 coups) — pas de recul
      visuel de dégâts pour l'instant (pas d'indicateur de PV, comportement attendu).
- [ ] Une fois le Spike Blockade détruit : le même zombie doit **reprendre son chemin vers le
      cristal** normalement (`AttackPriorityTargetGoal` ne trouve plus rien au palier "Corps à
      corps", retombe sur "Cristal"), sans rester bloqué à l'ancien emplacement du bloc.
- [ ] Se tenir soi-même (joueur) au contact du Spike Blockade : vérifier qu'il n'inflige
      **aucun dégât au joueur** — la détection de contact (`serverTick`,
      `getEntitiesOfClass(Monster.class, ...)`) ne cible que les `Monster`, pas les joueurs.
- [ ] Faire stationner un zombie **au contact** du bloc sans qu'il ait besoin d'attaquer
      explicitement (le pousser dessus, ou le laisser s'arrêter dessus) : il doit prendre des
      **dégâts de contact périodiques** (`CONTACT_DAMAGE=2`, toutes les `CONTACT_DAMAGE_
      INTERVAL_TICKS=20`, soit 1 fois par seconde) — indépendamment des dégâts de
      `AttackPriorityTargetGoal`, ce sont deux mécanismes séparés qui peuvent s'additionner.
- [ ] Faire spawn un squelette à distance avec un Spike Blockade sur le chemin : contrairement
      au zombie, le squelette **ne doit pas** se dérouter vers le bloc (il ne l'attaque pas au
      corps à corps) — il doit continuer à viser le cristal à distance comme avant, le Spike
      Blockade ne bloque pas ses flèches.
- [ ] Casser le Spike Blockade soi-même à la pioche (hors combat) : il se drope (comme
      l'ancien `spike_trap`), pas de remboursement de mana pour l'instant (manques
      connus, voir 05-etat-et-problemes-connus.md). Récupérer l'item droppé et essayer de le
      reposer à la main : **doit échouer** comme l'item obtenu par `/give` plus haut — même
      classe `TowerBlockItem`, aucune exception pour un item légitimement récupéré en jeu.
- [ ] Poser plusieurs Spike Blockade côte à côte formant un mur : un zombie coincé derrière
      doit s'attaquer à **celui qui bloque effectivement son chemin**, pas se figer ou choisir
      un bloc au hasard plus loin.
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `SpikeBlockadeBlock`,
      `SpikeBlockadeBlockEntity`, ou `AttackPriorityTargetGoal`.

## Le Harpoon Turret (`HarpoonTurretBlock`, `AbstractTurretBlockEntity`)

Premier membre de la catégorie "Turret" (voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md#système-de-tours-catégories-blockade-et-turret-démarrées)) :
contrairement au Spike Blockade, c'est la tour elle-même qui scanne et tire à chaque tick (pas
de goal porté par un monstre) — jamais vérifié visuellement, y compris le premier vrai cône de
détection et la première vraie propriété d'orientation (`HORIZONTAL_FACING`) du mod. Poser un
Harpoon Turret via la roue (voir section dédiée plus bas) avant les tests suivants.

- [ ] Poser un Harpoon Turret, orienté (via la rotation en étape "orientation" de la roue) dans
      une direction connue (ex. NORD). Vérifier que la texture visible (face avant façon
      furnace) pointe bien dans cette direction une fois le bloc réellement posé — sinon la
      convention `HORIZONTAL_FACING`/blockstate est inversée quelque part.
- [ ] Faire spawn un zombie **dans le cône** (à moins de 12 blocs, dans un secteur de 45°
      devant la face avant du turret) : au bout d'au plus 1,5 s (`ATTACK_INTERVAL_TICKS=30`),
      une **flèche visuelle** doit partir du turret vers le zombie, et le zombie doit perdre
      **6 PV** (message de dégâts habituel) — pas besoin que la flèche touche physiquement
      (dégâts appliqués directement, comme le squelette archer sur le cristal).
- [ ] Faire spawn un zombie **derrière** le turret (hors du cône de 45°, même à moins de 12
      blocs) : le turret ne doit **jamais** tirer dessus tant qu'aucune autre cible n'entre
      dans le cône — vérifie le filtre d'angle (`AbstractTurretBlockEntity.findTarget`).
- [ ] Faire spawn un zombie **dans l'axe mais au-delà de 12 blocs** : pas de tir tant qu'il
      reste hors de portée ; s'approcher jusqu'à passer sous les 12 blocs (en restant dans le
      cône) doit déclencher le tir.
- [ ] Avec plusieurs zombies dans le cône à des distances différentes : le turret doit tirer
      sur **le plus proche**, pas un zombie plus loin.
- [ ] Chronométrer la cadence : deux tirs successifs sur une cible qui reste à portée doivent
      être espacés d'environ 1,5 s (30 ticks), pas plus vite.
- [ ] Le Harpoon Turret **bloque le passage** comme n'importe quel bloc plein (gratuit, comme
      Spike Blockade). Avec un Spike Blockade **également** à portée (8 blocs) du même zombie :
      le zombie doit préférer le Spike Blockade (priorité "Corps à corps", 20) et ignorer le
      Harpoon Turret (priorité "Tourelle", 40) tant que le premier n'est pas détruit — voir la
      section "Système de priorité IA" plus bas pour le test complet (turret ciblé en dernier
      recours uniquement).
- [ ] Casser le Harpoon Turret à la pioche : il se drope (comme Spike Blockade), pas de
      remboursement de mana. L'item récupéré ne doit **pas** se poser à la main (même
      comportement que Spike Blockade, voir section dédiée plus haut).
- [ ] Un squelette (archer) dans le cône : doit se faire tirer dessus comme un zombie, le
      turret ne fait pas de distinction entre types de `Monster`.
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `HarpoonTurretBlock`,
      `HarpoonTurretBlockEntity`, `AbstractTurretBlockEntity`, ou à la création de l'entité
      `Arrow` sans propriétaire.

**Important en testant cette section (2026-08-29)** : deux bugs viennent d'être trouvés et
corrigés en construisant le reste du roster (voir 05-etat-et-problemes-connus.md) —
`lastFireTick` mal initialisé faisait qu'**aucune tourelle ne tirait jamais**, y compris
Harpoon Turret. Si les points ci-dessus n'ont encore jamais été cochés, c'est potentiellement
la cause : à re-tester avec le correctif.

## Le reste du roster de l'Écuyer (Bouncer Blockade, Bowling Ball Turret)

(Bouncer/Slice N Dice Blockade, Bowling Ball/Mortar Turret) — design discuté et validé avec le
joueur avant d'être codé, voir 02-gameplay.md pour le détail. **Trois tours de test effectués
(2026-08-29)** : Slice N Dice confirmée sans souci dès le premier ; Mortar Turret confirmée
bonne au troisième, après deux passes de correction (flèche → explosion trop grosse →
explosion réduite) ; Bowling Ball corrigée en une passe (tir horizontal), pas resignalée depuis.
Sections retirées d'ici une fois confirmées. **Seul le Bouncer reste actif** : sens du
knockback corrigé, puis force ajustée deux fois (1.6F trop faible dans le mauvais sens → trop
fort une fois corrigé → 0.8F). Poser chaque tour via la roue (section dédiée plus bas).

### Bouncer Blockade

**Troisième passe de correction (2026-08-29)** : le sens du vecteur (les monstres étaient
attirés au lieu d'être repoussés) a été corrigé au tour précédent. Une fois ce sens validé
comme correct, la force à 1.6F s'est avérée trop forte — remise à **0.8F** (sa valeur
d'origine). Le joueur a aussi demandé si le Bouncer avait une "vitesse d'attaque" : oui, déjà —
`contactDamageIntervalTicks` (10 ticks/0,5s) est le même cooldown par monstre que les dégâts,
partagé, pas une repousse à chaque tick.

- [ ] Un monstre au contact est visiblement repoussé **loin** du blockade (pas attiré vers lui).
- [ ] Avec la force revenue à 0.8F, le monstre doit être poussé de façon nette mais pas
      excessive — ni un tremblement imperceptible (le souci d'origine), ni une projection
      disproportionnée (le souci de 1.6F). Si le réglage ne convient toujours pas, la valeur
      est probablement à affiner encore (voir `KNOCKBACK_STRENGTH` dans
      `BouncerBlockadeBlockEntity`).
- [ ] Un monstre déjà repoussé et qui revient au contact n'est repoussé à nouveau qu'après le
      cooldown (10 ticks/0,5s), pas immédiatement en boucle — vérifie la "vitesse d'attaque"
      partagée avec les dégâts.
- [ ] Un monstre au contact perd aussi des PV (1 toutes les 10 ticks), en plus d'être repoussé.
- [ ] Un monstre juste hors de portée de contact (au-delà d'1 bloc) n'est ni endommagé ni
      repoussé.
- [ ] Plusieurs monstres au contact en même temps : chacun est repoussé indépendamment.

### Bowling Ball Turret

Le tir à l'horizontale (corrigé au deuxième tour, ne partait plus vers le ciel) n'a pas été
resignalé comme cassé depuis — reste à confirmer explicitement, les points suivants n'ont
jamais été spécifiquement cochés.

- [ ] Un seul zombie dans le cône : la boule (visuellement une flèche, voir limite assumée en
      02-gameplay.md) part à l'horizontale, sans monter vers le ciel, et le touche (~5 dégâts).
- [ ] **Point le plus important** : plusieurs zombies alignés les uns derrière les autres dans
      le cône — la boule doit blesser **chacun d'entre eux** en continuant sa trajectoire après
      le premier impact, pas s'arrêter au premier touché. C'est la mécanique centrale demandée
      par le joueur.
- [ ] La boule finit par disparaître (`MAX_BALL_DISTANCE`) si elle ne touche personne, pas de
      projectile qui vole indéfiniment.
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `BowlingBallEntity` (notamment
      autour de l'enchantement de Perforation appliqué à la fausse arme au moment du tir).


## Système de priorité IA (`AttackPriorityTargetGoal`, `AiAttackTarget`)

Remplace les deux anciens goals de mêlée (`AttackBlockadeGoal`/`AttackEterniaCrystalGoal`,
supprimés) par un seul goal à paliers — voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md#système-de-priorité-ia-block--corps-à-corps--cristal--tourelle).
Premier test possible du palier "Tourelle" (jusqu'ici totalement inatteignable), et première
vérification que la recherche par palier (pas juste "le plus proche, tous types confondus")
fonctionne vraiment. Nécessite de contrôler précisément ce qu'un zombie a à portée : préparer
une petite zone dégagée où poser/retirer un Spike Blockade et un Harpoon Turret facilement.

- [ ] Zombie sans rien à portée (ni Blockade à 8 blocs, ni Turret à 8 blocs), cristal à moins de
      16 blocs : doit foncer sur le cristal, comme avant ce chantier (comportement de référence,
      non régressé).
- [ ] Zombie avec un Spike Blockade à portée (8 blocs) et le cristal également à portée (16
      blocs) : doit préférer le Spike Blockade (palier 20 < 30) — déjà couvert dans la section
      dédiée plus haut, revérifier ici dans le contexte du nouveau goal.
- [ ] Zombie avec **uniquement** un Harpoon Turret à portée (8 blocs) — **ni** Spike Blockade
      à portée, **ni** cristal à portée (au-delà de 16 blocs, ou hors du monde de test) : doit
      pour la première fois **s'attaquer au turret** (le percuter, lui donner des coups comme à
      un Spike Blockade) — nouveau comportement, jamais possible avant ce chantier. Le turret a
      20 PV : compter ~4 coups (`DAMAGE_PER_HIT=5`) pour le détruire au corps à corps (en plus
      de ce qu'il encaisse en se faisant tirer dessus par... rien, un zombie ne tire pas).
- [ ] Zombie avec Spike Blockade **et** Harpoon Turret tous les deux à portée (8 blocs) : doit
      ignorer le turret et s'attaquer au Spike Blockade en premier (20 < 40) — même une fois le
      Spike Blockade détruit, doit ensuite préférer le cristal (30 < 40) s'il est à portée,
      **pas** le turret.
- [ ] Détruire tout ce qui est à portée d'un zombie (Blockade, puis rien d'autre que le
      turret) : vérifie que le goal **change bien de cible** au fil du combat, palier par
      palier, sans rester bloqué sur une cible qui n'existe plus.
- [ ] Vérifier `run/logs/latest.log` sur toute cette section : aucune exception liée à
      `AttackPriorityTargetGoal` ou `AiAttackTarget`.

## La roue de sélection des tours et la pose (`TowerWheelScreen`, `TowerPlacementClientEvents`)

Premier menu radial du mod, premier hologramme de pose, et première utilisation du pipeline de
rendu "submit node" (`ExtractLevelRenderStateEvent`/`SubmitCustomGeometryEvent`) en dehors d'un
block entity — rien de tout ça n'a pu être vérifié visuellement pendant le développement.

- [ ] **En phase Combat** (basculer via le spawner, voir section dédiée), appuyer sur `R` :
      la roue **ne doit pas s'ouvrir**, un message "Les tours ne peuvent être posées qu'en
      phase de Construction !" doit apparaître — vérifie le refus côté client
      (`TowerPlacementClientEvents`), avant même d'atteindre le serveur.
- [ ] **En phase Construction**, appuyer sur `R` (touche par défaut, configurable dans
      Options > Touches > Gameplay, libellé "Ouvrir la roue des tours") : un écran radial
      s'ouvre, sans crash ni écran noir. **Deux** secteurs/icônes doivent apparaître désormais
      (Spike Blockade et Harpoon Turret), répartis autour du centre — pas superposés.
- [ ] Bouger la souris tout autour du centre de l'écran, en restant proche du centre (moins de
      ~20px) : **aucun** secteur ne doit être en surbrillance (zone morte).
- [ ] Éloigner la souris du centre (au-delà de la zone morte) vers chacun des deux secteurs :
      le bon secteur doit passer en surbrillance à chaque fois (pas l'autre), et son nom + son
      coût en mana ("Coût : 30 mana" pour Spike Blockade, "Coût : 50 mana" pour Harpoon
      Turret) doivent s'afficher sous la roue.
- [ ] **Cliquer directement** sur l'icône en surbrillance : l'écran se ferme, aucune erreur
      dans les logs — doit démarrer le mode pose (voir plus bas).
- [ ] Rouvrir la roue, **maintenir `R`**, viser le secteur (surbrillance visible), puis
      **relâcher `R`** (sans cliquer) : doit avoir le même effet que le clic direct — démarre
      le mode pose. Les deux gestes doivent être équivalents.
- [ ] Ouvrir la roue puis appuyer sur `Échap` sans cliquer : l'écran se ferme, **aucun** mode
      pose ne démarre (pas d'hologramme qui apparaît ensuite en jeu).
- [ ] Après avoir sélectionné une tour (mode pose, étape "visée") : un **contour filaire**
      (hologramme) doit suivre le curseur/la visée du joueur en regardant le monde, collé sur
      la position juste après le premier bloc visé (comme la pose d'un bloc normal).
- [ ] Viser un emplacement **libre** (air, herbe remplaçable...) : le contour doit être
      **vert**. Viser un bloc plein existant (mur, sol, une autre tour déjà posée...) : le
      contour affiché doit être sur la position **adjacente** à ce bloc, toujours en fonction
      de sa propre validité — et viser un endroit où la position candidate est déjà occupée
      (ex. coincé entre deux blocs) doit donner un contour **rouge**.
- [ ] Regarder au loin (>20 blocs, hors de portée du rayon) ou vers le ciel/le vide sans rien
      viser : l'hologramme doit **disparaître** (pas de contour flottant sans cible).
- [ ] **Clic gauche** pendant l'étape "visée" : annule tout le mode pose, l'hologramme
      disparaît, rien n'est posé.
- [ ] **Clic droit** sur une position **verte** : l'hologramme doit **arrêter de suivre le
      regard** (position verrouillée) — bouger la caméra ne doit plus déplacer le contour.
- [ ] **Clic droit** sur une position **rouge** (viser un bloc invalide) : ne doit **rien**
      faire (pas de verrouillage, reste en étape "visée").
- [ ] Sélectionner **Spike Blockade** et verrouiller une position (étape "orientation") :
      appuyer sur `T` (touche par défaut, "Faire pivoter la tour (pose)") plusieurs fois :
      aucune erreur, mais **aucun changement visuel attendu** sur le contour (cube symétrique,
      limite connue — voir 05-etat-et-problemes-connus.md) — vérifie surtout l'absence de
      crash/d'exception.
- [ ] Recommencer avec **Harpoon Turret** : dès l'étape "visée" (avant même de verrouiller), un
      **cône jaune** (pas un cercle complet) doit apparaître autour de l'hologramme, orienté
      par défaut vers le NORD — nouveau test important, jamais vérifié visuellement (premier
      cône du mod). Une fois la position verrouillée, appuyer sur `T` plusieurs fois : le cône
      doit **pivoter par pas de 90°** avec l'hologramme, dans le même sens que le contour du
      bloc.
- [ ] Avec Harpoon Turret, comparer visuellement le cône à la face avant du bloc une fois posé
      (texture directionnelle de la furnace, voir "Apparence" dans doc/02-gameplay.md) : le
      cône doit pointer **du même côté que la face avant**, à chaque rotation (Nord/Est/Sud/
      Ouest) — **point non garanti** (convention de rotation du cône vérifiée par construction
      seulement pour NORD, pas les 3 autres, voir 02-gameplay.md) : si le cône pointe à
      l'envers ou sur le côté pour EST/SUD/OUEST, c'est une inversion de sens de rotation à
      corriger dans `TowerPlacementClientEvents.renderRangeArea`/`onSubmitCustomGeometry`.
- [ ] **Clic droit** une seconde fois (étape "orientation"), pour Spike Blockade **et** Harpoon
      Turret séparément : doit **poser réellement** le bloc à la position verrouillée, fermer
      le mode pose (hologramme disparaît), et débiter le mana (`-30 mana pour la tour (X/100)`
      pour Spike Blockade, `-50 mana pour la tour (X/100)` pour Harpoon Turret, voir section HUD
      mana) — test le plus important de cette section : vérifie que `PlaceTowerPayload`
      déclenche bien `ModEvents.onTowerPlace` via le hook NeoForge réutilisé, pour les deux
      catégories, sans double implémentation.
- [ ] Une fois un Harpoon Turret réellement posé : vérifier qu'il est bien **orienté** dans la
      direction choisie pendant l'étape "orientation" (comparer au cône prévisualisé) — valide
      que `ModNetworking.handlePlaceTower` applique correctement `HORIZONTAL_FACING`.
- [ ] **Clic gauche** pendant l'étape "orientation" : annule tout (pas de pose, pas de
      débit de mana), retour à zéro — pas de retour à l'étape "visée".
- [ ] Refaire tout le mode pose avec **moins de mana que le coût de la tour choisie** (30 pour
      Spike Blockade, 50 pour Harpoon Turret) : le clic droit final doit **échouer** (message
      "Pas assez de mana...", le bloc n'apparaît pas dans le monde).
- [ ] Démarrer le mode pose **en phase Construction**, puis faire basculer la partie en Combat
      pendant que le mode pose est actif (ex. via le harnais de test du spawner, shift + clic
      droit) avant de confirmer : le clic droit final doit être **refusé** côté serveur (message
      de phase, pas de pose) même si le client a laissé aller jusque-là — vérifie que le
      serveur reste la seule autorité réelle, pas seulement le refus client à l'ouverture de la
      roue.
- [ ] Poser une tour via la roue à un endroit, puis ouvrir la roue une seconde fois et essayer
      de poser une autre tour **au même endroit exact** : doit apparaître **rouge** (position
      occupée par la tour tout juste posée), pas vert.
- [ ] Redimensionner la fenêtre pendant que la roue est ouverte : les secteurs doivent rester
      centrés sur le nouveau centre de l'écran, pas figés à une position absolue.
- [ ] Vérifier `run/logs/latest.log` après une session de test complète : aucune exception liée
      à `TowerWheelScreen`, `TowerPlacementClientEvents`, `TowerPlacementState`,
      `PlaceTowerPayload`, `TowerBlockItem`, ou au rendu
      (`ExtractLevelRenderStateEvent`/`SubmitCustomGeometryEvent`).

## HUD vanilla masqué

- [ ] La faim (icônes en bas à droite), l'expérience (barre verte + niveau) et la hotbar
      (barre d'objets tout en bas) ont **disparu** de l'écran.
- [ ] Aucune erreur au changement de slot sélectionné (touches 1-9 / molette) malgré la
      hotbar invisible — elle doit continuer à fonctionner en arrière-plan (l'objet en main
      change bien), seul l'affichage est masqué.
- [ ] La barre d'armure (si équipée) : vérifier si elle s'affiche seule, à une position
      bizarre, maintenant que la hotbar en dessous d'elle est vide (comportement attendu mais
      pas encore vérifié visuellement).

## Général

- [ ] Aucune erreur/exception dans les logs (`run/logs/latest.log`) au chargement du mod ni
      à l'usage des deux points ci-dessus.
- [ ] Les fonctionnalités précédentes (Cristal d'Eternia, Piège à Pics, IA zombie) n'ont pas
      régressé — rien dans ce qui précède ne les touche directement, mais à vérifier une fois
      qu'un test complet est possible.

## Une fois testé

Reporter les résultats dans [05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md)
(déplacer ce qui fonctionne vers « Ce qui est implémenté », et ce qui casse vers
« Ce qui reste »), puis vider ou réduire ce fichier aux prochaines fonctionnalités non
testées.
