# Dungeon Defenders (NeoForge)

Mod Minecraft inspiré de *Dungeon Defenders* : le joueur place un **Cristal d'Eternia** que
les monstres tentent de détruire, en se défendant avec des tours (Spike Blockade, Harpoon
Turret...) posées pendant une phase de construction avant chaque vague.

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

## Documentation

Toute la documentation du projet (architecture, gameplay, build, état d'avancement, backlog)
vit dans [`doc/`](doc/README.md) — c'est le point d'entrée à lire en premier.

## Démarrage rapide

```bash
./gradlew build      # compile + vérifie
./gradlew runClient  # lance un client de dev avec le mod chargé
```

Voir [doc/03-build-et-lancement.md](doc/03-build-et-lancement.md) pour le détail des
commandes et configurations de run disponibles.

## Mappings

Ce projet utilise les mappings officiels Mojang pour les méthodes/champs de Minecraft, soumis
à une licence spécifique — voir le texte de référence :
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

## Ressources NeoForge

- Documentation communautaire : https://docs.neoforged.net/
- Discord NeoForged : https://discord.neoforged.net/

---

Ce dépôt est parti du MDK (Mod Development Kit) NeoForge ; les fichiers de gabarit encore
présents sous leur licence MIT d'origine (voir [`TEMPLATE_LICENSE.txt`](TEMPLATE_LICENSE.txt))
restent distincts de la licence du mod lui-même (« All Rights Reserved », ci-dessus).
