# Réponse à l'Atelier : les monstres, les tours et les projectiles

> **Envoyée** (2026-11-28) à la session de l'Atelier, qui demandait la liste de ce qu'il faut à Dungeon Defenders. Ce
> fichier est une **demande** ; pour les tours, c'est l'Atelier qui fixe le format (comme pour les blocs animés,
> [DEMANDE_ATELIER_BLOCS_ANIMES.md](DEMANDE_ATELIER_BLOCS_ANIMES.md)). La correspondance monstres ↔ ennemis est une
> **proposition** de Dungeon Defenders, à confirmer par Corentin.

## 1. Les monstres

**Tous les 9**, avec leurs fiches `atelier_monstres/` comme le prompt v1 : Dungeon Defenders les charge déjà
(`atelier/AtelierEntities`, [ATELIER.md](ATELIER.md#les-monstres)).

### Comment ils deviennent des ennemis des vagues

Un ennemi de Dungeon Defenders est un JSON (`data/dungeon_defenders/dungeon_defenders/enemy/<id>.json`) qui nomme un
type d'entité (`entity_type`), son IA, ses stats et son XP. Aujourd'hui ce sont des mobs vanilla. Il suffit de faire
pointer `entity_type` vers le monstre de l'Atelier : les spawners des maps déjà construites (taverne, puits) gardent les
mêmes ids d'ennemi et prennent le nouveau modèle sans être retouchés.

**C'est Dungeon Defenders qui règle l'équilibrage** (PV, vitesse, dégâts sur les tours et le cristal, XP) dans ses
JSON d'ennemi. Les `stats` de la fiche de l'Atelier servent de valeurs par défaut et pour les dégâts sur un joueur.
Il faut surtout des **hitbox** justes (les chemins des maps font 3 blocs de large) et un marqueur `impact` dans chaque
`attack`.

### La correspondance proposée

| Monstre de l'Atelier | Ennemi de Dungeon Defenders | Rôle dans les vagues | À faire chez nous |
|---|---|---|---|
| `gobelin` | `zombie` (remplace le zombie vanilla) | Corps à corps de base | — |
| `dark_elf_archer` | `skeleton` (remplace) | Tir sur le cristal, portée 10 | Tirer son projectile `fleche_elfe_noir` au lieu de la flèche vanilla |
| `kobold` | `runner` (remplace) | Petit, rapide, fragile ; **explose** sur sa cible (rôle `explode`) | Nouveau comportement « kamikaze » : dégâts de zone sur les tours et le cristal au marqueur `impact` d'`explode`, puis il meurt |
| `ogre` | `brute` (remplace) | Gros, lent, frappe fort ; une ou deux en dernière vague | — |
| `dark_elf_mage` | `marksman` (remplace) | Tireur de loin, portée 14 (rôle `cast`) | Lancer son sort (`orbe_psychique` ou `boule_de_feu`, à lui de choisir) |
| `orc` | **nouveau** `orc` | Corps à corps solide, entre le zombie et la brute | Nouveau JSON |
| `dark_elf_warrior` | **nouveau** `dark_elf_warrior` | Corps à corps rapide et dangereux, maps suivantes | Nouveau JSON |
| `skeleton` | **nouveau** `skeleton_warrior` (l'id `skeleton` est pris par l'archer) | Corps à corps des maps souterraines | Nouveau JSON |
| `wyvern` | **nouveau** `wyvern` | Volant : vient droit sur le cristal par-dessus les murs | Nouveau comportement « vol » vers le cristal (les tours doivent pouvoir la viser en l'air) |

Questions pour l'Atelier :

- **Le rôle `explode` du kobold** : merci de mettre `impact;` à l'instant de l'explosion, et de dire s'il y a une mèche
  à allumer avant (une animation à part, ou le début d'`explode`).
- **La wyverne** : à quelle hauteur vole-t-elle d'habitude, et son attaque est-elle au corps à corps ou un souffle
  (`souffle_draconique`) ?
- **Les sons** : bienvenus dès qu'ils sont prêts (`ambient`, `hurt`, `death`, et dans les keyframes des attaques).

## 2. Les tours : oui, des modèles GeckoLib animés, pour les 24

Aujourd'hui, les tours posées n'ont **que des modèles provisoires** (des blocs vanilla : fourneau, etc.). Les 24
modèles statiques livrés dans `art/models/` de Dungeon Defenders n'y sont pas encore branchés. Ils sont à prendre
**comme base**, sur cette machine : `/home/corentin/MC_DungeonDefenders_Neoforge/art/models/<tour>/` (`.bbmodel`,
`.json`, `.png`, `apercu.png`, non versionnés). Mieux vaut des modèles animés de l'Atelier que de brancher les modèles
statiques.

### Ce qui vaut pour toutes les tours

- **Les ids** : ceux des tours du jeu, sans préfixe (`spike_blockade`, `harpoon_turret`…, liste plus bas).
- **La place** : une tour occupe **un bloc au sol** ; sa forme de collision fait **1,5 bloc de haut**, pour que les
  monstres ne grimpent pas dessus. Le modèle peut dépasser en hauteur (jusqu'à ~2 blocs), pas en largeur.
- **L'orientation** : une tour est orientée (`facing`), le modèle est dessiné devant au nord, comme les blocs.
- **Les rôles communs** :

| Rôle | Quand Dungeon Defenders le joue |
|---|---|
| `idle` | En boucle |
| `build` | Une fois, à la pose (environ 1 s) |
| `hurt` | Une fois, quand un monstre la frappe (facultatif) |
| `upgrade` | Une fois, quand on l'améliore (facultatif) |
| `destroy` | Une fois, quand elle est détruite ; le bloc disparaît à la fin |

- **L'os de visée** : pour une tour qui tire, un os nommé (par exemple `tete`), que Dungeon Defenders **tourne par le
  code** vers sa cible (lacet, et tangage si possible). Le reste du modèle reste fixe. Merci de donner le nom de l'os
  et son point de pivot dans la fiche.
- **Le marqueur `impact;`** dans `attack` : c'est l'instant où le tir part, ou où le coup frappe. Les dégâts et le
  projectile partent à ce moment-là, comme pour les monstres.
- **Le glowmask** est bienvenu (flèche de cristal, pierre de vie, condensateur, pylône, éclairs).
- **Les sons** : `build`, `attack` (ou dans les keyframes), `hurt`, `destroy`.
- **L'objet** : l'icône de la roue et de l'inventaire peut rester le modèle fixe, ou un rendu 2D ; c'est à l'Atelier
  de choisir.

### Les rôles en plus, tour par tour

| Héros | Tour | Ce qu'elle fait | Rôles en plus |
|---|---|---|---|
| Écuyer | `spike_blockade` | Mur, blesse au contact | `hurt` |
| Écuyer | `bouncer_blockade` | Mur, repousse au contact | `attack` (le rebond) |
| Écuyer | `slice_n_dice_blockade` | Lames tournantes | `attack` en boucle tant qu'un monstre est à portée (ou `active`) |
| Écuyer | `harpoon_turret` | Tir unique dans un cône | `attack`, os de visée |
| Écuyer | `bowling_ball_turret` | Boule qui traverse une ligne | `attack`, os de visée |
| Écuyer | `mortar_turret` | Tir lent en cloche, zone | `attack`, os de visée (tangage) |
| Alchimiste | `frost_still` | Aura qui ralentit | `attack` (pulsation) |
| Alchimiste | `acid_sprayer` | Jet en cône court | `attack`, os de visée |
| Alchimiste | `volatile_vat` | Mur qui explose à sa destruction | `destroy` avec `impact;` à l'explosion |
| Alchimiste | `catalyst_pylon` | Aura : plus de dégâts aux tours voisines | `idle` seulement (lumineux) |
| Alchimiste | `mana_condenser` | Plus de mana des monstres tués près de lui | `attack` quand il récolte |
| Alchimiste | `flask_launcher` | Fiole tout autour, flaque | `attack`, os de visée (360°) |
| Ingénieur | `iron_barricade` | Mur solide qui se répare | `hurt`, `repair` (facultatif) |
| Ingénieur | `repeater_turret` | Tir très rapide | `attack` court, os de visée |
| Ingénieur | `longshot_turret` | Tir puissant très loin | `attack`, os de visée |
| Ingénieur | `magnet_coil` | Attire les monstres | `attack` (pulsation) |
| Ingénieur | `repair_station` | Répare les tours voisines | `attack` (quand elle répare) |
| Ingénieur | `piston_slammer` | Frappe le sol, repousse | `attack` avec `impact;` au contact du sol |
| Géomancien | `thorn_hedge` | Mur qui blesse quand on le frappe | `hurt` |
| Géomancien | `mud_totem` | Enlise par à-coups | `attack` (pulsation) |
| Géomancien | `geyser` | Projette en l'air | `attack` avec `impact;` au jaillissement |
| Géomancien | `boulder_thrower` | Gros rocher sur le plus costaud | `attack`, os de visée |
| Géomancien | `crystal_spire` | Éclair qui rebondit | `attack`, os de visée facultatif |
| Géomancien | `lifestone` | Soigne les joueurs proches | `attack` (quand elle soigne) |

## 3. Les projectiles et les effets

Aujourd'hui, une tourelle fait ses dégâts directement et lance une **flèche vanilla** pour l'image. La boule de bowling
est déjà une vraie entité (`BowlingBallEntity`), comme le tir des armes de héros (`WeaponProjectileEntity`). Il faut
donc surtout des **modèles** de projectiles (GeckoLib, ou un modèle fixe s'il ne bouge pas), que Dungeon Defenders
fait voler.

**Ceux de l'Atelier** : merci pour les 8. Ceux qui servent tout de suite sont `fleche_elfe_noir` (archer) et le sort du
mage. Les autres partent avec leurs monstres, on s'en servira quand on les branchera.

**À faire, pour les tours :**

| Projectile ou effet | Tour | Remarque |
|---|---|---|
| `harpon` | `harpoon_turret` | Tir tendu |
| `boule_bowling` | `bowling_ball_turret` | Roule au sol, tourne sur elle-même |
| `obus_mortier` | `mortar_turret` | Trajectoire en cloche ; une explosion au sol |
| `carreau_repetiteur` | `repeater_turret` | Petit et rapide |
| `carreau_long` | `longshot_turret` | Gros, avec une traînée |
| `rocher` | `boulder_thrower` | Gros, tourne en vol |
| `fiole` | `flask_launcher` | En cloche ; une flaque au sol (décor plat, ou particules) |
| `jet_acide` | `acid_sprayer` | Plutôt des particules qu'un projectile |
| `eclair` | `crystal_spire` | Un segment d'éclair entre deux points (texture ou particules), pas un projectile |

**Pour les armes des héros** (facultatif, plus tard) : `rivet` (pistolet de l'Ingénieur), `fiole_alchimiste`,
`orbe_geomancien`.

## 4. Comment les recevoir

- **Monstres et projectiles** : comme tu as dit, `atelier_monstres/` et les fichiers GeckoLib, sur `atelier/modeles`.
- **Tours** : oui, propose un **format de fiche de tour** dans l'Atelier ; on lira ce que tu décides, comme pour les
  blocs animés. Ce dont on a besoin dans la fiche :
  - `id` (celui de la tour), `modele`, `animations`, `texture`, et le glowmask à côté s'il existe ;
  - `roles_animation` (rôle → animation), avec les rôles des tableaux ci-dessus ;
  - l'**os de visée** (nom, pivot, lacet et tangage permis ou non) ;
  - la **bouche du tir** : un locator dans le modèle, d'où part le projectile ;
  - le **projectile** utilisé, s'il y en a un ;
  - les sons.
  Pas de stats : les dégâts, la portée et la cadence restent dans les JSON de tour de Dungeon Defenders
  (`data/dungeon_defenders/dungeon_defenders/tower/`).
- **Dossiers proposés** : la fiche dans `data/dungeon_defenders/atelier_tours/<id>.json`, et les fichiers dans
  `geckolib/models/block/`, `geckolib/animations/block/` et `textures/geckolib/block/`, comme les blocs animés. Si tu
  préfères un sous-dossier `tower/`, c'est très bien aussi.

## Ensuite, chez nous

1. Les monstres : faire pointer les JSON d'ennemi existants vers eux, ajouter les quatre nouveaux, puis le kamikaze,
   le vol et les tirs avec projectile.
2. Les tours : un renderer GeckoLib générique pour les tours, l'os de visée, les rôles, puis les projectiles.
3. Un gametest par rôle, et la liste à vérifier en jeu dans [06-a-tester.md](06-a-tester.md).
