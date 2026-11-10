# Data-driven, étape 2 : les stats des tours

> **Statut : implémenté le 2026-11-10** sur `feature/data-driven`, à la suite de
> [l'étape 1 (ennemis)](ennemis.md). **Pas encore testé en jeu** : procédure dans
> [06-a-tester.md](../06-a-tester.md).
>
> **Aucune valeur de gameplay ne change.** Les six JSON livrés reprennent les constantes
> d'avant. Vérifié par le gametest `tower_shipped_values` : pour chaque tour, le JSON est chargé
> et ses valeurs sont égales aux valeurs par défaut du code.

## Ce qui est data-driven, et ce qui ne l'est pas

- **Dans les JSON : les chiffres.** PV, coût en mana, dégâts, cadence, portée, cône, repousse,
  rayon de zone, vitesse et distance du projectile.
- **En Java : la liste des tours et leur comportement.** Une tour est un bloc et un block entity
  enregistrés par le mod. On ne crée pas une tour avec un JSON, contrairement à un ennemi.

## Format

Un fichier par tour : `data/<namespace>/dungeon_defenders/tower/<id>.json`, où
`<namespace>:<id>` est l'identifiant de la tour (`dungeon_defenders:spike_blockade`). C'est un
objet plat `"stat": nombre`. Exemple réel, `tower/bowling_ball_turret.json` :

```json
{
  "max_health": 20,
  "mana_cost": 55,
  "range": 12.0,
  "cone_angle_degrees": 45.0,
  "damage": 5.0,
  "attack_interval_ticks": 40,
  "projectile_velocity": 1.5,
  "projectile_extra_distance": 4.0
}
```

**Stats existantes** (`init/TowerStat.java`) :

| Stat | Type | Minimum | Tours concernées |
|---|---|---|---|
| `max_health` | entier | 1 | toutes |
| `mana_cost` | entier | 0 | toutes |
| `contact_damage` | décimal | 0 | Blockades |
| `contact_interval_ticks` | entier | 1 | Blockades |
| `contact_range` | décimal | 0 | Blockades |
| `knockback_strength` | décimal | 0 | Bouncer Blockade |
| `range` | décimal | 0 | Turrets |
| `cone_angle_degrees` | décimal | 1 | Turrets (≥ 360 = omnidirectionnel) |
| `damage` | décimal | 0 | Turrets |
| `attack_interval_ticks` | entier | 1 | Turrets |
| `splash_radius` | décimal | 0 | Mortar Turret |
| `projectile_velocity` | décimal | 0.1 | Bowling Ball Turret |
| `projectile_extra_distance` | décimal | 0 | Bowling Ball Turret |

Les valeurs livrées sont dans le tableau de
[02-gameplay.md](../02-gameplay.md#les-chiffres-data-driven-des-tours--inittowerstatsregistryjava-datansdungeon_defenderstowerjson).

## Règles

- **Une stat absente garde sa valeur par défaut.** Un JSON peut ne contenir que
  `{"mana_cost": 45}`.
- **Validation stricte, fichier refusé en entier** si :
  - une stat est inconnue (faute de frappe) ;
  - une stat n'est pas utilisée par cette tour (`range` sur une Blockade) ;
  - une valeur n'est pas un nombre, n'est pas entière là où il le faut, ou est sous son minimum.
- **Fichier absent ou refusé : la tour garde les valeurs par défaut du code**
  (`TowerDefinition`). Une tour a donc toujours des stats. Le log le dit :
  - `Stats de la tour dungeon_defenders:spike_blockade invalides (...) : stat inconnue "mana_cots" (stats de cette tour : ...) — les valeurs par défaut du code sont utilisées.`
  - `Aucun fichier de stats pour la tour ... : les valeurs par défaut du code sont utilisées.`
- **Un fichier qui ne correspond à aucune tour** est ignoré, avec un avertissement.
- **Au démarrage et à chaque `/reload`**, le log indique `Stats de tours chargées : 6 sur 6`.

## `/reload` et tours déjà posées

Les block entities ne recopient jamais leurs chiffres : ils les relisent à chaque usage
(`AbstractTowerBlockEntity#stats()`). Un `/reload` s'applique donc **aux tours déjà posées** :
dégâts, cadence, portée et coût de remboursement changent immédiatement.

Seule subtilité : les PV **actuels** d'une tour ne bougent pas. Si `max_health` augmente, les
tours déjà posées apparaissent entamées (barre de vie visible) ; s'il diminue sous leurs PV
actuels, elles gardent leurs PV jusqu'au prochain coup.

## Synchronisation client

Le client a besoin des chiffres pour la roue (coût en mana), l'aperçu de portée à la pose et la
barre de vie (PV max). `TowerStatsPayload` les envoie à la connexion et après chaque `/reload`
(`OnDatapackSyncEvent`).

`TowerStatsRegistry` garde **deux copies distinctes**, serveur et client, dans la même classe.
Elle est commune parce que les block entities de tours, chargés des deux côtés, y lisent leurs
PV max ; un block entity choisit sa copie selon `level.isClientSide()`. En solo, les deux copies
vivent dans la même JVM : les séparer évite qu'un bug de synchro passe inaperçu jusqu'au
multijoueur.

## Ajouter une stat ou une tour

- **Nouvelle stat** : une constante dans `TowerStat`, sa valeur par défaut dans le membre de
  `TowerDefinition` concerné, sa lecture dans le block entity, sa clé dans le JSON.
- **Nouvelle tour** : voir [04-guide-ajout-contenu.md](../04-guide-ajout-contenu.md).

## Vérification

- Gametests `tower_shipped_values` et `tower_stats_validation`.
- `tools/verifier-dist.py` : aucune classe cliente dans le graphe serveur.
