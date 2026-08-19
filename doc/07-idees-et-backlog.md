# Idées & backlog

Ce fichier remplace le rôle du `TODOLIST.xlsx` original (voir `doc/README.md`) : un
endroit où le joueur note ses idées au fil de l'eau (en testant, ou juste dans la
journée), et où Claude les range ensuite par catégorie/difficulté pour pouvoir les
reprendre une par une sur demande ("fais-moi une fonctionnalité de la catégorie
Tours", "prends la plus facile du Mana", etc.).

## Comment ça marche

1. **Toi** : dès qu'une idée te vient, tu l'ajoutes en une ligne dans la
   [Boîte à idées](#boîte-à-idées) ci-dessous, dans n'importe quel format (une phrase,
   un mot-clé, une comparaison avec le vrai Dungeon Defenders...). Pas besoin de
   structurer, pas besoin que ce soit dans une session Claude Code ouverte — juste
   éditer le fichier.
2. **Moi** : à la prochaine session (ou quand tu me le demandes explicitement,
   genre "range mes idées"), je relis la boîte, je pose des questions si une idée
   est ambiguë, puis je déplace chaque idée triée vers le [Backlog](#backlog) en
   lui donnant une catégorie, une difficulté estimée et un statut. La boîte à
   idées repart à vide (ou ne garde que ce qui n'a pas encore pu être trié).
3. **Toi** : pour lancer une fonctionnalité, tu me donnes soit une catégorie
   ("occupe-toi d'un truc dans Tours"), soit une ligne précise du backlog. Je
   choisis (ou tu choisis) une idée avec le statut `Idée`/`Prête`, on la travaille
   comme d'habitude (`EnterPlanMode` si c'est gros), et une fois faite je la
   passe à `Faite` avec un lien vers la doc qui la documente réellement
   (`02-gameplay.md`, `05-etat-et-problemes-connus.md`...).

Le backlog est une aide à la priorisation, pas une source de vérité sur l'état du
code — comme rappelé pour l'ancien Excel, seul `05-etat-et-problemes-connus.md`
fait foi sur ce qui est réellement implémenté.

## Boîte à idées

*(vide pour l'instant — ajoute tes idées ici, une par ligne, je les trie à la
prochaine occasion.)*

-

## Backlog

Catégories utilisées (ouvertes à en ajouter si besoin) : **Tours**, **Mana**,
**IA / Monstres**, **Vagues / Spawner**, **HUD / UI**, **Maps / Monde**,
**Classes / Héros**, **Sorts**, **Armes / Armures**, **Divers**.

Difficulté : **Facile** / **Moyen** / **Difficile** (estimation grossière, pas
un chiffrage précis). Statut : **Idée** (brute, pas encore creusée) / **Prête**
(assez précisée pour être lancée) / **En cours** / **Faite**.

| Catégorie | Idée | Difficulté | Statut | Notes |
|---|---|---|---|---|
| *(aucune idée triée pour l'instant)* | | | | |

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
