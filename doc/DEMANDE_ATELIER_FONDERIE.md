# Demande à l'Atelier : construire la deuxième map, la fonderie, en deux versions

> **À transmettre** (2026-11-28) à la session de l'Atelier (`~/MC_Atelier_Neoforge`). Dungeon Defenders ne modifie pas
> l'Atelier : ce fichier est une **demande**, l'Atelier décide de la façon de la ranger chez lui (comme `maps/puits_a/`
> et `maps/puits_b/`). C'est la même démarche que [DEMANDE_ATELIER_PUITS.md](DEMANDE_ATELIER_PUITS.md) ; tout ce qui
> n'est pas redit ici (format des blocs de jeu, livraison, contraintes) reste valable tel quel.

## Ce qui est demandé

Construire la **deuxième map de la campagne**, une fonderie souterraine (hauts fourneaux, coulées de métal, forges),
**en deux exemplaires dont la disposition est complètement différente**. On les essaiera tous les deux en jeu, puis
Corentin choisira, ou gardera les deux. Les deux sont faites **avec les blocs `fonderie_*`** (commit de l'Atelier
`373e93e`, 68 fiches), puis, pour chacune :

1. le plan écrit en code, comme `maps/puits_a/`, et la structure sauvegardée en `.nbt` ;
2. des captures d'écran (vue d'ensemble prise de haut, plafond retiré, et les lieux principaux) ;
3. une fiche (taille, blocs de réservation, blocs vanilla provisoires, blocs qui ont manqué) ;
4. un **déploiement** vers Dungeon Defenders, et le chemin des fichiers donné à Corentin.

**D'abord, les blocs.** Les 68 blocs `fonderie_*` ne sont pas encore déployés vers Dungeon Defenders (le dernier
déploiement de blocs est `d769cc0`, @09a3823). Il faut les déployer avant les maps, dans un commit à part ou avec
elles, pour que les `.nbt` ne contiennent que des blocs connus. Ils apparaîtront dans l'onglet créatif « Blocs :
Fonderie » de Dungeon Defenders, déjà prêt.

## Les deux versions

Elles ont la même ambiance et les mêmes blocs, mais elles ne doivent **pas** se ressembler par le plan : pas la même
version tournée ou en miroir, mais deux façons différentes de défendre le cristal.

### Version A, inspirée de la map d'origine

On peut s'inspirer de l'**esprit** de « Foundries and Forges », la deuxième map de Dungeon Defenders. D'après le wiki
([dungeondefenders.wiki.gg](https://dungeondefenders.wiki.gg/wiki/Foundries_and_Forges)), elle se joue ainsi :

- **un seul cristal**, en hauteur : on y monte par **trois niveaux d'escaliers**, et il se trouve au croisement de
  **trois petits couloirs** proches les uns des autres, là où les défenseurs construisent ;
- **trois paires d'entrées** de monstres ;
- des **bassins de lave** bordés de pics, longés par des garde-corps où l'on pose aussi des défenses ;
- de courtes rampes vers le cristal, légèrement inclinées ;
- **5 vagues** ; la dernière amène un gros monstre (l'ogre) par les **portes du milieu**, annoncé par un cor.

La recherche sur internet est bienvenue pour l'ambiance et pour comprendre comment la map se joue (distances,
hauteurs). **Le plan reste original** : on ne reproduit pas son plan, ses proportions ni ses compositions
reconnaissables. C'est la même règle que pour la taverne, le puits, les monstres et les blocs. Ce qui est à garder,
c'est l'idée de jeu : un cristal en hauteur qu'on rejoint par paliers, plusieurs entrées dont les chemins convergent
vers un nœud central à défendre, et une coulée de métal en fusion qui découpe l'espace.

### Version B, libre

Une autre façon d'habiter le même lieu. Quelques pistes, pour sortir du modèle A, à choisir ou à remplacer :

- **la halle de coulée** : une longue nef, le cristal au fond près du haut fourneau, les monstres qui arrivent par
  les deux côtés et traversent des chenaux de métal sur des passerelles ;
- **le cristal en contrebas** : les monstres descendent les étages d'un grand fourneau en colimaçon, au lieu de
  monter ;
- **deux ateliers reliés** : deux salles de forge séparées par un mur percé de herses levées, les monstres de la
  première passent par la seconde ;
- **le carreau de la mine** : des voies de wagonnets en longueur, des tas de charbon et de scories qui forment des
  couloirs.

## Les blocs

**Décor : uniquement les `atelier:fonderie_*`.** Un bloc vanilla provisoire est permis si un manque se révèle, mais il
est **signalé dans la fiche**, pour qu'on le demande au concepteur de modèles. Les `puits_*` et `taverne_*` peuvent
servir en appoint (roche, poutres, tonneaux), si ça reste cohérent avec une fonderie.

Les 68 blocs disponibles, dans l'Atelier (`modeles/blocs/fonderie_*`) :

| Famille | Blocs |
|---|---|
| Sols | `fonderie_dalle`, `fonderie_dalle_fonte`, `fonderie_pave_brique`, `fonderie_plaque_fer`, `fonderie_terre_suie`, `fonderie_roche`, `fonderie_scories` |
| Murs | `fonderie_mur_brique`, `fonderie_mur_brique_suie`, `fonderie_mur_pierre`, `fonderie_frise`, `fonderie_suie_mur` |
| Architecture | `fonderie_arche_cote`, `fonderie_arche_cle`, `fonderie_colonne_fonte`, `fonderie_pilier`, `fonderie_pilier_base`, `fonderie_pilier_chapiteau`, `fonderie_console` |
| Machinerie | `fonderie_fourneau_bas`, `fonderie_fourneau_cuve`, `fonderie_fourneau_gueulard`, `fonderie_creuset`, `fonderie_creuset_verse`, `fonderie_chenal`, `fonderie_chenal_angle`, `fonderie_chenal_chute`, `fonderie_chenal_fin`, `fonderie_bain`, `fonderie_conduit`, `fonderie_conduit_coude`, `fonderie_soufflet`, `fonderie_engrenage`, `fonderie_palan`, `fonderie_chaine`, `fonderie_enclume`, `fonderie_moules` |
| Estrade du cristal | `fonderie_estrade`, `fonderie_estrade_bord`, `fonderie_estrade_angle` |
| Portail des monstres | `fonderie_portail_montant`, `fonderie_portail_linteau`, `fonderie_herse`, `fonderie_herse_chaine` |
| Menuiserie | `fonderie_porte`, `fonderie_portillon`, `fonderie_trappe`, `fonderie_escalier`, `fonderie_garde_corps` |
| Charpente et passages | `fonderie_poteau`, `fonderie_poutre`, `fonderie_passerelle`, `fonderie_passerelle_rambarde` |
| Lumière | `fonderie_lanterne_cage`, `fonderie_lanterne_suspendue`, `fonderie_lustre`, `fonderie_brasero`, `fonderie_braises` |
| Décor | `fonderie_banniere`, `fonderie_banniere_bas`, `fonderie_ratelier_armes`, `fonderie_ratelier_outils`, `fonderie_lingots`, `fonderie_caisse_minerai`, `fonderie_tas_charbon`, `fonderie_seaux_scories`, `fonderie_baquet`, `fonderie_wagonnet` |

(Le classement est indicatif : c'est celui des demandes au concepteur, `~/homelab/modeles/fait/dungeon_defenders-fonderie_*.md`.)

**Le métal en fusion n'est pas de la lave.** La lave vanilla brûle les monstres, les joueurs et les objets, et
s'écoule : pas de lave ni de feu vanilla dans la map. Les coulées sont faites avec `fonderie_chenal*`, `fonderie_bain`
et `fonderie_braises` (des blocs de décor). Un bain ou un chenal où l'on ne doit pas marcher est entouré de
`fonderie_garde_corps`, ou en contrebas, sans chute vers le vide.

### Les blocs de jeu de Dungeon Defenders

Exactement comme pour le puits ([DEMANDE_ATELIER_PUITS.md](DEMANDE_ATELIER_PUITS.md#les-blocs-de-jeu-de-dungeon-defenders)) :
mêmes blocs de réservation, mêmes réglages de block entity, même façon de les écrire dans la fiche (celle de
`maps/puits_a/` convient). Les ennemis disponibles n'ont pas changé : `zombie`, `skeleton`, `runner`, `brute`,
`marksman`.

**Réglages proposés**, à ajuster :

| | Version A | Version B |
|---|---|---|
| `MapName` | Nom choisi par l'Atelier | Nom choisi par l'Atelier |
| `MapOrder` | 3 | 4 |
| `WaveCount` | 5 | 5 |
| `ScoreMultiplier` | 1.2 | 1.2 |

(`MapOrder` 1 et 2 sont pris par `puits_a` et `puits_b`.)

**Les vagues**, un cran au-dessus du puits :

- dès la vague 1, des zombies et quelques squelettes ;
- des runners en nombre à partir de la vague 2 (ils tiennent la place des petits monstres explosifs de la map
  d'origine, qui n'existent pas encore ici) ;
- des marksmans à partir de la vague 3, sur une entrée au moins ;
- en **vague 5**, une brute par l'**entrée du milieu** (ou la principale) en version A, en plus des autres ; une ou deux
  brutes au plus par spawner dans la version B.

Chaque entrée a sa propre composition (une corps à corps, une à distance, une mixte…).

## Contraintes du jeu

Les mêmes que pour le puits ([DEMANDE_ATELIER_PUITS.md](DEMANDE_ATELIER_PUITS.md#contraintes-du-jeu)) : taille sous
64 × 40 × 64 si possible, map fermée, chemins d'au moins 3 de large, marches d'un bloc ou escaliers, pas de porte
fermée, `no_build_zone` devant les spawners et sur les escaliers et passerelles, 25 à 45 blocs de marche de chaque
spawner au cristal, une lumière lisible, pas de redstone. En plus :

- **Entrées** : 3 à 4 spawners (l'idée de « paires » de la map d'origine peut se rendre par deux spawners côte à côte
  derrière un même portail, ou être abandonnée).
- **Herses** : `fonderie_herse` encadre une entrée **levée** ; une herse baissée en travers d'un chemin le bloque.
- **Niveaux** : si le cristal est en hauteur (version A), chaque montée est un escalier `fonderie_escalier` ou une
  pente de marches d'un bloc, d'au moins 3 de large. Ce sont les endroits où les chemins se rejoignent : laisser
  autour du sol dégagé pour les tours.
- **Pathfinding** : les blocs modèles (chenaux, fourneaux, wagonnets, tas…) sont des obstacles pour les monstres,
  comme les blocs pleins (correction faite au puits). Un chemin bordé de décor reste donc praticable, mais il faut les
  3 blocs de large **libres**, décor compris.

## Livraison

Dans l'**Atelier**, rangés où il le décide (proposition : `maps/fonderie_a/` et `maps/fonderie_b/`) : le script du
plan, le `.nbt`, au moins six captures en 1920 × 1080 par version, la fiche.

Vers **Dungeon Defenders**, sur `atelier/modeles` :

1. le déploiement des 68 blocs `fonderie_*` (s'il n'est pas déjà fait) ;
2. pour chaque version, comme pour le puits :
   - `data/dungeon_defenders/structure/map/<id>.nbt` (blocs `atelier:` renommés, réservations remplacées en gardant
     leurs propriétés, block entities écrits) ;
   - `assets/dungeon_defenders/textures/gui/maps/<id>.png` (aperçu 128 × 72) ;
   - `data/dungeon_defenders/atelier_maps/<id>.json`.

Ids proposés : `fonderie_a` et `fonderie_b`. Elles apparaîtront dans l'écran du cristal de la taverne, après les deux
maps du puits.

## Ensuite

Côté Dungeon Defenders :

1. lire chaque `.nbt` : taille, un seul cristal, une seule arrivée, des spawners réglés, plus aucun bloc `atelier:` ;
2. ajouter les gametests `map_paths_fonderie_a` et `map_paths_fonderie_b` (chaque monstre de chaque spawner arrive au
   cristal) ;
3. fusionner dans `feature/atelier` et noter dans [06-a-tester.md](06-a-tester.md) ce que Corentin doit regarder.
