# Demande à l'Atelier : blocs orientables et blocs à états (taverne)

À transmettre à la session de l'Atelier (`~/MC_Atelier_Neoforge`). Dungeon Defenders ne modifie pas l'Atelier : ce
fichier est une **proposition**. C'est l'Atelier qui fixe le format, et Dungeon Defenders lit ce qu'il décide (même règle
que pour les monstres : un bloc = des fichiers, pas de code par bloc).

## Pourquoi

Le 2026-11-27, 8 demandes `dungeon_defenders-taverne_*` ont été déposées chez le concepteur de modèles
(`~/homelab/modeles/a-faire/`). Elles sont livrées dans `modeles/blocs/taverne_*/` de l'Atelier : environ 41 blocs pour
construire une taverne entièrement en blocs custom.

Aujourd'hui, une fiche de bloc sait faire deux choses : un cube, ou un modèle fixe (`modele: true`). Il en manque deux
pour la taverne :

- **Les blocs orientés.** Fenêtres, torche murale, banc, comptoir, étagère, console et foyer ont un devant : posés, ils
  doivent se tourner vers le joueur. La poutre doit pouvoir se coucher comme une bûche.
- **Les blocs à états.** Une porte s'ouvre, un escalier fait des angles, une barrière se raccorde à ses voisines. C'est
  **du code**, pas du modèle : le concepteur ne livre qu'un modèle statique par état.

## Qui fait quoi

| Qui | Quoi |
|---|---|
| Concepteur de modèles | Un modèle statique par état, avec les mêmes emprises que les modèles « template » vanilla (déjà demandé). |
| **Atelier** | Le format de fiche ; ses propres blocs (pour les voir dans l'Établi et en jeu) ; le build : états de bloc, modèles, butin et tags ; les vérifications ; le déploiement vers Dungeon Defenders. |
| **Dungeon Defenders** (cette session) | Lire le nouveau champ et enregistrer la bonne classe de bloc. Pour un bloc orienté, tourner sa forme. |

## Format proposé

Deux champs facultatifs dans `data/atelier/blocs/<id>.json`. Les fiches actuelles restent valides.

- **`oriente: true`**, pour un cube ou un bloc `modele`. Le bloc se tourne à l'horizontale au placement, devant vers le
  joueur (propriété `facing`). Le modèle est dessiné devant au nord. Les états de bloc tournent le modèle (`y`: 0, 90,
  180, 270) et la forme `.forme.json` est tournée de la même façon.
- **`axe: true`**, pour un cube. Le bloc s'oriente comme une bûche (propriété `axis`), avec les textures `<id>.png` pour
  les côtés et `<id>_top.png` pour les bouts.
- **`sorte`**, qui ne se combine pas avec `modele` ni `oriente`. Chaque sorte reprend la classe vanilla, sa forme et ses
  états de bloc :

| `sorte` | Classe | Modèles attendus dans `modeles/blocs/<id>/` | Tags à générer |
|---|---|---|---|
| `porte` | `DoorBlock` | `<id>_bas_gauche`, `_haut_gauche`, `_bas_droite`, `_haut_droite` | `minecraft:doors`, `wooden_doors` selon le son |
| `trappe` | `TrapDoorBlock` | `<id>`, `<id>_haut`, `<id>_ouverte` | `minecraft:trapdoors`, `wooden_trapdoors` |
| `escalier` | `StairBlock` | `<id>`, `<id>_interieur`, `<id>_exterieur` | `minecraft:stairs`, `wooden_stairs` |
| `dalle` | `SlabBlock` | `<id>`, `<id>_haut` ; dalle double : un champ `bloc_plein` (id d'un cube de l'Atelier) | `minecraft:slabs`, `wooden_slabs` |
| `barriere` | `FenceBlock` | `<id>_poteau`, `<id>_cote` (multipart) | `minecraft:fences`, `wooden_fences` (c'est ce tag qui fait raccorder les barrières entre elles) |
| `portillon` | `FenceGateBlock` | `<id>`, `<id>_ouvert` | `minecraft:fence_gates` |

Pour toutes les sortes : l'objet en main utilise le modèle de l'état de base. La porte et la dalle double ont un butin
vanilla : une porte ne donne qu'un objet (moitié basse), une dalle double en donne deux. Les sortes `porte`, `trappe` et
`portillon` prennent un `BlockSetType` ou un `WoodType` choisi d'après `son` (`bois` pour du bois, `metal` pour du fer,
qui ne s'ouvre alors qu'à la redstone, comme en vanilla).

## Ce que Dungeon Defenders recevra au déploiement

Les fichiers que l'Atelier écrit déjà pour un bloc, adaptés à la sorte :

- la fiche ;
- `assets/dungeon_defenders/blockstates/<id>.json`, avec les variantes ou le multipart vanilla ;
- les modèles `models/block/<id>*.json` et leurs textures ;
- la définition d'objet ;
- le butin ;
- les tags `minecraft:` ci-dessus, **ajoutés** sans écraser les fichiers existants ;
- la forme, pour un bloc `modele` orienté.

## Ensuite

Une fois le format décidé dans l'Atelier, mettre à jour l'« Historique » de `docs/PROMPT_DUNGEON_DEFENDERS.md` pour le
signaler. Dungeon Defenders fera alors le code de son côté (lecture des champs, classes génériques, rotation des formes),
avec un gametest par sorte.
