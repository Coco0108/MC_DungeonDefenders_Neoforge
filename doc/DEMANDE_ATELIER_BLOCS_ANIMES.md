# Demande à l'Atelier : les blocs animés (forge, mannequin, cristal)

> **En attente** (2026-11-28). À transmettre à la session de l'Atelier (`~/MC_Atelier_Neoforge`). Ce fichier est une
> **proposition** : c'est l'Atelier qui fixe le format (même règle que pour les blocs orientés et les menuiseries,
> [DEMANDE_ATELIER_BLOCS.md](DEMANDE_ATELIER_BLOCS.md)), puis met à jour l'« Historique » de
> `docs/PROMPT_DUNGEON_DEFENDERS.md`. Dungeon Defenders lit ce qu'il décide.

## Pourquoi

Les modèles GeckoLib animés de la forge, du mannequin et du cristal existent dans l'Atelier (`modeles/dd_forge_v2/`,
`modeles/dd_dummy/`, `modeles/dd_cristal_v2/` : `.geo.json`, `.animation.json`, texture, glowmask). Mais une fiche de
bloc ne sait aujourd'hui décrire qu'un modèle **fixe** : le déploiement du 2026-11-27 a envoyé leur version figée
(`dd_*_v2.json`), que `tavern_crystal`, `forge` et `training_dummy` de Dungeon Defenders utilisent déjà. Le prompt de
l'Atelier le prévoit : « Plus tard : les blocs animés […]. Le format de fiche sera d'abord défini dans l'Atelier. »

| Modèle | Animations | Particularité |
|---|---|---|
| `dd_forge_v2` | `animation.idle` (boucle), `open` (1,8 s), `opened` (boucle), `close` (1,4 s) | orienté, glowmask (feu) |
| `dd_dummy` | `animation.idle` (boucle), `hurt` (1,2 s), `hurt_heavy` (2 s) | orienté |
| `dd_cristal_v2` | `animation.idle` (boucle, 16 s), `death` (1,6 s, marqueur `impact;` à 0,4 s) | glowmask |

## Qui fait quoi

| Qui | Quoi |
|---|---|
| **Atelier** | Le format ; ses propres blocs animés (les voir dans l'Établi et en jeu, déclencher chaque rôle à la main) ; le build ; les vérifications ; le déploiement des fichiers GeckoLib. |
| **Dungeon Defenders** | Un bloc animé générique (block entity GeckoLib et son renderer, sans code par bloc) ; le **moment** où chaque rôle se joue dans le jeu. |

## Format proposé

Un champ facultatif **`anime`** dans une fiche de bloc `modele: true`. C'est le même vocabulaire que pour les monstres
(`modele`, `animations`, `texture`, `roles_animation`). Les fiches actuelles restent valides.

```json
{ "id": "dd_forge_v2", "nom": "Forge (décor)", "modele": true, "orientation": "horizontale", "lumiere": 13, "son": "pierre",
  "anime": {
    "modele": "atelier:geckolib/models/block/dd_forge_v2.geo.json",
    "animations": "atelier:geckolib/animations/block/dd_forge_v2.animation.json",
    "texture": "atelier:textures/geckolib/block/dd_forge_v2.png",
    "roles_animation": { "idle": "animation.idle", "open": "animation.open", "opened": "animation.opened",
                         "close": "animation.close" }
  } }
```

- **Le modèle fixe reste.** Le modèle converti (`<id>.json`), sa texture et sa forme continuent de servir pour l'objet
  en main et l'inventaire, les particules quand on casse le bloc, la forme (sélection, collision) et le repli si
  GeckoLib manque. Le bloc posé, lui, est dessiné par GeckoLib.
- **La texture GeckoLib a son propre chemin**, hors de `textures/block/` : `textures/geckolib/block/<id>.png`.
  - Tout ce qui est sous `textures/block/` est cousu dans l'atlas des blocs, et la texture du modèle fixe porte déjà le
    nom `<id>.png`, avec ses UV ajustées et un remplissage transparent : les deux ne peuvent pas partager le fichier.
  - Une texture de 160 × 311 n'a de toute façon rien à faire dans l'atlas.
- **Glowmask** : `<texture>_glowmask.png` à côté, s'il existe, comme pour les monstres.
- **Orientation** : `horizontale` tourne aussi le modèle animé (dessiné devant au nord), d'un quart par `facing`, comme
  le modèle fixe.
- **`roles_animation`** : rôle → nom d'animation. Un rôle absent ne se joue pas, sans erreur. Les rôles connus :

| Rôle | Joué | Dans l'Atelier (pour tester) | Dans Dungeon Defenders |
|---|---|---|---|
| `idle` | En boucle, tout le temps | Automatique | Automatique |
| `open` puis `opened` | `open` une fois, puis `opened` en boucle | Clic droit sur le bloc | Ouverture de l'écran de la forge |
| `close` | Une fois, puis retour à `idle` | Clic droit à nouveau | Fermeture de l'écran (dernier joueur qui le ferme) |
| `hurt` | Une fois, puis retour à la boucle en cours | Frapper le bloc | Le mannequin encaisse un coup |
| `hurt_heavy` | Une fois | Frapper fort (à définir) | Un coup d'au moins 8 dégâts (réglage côté Dungeon Defenders) |
| `death` | Une fois ; l'effet tombe au marqueur `impact` (sinon à la moitié), comme le coup d'un monstre | Commande | Destruction du cristal d'Eternia : l'explosion au marqueur, puis la fin de partie |

- Une commande de test, par exemple `/atelier bloc_anim <pos> <role>`, aide à tout vérifier dans l'Atelier.
- **Sons et particules** : plus tard, avec le même principe que les monstres (keyframes de l'animation). Les fichiers
  actuels n'en ont pas.

## Ce que Dungeon Defenders recevra au déploiement

En plus de ce qu'un bloc `modele` envoie déjà :

- la fiche avec `anime` (chemins réécrits en `dungeon_defenders:`) ;
- `assets/dungeon_defenders/geckolib/models/block/<id>.geo.json` et
  `geckolib/animations/block/<id>.animation.json` ;
- `assets/dungeon_defenders/textures/geckolib/block/<id>.png`, et `_glowmask.png` s'il existe.

Dans la fiche, ce sont les rôles qui comptent, pas les noms d'animation : l'Atelier peut renommer ses animations sans
que Dungeon Defenders change.

## Ce que Dungeon Defenders fera ensuite

1. Un bloc animé générique pour toute fiche avec `anime` : un block entity GeckoLib, son renderer côté client
   seulement (contrôle `tools/verifier-dist.py`), la rotation selon `facing`, le glowmask, et le jeu des rôles
   déclenché par le serveur et synchronisé à tous les joueurs.
2. `forge`, `training_dummy` et `tavern_crystal` prendront le modèle animé de `dd_forge_v2`, `dd_dummy` et
   `dd_cristal_v2`, comme ils prennent déjà leur modèle fixe et leur forme (`block/FormeAtelier`).
3. Les déclencheurs du tableau, puis un gametest par rôle (le serveur demande le bon rôle au bon moment).

**Question pour Corentin** (côté Dungeon Defenders, pas pour l'Atelier) : `dd_cristal_v2` a une animation `death`, mais
le cristal de la taverne ne peut pas être détruit. Elle servira donc au **cristal d'Eternia** des maps, qui prendrait
aussi ce modèle. À confirmer.
