# Demande à l'Atelier : construire la première map, le puits, en deux versions

> **En attente** (2026-11-28). À transmettre à la session de l'Atelier (`~/MC_Atelier_Neoforge`). Dungeon Defenders ne
> modifie pas l'Atelier : ce fichier est une **demande**, l'Atelier décide de la façon de la ranger chez lui (comme
> `maps/taverne/`).

## Ce qui est demandé

Construire la **première map de la campagne**, des souterrains autour d'un grand puits, **en deux exemplaires dont la
disposition est complètement différente**. On les essaiera tous les deux en jeu, puis Corentin choisira, ou gardera
les deux. Les deux sont faites **avec les blocs `puits_*`** livrés le 2026-11-27, puis, pour chacune :

1. le plan écrit en code, comme `maps/taverne/construire.py`, et la structure sauvegardée en `.nbt` ;
2. des captures d'écran (vue d'ensemble prise de haut, toit ou plafond retiré, et les lieux principaux) ;
3. une fiche (taille, blocs de réservation, blocs vanilla provisoires, blocs qui ont manqué) ;
4. un **déploiement** vers Dungeon Defenders, et le chemin des fichiers donné à Corentin.

## Les deux versions

Elles ont la même ambiance et les mêmes blocs, mais elles ne doivent **pas** se ressembler par le plan : pas la même
version tournée ou en miroir, mais deux façons différentes de défendre le cristal.

- **Version A, inspirée de la map d'origine.** On peut s'inspirer de l'**esprit** de la première map de Dungeon
  Defenders : une grande salle souterraine autour d'un puits profond, le cristal sur une estrade, les monstres qui
  arrivent par de grands portails, plusieurs niveaux reliés par des escaliers et des passerelles. La recherche sur
  internet est bienvenue pour l'ambiance et pour comprendre comment la map se joue (nombre d'entrées, distances,
  hauteurs).
  **Le plan reste original** : on ne reproduit pas son plan, ses proportions ni ses compositions reconnaissables. C'est
  la même règle que pour la taverne, les monstres et les blocs.
- **Version B, libre.** Une autre façon d'habiter le même lieu. Quelques pistes, pour sortir du modèle A, à choisir ou
  à remplacer :
  - un puits qu'on longe en spirale, avec le cristal tout au fond ;
  - une galerie de mine en longueur, avec des monstres qui arrivent des deux bouts ;
  - un cristal sur une île au milieu de l'eau du puits, que l'on rejoint par des passerelles ;
  - deux niveaux superposés, où les monstres du haut descendent par des échafaudages.

## Les blocs

**Décor : uniquement les `atelier:puits_*`.** Un bloc vanilla provisoire est permis si un manque se révèle, mais il est
**signalé dans la fiche**, pour qu'on le demande au concepteur de modèles. Les `taverne_*` peuvent servir en appoint
(tonneaux, poutres), si ça reste cohérent avec un souterrain.

Les 57 blocs disponibles :

| Famille | Blocs |
|---|---|
| Sols | `puits_dalle`, `puits_dalle_gravee`, `puits_pave`, `puits_pave_mousse`, `puits_terre`, `puits_roche` |
| Murs | `puits_mur_fondation`, `puits_mur_moellons`, `puits_mur_taille`, `puits_frise` |
| Architecture | `puits_arche_cote`, `puits_arche_cle`, `puits_colonne`, `puits_pilier`, `puits_pilier_base`, `puits_pilier_chapiteau`, `puits_console` |
| Le puits | `puits_margelle`, `puits_margelle_angle`, `puits_eau`, `puits_treuil`, `puits_treuil_montant`, `puits_poulie`, `puits_corde_seau`, `puits_chaine` |
| Estrade du cristal | `puits_estrade`, `puits_estrade_bord`, `puits_estrade_angle` |
| Grand portail des monstres | `puits_portail_montant`, `puits_portail_linteau`, `puits_portail_battant`, `puits_portail_battant_haut` |
| Menuiserie | `puits_porte`, `puits_portillon`, `puits_grille`, `puits_escalier`, `puits_garde_corps` |
| Charpente et passages | `puits_echafaudage`, `puits_etai`, `puits_etai_tete`, `puits_passerelle`, `puits_passerelle_corde` |
| Lumière | `puits_torche_cage`, `puits_brasero`, `puits_lustre`, `puits_chandelles` |
| Décor | `puits_banniere`, `puits_banniere_bas`, `puits_chaines_mur`, `puits_caisses`, `puits_tonneau_brise`, `puits_ossements`, `puits_squelette_assis`, `puits_toile_coin`, `puits_toile_plafond`, `puits_cocon`, `puits_racines` |

### Les blocs de jeu de Dungeon Defenders

Ils n'existent pas dans l'Atelier : comme pour la taverne, l'Atelier **réserve leur place** avec un bloc qui ne sert à
rien d'autre, et le déploiement le remplace. Ces blocs-là ont des **réglages** (le contenu de leur block entity) que le
déploiement doit écrire. Le format exact est celui de `tools/generer-map-sanctuaire.py` de Dungeon Defenders, qui
produit une map jouable ; il est à lire avec
`gh api repos/Coco0108/MC_DungeonDefenders_Neoforge/contents/tools/generer-map-sanctuaire.py?ref=feature/atelier -H "Accept: application/vnd.github.raw"`.

| Bloc | Rôle | Nombre | Réglages (NBT du block entity) |
|---|---|---|---|
| `dungeon_defenders:eternia_crystal` | L'objectif : la partie est perdue s'il est détruit | **Un seul** | `CrystalHealth` : 100 |
| `dungeon_defenders:spawner` | Fait apparaître les monstres, vague par vague | **2 à 4**, un par entrée de monstres | `IntervalTicks` (40), `SpawnRadius` (2), `WaveStart`, `WaveEnd`, `LastCombatSessionHandled` (0), `Entries` : liste de `{Enemy, BaseCount, Spawned: 0, Accumulator: 0, EffectiveTotal: BaseCount}` |
| `dungeon_defenders:player_spawn` | Arrivée des joueurs | **Un seul** | aucun |
| `dungeon_defenders:mana_chest` | Donne du mana une fois par vague | 1 ou 2, près de l'arrivée | état `opened=false` ; `ManaAmount` (50), `LastOpenedWave` (0) |
| `dungeon_defenders:no_build_zone` | Interdit d'y poser des tours (marqueur invisible) | Autant que nécessaire | aucun |
| `dungeon_defenders:map_config` | Nom et réglages de la map | **Un seul**, n'importe où | `MapName`, `MapOrder`, `WaveCount`, `ScoreMultiplier` (float), `FormatVersion` : 1, `TimeOfDay` (facultatif, -1 = heure par défaut) |

Les ennemis disponibles (`Enemy`) :

| Ennemi | Rôle |
|---|---|
| `dungeon_defenders:zombie` | Corps à corps de base |
| `dungeon_defenders:skeleton` | Tir à distance |
| `dungeon_defenders:runner` | Petit et rapide, fragile |
| `dungeon_defenders:brute` | Gros et lent, frappe fort |
| `dungeon_defenders:marksman` | Tireur de loin, portée 14 |

Les blocs de réservation, les réglages de chaque spawner (par position ou par repère) et ceux de `map_config` sont dans
la fiche de la map, comme les `remplacements` de la taverne. C'est l'Atelier qui choisit comment l'écrire.

**Réglages proposés**, à ajuster :

| | Version A | Version B |
|---|---|---|
| `MapName` | Nom choisi par l'Atelier | Nom choisi par l'Atelier |
| `MapOrder` | 1 | 2 |
| `WaveCount` | 5 | 5 |
| `ScoreMultiplier` | 1.0 | 1.0 |

Les vagues sont faciles : c'est la première map. Elles commencent par des zombies, puis des squelettes et des runners
arrivent à partir de la vague 2. Une brute vient en vague 5 et pas avant, au plus une ou deux par spawner. Chaque
entrée a sa propre composition : par exemple, une entrée corps à corps, une entrée à distance et une entrée mixte.

## Contraintes du jeu

- **Taille** : jusqu'à 48 × 48 × 48 si la map est sauvegardée par un bloc de structure. Plus grand est possible si le
  `.nbt` est écrit directement par le script, mais il vaut mieux rester sous **64 × 40 × 64**. Dungeon Defenders pose la
  map centrée sur (10000, 65, 0) et nettoie la zone autour à chaque partie.
- **Fermée** : le monde est vide autour. Il faut des murs, un plafond ou un garde-corps partout où l'on pourrait
  tomber. Le fond du puits peut être un vrai fond (eau sur un sol), mais pas un trou vers le vide.
- **Chemins des monstres** : les monstres marchent (IA vanilla) du spawner jusqu'au cristal. Chaque chemin doit
  être praticable :
  - largeur d'au moins 3 blocs ;
  - marches d'un bloc au plus, ou des escaliers ;
  - pas de trou de plus de 2 blocs de haut à franchir ;
  - pas de porte fermée.
  Les portails (`puits_portail_*`) encadrent les entrées des monstres, battants ouverts.
- **Tours** : les joueurs posent leurs tours sur le sol le long des chemins. Il faut laisser de la place des deux
  côtés de chaque chemin, avec des zones dégagées pour les placer. Mettre des `no_build_zone` sur le chemin lui-même
  juste devant chaque spawner (environ 3 × 3), et sur les escaliers et les passerelles où une tour bloquerait le
  passage.
- **Distances** : de chaque spawner au cristal, compter 25 à 45 blocs de marche, pour que les tours aient le temps de
  tirer.
- **Lumière** : c'est un souterrain, mais il doit être lisible. Utiliser des torches en cage, des braseros et des
  lustres. Pas de zone trop noire : on doit voir les monstres arriver. Aucun monstre n'apparaît tout seul (le monde
  est un biome vide), seuls les spawners en font venir.
- **Pas de redstone** : la map est reposée à neuf à chaque partie.

## Livraison

Dans l'**Atelier**, rangés où il le décide (proposition : `maps/puits_a/` et `maps/puits_b/`), comme pour
`maps/taverne/` :

- le script du plan ;
- le `.nbt` ;
- au moins six captures en 1920 × 1080 par version ;
- la fiche.

Vers **Dungeon Defenders**, un commit de déploiement sur `atelier/modeles`, pour chaque version :

- `data/dungeon_defenders/structure/map/<id>.nbt`, avec les blocs `atelier:` renommés en `dungeon_defenders:`, les
  blocs de réservation remplacés en **gardant leurs propriétés** (`facing`…) et les block entities écrits ;
- `assets/dungeon_defenders/textures/gui/maps/<id>.png`, l'aperçu de la map dans l'écran de choix (128 × 72,
  recadré d'une capture) ;
- sa référence dans `data/dungeon_defenders/atelier_maps/<id>.json`, comme celle de la taverne.

Ids proposés : `puits_a` et `puits_b`. Les deux apparaîtront alors dans l'écran du cristal de la taverne, dans le pack
« dungeon_defenders ».

## Ensuite

Côté Dungeon Defenders :

1. lire chaque `.nbt` : taille, un seul cristal, une seule arrivée, des spawners réglés, plus aucun bloc `atelier:` ;
2. lancer chaque map en jeu de test (le chemin de chaque spawner au cristal existe) ;
3. fusionner dans `feature/atelier` et noter dans [06-a-tester.md](06-a-tester.md) ce que Corentin doit regarder.
