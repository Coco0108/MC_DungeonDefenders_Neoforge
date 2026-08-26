# Dossier d'échange pour les modèles 3D / textures

Ce dossier n'est **pas** un dossier de ressources du mod (rien ici n'est chargé par
Minecraft — ce n'est pas sous `src/main/resources/`). C'est juste un point de dépôt
pour que le joueur pousse ses fichiers Blockbench, que Claude récupère ensuite et
intègre aux bons endroits (`src/main/resources/assets/dungeon_defenders/models/`,
`textures/`, plus le code Java du renderer si le modèle est animé — voir
`doc/04-guide-ajout-contenu.md`).

## Comment déposer un modèle

1. Dans Blockbench, crée un dossier `art/models/<nom_de_la_tour_ou_du_bloc>/`
   (ex. `art/models/harpoon_turret/`).
2. Dedans, mets :
   - le fichier **`.bbmodel`** (le projet Blockbench natif — c'est ce que Claude lit
     pour extraire les pièces/pivots/UV, que le modèle soit animé ou non) ;
   - les textures utilisées, en **PNG**, telles qu'exportées par Blockbench (mêmes
     noms de fichiers que référencés dans le `.bbmodel`, pour que rien ne se
     décroche).
3. `git add`, commit, push sur la branche courante (ou une nouvelle branche si tu
   préfères que ce soit isolé).
4. Dis à Claude "va chercher le modèle de X dans art/models/" (ou juste "j'ai
   ajouté un modèle, regarde art/") — il l'intègre et peut supprimer le dossier une
   fois converti (ou le laisser comme historique, à voir au cas par cas).

## Pourquoi `.bbmodel` et pas directement le JSON de bloc exporté

Pour un modèle **statique** (pas d'animation), Claude peut travailler directement
depuis le JSON de bloc vanilla exporté par Blockbench si tu préfères — ça tombe
quasiment tel quel dans `models/block/`. Mais pour un modèle **animé** (recul au
tir, pièce qui pivote...), il faut un vrai modèle en pièces nommées traduit en Java
(`LayerDefinition`/`ModelPart`) plus un `BlockEntityRenderer` — le `.bbmodel` brut
est la source la plus fiable pour ça (JSON lisible, précis sur chaque pièce/pivot),
plutôt que dépendre de l'export Java généré par Blockbench (souvent à nettoyer).

Dans le doute, dépose toujours le `.bbmodel` (+ textures) : ça marche dans les deux
cas.
