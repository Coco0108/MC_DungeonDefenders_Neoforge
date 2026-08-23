# 6. À tester en jeu

Ce fichier liste ce qui a été écrit et compile, mais **jamais lancé en jeu** : le
développement s'est fait dans un environnement sans affichage (`./gradlew compileJava`
passe, mais `./gradlew runClient` n'a pas pu être vérifié visuellement). Coche au fur et à
mesure, et signale ici ce qui casse pour que ça reste une référence à jour.

Le monde/point de spawn, le cristal de la taverne, l'aller-retour vers une map, le vote
"prêt" et les dégâts du Cristal d'Eternia en combat, les spawners (gameplay, écran de config,
aperçu de composition), le squelette archer, le Spike Blockade, le Harpoon Turret, le système
de priorité IA, la victoire/défaite et la roue de sélection des tours ont été **testés en jeu
le 2026-08-23** (bugs trouvés au passage listés dans
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md#corrections-trouvées-lors-des-tests-en-jeu-du-2026-08-23),
tous corrigés) — leurs checklists ont été retirées d'ici, voir "Une fois testé" en bas de
fichier.

Lancer le client de dev :

```bash
./gradlew runClient
```

## HUD — groupe bas-gauche (mana, vie, expérience)

- [ ] En bas à gauche de l'écran : deux **losanges** côte à côte (vie en rouge à gauche, mana
      en bleu à droite — comme le jeu de référence ; pointes en haut/bas/gauche/droite, pas des
      rectangles), avec une barre horizontale verte (expérience) tout en bas, sous les deux
      losanges. Rien ne doit se chevaucher.
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
- [ ] Prendre `mana_fill_wand` (icône poudre de glowstone) : clic droit à `0/100` remplit le
      losange mana d'un coup (`100/100`), message « Mana rempli (100/100) ». Un second clic
      droit affiche « Le mana est déjà au maximum ! » sans erreur. Nouveau, jamais vérifié
      visuellement.
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

## Les cristaux de mana et le remboursement (`ManaCrystalEntity`, `ModEvents.onTowerBreak`)

Premier `Entity` custom du mod (jusqu'ici, uniquement des `Block`/`BlockEntity`) — jamais
vérifié visuellement, y compris le tout premier ramassage "hors inventaire" (comme l'XP
vanilla) et la toute première fois que le mana remonte en jeu.

- [ ] Tuer un zombie ou un squelette (spawner ou `/summon`) : un petit objet flottant doit
      apparaître à l'endroit de sa mort (visuellement une orbe verte/jaune — c'est le renderer
      vanilla de l'XP réutilisé tel quel, pas encore de couleur "mana" dédiée, comportement
      attendu). Il doit **flotter/bobiner** comme une vraie orbe d'XP, pas rester figé au sol.
- [ ] S'approcher du cristal flottant : il doit se **magnétiser** vers le joueur (accélérer
      dans sa direction) à mesure qu'on s'approche, comme une orbe d'XP vanilla.
- [ ] Marcher dessus : le cristal disparaît (son + petite animation de ramassage), le mana du
      joueur augmente de **5** (HUD mana à droite + message système), sans qu'aucun item
      n'apparaisse dans l'inventaire/la hotbar — c'est le point le plus important de cette
      section : vérifie que `playerTouch` donne bien du mana et pas de l'XP vanilla.
- [ ] Vérifier que le joueur ne gagne **aucune XP/niveau vanilla** en tuant un monstre du mod
      (regarder la barre d'XP vanilla — masquée, donc regarder plutôt l'absence de son de
      "level up" et l'attribut `experience` custom du joueur qui ne doit PAS bouger avec ça) —
      vérifie que `ModEvents.onExperienceDrop` annule bien le drop d'XP vanilla.
- [ ] Ramasser un cristal alors que le mana est déjà à 100/100 : le mana doit **rester à
      100**, pas dépasser (`Math.min(MAX_MANA, ...)`), et le cristal doit quand même disparaître
      (pas de "refus" de ramassage).
- [ ] Tuer plusieurs monstres proches les uns des autres : leurs cristaux doivent **fusionner**
      en un seul (comportement hérité d'`ExperienceOrb`, `count` cumulé) — ramasser ce cristal
      fusionné doit donner le mana correspondant au nombre de monstres tués (5 × count), pas
      juste 5.
- [ ] Poser une tour (30 ou 50 mana selon laquelle), puis la **casser soi-même à la pioche**
      (hors combat) : le mana doit remonter de **50% du coût** (15 pour Spike Blockade, 25 pour
      Harpoon Turret), avec un message dédié — vérifie `ModEvents.onTowerBreak`.
- [ ] Poser une tour, puis la laisser être **détruite par un monstre en combat** (0 PV) : le
      mana **ne doit PAS** être remboursé cette fois — vérifie que `BreakBlockEvent` ne se
      déclenche bien que pour une casse volontaire du joueur, pas pour
      `AbstractTowerBlockEntity.setHealth()` à 0.
- [ ] Casser un bloc qui n'est **pas** une tour (terrain quelconque) : aucun message de
      remboursement, aucune exception.
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `ManaCrystalEntity`,
      `ModEntities`, `ModEvents.onExperienceDrop` ou `onTowerBreak`.

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
