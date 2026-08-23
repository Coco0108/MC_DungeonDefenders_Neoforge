# 6. À tester en jeu

Ce fichier liste ce qui a été écrit et compile, mais **jamais lancé en jeu** : le
développement s'est fait dans un environnement sans affichage (`./gradlew compileJava`
passe, mais `./gradlew runClient` n'a pas pu être vérifié visuellement). Coche au fur et à
mesure, et signale ici ce qui casse pour que ça reste une référence à jour.

Le monde/point de spawn, le cristal de la taverne, l'aller-retour vers une map, le vote
"prêt" et les dégâts du Cristal d'Eternia en combat, les spawners (gameplay, écran de config,
aperçu de composition), le squelette archer, le Spike Blockade, le Harpoon Turret, le système
de priorité IA, la victoire/défaite, la roue de sélection des tours, et le **positionnement**
de tout le HUD (groupe bas-gauche vie/mana/expérience, vague/progression/phase, score/
personnage, emplacements de compétences) ont été **testés en jeu le 2026-08-23** (bugs trouvés
au passage listés dans
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md#corrections-trouvées-lors-des-tests-en-jeu-du-2026-08-23),
tous corrigés) — leurs checklists ont été retirées d'ici, voir "Une fois testé" en bas de
fichier. Il reste à vérifier le **comportement dynamique** du HUD mana/vie (baguettes de test,
persistance) ci-dessous.

Lancer le client de dev :

```bash
./gradlew runClient
```

## HUD — groupe bas-gauche (mana, vie, expérience)

Positionnement (losanges vie/mana, forme, barre d'expérience, cœurs/XP vanilla masqués, groupe
haut-droit vague/progression/phase, score/personnage, emplacements de compétences) **testé en
jeu et confirmé** le 2026-08-23 — reste à vérifier le comportement dynamique du mana/de la vie
ci-dessous.

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
      à l'usage des sections ci-dessus.
- [ ] Les fonctionnalités précédentes (Cristal d'Eternia, Piège à Pics, IA zombie) n'ont pas
      régressé — rien dans ce qui précède ne les touche directement, mais à vérifier une fois
      qu'un test complet est possible.

## Une fois testé

Reporter les résultats dans [05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md)
(déplacer ce qui fonctionne vers « Ce qui est implémenté », et ce qui casse vers
« Ce qui reste »), puis vider ou réduire ce fichier aux prochaines fonctionnalités non
testées.
