# Demande à l'Atelier : construire la taverne en jeu

> **En attente** (2026-11-27). À transmettre à la session de l'Atelier (`~/MC_Atelier_Neoforge`). Dungeon Defenders ne
> modifie pas l'Atelier : ce fichier est une **demande**, et c'est l'Atelier qui décide de la façon de la ranger chez lui.

## Ce qui est demandé

Construire en jeu, dans le monde de l'Atelier, la **taverne** de Dungeon Defenders (le hub d'où les joueurs partent en
map), **uniquement avec les blocs `taverne_*`** livrés le 2026-11-27, puis :

1. la sauvegarder avec un bloc de structure vanilla (un fichier `.nbt`) ;
2. faire des **captures d'écran** ;
3. ranger le tout dans une section **maps** de l'Atelier (à créer, par exemple `maps/taverne/` : le `.nbt`, les
   captures, et une courte fiche qui liste les blocs utilisés) ;
4. **déployer** la structure vers Dungeon Defenders (voir plus bas) et donner à Corentin le chemin du fichier.

## L'ambiance, pas une copie

Le but est une taverne **fidèle à l'esprit** d'une taverne-hub de jeu de défense de tours en vue cartoon : chaleureuse,
bois épais, pierre, lumière de feu, un lieu où l'on prépare la prochaine bataille. **Le plan est original.** On ne
reproduit ni le plan, ni les proportions, ni les compositions reconnaissables de la taverne du jeu d'origine : même règle
que pour les monstres et les blocs (dessin original, ambiance seulement).

Faire des recherches sur internet est bienvenu, pour comprendre :

- **ce qu'on fait dans un hub** de ce genre de jeu : choisir sa map, s'équiper à la forge, tester ses tours sur un
  mannequin, retrouver les autres joueurs ;
- **ce qui rend une taverne de fantasy chaleureuse** : architecture à colombages, charpente apparente, mezzanine,
  comptoir et tonneaux, cheminée, éclairage au feu, tables de banquet ;
- **les techniques de construction Minecraft** pour l'intérieur : profondeur des murs, décrochés, poutres qui
  débordent, éclairage par zones.

## Les blocs

**Tous les blocs de décor sont les `atelier:taverne_*`.** Pas de bloc vanilla visible, sauf les blocs techniques
ci-dessous et, si un manque se révèle, un bloc vanilla provisoire **signalé dans la fiche** de la map, pour qu'on demande
le bloc custom au concepteur de modèles.

Les 39 blocs disponibles :

| Famille | Blocs |
|---|---|
| Sols | `taverne_plancher`, `taverne_plancher_sombre`, `taverne_dalles`, `taverne_tapis` |
| Murs | `taverne_mur_pierre`, `taverne_mur_enduit`, `taverne_mur_colombage`, `taverne_mur_colombage_croix`, `taverne_soubassement` |
| Charpente | `taverne_poutre` (axe), `taverne_poteau`, `taverne_poteau_base`, `taverne_console`, `taverne_rambarde`, `taverne_rambarde_poteau` |
| Ouvertures | `taverne_porte`, `taverne_trappe`, `taverne_fenetre`, `taverne_fenetre_haut`, `taverne_volets`, `taverne_barriere`, `taverne_portillon` |
| Menuiserie | `taverne_escalier`, `taverne_dalle` |
| Mobilier | `taverne_comptoir`, `taverne_table_longue`, `taverne_table_ronde`, `taverne_banc`, `taverne_tabouret`, `taverne_etagere`, `taverne_tonneau`, `taverne_tonneau_couche` |
| Feu et lumière | `taverne_foyer`, `taverne_torche_murale`, `taverne_lanterne_posee`, `taverne_lanterne_suspendue`, `taverne_lustre`, `taverne_bougies` |
| Décor | `taverne_chopes` |

### Les blocs propres à Dungeon Defenders

Quatre blocs du jeu n'existent pas dans l'Atelier. Il faut **réserver leur place** avec un bloc vanilla qui ne sert à
rien d'autre dans la construction, et qui sera remplacé au déploiement :

| Bloc de Dungeon Defenders | Rôle | Obligatoire | Bloc de réservation proposé |
|---|---|---|---|
| `dungeon_defenders:tavern_crystal` | Ouvre l'écran de choix de map | **Oui, un seul** | `minecraft:beacon` |
| `dungeon_defenders:player_spawn` | Point d'arrivée des joueurs | Conseillé, **un seul** | `minecraft:lodestone` |
| `dungeon_defenders:forge` | Équipement et inventaire | Conseillé | `minecraft:smithing_table` |
| `dungeon_defenders:training_dummy` | Mannequin pour tester les tours | Facultatif | `minecraft:target` |

Les blocs de réservation sont une proposition : l'Atelier peut en choisir d'autres, s'il les note dans la fiche de la
map.

## Contraintes du jeu

- **Taille** : au plus **48 × 48 × 48** (la limite d'un bloc de structure). Dungeon Defenders ne charge qu'une
  structure pour la taverne.
- **Fermée** : le monde de la taverne est **vide** autour. Il faut des murs et un toit (ou un garde-corps partout), sinon
  on tombe dans le vide.
- **Placement** : la structure est centrée horizontalement sur (0, 65, 0), sa couche la plus basse à Y = 64. Le sol de
  la salle est donc la première couche.
- **Pas de redstone ni de mécanisme** à l'état : la taverne est reposée à neuf à chaque chargement du monde.
- **Zones à prévoir** :
  - une entrée avec porte ;
  - la salle commune : tables, bancs, comptoir, tonneaux, cheminée ;
  - un coin pour le cristal de choix de map, bien en vue (estrade, lumière) ;
  - un coin forge ;
  - un espace dégagé d'environ 7 × 7 pour le mannequin et quelques tours de test ;
  - si la hauteur le permet, une mezzanine avec rambarde et escalier.
- **Entités décoratives** permises (cadres, supports d'armure) : la structure les garde.

## Livraison

Dans l'**Atelier**, rangés où il le décide (proposition : `maps/taverne/`) :

- `taverne.nbt`, la structure avec les blocs `atelier:` et les blocs de réservation ;
- `captures/`, au moins six vues en 1920 × 1080 :
  - la vue d'ensemble, prise en hauteur ou en coupe (toit retiré pour la capture) ;
  - l'entrée ;
  - le comptoir ;
  - la cheminée ;
  - le coin du cristal ;
  - le coin forge et mannequin ;
  - et, si possible, une vue de nuit pour l'éclairage ;
- une fiche courte : taille, blocs de réservation utilisés, blocs vanilla provisoires éventuels, blocs qui ont manqué.

Vers **Dungeon Defenders**, un commit de déploiement sur la branche `atelier/modeles`, comme pour les blocs :

- le fichier `data/dungeon_defenders/structure/tavern.nbt`, avec :
  - les blocs `atelier:taverne_*` renommés en `dungeon_defenders:taverne_*` ;
  - les blocs de réservation remplacés par les blocs de Dungeon Defenders du tableau.

Ce fichier remplace le premier jet construit par Corentin (2026-09-11), qui reste dans l'historique git. Si l'Atelier
préfère ne pas faire le remplacement des blocs, il peut déployer le `.nbt` tel quel en le disant : Dungeon Defenders
s'en chargera.

## Ensuite

Côté Dungeon Defenders :

1. vérifier le fichier : taille, palette, aucun bloc `atelier:` restant, un seul cristal ;
2. l'ajouter à `feature/atelier` pour que Corentin le teste ;
3. noter dans [06-a-tester.md](06-a-tester.md) ce qu'il faut regarder en jeu.
