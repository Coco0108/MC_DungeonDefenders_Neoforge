# Idées & backlog

Ce fichier remplace le rôle du `TODOLIST.xlsx` original (voir `doc/README.md`) : un
endroit où le joueur note ses idées au fil de l'eau (en testant, ou juste dans la
journée), et où Claude les range ensuite par catégorie/difficulté pour pouvoir les
reprendre une par une sur demande ("fais-moi une fonctionnalité de la catégorie
Tours", "prends la plus facile du Mana", etc.).

## Comment ça marche

1. **Toi** : dès qu'une idée te vient (même depuis l'appli mobile GitHub), tu
   crées une **issue** sur le dépôt avec le label **`Idea`**. Pas besoin de
   structurer — un titre et une description en vrac suffisent.
2. **Moi** : quand tu me demandes ("va voir mes idées", "check les issues"), je
   liste les issues ouvertes avec le label `Idea` (`gh issue list --label Idea`),
   je lis chacune, je pose des questions si une idée est ambiguë, puis je la
   range dans le [Backlog](#backlog) ci-dessous avec une catégorie, une
   difficulté estimée et un statut — avec un lien vers l'issue d'origine pour
   la traçabilité. Je **ferme l'issue** une fois rangée (avec un commentaire
   pointant vers sa ligne du backlog), pour ne plus la re-traiter au prochain
   passage.
3. **Toi** : pour lancer une fonctionnalité, tu me donnes soit une catégorie
   ("occupe-toi d'un truc dans Tours"), soit une ligne précise du backlog. Je
   choisis (ou tu choisis) une idée avec le statut `Idée`/`Prête`, on la travaille
   comme d'habitude (`EnterPlanMode` si c'est gros), et une fois faite je la
   passe à `Faite` avec un lien vers la doc qui la documente réellement
   (`02-gameplay.md`, `05-etat-et-problemes-connus.md`...).

Le backlog est une aide à la priorisation, pas une source de vérité sur l'état du
code — comme rappelé pour l'ancien Excel, seul `05-etat-et-problemes-connus.md`
fait foi sur ce qui est réellement implémenté.

## Backlog

Catégories utilisées (ouvertes à en ajouter si besoin) : **Tours**, **Mana**,
**IA / Monstres**, **Vagues / Spawner**, **HUD / UI**, **Maps / Monde**,
**Classes / Héros**, **Sorts**, **Armes / Armures**, **Score / Progression**, **Divers**.

Difficulté : **Facile** / **Moyen** / **Difficile** (estimation grossière, pas
un chiffrage précis). Statut : **Idée** (brute, pas encore creusée) / **Prête**
(assez précisée pour être lancée) / **En cours** / **Faite**.

| Catégorie | Idée | Difficulté | Statut | Notes |
|---|---|---|---|---|
| HUD / UI | Voir la range d'une tour déjà posée (pas seulement à la pose) : un clic droit ou une touche paramétrable sur une tour affiche sa range comme lors de la pose ; reste 5s ou se referme sur un second clic (sur elle ou une autre tour). | Moyen | Prête | Réutilise le renderer de range déjà construit pour la pose (`client/TowerPlacementRenderState`) — le nouveau est le déclenchement hors phase de pose (input + timer/toggle). [Issue #13](https://github.com/Coco0108/MC_DungeonDefenders_Neoforge/issues/13) |
| HUD / UI | Voir la barre de vie d'une tour déjà posée — idéalement juste en la regardant (survol), plutôt qu'uniquement via un clic. | Moyen | Prête | Même famille d'interaction que la range (ligne au-dessus, issue #13) : peut se construire ensemble (un seul déclencheur "regarder/cliquer une tour" affichant range + PV). Nécessite un raycast continu en phase Construction pour détecter la tour visée. [Issue #16](https://github.com/Coco0108/MC_DungeonDefenders_Neoforge/issues/16) |
| Tours | Roue de sélection des tours : afficher sous chaque tour (empilé) son nom, une courte description, et son coût en mana — actuellement seuls le nom et le coût s'affichent, pas de description. | Facile | Prête | `TowerWheelScreen.extractRenderState` affiche déjà `displayName()` + `mana_cost` ; il manque un champ description sur `TowerDefinition` (+ une clé de lang par tour) et une ligne de texte de plus. [Issue #15](https://github.com/Coco0108/MC_DungeonDefenders_Neoforge/issues/15) |
| Tours | Upgrade de tour : 4 niveaux par tour (comme le jeu de référence), payant en mana, améliore probablement PV/dégâts/portée. | Difficile | Idée | Touche l'équilibrage de toutes les tours existantes et la roue/l'UI de pose (afficher/choisir un niveau). Proposée par Claude (2026-08-27), pas encore creusée avec le joueur — formule de coût/gain à définir. |
| IA / Monstres | Vague boss/mini-boss périodique (ex. toutes les 4 vagues) : un ennemi plus costaud que zombie/squelette dans la composition de la vague. | Moyen | Idée | Le Spawner (`SpawnableEnemy`, `SpawnEntry`) sait déjà composer une vague par type/nombre ; le nouveau est surtout le monstre lui-même (stats, éventuellement un comportement dédié) et le déclenchement périodique. Proposée par Claude (2026-08-27). |
| Vagues / Spawner | Compte à rebours visible avant le début du Combat (ex. "3, 2, 1") au lieu d'un démarrage instantané dès que tout le monde a voté prêt — laisse le temps de finir de placer une tour. | Facile | Idée | Nouvel état transitoire entre le vote et `PhaseTransitions.enterCombat` (ou juste un délai avant l'appel), plus un overlay HUD pour l'afficher. Proposée par Claude (2026-08-27). |
| Score / Progression | Bonus de score à la fin d'une vague nettoyée (en plus du score déjà donné par chaque kill). | Facile | Idée | Point d'accroche évident : là où `WAVE_ENEMIES_KILLED >= WAVE_ENEMIES_TOTAL` déclenche déjà `PhaseTransitions.enterBuild` (`ModEvents.onMonsterDeath`). Valeur pas encore décidée. Voir [02-gameplay.md](02-gameplay.md#feuille-de-route-du-score-décidée-avec-le-joueur-pas-codée-pour-linstant). |
| Score / Progression | Bonus de score à la fin de la map (victoire). | Facile | Idée | Point d'accroche : `PhaseTransitions.onVictory`. Valeur pas encore décidée. |
| Score / Progression | Multiplicateur de score par vague : aucun dégât pris par un joueur **et** aucun dégât pris par le Cristal d'Eternia (deux conditions distinctes, pas fusionnées, pour pouvoir les valoriser indépendamment plus tard). | Facile | Prête | Deux booléens `Level` (`WAVE_PLAYER_DAMAGED`/`WAVE_CRYSTAL_DAMAGED`), remis à `false` au début de chaque combat (`PhaseTransitions.enterCombat`), mis à `true` par un nouveau `LivingDamageEvent` filtré sur `Player` (`ModEvents`) et directement dans `EterniaCrystalBlockEntity.damage(int)` (déjà le point de passage unique des dégâts au cristal, pas besoin d'event). N'importe quelle source de dégât compte (pas seulement un ennemi) — à confirmer avec le joueur. Mécanisme discuté et validé avec le joueur (2026-08-27), valeur du multiplicateur pas encore décidée. |
| Score / Progression | Multiplicateur de score par vague : aucune tour détruite par les ennemis. | Moyen | Idée | Séparée de la ligne ci-dessus : plus délicat, il faut distinguer une tour détruite en combat (PV à 0) d'une tour retirée volontairement par le joueur (`TowerRemovalClientEvents`) — la seconde ne doit pas casser le multiplicateur. Pas encore discuté avec le joueur. |
| Score / Progression | Multiplicateur de score selon la difficulté choisie (`ModAttachments.DIFFICULTY`). | Facile | Idée | L'attachment existe déjà ; il ne manque qu'une table de multiplicateurs et le point où l'appliquer (probablement au moment de créditer le score, `ModEvents.awardExperienceAndScore`). |
| Score / Progression | Multiplicateur de score selon la difficulté de la map, un paramètre à définir par le créateur de chaque map. | Difficile | Idée | Nécessite d'ajouter un champ à `init/GameMap.java` (aucun aujourd'hui) — lié au futur système de maps/structures, pas indépendant. |
| Maps / Monde | Zones **interdites** à la pose de tours, marquées par le créateur de map (liste noire, pas liste blanche). | Moyen | Prête | Décidé avec le joueur (2026-08-31) : liste noire choisie exprès plutôt que « zones autorisées » façon jeu de référence — plus ouvert à la créativité du joueur, et un oubli du mappeur autorise une pose en trop au lieu de rendre un endroit injouable. Probablement un bloc marqueur invisible/traversable sur le patron de `PlayerSpawnBlock`, plus une vérification dans `ModNetworking.handlePlaceTower` (autorité serveur) et un retour visuel côté `TowerPlacementClientEvents` (hologramme rouge). Reste à décider : marqueur par bloc, ou marqueur de coin définissant un volume. |
| Maps / Monde | Une phase (ou un état) "taverne" distincte de Construction, pour pouvoir construire dans la taverne sans que la roue des tours s'y ouvre. | Moyen | Idée | `GAME_PHASE` vaut `BUILD` par défaut et c'est un état de la `Level` entière : rien ne distingue aujourd'hui la taverne d'une map, donc la roue des tours s'ouvre dans le hub et la pose y est acceptée. Évoqué par le joueur (2026-08-31) en même temps que la construction de la taverne, forme pas encore tranchée (nouvelle valeur de `GamePhase` ? drapeau "partie en cours" séparé ?). |
| Maps / Monde | Entités décoratives dans les structures (cadres, supports à armure, tableaux). | Moyen | Idée | Aujourd'hui `setIgnoreEntities(true)` dans `TavernSpawn` : la structure étant reposée à chaque chargement du monde et `clearZone` ne remettant que des blocs, poser les entités les dupliquerait à chaque redémarrage. Lever la limite demande de nettoyer aussi les entités de la zone avant de poser — faisable, mais à valider (les requêtes d'entités au moment de `LevelEvent.Load` portent sur des chunks pas forcément chargés). |
| Score / Progression | Popup de gain d'XP, symétrique au popup de score (`ScoreGainOverlay`) mais près de la barre d'XP en bas à gauche plutôt qu'en bas à droite. | Facile | Idée | Même événement source que le popup de score (`ModEvents.grantExperience`, appelé juste après `grantScore` dans `awardExperienceAndScore`) — surtout un nouveau paquet clientbound + un nouvel overlay sur le même patron que `ScoreGainPayload`/`ScoreGainOverlay`. Proposée par Claude (2026-08-27). |
| Score / Progression | Retour visuel au passage de niveau (au-delà du message système actuel) — un flourish HUD plutôt qu'un texte de chat qui disparaît. | Moyen | Idée | Cohérence avec la direction prise sur `GameOverScreen` (retrait des messages de chat redondants avec le HUD, voir `PR#21`) : un "level up" qui ne vit que dans le chat détonne un peu. Pas de mécanisme précis proposé, juste le constat. Proposée par Claude (2026-08-27). |

## Référence : roadmap de l'ancien prototype

L'ancien `TODOLIST.xlsx` (prototype précédent, pas ce dépôt) reste une référence
utile pour le contenu à venir, notamment :

- **Tours par classe** (Écuyer : Spike Blockade ✅, Bouncer Blockade, Harpoon
  Turret ✅, Slice N Dice Blockade, Bowling Ball Turret, Mortar Turret ;
  Apprenti, Chasseresse, Moine : rosters complets non listés ici, voir mémoire
  `project-roadmap-excel`).
- **Maps de la campagne DD** (The Deeper Well, Foundries and Forges, Magus
  Quarters, etc.) pour du contenu de map futur.
- **Algorithme de spawner** (compteur cumulatif par type d'ennemi, déclenche un
  spawn au-delà de 20) — toujours pertinent pour le Spawner (voir
  `05-etat-et-problemes-connus.md`).

Ce n'est qu'une base d'inspiration : toute idée qui en vient doit quand même
passer par une issue `Idea` / le Backlog ci-dessus pour être suivie dans ce
dépôt.
