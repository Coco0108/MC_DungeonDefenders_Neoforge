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
- [ ] L'œuf d'invocation correspondant (zombie ou squelette) apparaît à gauche du texte, bien
      centré verticalement dessus, ni minuscule ni énorme (16px, comme dans la hotbar).
- [ ] L'œuf **disparaît d'un coup** avec le popup à la fin des 1,5 secondes (pas de fondu
      progressif sur l'icône, contrairement au texte qui s'estompe bien lui) — comportement
      attendu, pas un bug (voir doc/05).
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

## Config CLIENT — options d'affichage HUD (`ClientDisplayConfig`)

Nouveau (2026-08-27), jamais vérifié visuellement.

- [ ] Au premier lancement, un fichier `config/dungeon_defenders-client.toml` apparaît, avec
      `showScoreGainPopup=true` dedans.
- [ ] Menu Mods > Dungeon Defenders > Config : un écran distinct "HUD Display"/"Affichage
      (HUD)" apparaît (en plus de celui déjà existant pour la config de gameplay), avec une
      case à cocher "Show score gain popup"/"Afficher le popup de gain de score".
- [ ] Décocher l'option, valider : tuer un monstre n'affiche plus le popup "+X" en bas à
      droite — le reste du HUD (Score en bas centre, HUD général) continue de fonctionner
      normalement.
- [ ] Recocher l'option : le popup réapparaît au kill suivant.
- [ ] L'option ne touche que **ce client** : en multijoueur, un autre joueur qui n'a pas
      décoché la sienne continue de voir ses propres popups normalement.
- [ ] Modifier directement `showScoreGainPopup=false` dans le fichier `.toml` (jeu fermé) puis
      relancer : l'option décochée dans l'écran de config reflète bien la valeur du fichier.
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `ClientDisplayConfig`.

## Contour de sélection masqué sur les tours et cristaux (`BlockOutlineClientEvents`)

Nouveau (2026-08-30), jamais vérifié visuellement. Le risque principal n'est pas que le contour
reste affiché, c'est qu'il **emporte le ciblage avec lui** — d'où les points d'interaction
ci-dessous, à vérifier au moins autant que le rendu.

- [ ] Viser une tour posée (n'importe laquelle : Spike/Bouncer/Slice N Dice Blockade, Harpoon/
      Bowling Ball/Mortar Turret) : **aucun contour noir**, à aucune hauteur de visée.
- [ ] Viser le Cristal d'Eternia, puis le cristal de la taverne : **aucun contour noir** non
      plus (le cristal d'Eternia en avait un de 3 blocs de haut, très visible).
- [ ] Viser un bloc quelconque à côté (terrain, coffre de mana) : le contour noir **est toujours
      là** — seuls les tours et cristaux sont concernés. (Le bloc de spawn joueur n'a lui de
      contour qu'en créatif, pour une autre raison — voir sa propre section.)
- [ ] Clic droit sur le Cristal d'Eternia en Construction : le vote « prêt » fonctionne toujours
      (message de progression) — c'est la preuve que le ciblage n'a pas été cassé.
- [ ] Clic droit sur le cristal de la taverne : `MapSelectionScreen` s'ouvre toujours.
- [ ] Mode suppression de tour (`X`) : le contour **orange** apparaît toujours sur la tour visée
      (et devrait être plus lisible qu'avant, sans la boîte noire par-dessus), et le clic gauche
      retire bien la tour.
- [ ] En créatif, casser une tour/un cristal reste possible (viser + clic gauche maintenu) même
      sans contour — un peu moins confortable, à confirmer que c'est acceptable en usage réel.
- [ ] La barre de vie des tours (`TowerHealthBarRenderer`) s'affiche toujours normalement.
- [ ] Option `showTowerBlockOutline` : présente et **décochée par défaut** dans Menu Mods >
      Dungeon Defenders > Config > "Affichage (HUD)". La cocher fait **revenir** le contour noir
      sur les tours et cristaux (comportement d'avant), la décocher le fait repartir.
- [ ] Sur le serveur dédié : le comportement est le même, et un autre joueur qui n'a pas touché
      à son option voit la même chose (config purement locale, rien de synchronisé).
- [ ] Vérifier `run/logs/latest.log` : aucune exception liée à `BlockOutlineClientEvents`.

## Le reste du roster de l'Écuyer (Bouncer Blockade, Bowling Ball Turret)

(Bouncer/Slice N Dice Blockade, Bowling Ball/Mortar Turret) — design discuté et validé avec le
joueur avant d'être codé, voir 02-gameplay.md pour le détail. **Trois tours de test effectués
(2026-08-29)** : Slice N Dice confirmée sans souci dès le premier ; Mortar Turret confirmée
bonne au troisième, après deux passes de correction (flèche → explosion trop grosse →
explosion réduite) ; Bowling Ball corrigée en une passe (tir horizontal), pas resignalée depuis.
Sections retirées d'ici une fois confirmées. **Seul le Bouncer reste actif** : sens du
knockback corrigé, puis force ajustée deux fois (1.6F trop faible dans le mauvais sens → trop
fort une fois corrigé → 0.8F). Le Harpoon Turret et le Spike Blockade, eux, ont déjà été testés
bien avant ce roster (voir l'intro de ce fichier) : c'est en reconstruisant leur base commune
(`AbstractTurretBlockEntity`) pour ce roster que deux bugs déjà corrigés dans cette branche
(cooldown de tir, origine de la flèche) ont été retrouvés puis réappliqués sans régression —
pas la peine de re-tester le Harpoon Turret pour ça. Poser chaque tour via la roue (section
dédiée plus bas).

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

## L'export d'un pack de maps (`/dd_export`)

Nouveau (2026-09-02). Le TOML et le JSON generes ont ete valides hors jeu (analyse syntaxique),
et `lowcodefml` existe bien dans le loader de cette version — mais **le jar produit n'a jamais
ete charge par Minecraft**, c'est tout l'objet de ce test.

- [ ] Creer deux maps sous un meme namespace personnalise (ex. `test_pack:map/a` et
      `test_pack:map/b`), puis lancer `/dd_export test_pack`.
- [ ] Un message de succes indique le nombre de maps, d'apercus (0 pour l'instant) et le chemin.
- [ ] Le fichier existe dans `<dossier du serveur>/dungeon_defenders_export/test_pack.jar`.
      L'ouvrir avec un gestionnaire d'archives : il doit contenir `META-INF/neoforge.mods.toml`,
      deux `.nbt` sous `data/test_pack/structure/map/`, et deux fichiers de langue.
- [ ] **Le test qui compte** : deposer ce jar dans le dossier `mods` d'une installation
      **propre** (un autre monde, ou apres avoir supprime les maps de la sauvegarde), demarrer,
      et verifier que les deux maps apparaissent dans le carrousel sous un pack nomme
      "Test Pack" — sans erreur au demarrage.
- [ ] Verifier que les maps du jar sont bien **non supprimables** (bouton grise) : ce sont des
      ressources en lecture seule.
- [ ] `/dd_export` sur un namespace inexistant, ou avec des majuscules : message d'erreur clair,
      pas de plantage, pas de jar vide cree.
- [ ] `/dd_export dungeon_defenders` alors que la campagne vient du jar du mod : doit refuser
      (aucune map creee en jeu sous ce namespace), sauf si tu as toi-meme cree des maps dedans.
- [ ] La commande refuse bien en dessous du niveau de permission gamemaster.

## Le force-chargement de la zone de map (`ModChunkTickets`)

Nouveau (2026-09-02), jamais verifie en jeu. Difficile a observer directement : le test consiste
surtout a verifier qu'un spawner **loin** du groupe fonctionne quand meme.

- [ ] Construire une map assez large pour que deux spawners soient a plus de ~150 blocs l'un de
      l'autre (au-dela de la distance de rendu/simulation par defaut).
- [ ] Lancer la partie, rester pres d'un seul spawner pendant toute une vague : les ennemis du
      spawner **eloigne** doivent quand meme apparaitre et arriver. C'est le test central.
- [ ] `/forceload query` dans la zone de map pendant une partie : des chunks doivent etre
      signales comme forces.
- [ ] Retourner a la taverne (`/dd_leave` ou bouton Abandonner) puis refaire `/forceload query` :
      **plus aucun** chunk force dans la zone de map.
- [ ] Quitter le serveur **en pleine partie**, le relancer, puis `/forceload query` : plus aucun
      chunk force non plus — c'est le rappel de validation qui doit avoir fait le menage.
- [ ] Enchainer deux parties de tailles de map differentes : aucun chunk de la premiere ne doit
      rester force pendant la seconde.
- [ ] Sur une tres grande map, verifier l'avertissement dans les logs au-dela de 1024 chunks (et
      surveiller le TPS du serveur).
- [ ] Aucune exception liee a `ModChunkTickets` ou au systeme de tickets.

## La creation de map en jeu (`MapRegistry`, `MapConfigBlock`, ecran a trois colonnes)

Nouveau (2026-09-02), jamais verifie en jeu. **La plus grosse modification a ce jour** : l'enum
GameMap disparait, le carrousel devient dynamique, StartGamePayload change de forme, et le
nombre de vagues n'est plus une constante. Beaucoup de regressions possibles sur l'existant.

> Une **map de test est desormais livree** dans le mod (`dungeon_defenders:map/test_arena`) :
> l'essentiel de cette section se teste donc sans rien construire. Elle s'appelle "Arene de
> test", tient en 3 vagues, et contient deja cristal, spawner, coffre de mana, marqueur de spawn
> et zones interdites.

- [ ] Clic droit sur le cristal de la taverne : "Arene de test" apparait dans le pack
      "Campagne". L'apercu affiche la texture manquante (aucun PNG) — attendu.
- [ ] Cliquer "Jouer" : l'arene se pose a (10000, 65, 0), murs compris. On arrive devant le
      cristal, pas au milieu du vide.
- [ ] Le HUD affiche **Vague 1/3**, pas 1/5 — la preuve que le nombre de vagues vient de la map.
- [ ] Le spawner fait bien apparaitre 8 zombies et 4 squelettes en combat.
- [ ] Le coffre de mana s'ouvre et donne 50 mana.
- [ ] Poser une tour pres du spawner (dans la zone rouge) est refuse avec le message ; quelques
      blocs plus loin, ca passe.
- [ ] Enchainer les 3 vagues jusqu'a la victoire, puis "Rejouer" : l'arene est reposee a neuf,
      cristal a 100 PV, tours effacees.

Sans aucune map (retirer temporairement test_arena du jar, ou tester avant de l'ajouter) :

- [ ] Clic droit sur le cristal de la taverne : l'ecran s'ouvre, colonne des packs vide, message
      "Aucune map disponible" a la place de l'apercu, bouton "Jouer" **grise**.
- [ ] Aucune exception dans les logs.

Creer une map de bout en bout :

- [ ] En creatif, construire une petite arene **loin de (0,65,0) et de (10000,65,0)**, avec un
      `eternia_crystal`, un `spawner` configure, un `player_spawn`, et un bloc
      **Configuration de map**.
- [ ] Clic droit sur le bloc de config : l'ecran s'ouvre (creatif seulement — verifier qu'en
      survie il est introuvable et que le message apparait si on force). Renseigner nom, ordre,
      nombre de vagues (mettre **3**, pas 5, pour verifier que ce n'est plus une constante) et
      multiplicateur. Valider.
- [ ] Rouvrir l'ecran : les valeurs saisies sont bien la (persistance + synchro du block entity).
- [ ] Sauvegarder la zone avec un bloc de structure vanilla, nommee
      `dungeon_defenders:map/ma_map`.
- [ ] **Sans redemarrer**, retourner a la taverne, clic droit sur le cristal : la map apparait,
      dans le pack "Campagne", avec le nom saisi. C'est le point central de toute la
      fonctionnalite.
- [ ] L'apercu affiche la texture "manquante" (aucun PNG fourni) — comportement attendu, pas un
      bug.
- [ ] Cliquer "Jouer" : la **vraie structure** est posee a (10000,65,0), pas la plateforme de
      pierre lisse. On arrive sur le `player_spawn`, qui a disparu.
- [ ] Le HUD affiche **Vague 1/3**, pas 1/5 — c'est ce qui verifie que le nombre de vagues vient
      bien de la map.
- [ ] Enchainer les 3 vagues jusqu'a la victoire : l'ecran de fin s'ouvre bien a la 3e.
- [ ] Bouton "Rejouer" de l'ecran de fin : relance **la meme map** (et pas la plateforme
      placeholder) — c'est ce que verifie l'attachment CURRENT_MAP.

Les packs et le regroupement :

- [ ] Creer une deuxieme map dans le meme namespace : les deux apparaissent sous "Campagne", et
      les fleches du carrousel passent de l'une a l'autre avec le compteur "1 / 2".
- [ ] Donner des ordres differents dans leurs blocs de config, resauvegarder : l'ordre du
      carrousel suit.
- [ ] Sauvegarder une map sous un **autre namespace** (ex. `test_pack:map/essai`) : un deuxieme
      pack apparait dans la colonne de gauche, sous "Campagne", et son carrousel ne contient que
      ses maps. Son nom affiche est `test_pack` (pas de traduction fournie).
- [ ] Selectionner un pack puis l'autre : le carrousel repart bien a la premiere map du pack.

La suppression :

- [ ] **En survie**, ouvrir l'ecran de choix : **aucun** bouton "Supprimer cette map".
- [ ] **En creatif**, le bouton apparait sous "Jouer", en rouge.
- [ ] Sur une map creee en jeu : le bouton est **actif**. Cliquer -> confirmation.
- [ ] Repondre Non : rien n'est supprime, retour a l'ecran de choix.
- [ ] Repondre Oui : message en chat, et la map disparait **immediatement** du carrousel (pas
      besoin de rouvrir l'ecran).
- [ ] Supprimer la derniere map d'un pack : le pack disparait aussi de la colonne de gauche, et
      la selection retombe sur un pack valide sans planter.
- [ ] Sur une map livree dans le jar du mod (quand la campagne existera) : le bouton est
      **grise** — c'est une ressource en lecture seule.

Regressions a surveiller :

- [ ] Les spawners fonctionnent toujours : leur ecran de config s'ouvre, la borne "derniere
      vague active" propose par defaut le nombre de vagues de la map en cours (et non plus 5).
- [ ] `/dd_leave` et le bouton "Abandonner le niveau" ramenent toujours a la taverne, et le HUD
      repasse en phase Taverne.
- [ ] Le HUD Vague reaffiche bien la bonne borne apres un retour a la taverne puis une nouvelle
      partie sur une map a nombre de vagues different.
- [ ] Aucune exception liee a `MapRegistry`, `MapConfigBlockEntity`, `StartGamePayload` ou
      `OpenMapSelectionPayload` — ce dernier a change de forme (il portait zero champ, il porte
      maintenant une liste), donc un client et un serveur de versions differentes ne peuvent pas
      se parler : verifier que les deux sont bien a jour.

## Le bouton "Abandonner le niveau" (menu pause)

Nouveau (2026-09-02), jamais verifie en jeu. Premier ajout du mod a un ecran **vanilla** : le
risque principal est un bouton mal place ou superpose a ceux de Minecraft.

- [ ] **A la taverne**, ouvrir le menu pause (Echap) : **aucun** bouton "Abandonner le niveau".
- [ ] **Dans une map** (apres avoir clique "Jouer"), ouvrir le menu pause : le bouton apparait,
      en **rouge**, **sous** le bouton "Sauvegarder et quitter" — pas superpose, pas hors ecran.
- [ ] Verifier a plusieurs tailles de fenetre et plusieurs valeurs de "GUI Scale" (surtout la
      plus grande) : le bouton doit rester visible et ne jamais chevaucher un bouton vanilla.
- [ ] Cliquer dessus : un ecran de confirmation s'ouvre, avec le titre et un message qui precise
      que **tous les joueurs** sont ramenes.
- [ ] Repondre **Non** : retour au **menu pause** (pas au jeu), la partie continue.
- [ ] Repondre **Oui** : l'ecran se ferme, retour a la taverne, la souris est rendue au jeu (pas
      de curseur bloque), et le HUD repasse en "Phase : Taverne" sans compteurs de vague.
- [ ] Abandonner **en pleine phase de Combat** avec des monstres vivants : ca doit marcher aussi,
      et les monstres de la map disparaitre avec la zone nettoyee.
- [ ] **En multijoueur** : un joueur qui abandonne ramene **tout le monde** a la taverne. C'est
      voulu (une seule partie active a la fois), a confirmer que ce n'est pas vecu comme un bug.
- [ ] Relancer une partie juste apres un abandon : elle repart bien **vague 1**.
- [ ] `/dd_leave` fonctionne toujours en parallele.
- [ ] Aucune exception dans les logs liee a `PauseMenuClientEvents` ou `LeaveMapPayload`.

## Les zones interdites a la pose (`NoBuildZoneBlock`)

Nouveau (2026-09-02), jamais verifie en jeu.

- [ ] En creatif, l'item "Marqueur de zone interdite" apparait dans l'onglet du mod et se pose
      sans crash. Le bloc pose est **invisible** ; en creatif on le retrouve au contour de
      visee, en survie il est traversable et introuvable.
- [ ] Marcher a travers : aucune collision, ni pour le joueur ni pour un monstre (a verifier en
      combat, un monstre doit traverser la zone normalement).
- [ ] Poser un marqueur, ouvrir la roue des tours, viser la case du marqueur : l'hologramme doit
      etre **rouge** (position refusee).
- [ ] Clic droit pour confirmer quand meme : rien ne se pose, et le message "Impossible de poser
      une tour ici." apparait. C'est le point important — sans ce message le refus serait
      inexplicable.
- [ ] Viser une case **voisine** libre : hologramme vert, la tour se pose normalement. Le
      marqueur ne doit interdire que sa propre case.
- [ ] `/fill` une zone de marqueurs (ex. un couloir 5x1x10) : toute la zone refuse la pose,
      d'un bout a l'autre.
- [ ] Le marqueur ne fait **ni ombre ni trou noir** : poser un bloc plein a cote, sa face
      tournee vers le marqueur s'affiche normalement ; une torche d'un cote eclaire bien de
      l'autre.
- [ ] Sauvegarder une structure contenant des marqueurs avec un bloc de structure, la recharger
      ailleurs : les marqueurs sont bien presents et interdisent toujours la pose.
- [ ] A la Taverne (ou la pose est gratuite) : un marqueur interdit quand meme la pose — la
      gratuite ne doit pas contourner l'interdiction.
- [ ] Aucune exception dans les logs liee a `NoBuildZoneBlock`.

## La phase Taverne (`GamePhase.TAVERN`)

Nouveau (2026-09-01), jamais verifie en jeu. Touche 11 points de decision dans le code : le
risque n'est pas tant la taverne elle-meme que les **regressions sur une vraie partie**, la
phase par defaut ayant change.

A la taverne :

- [ ] Le HUD affiche "Phase : Taverne". Les compteurs **Vague** (haut droite) et **Ennemis**
      (barre haut centre) sont **absents**.
- [ ] La roue des tours (`R`) s'ouvre, et poser une tour marche — **sans rien couter en mana**
      (le HUD mana ne bouge pas, aucun message "-X mana").
- [ ] En poser cinq ou six d'affilee avec 0 mana : toutes doivent passer.
- [ ] Le mode suppression (`X`) fonctionne, et retirer une tour **ne rembourse rien** (pas de
      message "+X mana remboursé", HUD mana inchange).
- [ ] Poser une tourelle a portee du mannequin : elle lui tire dessus (c'est tout l'interet).

Le passage taverne -> map :

- [ ] Poser deux ou trois tours d'essai dans la taverne, puis cliquer "Jouer" : elles ont
      **disparu** de la taverne au retour (`clearTestTowers`).
- [ ] Une fois sur la map, le HUD passe en "Phase : Construction", affiche **Vague 1/5** et la
      barre d'ennemis revient.
- [ ] Sur la map, poser une tour **coute bien du mana** a nouveau, et la retirer **rembourse**
      50 %.
- [ ] Enchainer une vague complete : Construction -> Combat -> Construction, la vague avance
      normalement (verifie que `startNewGame` n'a pas casse la progression).
- [ ] Deux parties d'affilee : "Jouer", quitter avec `/dd_leave`, "Jouer" de nouveau — la
      seconde doit repartir **vague 1**, pas vague 2 (c'est le piege que `startNewGame` evite).

Le retour map -> taverne :

- [ ] `/dd_leave` depuis une map : retour a la taverne ET le HUD repasse en "Phase : Taverne",
      compteurs de vague masques.
- [ ] Meme chose via le bouton "Retour a la taverne" de l'ecran de fin de partie.
- [ ] Redemarrer le serveur alors que la sauvegarde etait en pleine partie (quitter le serveur
      en phase Combat sur une map) : au redemarrage on doit etre a la **Taverne**, pas coince en
      Combat.

Cas limites :

- [ ] En Combat, la roue des tours refuse toujours de s'ouvrir (message "phase de Construction")
      — la phase Taverne ne doit pas avoir ouvert une breche.
- [ ] Maj + clic droit sur un spawner depuis la taverne : bascule en Construction. Sans interet,
      mais ne doit rien casser.
- [ ] Aucune exception dans les logs liee a `PhaseTransitions`, `GamePhase` ou
      `TavernSpawn.clearTestTowers`.

## Le mannequin d'entrainement (`TrainingDummyEntity`, `TrainingDummyBlock`)

Nouveau (2026-08-31), jamais verifie en jeu. Deuxieme `Entity` custom du mod, et le premier a
etre un `Mob` (attributs, IA, rendu) — plusieurs points peuvent casser independamment.

Le bloc et l'invocation :

- [ ] En creatif, l'item "Support de mannequin" apparait dans l'onglet du mod et se pose sans
      crash. Le bloc pose est **invisible** ; en creatif on le retrouve au contour de visee, en
      survie il est traversable et introuvable (meme comportement que le spawner).
- [ ] Dans la seconde qui suit la pose, un mannequin apparait **juste au-dessus** du bloc.
- [ ] Le tuer n'est pas possible, mais le supprimer a la main (`/kill @e[type=dungeon_defenders:training_dummy]`)
      doit le faire **reapparaitre** en moins d'une seconde.
- [ ] Casser le bloc en creatif : le mannequin **disparait avec lui**
      (`affectNeighborsAfterRemoval`), pas d'entite orpheline laissee derriere.
- [ ] Poser deux blocs cote a cote : deux mannequins distincts, un par bloc. Poser deux blocs
      l'un sur l'autre est un cas limite (le rayon de recherche fait 1,5 bloc) : verifier qu'il
      n'en manque pas un.
- [ ] Redemarrer le serveur plusieurs fois : il ne doit **jamais** y avoir deux mannequins
      empiles au meme endroit.

Le mannequin lui-meme :

- [ ] Il ressemble a un **zombie immobile** — comportement attendu (placeholder, pas de modele
      de mannequin dedie), pas un bug.
- [ ] Il ne bouge **jamais** : pas de marche, pas de rotation vers le joueur, pas de chute meme
      sans bloc solide sous lui.
- [ ] Il ne **brule pas** au soleil (un zombie normal, lui, prend feu).
- [ ] **Aucune barre de vie** ne s'affiche au-dessus de lui, meme apres l'avoir frappe.
- [ ] Le frapper a l'epee : l'animation de degat joue, mais il ne meurt jamais et sa vie ne
      descend pas durablement.
- [ ] Poser une **Harpoon Turret** (ou n'importe quelle tourelle) a portee : elle doit **lui
      tirer dessus** — c'est le point central de toute la fonctionnalite, et ce qui verifie que
      le ciblage `Monster` fonctionne pour un type custom.
- [ ] Poser un **Bouncer Blockade** contre lui : il encaisse les coups **sans etre repousse**
      (`KNOCKBACK_RESISTANCE`).
- [ ] Il n'attaque **personne** et ne se dirige jamais vers le Cristal d'Eternia (verifie que
      `ModEvents.onMonsterSpawn` l'ignore bien).
- [ ] Le tuer etant impossible, verifier qu'il ne donne **ni cristal de mana, ni XP, ni score**,
      et qu'il ne compte **pas** dans les ennemis d'une vague (`WAVE_ENEMIES_TOTAL/KILLED`).
- [ ] Aucune exception dans les logs liee a `TrainingDummyEntity`, `TrainingDummyBlockEntity` ou
      `EntityAttributeCreationEvent` (un Mob custom sans attributs plante a l'invocation).

## Le chargement de la structure de la taverne (`TavernSpawn#placeTavern`)

Nouveau (2026-08-31), jamais vérifié en jeu — et **jamais essayé avec une vraie structure**,
puisqu'aucune n'existe encore. Les premiers points sont donc testables tout de suite (repli),
les suivants seulement une fois `tavern.nbt` livré.

Sans fichier de structure (état actuel du dépôt) :

- [ ] Démarrer le monde/serveur : la plateforme 9x9 de pierre lisse apparait comme avant, on
      arrive bien dessus, rien n'a changé pour le joueur.
- [ ] Les logs contiennent l'avertissement `Structure de taverne introuvable ... repli sur la
      plateforme provisoire.` (une fois par chargement du monde, pas en boucle).
- [ ] `/dd_leave` depuis une map ramene toujours au meme endroit.

Une fois `data/dungeon_defenders/structure/tavern.nbt` livre :

- [ ] Au demarrage, la taverne apparait entierement, centree sur (0, 65, 0) en X/Z, son sol a
      Y=64. Aucun bloc manquant sur les bords.
- [ ] Plus aucune trace de l'ancienne plateforme de pierre lisse autour (c'est ce que doit
      effacer le nettoyage de zone) — ni d'une version precedente de la taverne, a tester en
      livrant une structure plus petite apres une plus grande.
- [ ] Le `tavern_crystal` de la structure est bien la et son clic droit ouvre l'ecran de choix
      de map (les block entities d'une structure gardent leurs donnees NBT).
- [ ] Avec un `player_spawn` pose dans la structure : on arrive **a sa position**, pas au
      centre. Sans marqueur : on arrive au centre (0, 65, 0).
- [ ] Le marqueur **n'est pas consomme** : quitter la taverne, y revenir (`/dd_leave`), et
      redemarrer le serveur — on doit arriver au meme endroit a chaque fois.
- [ ] Mourir dans une map fait reapparaitre a la position d'arrivee de la taverne, pas a
      (0, 65, 0) si le marqueur est ailleurs.
- [ ] Les entites de la structure (cadres, supports a armure, tableaux) **sont bien posees**.
- [ ] **Le point le plus incertain** : redemarrer le serveur trois ou quatre fois d'affilee et
      recompter ces entites. Elles doivent rester au **meme nombre**, pas se dupliquer a chaque
      demarrage — c'est ce que doit empecher la suppression d'entites de `clearZone`, et c'est
      justement ce qui depend du fait que les chunks soient bien charges a ce moment-la.
- [ ] Laisser tomber un objet au sol dans la taverne, redemarrer : il a disparu (nettoyage
      volontaire).
- [ ] Se tenir dans la taverne pendant un redemarrage (multijoueur, ou juste se reconnecter) :
      le joueur n'est **jamais** supprime par le nettoyage.
- [ ] Modifier un bloc de la taverne en creatif, redemarrer : la modification est ecrasee (la
      structure est reposee a chaque chargement).
- [ ] Aucune exception dans les logs liee a `TavernSpawn`, `StructureTemplate` ou
      `StructureTemplateManager`.

## Le bloc de spawn joueur (`ModBlocks.PLAYER_SPAWN`, `MapInstance#findAndConsumeSpawnMarker`)

Jamais testé en jeu. **Le mécanisme complet n'est pas testable pour l'instant** — voir
05-etat-et-problemes-connus.md, "Système de maps/structures" : il ne peut être exercé qu'une
fois qu'une vraie structure `.nbt` de map (avec un `PLAYER_SPAWN` posé dedans) remplace
`buildPlaceholderArena()`. Seuls les points suivants sont vérifiables dès maintenant :

- [ ] En créatif, ouvrir l'onglet créatif du mod : un item "Spawn Joueur" doit apparaître,
      poser le bloc correspondant sans crash.
- [ ] **Corrigé (2026-08-26)** : l'item dans la main/l'inventaire doit maintenant afficher la
      texture de lodestone (auparavant cassée, `models/item/player_spawn.json` manquant) —
      identique à l'apparence du bloc posé, pas de texture manquante/magenta-noir.
- [ ] **Changé (2026-08-30)** : poser le bloc n'importe où (taverne, placeholder de map...) —
      il ne doit **rien** apparaître à l'écran, le bloc est désormais invisible (`PlayerSpawnBlock`,
      même traitement que le spawner). Pas de trou noir ni de face voisine qui disparaît autour
      de la position : rien du tout, comme si l'air était resté.
- [ ] **En créatif**, viser précisément l'endroit où il est posé : un **contour de sélection**
      doit apparaître — c'est le seul moyen de le retrouver, exactement comme pour le spawner.
      (Il n'est pas concerné par le masquage de contour des tours/cristaux, voir sa section.)
- [ ] **En survie**, viser précisément l'endroit où il est posé : **aucun contour**, le rayon de
      visée doit traverser comme s'il n'y avait rien.
- [ ] Marcher sur la position du bloc, en survie comme en créatif : on doit **traverser** (pas
      de collision), et ne pas rester "posé dessus" — attention à ne pas se faire piéger si le
      bloc a été posé au-dessus d'un vide.
- [ ] L'item reste bien **visible** dans l'onglet créatif et dans la main/l'inventaire (texture
      de lodestone) : c'est le bloc **posé** qui est invisible, pas l'item.
- [ ] Poser un bloc plein juste à côté d'un `PLAYER_SPAWN` : sa face tournée vers le marqueur
      doit s'afficher normalement (pas de face manquante/noire — le marqueur ne doit rien
      occlure).
- [ ] Vérifier qu'il ne fait pas d'ombre / ne bloque pas la lumière : poser une torche d'un côté
      du marqueur, l'éclairage de l'autre côté ne doit pas être coupé.
- [ ] Cliquer sur "Jouer" dans la taverne avec un `PLAYER_SPAWN` posé quelque part dans le
      placeholder de map (par exemple posé juste avant de cliquer, en restant sur place) :
      comportement attendu **inchangé** pour l'instant (téléportation à `MAP_POS` comme avant),
      puisque `clearZone` efface la zone avant que le scan n'ait lieu — **pas un bug**, la
      limite est documentée ci-dessus.

## L'écran de fin de partie (`GameOverScreen`, `GameOverPayload`)

Premier paquet **clientbound** du mod — à surveiller particulièrement (risque de
`NoClassDefFoundError` côté serveur dédié si le handler avait été enregistré au mauvais
endroit, voir doc/02-gameplay.md). **Deux corrections le 2026-08-26** suite à un premier test :
le titre ne s'affichait pas (couleurs sans octet alpha explicite, corrigé) et les messages de
chat victoire/défaite ont été retirés (redondants avec l'écran). Les points ci-dessous sont à
reconfirmer avec ces deux changements.

- [ ] À la **victoire** (voir section précédente) : un écran plein doit s'ouvrir
      automatiquement, avec le titre **"Victoire ! Toutes les vagues sont nettoyées."** bien
      **visible** en **vert** (auparavant invisible, seuls les boutons apparaissaient), avec
      deux boutons **"Rejouer"** et **"Retour à la taverne"**.
- [ ] À la **défaite** : même écran, titre **"Défaite ! Le Cristal d'Eternia est tombé."** bien
      visible en **rouge**.
- [ ] **Changé (2026-08-26)** : plus aucun message système de victoire/défaite ni lien "Retour
      à la taverne" ne doit apparaître dans le chat — seul l'écran `GameOverScreen` doit
      annoncer le résultat désormais (le message de destruction du cristal lui-même,
      `eternia_crystal.destroyed`, reste inchangé, c'est un message distinct).
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

## Le mod sur un serveur dédié

Le mod a été déployé pour la première fois sur le serveur dédié du joueur le 2026-08-30 : il ne
démarrait pas du tout (`NoClassDefFoundError: net/minecraft/client/gui/screens/Screen` dès
`constructMods`, à cause de `TavernCrystalBlock` — voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md#le-mod-ne-démarrait-pas-du-tout-sur-un-serveur-dédié-2026-08-30)).
Corrigé, mais **rien de ce qui suit n'a encore été rejoué sur un vrai serveur** : le solo ne
prouve rien ici, un client embarque les deux côtés.

Avant même de déployer, vérifier le jar produit (ni `build` ni `runServer` ne détectent ce type
de bug, l'environnement de dev contenant les classes des deux côtés) :

```bash
./gradlew build -x test
python3 tools/verifier-dist.py build/libs/dungeon_defenders-0.0.1.jar
```

Le script refait le raisonnement de la JVM d'un serveur dédié : il part des classes du mod
chargeables côté serveur et suit leurs références jusqu'à trouver — ou non — une classe
`net.minecraft.client.*`. Sortie attendue : `OK : aucune classe cliente nommée dans le graphe
serveur.` Sinon il affiche la chaîne fautive, à corriger avec un des deux patrons décrits dans
[01-architecture.md](01-architecture.md#la-règle-clientserveur--nommer-une-classe-cliente-suffit-à-casser-un-serveur-dédié).

- [ ] Le serveur démarre et le mod se charge (plus de `NoClassDefFoundError` dans les logs
      serveur).
- [ ] Un client peut se connecter (le protocole custom du mod négocie sans erreur : les paquets
      sont bien enregistrés des deux côtés).
- [ ] Clic droit sur le cristal de la taverne : `MapSelectionScreen` s'ouvre bien, avec le
      carrousel de maps et la difficulté courante — c'est le chemin qui a changé (paquet
      `OpenMapSelectionPayload` au lieu d'un appel direct), donc le plus à risque.
- [ ] Choisir une difficulté puis « Jouer » fonctionne toujours (aller-retour
      `SetDifficultyPayload` + `StartGamePayload`).
- [ ] Les autres écrans s'ouvrent toujours en multijoueur : config du spawner, config du coffre
      de mana, roue des tours, écran de fin de partie.
- [ ] Le HUD (mana, vie, vague, score, popup de gain) s'affiche et se met à jour pour un joueur
      connecté à distance, pas seulement en solo.
- [ ] À deux joueurs si possible : le score/l'XP sont bien partagés, et chacun voit ses propres
      popups.

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
