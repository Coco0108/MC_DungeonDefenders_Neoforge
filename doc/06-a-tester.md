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

## Les cristaux de mana (`ManaCrystalEntity`)

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
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `ManaCrystalEntity`,
      `ModEntities`, ou `ModEvents.onExperienceDrop`.

> Le remboursement de mana à la casse d'une tour se teste désormais via la **touche dédiée de
> suppression**, pas la pioche — voir la section "La suppression de tour" plus bas.
> `ModEvents.onTowerBreak` (l'ancien remboursement au clic-pioche de cette branche) a été
> retiré dans cette fusion locale de test : il serait devenu exploitable une fois combiné à
> "Casser un bloc est désactivé" (voir 05-etat-et-problemes-connus.md).

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

## Le coffre de mana (`ManaChestBlock`, `ManaChestBlockEntity`)

Nouveau meuble de map (comme le Cristal d'Eternia/le Spawner) : donne du mana au clic droit en
survie, une fois par vague, configurable en créatif — jamais vérifié visuellement.

- [ ] Prendre `mana_chest` dans l'onglet créatif, le poser en Construction.
- [ ] **En créatif**, clic droit dessus : ouvre l'écran de configuration (un seul champ,
      "Quantité de mana", pré-rempli à 25). Changer la valeur, valider, refermer et
      recliquer : la nouvelle valeur doit être là (bien appliquée et resynchronisée), pas
      revenue à 25.
- [ ] **En survie**, clic droit : donne bien la quantité configurée de mana (message + HUD),
      une seule fois — dans **n'importe quelle phase** (2026-08-26 : la restriction
      "Construction uniquement" a été retirée, tester Construction **et** Combat).
- [ ] **Après ouverture, le coffre doit disparaître** (invisible) et devenir traversable — le
      point le plus important de cette section : vérifie `OPENED`/`getRenderShape`/`getShape`/
      `getCollisionShape` dans `ManaChestBlock`. Essayer de re-cliquer à l'endroit où il était :
      ne doit plus rien cibler (le clic doit traverser, pas afficher "coffre vide").
- [ ] **Priorité (signalé cassé le 2026-08-26, cause non trouvée en relisant le code)** : passer
      en Combat puis revenir en Construction (nouvelle vague) : le coffre doit **réapparaître**
      (visible, solide) et s'ouvrir à nouveau une fois — vérifie `ManaChestBlock#respawnAll`
      appelé par `PhaseTransitions#enterBuild`. Si ça casse encore, tester séparément : via le
      harnais de test du spawner (shift + clic droit, changement de phase manuel) **et** via la
      fin de vague automatique (tuer tous les monstres) — noter lequel des deux (ou les deux)
      ne fait pas réapparaître le coffre, ça aiderait à resserrer le diagnostic.
- [ ] Plusieurs coffres sur la même map : ouvrir l'un ne doit pas faire disparaître les autres,
      et tous doivent réapparaître à la vague suivante (pas seulement le premier du registre).
- [ ] Mana déjà à 100/100 : le coffre s'ouvre quand même (marqué comme utilisé pour la vague),
      mais le mana ne dépasse pas 100 — l'excédent est pour l'instant simplement perdu (pas de
      drop au sol, voir 05-etat-et-problemes-connus.md, en attente de PR #12).
- [ ] Un son de coffre qui s'ouvre doit se faire entendre à l'ouverture réussie.
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `ManaChestBlock`,
      `ManaChestBlockEntity`, `ManaChestConfigScreen`/`Menu`/`Payload`.

## Le spawner n'est plus jamais un obstacle physique (`SpawnerBlock`)

Nouveau (2026-08-25), par-dessus le gameplay du spawner déjà testé et confirmé le 2026-08-23 —
voir [02-gameplay.md](02-gameplay.md#jamais-un-obstacle-physique--getshapegetcollisionshapegetrendershape).
Jamais vérifié visuellement dans ce nouvel état.

- [ ] **En créatif**, le spawner reste **invisible** (pas de modèle de bloc affiché), mais en
      visant précisément l'endroit où il est posé, un **contour de sélection** doit apparaître
      (comme n'importe quel bloc ciblé) — c'est ce contour qui permet de le retrouver/cliquer.
- [ ] **En créatif**, marcher à travers l'endroit où il est posé : **aucune collision**, on
      traverse comme si rien n'était là.
- [ ] **En survie**, viser précisément l'endroit où il est posé : **aucun contour**
      n'apparaît, le clic droit ne fait strictement rien (ni message, ni interaction) — le bloc
      doit être **totalement introuvable** en survie, contrairement à avant où un message
      "créatif uniquement" s'affichait encore.
- [ ] Faire spawn un monstre par-dessus/à côté d'un spawner (créatif ou survie) : le monstre ne
      doit **jamais rester coincé/bloqué** par le bloc du spawner en essayant de se déplacer.
- [ ] Poser un spawner **sans configurer de rayon** (0 par défaut), passer en Combat : les
      monstres doivent apparaître normalement, **debout sur le vrai sol de la map**, pas en
      train de tomber dans le vide — c'est le point le plus important de cette section
      (`findSafeSpawnPos` spawn maintenant à `pos` directement, plus au-dessus du bloc du
      spawner, qui n'est plus solide). Si des monstres disparaissent ou tombent indéfiniment
      juste après leur apparition, c'est ce changement qui est en cause.
- [ ] Aucune erreur/exception dans les logs liée à `SpawnerBlock`/`findSafeSpawnPos` au premier
      spawn d'un monstre.

## La suppression de tour (`TowerRemovalState`, `TowerRemovalClientEvents`, `RemoveTowerPayload`)

Deux corrections le 2026-08-26 suite à un premier passage de test — le mode reste actif d'une
suppression à l'autre a été retiré, et l'exclusion mutuelle avec la roue de pose (qui ne
bloquait que dans un sens) a été rendue symétrique. Les deux restent à reconfirmer.

- [ ] Poser une tour (Spike Blockade ou Harpoon Turret) via la roue, en phase Construction.
      Appuyer sur `X` (touche par défaut, "Basculer le mode suppression de tour") : un message
      système "Mode suppression de tour : ACTIVÉ..." doit apparaître.
- [ ] Viser la tour posée pendant que le mode est actif : un **contour orange** doit apparaître
      autour d'elle (distinct du vert/rouge de la roue de pose). Viser autre chose (sol, mur,
      Cristal d'Eternia, un monstre...) : **aucun** contour ne doit apparaître.
- [ ] Clic gauche en visant la tour (contour orange visible) : la tour doit **disparaître
      instantanément**, sans jeter d'item au sol, et un message "+X mana remboursé (Y/100)"
      doit s'afficher (X = la moitié du coût de pose arrondi : 15 pour Spike Blockade coût 30,
      25 pour Harpoon Turret coût 50) — vérifier aussi que le HUD mana (losange bas-gauche)
      reflète bien le nouveau total.
- [ ] Clic gauche en visant autre chose qu'une tour pendant le mode actif (sol, mur, un
      monstre à proximité) : **rien** ne doit se passer (pas de dégât au monstre, pas de bloc
      cassé) — le clic gauche doit être totalement neutralisé pendant ce mode, cible valide ou
      non.
- [ ] **Changé (2026-08-26)** : après avoir supprimé une tour (clic gauche réussi), le mode
      doit se **désactiver automatiquement** — message "Mode suppression de tour : DÉSACTIVÉ.",
      contour orange disparaît, clic gauche redevient normal. Poser deux tours proches, activer
      le mode, en supprimer une : viser la seconde immédiatement après ne doit **plus** afficher
      de contour orange (il faut rappuyer sur `X` pour la retirer aussi).
- [ ] Rappuyer sur `X` (bascule manuelle, sans avoir rien supprimé) pendant que le mode est
      actif : mêmes effets que ci-dessus (désactivation, message, contour disparaît).
- [ ] Activer le mode en phase Construction, puis faire basculer en Combat (harnais de test du
      spawner) pendant que le mode est actif : doit se désactiver **automatiquement**, avec le
      message "Les tours ne peuvent être posées qu'en phase de Construction !" (message
      réutilisé, un peu trompeur ici puisqu'il ne parle que de pose — à surveiller si ça prête
      à confusion en jeu).
- [ ] **Corrigé (2026-08-26)** : essayer d'ouvrir la roue de pose (`R`) pendant que le mode
      suppression est actif : ne doit **rien** faire (auparavant, ça ouvrait la roue quand
      même — bug signalé en jeu : les deux modes restaient actifs en même temps, un clic gauche
      déclenchait alors à la fois l'annulation de la pose et la suppression de la tour visée).
      Vérifier aussi le sens inverse : activer le mode suppression (`X`) pendant que le mode
      pose est actif (hologramme affiché) ne doit rien faire non plus (déjà correct avant).
- [ ] Essayer de casser une tour à la **pioche**, en survie, hors du mode suppression : ne doit
      **plus rien** faire (voir la section dédiée juste en dessous, "Casser un bloc est
      désactivé") — la touche dédiée est désormais la seule vraie façon de les retirer.
- [ ] Vérifier `run/logs/latest.log` après la session : aucune exception liée à
      `TowerRemovalState`, `TowerRemovalClientEvents`, `RemoveTowerPayload`, ou
      `ModNetworking.handleRemoveTower`.

## Casser un bloc est désactivé (`ModEvents.onBlockBreakAttempt`)

Jamais testé en jeu. Changement global, à vérifier sur plusieurs types de blocs différents, pas
seulement les tours.

- [ ] En **survie**, essayer de casser un bloc de terrain quelconque (sol/mur de la taverne ou
      d'une map) à la main ou avec un outil : le bloc ne doit **jamais se casser**, un message
      "Impossible de casser des blocs dans ce monde." doit apparaître (une seule fois par
      tentative, pas en double).
- [ ] Comparer visuellement les **fissures de minage** (cracks) pendant que la touche est
      maintenue : elles peuvent apparaître le temps de l'appui (comportement vanilla normal),
      mais le bloc ne doit **jamais** finir par disparaître, quel que soit le temps maintenu.
- [ ] Refaire le test sur une **tour posée** (Spike Blockade/Harpoon Turret), le **Cristal
      d'Eternia**, un **coffre de mana**, et le **spawner** (si ciblable, voir sa propre
      section) : aucun de ces blocs ne doit se casser en survie non plus — le handler est
      générique, pas de cas particulier attendu par type de bloc.
- [ ] Repasser en **créatif** : casser un bloc quelconque (terrain, tour...) doit fonctionner
      **normalement**, comme avant ce changement (instantané, sans item — comportement vanilla
      standard du créatif) — le créatif ne doit jamais être affecté par cette restriction.
- [ ] Vérifier qu'aucun message en double n'apparaît dans le chat pour une seule tentative de
      casse ratée (le message ne doit venir que du serveur, pas aussi du client).
- [ ] Vérifier `run/logs/latest.log` après la session : aucune exception liée à
      `ModEvents.onBlockBreakAttempt` ou `BreakBlockEvent`.

## Le bloc de spawn joueur (`ModBlocks.PLAYER_SPAWN`, `MapInstance#findAndConsumeSpawnMarker`)

Jamais testé en jeu. **Le mécanisme complet n'est pas testable pour l'instant** — voir
05-etat-et-problemes-connus.md, "Système de maps/structures" : il ne peut être exercé qu'une
fois qu'une vraie structure `.nbt` de map (avec un `PLAYER_SPAWN` posé dedans) remplace
`buildPlaceholderArena()`. Seuls les points suivants sont vérifiables dès maintenant :

- [ ] En créatif, ouvrir l'onglet créatif du mod : un item "Spawn Joueur" doit apparaître,
      poser le bloc correspondant sans crash.
- [ ] Poser le bloc n'importe où (taverne, placeholder de map...) : doit avoir l'apparence
      d'un bloc de lodestone (texture placeholder, voir doc/02-gameplay.md), pas de texture
      manquante.
- [ ] Cliquer sur "Jouer" dans la taverne avec un `PLAYER_SPAWN` posé quelque part dans le
      placeholder de map (par exemple posé juste avant de cliquer, en restant sur place) :
      comportement attendu **inchangé** pour l'instant (téléportation à `MAP_POS` comme avant),
      puisque `clearZone` efface la zone avant que le scan n'ait lieu — **pas un bug**, la
      limite est documentée ci-dessus.

## L'écran de fin de partie (`GameOverScreen`, `GameOverPayload`)

Jamais testé en jeu. Premier paquet **clientbound** du mod — à surveiller particulièrement
(risque de `NoClassDefFoundError` côté serveur dédié si le handler avait été enregistré au
mauvais endroit, voir doc/02-gameplay.md).

- [ ] À la **victoire** (voir section précédente) : en plus du message système, un écran plein
      doit s'ouvrir automatiquement, titre **"Victoire ! Toutes les vagues sont nettoyées."**
      en **vert**, avec deux boutons **"Rejouer"** et **"Retour à la taverne"**.
- [ ] À la **défaite** : même écran, titre **"Défaite ! Le Cristal d'Eternia est tombé."** en
      **rouge**.
- [ ] Avec **plusieurs joueurs** présents : l'écran doit s'ouvrir pour **tous**, pas seulement
      celui qui a déclenché la victoire/défaite (ex. celui qui a tué le dernier monstre, ou
      celui dont l'attaque a détruit le cristal).
- [ ] Cliquer **"Rejouer"** : l'écran se ferme, et le comportement doit être identique à un
      clic sur "Jouer" dans la taverne (zone nettoyée et reposée, téléportation) — vérifier
      aussi que si le Cristal d'Eternia avait été détruit (cas défaite), il **reste absent**
      après "Rejouer" (limite connue, voir 05-etat-et-problemes-connus.md — pas un bug si
      c'est le cas, un bug seulement si autre chose casse).
- [ ] Cliquer **"Retour à la taverne"** : l'écran se ferme, effet identique à taper `/dd_leave`
      soi-même (retour à la taverne, zone de map nettoyée).
- [ ] Ouvrir l'écran puis appuyer sur `Échap` sans cliquer aucun bouton : l'écran doit se
      fermer sans rien envoyer au serveur (pas de téléportation, pas de nettoyage de zone).
- [ ] Vérifier `run/logs/latest.log`, **côté serveur en particulier** (si testé avec un serveur
      dédié) : aucune exception liée à `GameOverPayload`, `ModNetworking`, ou un
      `NoClassDefFoundError`/`ClassNotFoundException` mentionnant `Minecraft`/`Screen` —
      signerait que le handler client a fini par se charger côté serveur malgré la séparation
      voulue.

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
