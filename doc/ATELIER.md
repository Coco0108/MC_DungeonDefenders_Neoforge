# Ce que Dungeon Defenders lit de l'Atelier

L'**Atelier** est le mod bac à sable de Corentin (repo privé `Coco0108/MC_Atelier_Neoforge`). Chaque modèle
Blockbench y est testé en jeu et réglé dans une **fiche** JSON, puis **déployé** vers les mods qui le demandent
(`destinations` de la fiche). Ce document dit ce que Dungeon Defenders lit de ce qui arrive, où, et ce qu'il ignore.
Le format exact des fiches est dans le `CLAUDE.md` de l'Atelier (« Format de la fiche », « Objets et blocs ») et dans
ses `schemas/`.

## Les règles

- **L'Atelier est la seule source de vérité des modèles.** Les fichiers qu'il déploie ne se retouchent **jamais** ici :
  ni modèle, ni texture, ni fiche, ni son. Chaque fiche copiée porte un bloc `origine` avec une empreinte : une copie
  modifiée à la main bloque les déploiements suivants. Un fichier reçu qui pose problème se corrige **dans l'Atelier**,
  puis se redéploie.
- **Un monstre = des fichiers, pas du code.** Aucune classe par monstre, par objet ni par bloc : tout passe par le code
  générique du package `atelier/`. Si un besoin ne rentre pas dans la fiche, on fait évoluer le format (dans l'Atelier)
  et ce code générique, jamais un cas particulier.
- **Une fiche invalide est ignorée avec un message de log clair**, elle ne fait jamais planter le jeu.
- Les déploiements arrivent par GitHub, en commits `modele: <id> depuis l'atelier @<commit>`.

## Où arrivent les fichiers

Le namespace `atelier:` devient `dungeon_defenders:` dans tout ce qui est copié.

| Quoi | Où (sous `src/main/resources/`) | Lu par |
|---|---|---|
| Fiche d'un monstre | `data/dungeon_defenders/atelier_monstres/<id>.json` | `AtelierLoader` |
| Géométrie, animations (GeckoLib) | `assets/dungeon_defenders/geckolib/models/entity/<id>.geo.json`, `geckolib/animations/entity/<id>.animation.json` | GeckoLib (client), `AtelierLoader` (vérification, instants d'impact) |
| Textures, variantes, glowmask | `assets/dungeon_defenders/textures/entity/<id>/…` (`<texture>_glowmask.png` à côté si elle existe) | `AtelierMonsterRenderer` |
| Sons | `assets/dungeon_defenders/sounds/entity/<id>/<nom>.ogg` + entrées `entity.<id>.<nom>` de `assets/dungeon_defenders/sounds.json` | `AtelierSounds` |
| Ennemi de vague | `data/dungeon_defenders/dungeon_defenders/enemy/<id>.json` | `EnemyRegistry` (le système d'ennemis du mod, voir [data-driven/ennemis.md](data-driven/ennemis.md)) |
| Fiche d'un objet, sa texture | `data/dungeon_defenders/atelier_objets/<id>.json`, `assets/dungeon_defenders/textures/item/<id>.png` | `AtelierLoader`, `AtelierObjets` |
| Fiche d'un bloc, sa texture | `data/dungeon_defenders/atelier_blocs/<id>.json`, `assets/dungeon_defenders/textures/block/<id>.png` (et `_top`, `_bottom`, `_side`) | `AtelierLoader`, `AtelierObjets` |
| Bloc `modele` : modèle converti, forme | `assets/dungeon_defenders/models/block/<id>.json`, `data/dungeon_defenders/atelier_formes_bloc/<id>.json` | Minecraft (modèle), `BlocModele` (forme) |
| Fichiers natifs des objets et blocs | états de bloc, modèles et définitions d'objet (`items/<id>.json`), butin du bloc, tags d'outil, noms | Minecraft : **écrits par le déploiement**, pas par ce code |
| Effets, projectiles | `data/dungeon_defenders/atelier_effets/<id>.json`, `atelier_projectiles/<id>.json` | **rien** (références inertes, voir plus bas) |

## Au démarrage

`AtelierLoader.load` tourne à la **construction du mod** (`DungeonDefendersMod`), avant le gel des registres :
chaque fiche devient son propre type d'entité, et chaque son son événement. On ne peut pas en ajouter à chaud : ce
chargement ne passe donc **pas** par un reload listener de datapack, et les fiches doivent être **dans le jar**.
Ajouter ou modifier un monstre demande de rebâtir le mod (le `/reload` ne le relit pas).

Le log résume ce qui a été chargé :

```
Atelier : 1 monstre(s) chargé(s) [gluant], 0 fiche(s) rejetée(s) []
```

### Vérifications

Pour chaque fiche, un message par problème, puis la fiche est ignorée (repris de `FicheLoader` de l'Atelier) :

- nom de fichier invalide, `id` différent du nom du fichier, `id` déjà pris par une entité du mod
  (`mana_crystal`, `training_dummy`, `weapon_projectile`) ;
- valeur inconnue ou mal formée (le codec s'arrête à la première erreur de forme : `comportement`, `hitbox`…) ;
- modèle, animations ou texture introuvable, hors du namespace `dungeon_defenders`, ou hors du dossier que le jeu lit
  (GeckoLib 5 ne charge que `geckolib/models/` et `geckolib/animations/`) ; modèle sans `minecraft:geometry` ;
- rôle `idle` ou `walk` manquant, rôle vers une animation absente du `.animation.json` ;
- marqueur `impact` après la fin de son animation, instant de timeline qui n'est pas un nombre ;
- son de la fiche (`sons`) absent du `sounds.json` ;
- variante sans texture, déclarée deux fois, ou d'id invalide.

Simples avertissements (la fiche est chargée) : une section que Dungeon Defenders ne lit pas (voir « Ignoré »), un
champ inconnu, un monstre `neutre`/`hostile` sans rôle `attack`, une animation `attack` sans marqueur `impact`,
plusieurs marqueurs `impact`, une keyframe de son vers un événement non déclaré (elle serait muette), un comportement
`fuyard`/`marchand`.

## Les monstres

Un **type d'entité par fiche**, `dungeon_defenders:<id>`, et son œuf `dungeon_defenders:<id>_spawn_egg` (onglets
« Œufs d'apparition » et « Dungeon Defenders »). **Une seule classe d'entité** (`atelier/AtelierMonster`), **un seul
modèle et un seul renderer** (`client/atelier/`), qui prennent tout dans la fiche.

| Champ de la fiche | Effet dans Dungeon Defenders |
|---|---|
| `modele`, `animations`, `texture` | Modèle et animations GeckoLib, texture principale |
| `variantes` | Tirée au hasard à l'apparition (la texture principale compte comme une variante), synchronisée, sauvegardée sous `Variante` : `/summon dungeon_defenders:<id> ~ ~ ~ {Variante:"rouge"}` |
| glowmask | Calque lumineux GeckoLib si `<texture>_glowmask.png` existe, pour cette texture seulement ; rien sinon |
| `roles_animation` | `idle`/`walk` selon le mouvement ; `attack` à chaque coup ; `hurt` quand il est blessé (jamais au milieu d'un coup) ; `death` à sa mort (le corps reste le temps de l'animation, 5 s au plus, sans basculer sur le côté) |
| `echelle`, `hitbox` | Taille du rendu (et de l'ombre), boîte de collision |
| `stats.pv`, `degats`, `vitesse` | Attributs vanilla : PV max, dégâts d'un coup **sur un joueur**, vitesse |
| `stats.recharge_attaque` | Secondes entre deux coups (défaut 1), jamais moins que l'animation `attack` |
| `comportement` | La réaction aux **joueurs** : `passif` ne les attaque jamais, `neutre` riposte, `hostile` les attaque à vue comme un zombie. `fuyard`/`marchand` se comportent en `passif` |
| `deplacement` | `sol` ; `vol` vole comme une abeille (pas de gravité ni de chute, vitesse de vol 2 × `vitesse`) |
| `sons` | `ambient`, `hurt`, `death` joués automatiquement (sinon les sons par défaut d'un monstre vanilla) ; les autres ne servent qu'aux keyframes des animations |
| `nom` | Nom affiché si le fichier de langue n'a pas `entity.dungeon_defenders.<id>` |
| `origine` | Réservé au déploiement : le jeu l'ignore |

### Le coup et son impact

Les dégâts d'un coup tombent à l'instruction `impact;` de la timeline de l'animation `attack`
(`"timeline": { "0.4": "impact;" }`), sinon à la moitié de l'animation. GeckoLib ne lit les instructions que côté client :
le serveur lit donc cet instant dans le `.animation.json` au démarrage (`Impact`, repris de l'Atelier). Une nouvelle
attaque ne part pas avant la fin de l'animation, ni avant `stats.recharge_attaque` secondes.

C'est vrai pour **tous** ses coups :
- sur un joueur (`comportement` `neutre`/`hostile`) : touché seulement s'il est encore à portée à l'impact, avec 1 bloc
  de marge pour un recul pendant le geste ;
- sur une tour ou le cristal, et pour un tir sur le cristal : les goals de Dungeon Defenders (`AttackPriorityTargetGoal`,
  `RangedAttackEterniaCrystalGoal`) passent par `AnimatedAttacker.strike`. Un zombie ou un squelette vanilla frappe
  toujours tout de suite ; un monstre de l'Atelier joue `attack` et les dégâts tombent à l'impact.

### C'est un `Monster`

L'entité générique étend `Monster`, pas seulement `PathfinderMob` : dans Dungeon Defenders, les tours, les armes des
héros, l'IA des vagues (`ModEvents.onMonsterSpawn`), l'XP, le score et le butin ne visent que des `Monster`.
Catégorie `MONSTER`, sans aucune apparition naturelle : il n'apparaît que par un spawner de map, un œuf ou `/summon`.

## Les objets et les blocs

Même principe : un objet par fiche d'objet, un bloc et son objet par fiche de bloc, enregistrés au démarrage
(`atelier/AtelierObjets`, repris d'`objet/AtelierObjets.java` de l'Atelier), tous dans l'onglet « Dungeon Defenders ».
Ce code ne fait qu'**enregistrer** : les fichiers natifs (états de bloc, modèles, définitions d'objet, butin du bloc,
tags d'outil, noms) arrivent avec le déploiement, comme l'Atelier les génère pour lui-même.

**Objet** `dungeon_defenders:<id>` : `pile` (1 à 99, défaut 64), `rarete` (`commun`, `peu_commun`, `rare`, `epique` :
couleur du nom), `brillant` (reflet d'enchantement), `nourriture` (`faim`, `saturation`, `toujours`).

**Bloc** `dungeon_defenders:<id>` et son objet :

| Champ | Effet |
|---|---|
| `durete`, `resistance` | Temps pour le casser ; résistance aux explosions (défaut : `durete`) |
| `outil`, `niveau` | `niveau` autre que `aucun` : ne tombe qu'avec le bon outil (le tag d'outil arrive avec le déploiement) ; un `niveau` sans `outil` est refusé |
| `lumiere` | 0 à 15 |
| `son` | `pierre`, `bois`, `terre`, `herbe`, `sable`, `metal`, `verre`, `laine`, `amethyste` |
| `transparent` | Comme le verre : ne cache rien derrière lui, rien n'y apparaît, on n'y étouffe pas |
| `modele: true` | Modèle Blockbench converti en modèle de bloc : sélection et collision suivent les boîtes de sa forme (`BlocModele`, repris de l'Atelier), il ne cache jamais les faces voisines et laisse passer la lumière du ciel |

Vérifications (fichier ignoré, un message par problème) : texture absente, `id` différent du nom du fichier, `id` déjà
pris (par un objet ou un bloc du mod, un œuf de monstre, ou entre un objet et un bloc de l'Atelier), `_spawn_egg`
réservé, `niveau` sans `outil`, bloc `modele` sans son modèle ou avec une forme invalide (boîtes de 6 nombres, de -16 à
32 pixels). `cache` (bloc-cachette) : ignoré, avec un avertissement.

## Le lien avec les vagues

Un monstre de l'Atelier devient un ennemi de vague par un fichier `enemy/<id>.json` du système d'ennemis du mod
([data-driven/ennemis.md](data-driven/ennemis.md)), qui pointe vers son type d'entité :

```json
{
  "entity_type": "dungeon_defenders:orc",
  "icon": "dungeon_defenders:orc_spawn_egg",
  "xp_value": 20,
  "order": 10,
  "behavior": { "type": "dungeon_defenders:melee_priority", "damage_per_hit": 4 }
}
```

- `entity_type` : `dungeon_defenders:<id>`. `icon` : un objet, l'œuf du monstre est le choix naturel.
- `xp_value` (XP et score), `order` (rang dans l'éditeur de spawner).
- `behavior` : `dungeon_defenders:melee_priority` (tours puis cristal, au corps à corps ; `damage_per_hit` optionnel, les
  dégâts **sur les tours et le cristal**) ou `dungeon_defenders:ranged_crystal` (tir sur le cristal ; `damage_per_shot`,
  `ticks_between_shots`, `shoot_range`).
- Optionnels, par-dessus la fiche : `max_health`, `movement_speed`, `scale`.
- Nom dans l'éditeur de spawner : clé `dungeon_defenders.enemy.<id>` des fichiers de langue (sinon l'identifiant brut).

Sans ce fichier, le monstre reçoit quand même l'IA de mêlée par défaut s'il apparaît (œuf, `/summon`), mais ne peut pas
être choisi dans un spawner. Les deux sources de dégâts sont distinctes : `stats.degats` de la fiche sur un joueur,
`damage_per_hit` (ou la config) sur les tours et le cristal.

## Ce que Dungeon Defenders attend de l'Atelier

Le code est prêt à lire ce qui arrive ; voici ce qui doit arriver avec le déploiement, en plus de la copie habituelle
(fiche, modèle, animations, textures, glowmasks, sons et entrées de `sounds.json`).

1. **Branche de déploiement.** Le code qui lit les fiches est sur `feature/atelier` (au-dessus de `integration/tout`,
   PR #67, qui contient `feature/data-driven`), pas encore dans `main`. **À terme : `main`**, une fois ces deux PR
   fusionnées. D'ici là, `atelier/modeles` convient, mais créée **depuis `feature/atelier`** (`"depuis":
   "feature/atelier"` dans `deploiement.json`) plutôt que depuis `main`, qui n'a ni le système d'ennemis ni ce code.
2. **Un `format: "dungeon_defenders"`** qui écrit `data/dungeon_defenders/dungeon_defenders/enemy/<id>.json` depuis une
   carte « Réglages Dungeon Defenders » (`cibles.dungeon_defenders`) :
   - toujours : `entity_type` = `dungeon_defenders:<id>`, `icon` = `dungeon_defenders:<id>_spawn_egg` ;
   - réglables : `xp_value` (défaut conseillé 10), `order`, `behavior` (`type` `dungeon_defenders:melee_priority` avec
     `damage_per_hit` optionnel, ou `dungeon_defenders:ranged_crystal` avec `damage_per_shot`, `ticks_between_shots`,
     `shoot_range`), et en option `max_health`, `movement_speed`, `scale` ;
   - le nom de l'ennemi : clé `dungeon_defenders.enemy.<id>` des fichiers de langue.
   Rien ne reste à fusionner de ce côté : l'Atelier écrit tout le fichier de ses monstres. Les ennemis propres au mod
   (`zombie`, `skeleton`, `brute`, `runner`, `marksman`) restent écrits à la main et ne sont jamais touchés.
   Sans section `cibles.dungeon_defenders`, pas de fichier : le monstre existe (œuf, `/summon`) mais n'est pas dans
   les spawners.
3. **L'œuf** `dungeon_defenders:<id>_spawn_egg` sert d'icône (spawners, HUD des vagues). Le code l'enregistre ; son
   apparence doit arriver comme dans l'Atelier : `assets/dungeon_defenders/items/<id>_spawn_egg.json` (le modèle
   partagé teinté de ses deux couleurs), le modèle `models/item/oeuf.json` et ses textures `textures/item/oeuf.png`,
   `oeuf_taches.png`. Sans eux, l'œuf s'affiche en damier violet et noir.
4. **Les noms**, dans `lang/fr_fr.json` et `lang/en_us.json` du mod (seulement les lignes de l'Atelier, comme pour
   DQM) : `entity.dungeon_defenders.<id>`, `item.dungeon_defenders.<id>_spawn_egg`, `dungeon_defenders.enemy.<id>`,
   `item.dungeon_defenders.<objet>`, `block.dungeon_defenders.<bloc>`. Sans nom de monstre, son `nom` de fiche sert de
   repli.
5. **Les fichiers natifs des objets et des blocs**, comme le build de l'Atelier les génère pour lui-même : états de bloc
   (`blockstates/<id>.json`), modèles (`models/block/<id>.json`, `models/item/<id>.json`), définitions d'objet
   (`items/<id>.json`), butin du bloc (`data/dungeon_defenders/loot_table/blocks/<id>.json` : il se ramasse lui-même),
   tags d'outil (`data/minecraft/tags/block/mineable/<outil>.json`, `needs_<niveau>_tool.json` : à **compléter**, jamais
   à écraser), et pour un bloc `modele` sa forme en `data/dungeon_defenders/atelier_formes_bloc/<id>.json`.
6. **Des ids libres** : un id déjà pris par le mod est refusé au démarrage (entités `mana_crystal`, `training_dummy`,
   `weapon_projectile` ; tous les objets et blocs du mod, dont `forge`, `eternia_crystal`, les tours…).

## Ignoré (pour l'instant)

Présent dans une fiche, sans erreur (un avertissement au démarrage) : `capacites` (et l'ancien `tir`), effets et
projectiles (`atelier_effets/`, `atelier_projectiles/` restent des références inertes), `apparition` (les ennemis
viennent des vagues), `butin` (les vagues ont leur propre économie : mana, butin de héros), `fuite`/`commerce` (la monnaie
vivante), `oeuf`, `cibles`, `destinations`.

Plus tard : les **blocs animés** (forge, mannequin, cristal ont des animations GeckoLib qu'un bloc fixe ne joue pas :
il faudra un `GeoBlockEntity` générique, une fois le format défini dans l'Atelier).

Plus tard aussi : les **blocs de la taverne** (2026-11-27, 7 demandes `dungeon_defenders-taverne_*` déposées chez le
concepteur de modèles, livrées dans `modeles/blocs/taverne_*/` de l'Atelier). Ils sont dessinés pour les deux sortes de
bloc qui existent (cube, `modele`), mais il leur faudra un champ de fiche générique, défini **dans l'Atelier** puis lu
ici : `oriente` (rotation horizontale au placement : fenêtres, consoles, torche murale, banc, comptoir, étagère, foyer ;
modèle dessiné devant au nord) et `axe` (comme une bûche : `taverne_poutre`). Une 8ᵉ demande,
`taverne_menuiserie`, livre les modèles **par état** de la porte (bas/haut, charnière gauche/droite), de la trappe, de
l'escalier (droit, intérieur, extérieur, comme vanilla), de la dalle (bas/haut), de la barrière qui se raccorde (poteau +
bras nord, en multipart) et du portillon (fermé/ouvert) : il faudra ici des sortes de bloc génériques correspondantes
(`porte`, `trappe`, `escalier`, `dalle`, `barriere`, `portillon`), à définir dans le format de fiche de l'Atelier.

## Tester

- `./gradlew runGameTestServer` : le test `atelier_monsters` fait apparaître **chaque** monstre chargé et vérifie sa
  fiche (PV, dégâts), l'IA de vague, l'apparition par son `enemy/<id>.json`, et que son coup ne tombe qu'à l'impact ;
  `atelier_items_and_blocks` vérifie chaque objet (pile, rareté, nourriture) et chaque bloc (dureté, lumière, outil,
  forme d'un bloc `modele`). Sans rien de déployé, ils n'ont rien à vérifier. Ils tournent dans leur propre
  environnement (`dungeon_defenders:atelier`), donc dans un lot à part : les tests des tours et des armes visent le
  monstre le plus proche, et un monstre d'un test voisin du même lot pourrait les fausser.
- En jeu : l'œuf, les objets et les blocs dans l'onglet « Dungeon Defenders », ou `/summon dungeon_defenders:<id>` ; voir
  [06-a-tester.md](06-a-tester.md#monstres-de-latelier).
