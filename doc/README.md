# Documentation — Dungeon Defenders (NeoForge)

Mod Minecraft inspiré de *Dungeon Defenders* : le joueur place un **Cristal d'Eternia**
que les monstres tentent de détruire, et peut se défendre avec des pièges comme le
**Piège à Pics**. Si le cristal tombe à 0 PV, c'est un *Game Over*.

| Info | Valeur |
|---|---|
| `mod_id` | `dungeon_defenders` |
| Nom affiché | Dungeon Defenders |
| Version du mod | `0.0.1` |
| Minecraft | `26.1.2` |
| NeoForge | `26.1.2.76` |
| Java | 25 (toolchain Gradle) |
| Package racine | `com.github.c0c0tier.dungeon_defenders` |
| Auteur | C0C0TIER |
| Licence | All Rights Reserved |

## Sommaire

1. [Architecture du projet](01-architecture.md) — arborescence, rôle de chaque classe, cycle d'enregistrement.
2. [Gameplay & mécaniques](02-gameplay.md) — le Cristal d'Eternia, les PV, l'IA des zombies, le rendu de la barre de vie, et le HUD custom (mana, vie, expérience, vague, phase, score, personnage, compétences).
3. [Build & lancement](03-build-et-lancement.md) — commandes Gradle, configurations de run, CI GitHub Actions.
4. [Guide : ajouter du contenu](04-guide-ajout-contenu.md) — recettes pour ajouter un bloc, un block entity, une traduction.
5. [État du projet & problèmes connus](05-etat-et-problemes-connus.md) — ce qui reste du template, ce qui est cassé, pistes.
6. [À tester en jeu](06-a-tester.md) — fonctionnalités écrites mais jamais lancées en jeu, checklist à cocher.
7. [Idées & backlog](07-idees-et-backlog.md) — boîte à idées en vrac + backlog trié par catégorie/difficulté, pour lancer une fonctionnalité à la demande.

**Atelier** : [ATELIER.md](ATELIER.md) — ce que le mod lit des monstres, objets et blocs
déployés depuis l'Atelier (le mod bac à sable des modèles Blockbench), où, et ce qu'il ignore.

**Demandes à l'Atelier** :

- [DEMANDE_ATELIER_BLOCS.md](DEMANDE_ATELIER_BLOCS.md) — blocs orientés et blocs à états (porte, escalier, barrière…) pour la taverne (traitée)
- [DEMANDE_ATELIER_BLOCS_ANIMES.md](DEMANDE_ATELIER_BLOCS_ANIMES.md) — format des blocs animés (forge, mannequin, cristal) (traitée)
- [DEMANDE_ATELIER_PUITS.md](DEMANDE_ATELIER_PUITS.md) — construire la première map (le puits) en deux versions de plan différent, jouables (traitée)
- [DEMANDE_ATELIER_FONDERIE.md](DEMANDE_ATELIER_FONDERIE.md) — construire la deuxième map (la fonderie) en deux versions, inspirée de « Foundries and Forges » pour la A (traitée)
- [DEMANDE_ATELIER_MONSTRES_TOURS.md](DEMANDE_ATELIER_MONSTRES_TOURS.md) — réponse à l'Atelier : les 9 monstres et leurs ennemis, les 24 tours animées, les projectiles (envoyée)
- [DEMANDE_ATELIER_TAVERNE.md](DEMANDE_ATELIER_TAVERNE.md) — construire la taverne en jeu avec les blocs `taverne_*`, captures, structure déployée (traitée)

**Data-driven** (équilibrer avec des JSON et `/reload`, sans recompiler) :
[ennemis](data-driven/ennemis.md), [stats des tours](data-driven/tours.md),
[réglages globaux](data-driven/equilibrage.md).

## Démarrage rapide

```bash
./gradlew runClient
```

Les blocs se trouvent dans l'onglet créatif du mod. Voir
[03-build-et-lancement.md](03-build-et-lancement.md) pour le détail des commandes,
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md) pour ce qui reste à faire,
et [06-a-tester.md](06-a-tester.md) pour ce qui attend une vérification manuelle en jeu.
