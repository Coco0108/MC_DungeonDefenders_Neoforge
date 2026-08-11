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
2. [Gameplay & mécaniques](02-gameplay.md) — le Cristal d'Eternia, les PV, l'IA des zombies, le rendu de la barre de vie.
3. [Build & lancement](03-build-et-lancement.md) — commandes Gradle, configurations de run, CI GitHub Actions.
4. [Guide : ajouter du contenu](04-guide-ajout-contenu.md) — recettes pour ajouter un bloc, un block entity, une traduction.
5. [État du projet & problèmes connus](05-etat-et-problemes-connus.md) — ce qui reste du template, ce qui est cassé, pistes.
6. [À tester en jeu](06-a-tester.md) — fonctionnalités écrites mais jamais lancées en jeu, checklist à cocher.

## Démarrage rapide

```bash
./gradlew runClient
```

Les blocs se trouvent dans l'onglet créatif du mod. Voir
[03-build-et-lancement.md](03-build-et-lancement.md) pour le détail des commandes,
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md) pour ce qui reste à faire,
et [06-a-tester.md](06-a-tester.md) pour ce qui attend une vérification manuelle en jeu.
