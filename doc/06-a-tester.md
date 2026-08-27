# 6. À tester en jeu

Ce fichier liste ce qui a été écrit et compile, mais **jamais lancé en jeu** : le
développement s'est fait dans un environnement sans affichage (`./gradlew compileJava`
passe, mais `./gradlew runClient` n'a pas pu être vérifié visuellement). Coche au fur et à
mesure, et signale ici ce qui casse pour que ça reste une référence à jour.

Le monde/point de spawn, le cristal de la taverne, l'aller-retour vers une map, le vote
"prêt" et les dégâts du Cristal d'Eternia en combat, les spawners (gameplay, écran de config,
aperçu de composition), le squelette archer, le Spike Blockade, le Harpoon Turret, le système
de priorité IA, la victoire/défaite, la roue de sélection des tours, tout le HUD (positionnement
+ comportement dynamique du mana et de la vie : baguettes de test, dégâts, persistance à la
reconnexion, spawn d'un nouveau joueur) ont été **testés en jeu le 2026-08-23** (bugs trouvés
au passage listés dans
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md#corrections-trouvées-lors-des-tests-en-jeu-du-2026-08-23),
tous corrigés) — leurs checklists ont été retirées d'ici, voir "Une fois testé" en bas de
fichier.

Lancer le client de dev :

```bash
./gradlew runClient
```

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

## Expérience, score et niveau (`ModEvents.awardExperienceAndScore`/`grantExperience`)

Nouveau (2026-08-27), jamais vérifié visuellement. Branché juste après le drop de cristal de
mana dans `onMonsterDeath` — même monstre tué, deux effets à vérifier en même temps.

- [ ] Tuer un zombie : le HUD **Score** (bas centre) augmente de **10**, et le HUD
      **Expérience** (barre verte bas gauche) augmente aussi de **10**.
- [ ] Tuer un squelette : les deux augmentent de **15** cette fois (zombie et squelette n'ont
      pas la même valeur, vérifie `SpawnableEnemy.xpValue()`).
- [ ] **En multijoueur (2 joueurs ou plus)** : tuer un monstre donne bien l'XP à **tous les
      joueurs présents** en même temps, pas seulement à celui qui a porté le coup — décidé
      volontairement, à vérifier que ce n'est pas juste un seul joueur qui progresse.
- [ ] Faire monter l'expérience jusqu'à 100 (plusieurs kills) : au palier, la barre d'XP
      revient à une valeur basse (pas juste bloquée à 100), le HUD **Nom - niv X** passe au
      niveau supérieur, et un message système "Niveau supérieur !" apparaît **au joueur
      concerné seulement**.
- [ ] Faire un très gros kill d'un coup (si possible, ou vérifier par relecture) ne devrait pas
      être nécessaire pour ce test — mais si l'XP dépasse 200 en un seul kill, vérifier que le
      niveau peut monter de 2 d'un coup (boucle `while`, pas juste un `if`).
- [ ] Aller jusqu'à la victoire ou la défaite (ou utiliser le harnais de test) : le **Score**
      revient à **0** au redémarrage de la partie (`PhaseTransitions.resetGameState`), mais le
      **Niveau** et l'**Expérience** du joueur restent inchangés (ils sont censés persister
      au-delà d'une carte).
- [ ] Se déconnecter/reconnecter après avoir gagné de l'expérience/un niveau : les deux valeurs
      sont bien conservées (persistance de l'attachment joueur).
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `awardExperienceAndScore`,
      `grantExperience` ou `SpawnableEnemy.xpValueFor`.

## Gain de score flottant, avec sa source (`ScoreGainOverlay`, `ScoreGainPayload`)

Nouveau (2026-08-27), jamais vérifié visuellement. Refait une fois pour passer d'un simple
"+X" deviné côté client à un vrai paquet réseau portant la source — les deux versions sont à
vérifier ensemble ici (le mécanisme a changé, le résultat visible attendu aussi).

- [ ] Tuer un monstre : un "+10 Ennemi tué" (zombie) ou "+15 Ennemi tué" (squelette) apparaît en
      bas à **droite** de l'écran (pas au même endroit que le Score en bas centre), monte
      doucement puis s'estompe en environ 1,5 seconde.
- [ ] Tuer plusieurs monstres à quelques instants d'écart : chaque kill affiche son propre
      popup (pas un seul cumulé) — cette fois, ça doit être vrai **même pour des kills
      quasi simultanés** (contrairement à l'ancienne version basée sur la sync d'attachment,
      chaque `grantScore` envoie désormais son propre paquet).
- [ ] Se connecter en cours de partie (score déjà > 0, ex. rejoindre après quelques kills) :
      **aucun** popup ne doit apparaître au premier affichage du HUD (plus aucun mécanisme de
      "diff" qui pourrait en générer un par erreur).
- [ ] Aller jusqu'à la victoire/défaite : le score revient à 0 (voir section précédente) sans
      qu'un popup n'apparaisse à ce moment-là (`resetGameState` ne passe pas par `grantScore`).
- [ ] **En multijoueur** : chaque joueur présent reçoit bien son propre popup au même kill (le
      paquet est envoyé à tous les joueurs de la `Level`, comme l'XP).
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `ScoreGainOverlay`,
      `ScoreGainPayload` ou `handleScoreGain`.

## HUD vanilla masqué, faim et hotbar désactivées

- [ ] La faim (icônes en bas à droite), l'expérience (barre verte + niveau) et la hotbar
      (barre d'objets tout en bas) ont **disparu** de l'écran.
- [ ] La faim ne baisse **jamais**, même en sprintant/sautant/minant en continu longtemps —
      vérifie `ModEvents.onPlayerTick`, qui la repousse au maximum à chaque tick serveur.
- [ ] Les touches 1-9 et la molette (hors inventaire/coffre) ne **changent plus rien** :
      l'objet en main reste le même, comportement inverse du point précédent (avant ce
      changement, la hotbar tournait encore en arrière-plan malgré l'affichage masqué) —
      vérifie `DungeonDefendersModClient#onClientTickPre`/`onMouseScroll`.
- [ ] Les touches 1-9/la molette fonctionnent normalement dans un écran (inventaire créatif,
      coffre...) — la neutralisation ne doit s'appliquer qu'en jeu, hors écran.
- [ ] La barre d'armure (si équipée) : vérifier si elle s'affiche seule, à une position
      bizarre, maintenant que la hotbar en dessous d'elle est vide (comportement attendu mais
      pas encore vérifié visuellement).

## Clic droit sur le Cristal d'Eternia en Combat

Le harnais de test qui infligeait 10 dégâts au clic droit en Combat a été retiré.

- [ ] Clic droit sur le cristal pendant le Combat : ne fait plus rien (pas de message, pas de
      dégâts) — seuls les monstres l'endommagent désormais.
- [ ] Le vote "prêt" (clic droit en Construction) fonctionne toujours normalement.

## Hitbox anti-escalade des tours (Spike Blockade, Harpoon Turret)

`getCollisionShape`/`getShape` passés de 1 à 1,5 bloc de haut sur les deux blocs, pour empêcher
les monstres de sauter dessus — jamais vérifié visuellement.

- [ ] Poser un Spike Blockade sur le chemin d'un monstre : il doit s'arrêter et attaquer le
      blockade au lieu de sauter dessus pour continuer vers le cristal.
- [ ] Même vérification avec un Harpoon Turret.
- [ ] Le joueur lui-même ne doit plus pouvoir sauter sur une tour d'un simple saut (nécessite
      un bloc adjacent ou un saut assisté pour y monter) — comportement attendu, pas un bug.
- [ ] Vérifier que le contour de sélection (regarder la tour) correspond bien à la nouvelle
      hauteur de 1,5 bloc, pas à l'ancien cube plein.

## Icônes du spawner (`SpawnerBlockEntityRenderer`)

Chaque ligne de détail de l'aperçu de composition affiche maintenant l'œuf d'invocation de
l'ennemi à côté du texte (`ItemStackRenderState`/`ItemModelResolver`, réutilisé tel quel côté
rendu) — première fois que ce mod dessine un item en 3D dans le monde en dehors d'une main de
joueur, jamais vérifié visuellement. Taille/écart (`ICON_SIZE`/`ICON_GAP` dans
`SpawnerBlockEntityRenderer`) sont une première estimation à l'aveugle, sans doute à retoucher.

- [ ] En phase Construction, regarder un spawner configuré : chaque ligne de détail affiche
      bien un œuf (zombie ou squelette selon l'entrée) à gauche du texte, ni minuscule ni
      énorme, sans chevaucher le texte.
- [ ] L'icône est correctement centrée verticalement sur sa ligne de texte.
- [ ] Contrairement au texte (visible à travers les murs), l'icône est bloquée par les murs —
      comportement attendu (limite connue, voir doc/05), à confirmer que ça ne rend pas
      illisible (texte visible sans son icône à travers un mur).
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `ItemModelResolver`/
      `ItemStackRenderState` dans `SpawnerBlockEntityRenderer`.

## Barre de vie du cristal animée (`EterniaCrystalBlockEntityRenderer`)

Avant : la barre sautait instantanément d'un palier de PV à l'autre. Maintenant : glisse vers
la nouvelle valeur en 300 ms (comme les barres de boss vanilla) — jamais vu en jeu.

- [ ] Endommager le cristal (monstre ou spawner) : la barre glisse visiblement vers la
      nouvelle valeur au lieu de sauter d'un coup.
- [ ] Enchaîner deux coups rapprochés (avant la fin de l'animation du premier) : la barre se
      redirige en douceur vers la nouvelle cible, sans à-coup ni saut en arrière.
- [ ] Rejoindre/quitter la zone du cristal (le sortir puis le rentrer dans le champ de la
      caméra) : pas d'erreur, pas de comportement bizarre au premier affichage.

## Barre de vie des tours (`TowerHealthBarRenderer`)

Générique à toute catégorie de tour (Spike Blockade, Harpoon Turret), cachée à PV pleins et
au-delà de 16 blocs de la caméra — jamais vue en jeu.

- [ ] Une tour à PV pleins n'affiche **aucune** barre.
- [ ] Endommager une tour (monstre) : une barre apparaît au-dessus, glisse vers la nouvelle
      valeur (comme le cristal), et disparaît si on s'éloigne à plus de 16 blocs environ.
- [ ] Plusieurs tours endommagées en même temps : chacune affiche sa propre barre
      indépendamment (pas de confusion entre tours proches l'une de l'autre).
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `TowerHealthBarRenderer`.

## Barre de vie des monstres (`MobHealthBarRenderer`)

Même principe que la barre des tours (endommagé + à portée de 16 blocs). **Corrigée le
2026-08-26** : la première version (un `RenderLayer` branché via `EntityRenderersEvent.
AddLayers`) soumettait bien la géométrie, mais dans le mauvais repère de pose (celui, local, du
modèle de l'entité) — invisible en pratique, sans aucune exception dans les logs. Remplacée par
un handler sur `RenderLivingEvent.Post` (bus de jeu), le même repère que le nametag vanilla —
voir [02-gameplay.md](02-gameplay.md#la-barre-de-vie-des-monstres--entitymobhealthbarrendererjava)
pour le détail. Le mécanisme de transit de la vie (`RegisterRenderStateModifiersEvent`/
`ContextKey`) n'a pas changé, il n'a jamais été le problème.

- [ ] Un zombie/squelette à PV pleins n'affiche **aucune** barre.
- [ ] Endommager un zombie/squelette (sans le tuer) : une barre apparaît au-dessus de sa tête,
      le suit quand il bouge, glisse vers la nouvelle valeur, disparaît à plus de 16 blocs.
- [ ] Plusieurs monstres endommagés proches les uns des autres : chacun garde sa propre barre
      (pas de confusion/écrasement entre eux).
- [ ] Aucun crash/erreur au chargement du mod ni à l'apparition d'un premier monstre.
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `MobHealthBarRenderer`,
      `RegisterRenderStateModifiersEvent` ou `RenderLivingEvent`.

## Config (`Config.java`, `config/dungeon_defenders-common.toml`)

Config réellement enregistrée pour la première fois ce mod (avant : spec d'exemple jamais
branchée) — jamais vérifié visuellement.

- [ ] Le fichier `config/dungeon_defenders-common.toml` apparaît après un premier lancement,
      avec `defaultHealth`, `damagePerHit`, `searchRange` dedans (valeurs par défaut 100/5/16).
- [ ] Modifier une valeur (ex. `defaultHealth=50`) puis relancer : le Cristal d'Eternia
      démarre bien avec ce nouveau maximum (barre de vie pleine dès la pose).
- [ ] L'écran de config du mod (menu Mods > Dungeon Defenders > Config) s'ouvre sans erreur et
      affiche bien les 3 options (au lieu de l'ancien contenu d'exemple du template).

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
