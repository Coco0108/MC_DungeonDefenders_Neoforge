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

- [ ] En bas à gauche de l'écran : deux colonnes verticales côte à côte (mana en bleu à
      gauche, vie en rouge à droite), avec une barre horizontale verte (expérience) tout en
      bas, sous les deux colonnes. Rien ne doit se chevaucher.
- [ ] Le texte `Mana: 100/100` apparaît **au-dessus** de la colonne mana, centré sur sa
      largeur. Idem pour `Vie : 100/100` au-dessus de la colonne vie.
- [ ] Le texte `Expérience : 0/100` apparaît à droite de la barre verte (comme avant, ça n'a
      pas changé pour l'expérience).
- [ ] Les colonnes mana/vie sont pleines (bleu/rouge sur toute la hauteur) à `100/100` ; la
      barre d'expérience est vide (fond gris) à `0/100`.
- [ ] Les cœurs vanilla (habituellement en bas à gauche, au-dessus de la barre de faim) sont
      **absents** — remplacés par la colonne rouge.
- [ ] Vérifier que la barre d'XP vanilla est bien invisible (`EXPERIENCE_LEVEL` masqué) et ne
      se confond pas avec la barre verte du mod.
- [ ] Prendre `mana_test_wand` dans l'onglet créatif Dungeon Defenders (icône bâton de
      blaze, pas de texture dédiée). Clic droit : la colonne mana perd 10 % de sa hauteur
      **par le haut** (elle se vide du haut vers le bas puisqu'elle se remplit du bas vers le
      haut), le texte passe à `90/100`, un message système confirme `-10 mana (90/100)`.
- [ ] Répéter jusqu'à `0/100` : la colonne mana doit être entièrement vide (fond gris), et un
      nouveau clic droit affiche le message « Pas assez de mana ! » sans repasser en négatif.
- [ ] Quitter le monde et y revenir (ou `/reload` + relog) : le mana affiché doit être celui
      d'avant la déconnexion, pas remis à 100 (l'attachment est censé persister).
- [ ] Se prendre des dégâts (chute, mob, `/damage`) : la colonne vie diminue par le haut, en
      cohérence avec les dégâts subis.
- [ ] Se reconnecter (relog) après avoir perdu de la vie : la vie perdue doit être conservée,
      pas remise à 100/100 (seul un joueur qui était déjà à son maximum doit se retrouver à
      100/100 après coup).
- [ ] Un nouveau joueur (jamais connecté à ce monde) doit spawn à 100/100, pas à 20/20.
- [ ] Redimensionner la fenêtre : le groupe entier reste collé au bord bas-gauche, colonnes et
      barre toujours alignées entre elles.

## HUD — vague

- [ ] Le texte `Vague 1/5` apparaît en haut à droite de l'écran, collé au bord droit (pas de
      jauge, juste du texte).
- [ ] Il reste stable (`1/5`) quoi qu'il se passe en jeu — rien ne le fait encore varier,
      c'est attendu.
- [ ] Redimensionner la fenêtre de jeu : le texte doit rester collé au bord droit (position
      recalculée à chaque frame via `guiWidth()`), pas figé à une position absolue de l'écran
      d'origine.

## HUD — progression de la vague

- [ ] Sur la **même ligne** que `Vague 1/5` (pas en dessous), de droite à gauche : `Vague
      1/5`, une jauge orange, puis le texte `Ennemis : 0/10`. Tout doit tenir sur une seule
      ligne sans se chevaucher.
- [ ] La jauge est vide (fond gris) puisque `0/10` : rien ne doit apparaître en orange tant
      que rien ne tue d'ennemis, c'est attendu.
- [ ] En redimensionnant la fenêtre, l'ensemble reste collé au bord droit et la jauge reste
      juste à gauche du texte `Vague X/Y` (elle suit sa largeur, qui peut varier avec le
      nombre de chiffres).

## HUD — phase

- [ ] Le texte `Phase : Construction` apparaît juste en dessous de la rangée `Vague X/Y` /
      `Ennemis : X/Y`, collé au bord droit.
- [ ] Il reste stable sur `Construction` — rien ne le fait encore changer, c'est attendu.
- [ ] Les trois lignes en haut à droite (vague+ennemis, phase) sont bien empilées sans se
      chevaucher ni se toucher.

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
