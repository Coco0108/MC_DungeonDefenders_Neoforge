# Data-driven, étape 3 : les réglages globaux d'équilibrage

> **Statut : implémenté le 2026-11-10** sur `feature/data-driven`, à la suite des étapes
> [1 (ennemis)](ennemis.md) et [2 (tours)](tours.md). **Pas encore testé en jeu** : procédure
> dans [06-a-tester.md](../06-a-tester.md).
>
> **Aucune valeur de gameplay ne change.** Les JSON livrés reprennent les constantes d'avant
> (gametest `balance_shipped_values`).

## À quoi ça sert

Les nombres d'équilibrage qui n'appartiennent ni à un ennemi ni à une tour vivaient dans des
constantes Java éparpillées. Ils sont maintenant dans des JSON relus par `/reload`. Les
**vagues** elles-mêmes (quels ennemis, combien, à quelles vagues) ne sont pas concernées :
elles sont déjà des données, dans les spawners de chaque map.

## Les réglages

Fichiers : `data/dungeon_defenders/dungeon_defenders/balance/<fichier>.json`, chacun un objet
plat `"clé": nombre`. Liste complète (`init/BalanceValue.java`) :

| Fichier | Clé | Défaut | Minimum | Effet |
|---|---|---|---|---|
| `difficulty.json` | `easy_multiplier` | 0.75 | 0.01 | Nombre d'ennemis par spawner en Facile |
| `difficulty.json` | `normal_multiplier` | 1.0 | 0.01 | Idem en Normal |
| `difficulty.json` | `hard_multiplier` | 1.5 | 0.01 | Idem en Difficile |
| `difficulty.json` | `per_wave_increase` | 0.1 | 0 | Ennemis en plus par vague au-delà de la première (0.1 = +10 %) |
| `towers.json` | `refund_ratio` | 0.5 | 0 | Part du coût en mana rendue quand on retire une tour |
| `progression.json` | `xp_per_level` | 100 (entier) | 1 | Expérience nécessaire pour passer un niveau |

**Ajoutés avec les bonus de score et le compte à rebours** (2026-11-10, voir
[02-gameplay.md](../02-gameplay.md#bonus-et-multiplicateurs-de-score--initscorekeeperjava)) :

| Fichier | Clé | Défaut | Minimum | Effet |
|---|---|---|---|---|
| `score.json` | `wave_clear_bonus` | 50 | 0 | Bonus de score à chaque vague nettoyée |
| `score.json` | `victory_bonus` | 200 | 0 | Bonus de score à la victoire |
| `score.json` | `no_player_damage_multiplier` | 1.5 | 1 | Multiplie le bonus de vague si aucun joueur n'a été blessé |
| `score.json` | `no_crystal_damage_multiplier` | 1.5 | 1 | Le multiplie encore si le cristal est intact |
| `score.json` | `easy_multiplier` / `normal_multiplier` / `hard_multiplier` | 0.75 / 1.0 / 1.5 | 0 | Multiplie tout gain de score selon la difficulté |
| `pacing.json` | `combat_countdown_seconds` | 3 (entier) | 0 | Secondes entre le vote « prêt » unanime et le Combat (0 = immédiat) |

Attention : `score.json` et `difficulty.json` ont chacun des clés `easy_multiplier`... La
première règle le **score**, la seconde le **nombre d'ennemis**.

## Règles

- **Une clé absente garde sa valeur par défaut.**
- **À la moindre erreur, le fichier entier est refusé** (clé inconnue, valeur non numérique, non
  entière là où il le faut, sous son minimum) et tous ses réglages gardent leur défaut. Jamais
  de fichier à moitié appliqué. Exemple de log :
  `Fichier d'équilibrage dungeon_defenders:dungeon_defenders/balance/difficulty.json invalide : clé inconnue "hard_multiplierr" (clés de ce fichier : ...) — les valeurs par défaut du code sont utilisées pour ce fichier.`
- **Les réglages sont ceux du mod** : pour les modifier, un datapack **remplace** le fichier du
  mod (même chemin, namespace `dungeon_defenders`). Un fichier sous un autre namespace est
  ignoré avec un avertissement.
- Au démarrage et à chaque `/reload` : `Réglages d'équilibrage chargés : 6 sur 6`.
- Un `/reload` recalcule le total d'ennemis de la vague en cours, qui dépend du multiplicateur
  de difficulté. Les spawners, eux, ne recalculent leur plafond qu'au début de la vague
  suivante (comportement existant).

## Synchronisation client

`GameBalancePayload`, à la connexion et après chaque `/reload`. Le client s'en sert pour
l'aperçu au-dessus des spawners (multiplicateur de difficulté) et la barre d'expérience
(`xp_per_level`). Deux copies distinctes, serveur et client, comme pour les tours.

## Ajouter un réglage

1. Une ligne dans `BalanceValue` (fichier, clé, défaut, minimum, entier ou non).
2. La clé dans le JSON correspondant.
3. `GameBalance.get(BalanceValue.X, clientSide)` à la place de la constante.

Le chargement, `/reload`, la validation et la synchro sont génériques.

## Vérification

- Gametests `balance_shipped_values` et `balance_validation`.
