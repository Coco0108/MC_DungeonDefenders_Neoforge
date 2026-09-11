# Dossier d'échange pour les structures de map/taverne

Ce dossier n'est **pas** un dossier de ressources du mod (rien ici n'est chargé par
Minecraft — ce n'est pas sous `src/main/resources/`). C'est un point de dépôt, même
principe que `art/` pour les modèles 3D : tu pousses ton `.nbt` ici, Claude le récupère,
le place au bon endroit dans `src/main/resources/data/dungeon_defenders/structure/`,
vérifie que ça compile et se charge, et documente.

**Ce dossier ne touche jamais le serveur dédié directement.** Déposer un fichier ici ne
fait rien de plus que le pousser dans ce dépôt — aucune copie automatique vers le
conteneur du serveur dédié, aucun redémarrage. Ça reste une étape volontairement à part,
que tu déclenches toi-même quand tu le décides (bloc de structure sauvegardé directement
sur le monde du serveur si tu veux tester en vrai, ou demande explicite à Claude si tu
veux qu'il le fasse un jour).

## Comment déposer une structure

1. En jeu, sauvegarde ta construction avec un bloc de structure (mode `SAVE`), sous le
   nom exact attendu par le mod :
   - `dungeon_defenders:tavern` pour la taverne ;
   - `dungeon_defenders:map/<id>` pour une map (ex. `dungeon_defenders:map/ma_map`).
2. Retrouve le fichier `.nbt` généré dans la sauvegarde de ton monde :
   `<dossier_du_monde>/generated/dungeon_defenders/structures/tavern.nbt` (ou
   `.../structures/map/<id>.nbt`).
3. Copie-le ici, dans `map-handoff/`, sous n'importe quel nom clair (`tavern.nbt`,
   `ma_map.nbt`...) — Claude le renomme/déplace au bon endroit à l'intégration.
4. `git add`, commit, push.
5. Dis à Claude "j'ai déposé la taverne/une map dans map-handoff/" — il l'intègre.

## Pourquoi ce détour plutôt qu'écrire directement dans `src/main/resources/`

Rien n'empêche de pousser directement au bon chemin final si tu préfères — ce dossier
existe juste pour que le dépôt soit sans ambiguïté (Claude sait que tout ce qui est ici
attend d'être intégré, pas encore vérifié) et pour laisser une trace de ce qui a été
déposé récemment sans fouiller l'historique Git.
