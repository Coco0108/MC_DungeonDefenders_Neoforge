# 6. À tester en jeu

Ce fichier liste ce qui a été écrit et compile, mais **jamais lancé en jeu** : le
développement s'est fait dans un environnement sans affichage (`./gradlew compileJava`
passe, mais `./gradlew runClient` n'a pas pu être vérifié visuellement). Coche au fur et à
mesure, et signale ici ce qui casse pour que ça reste une référence à jour.

Lancer le client de dev :

```bash
./gradlew runClient
```

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

## Le Spawner (premier vrai gameplay, pas juste du HUD)

- [ ] Prendre `spawner` dans l'onglet créatif Dungeon Defenders (texture : cage de spawner
      vanilla), le poser.
- [ ] **Shift + clic droit** dessus : un message système confirme le changement de phase
      (`Phase changée : Combat` / `Phase changée : Construction`), et le texte `Phase : ...`
      en haut à droite du HUD change en conséquence.
- [ ] **Clic droit sans shift** : ouvre l'écran de configuration (voir section dédiée
      ci-dessous), au lieu de basculer la phase.
- [ ] Une fois en `Combat`, attendre : des **zombies et des squelettes** doivent apparaître
      au-dessus du bloc, les zombies plus fréquemment que les squelettes (nombre de base 15
      contre 5 par défaut — environ 3x plus de zombies sur la durée, pas un ratio exact vague
      par vague).
- [ ] Laisser tourner un moment : **chaque type doit finir par s'arrêter** une fois son
      plafond atteint (15 zombies et 5 squelettes par défaut, sur la vague 1 avec la
      difficulté Normal) — le spawner ne doit **pas** spawn indéfiniment tant qu'on reste en
      combat, contrairement à avant cette évolution.
- [ ] Le squelette apparu se comporte comme le zombie : il converge à pied vers le cristal et
      le frappe au corps à corps une fois arrivé — **pas de tir à l'arc**, c'est un manque
      connu (voir 05-etat-et-problemes-connus.md), pas un bug si vous vous attendiez à le
      voir tirer.
- [ ] Reclic droit pour repasser en `Construction` : plus aucun nouveau zombie/squelette
      n'apparaît, même en attendant.
- [ ] Tuer un zombie ou un squelette apparu ainsi (ou n'importe quel autre monstre)
      **pendant que la phase est Combat** : le texte `Ennemis : X/10` en haut de l'écran doit
      voir son `X` s'incrémenter, et la jauge orange se remplir un peu.
- [ ] Tuer un monstre **pendant que la phase est Construction** : `Ennemis : X/10` ne doit
      **pas** bouger (le compteur ne compte que les morts en combat, c'est attendu).
- [ ] Casser le spawner à la pioche : il se drope (comme spike_trap), et l'accumulateur
      redémarre à 0 si on le repose (comportement attendu, pas de sauvegarde de position
      liée au bloc en tant que tel).
- [ ] Aucune erreur dans les logs au placement, au tick, ou à la casse du bloc.
- [ ] `Ennemis : X/10` reste bloqué sur `/10` quel que soit le nombre d'ennemis tués (le
      total n'est pas encore recalculé, c'est un `TODO` connu — voir
      05-etat-et-problemes-connus.md).

## L'écran de configuration du spawner (GUI custom, liste dynamique, avec réseau)

C'est le morceau le plus à risque de cette session (aucun test visuel possible pendant le
développement, ni pour la reconstruction dynamique des widgets à l'ajout/retrait de lignes) —
à vérifier avec le plus d'attention.

- [ ] Clic droit (sans shift) sur un spawner ouvre bien un écran, sans crash ni écran noir.
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
