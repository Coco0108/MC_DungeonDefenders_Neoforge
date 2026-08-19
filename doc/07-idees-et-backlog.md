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
**Classes / Héros**, **Sorts**, **Armes / Armures**, **Divers**.

Difficulté : **Facile** / **Moyen** / **Difficile** (estimation grossière, pas
un chiffrage précis). Statut : **Idée** (brute, pas encore creusée) / **Prête**
(assez précisée pour être lancée) / **En cours** / **Faite**.

| Catégorie | Idée | Difficulté | Statut | Notes |
|---|---|---|---|---|
| HUD / UI | Voir la range d'une tour déjà posée (pas seulement à la pose) : un clic droit ou une touche paramétrable sur une tour affiche sa range comme lors de la pose ; reste 5s ou se referme sur un second clic (sur elle ou une autre tour). | Moyen | Prête | Réutilise le renderer de range déjà construit pour la pose (`client/TowerPlacementRenderState`) — le nouveau est le déclenchement hors phase de pose (input + timer/toggle). [Issue #13](https://github.com/Coco0108/MC_DungeonDefenders_Neoforge/issues/13) |

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
passer par la Boîte à idées / le Backlog ci-dessus pour être suivie dans ce
dépôt.
