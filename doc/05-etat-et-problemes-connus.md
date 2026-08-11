# 5. État du projet & problèmes connus

État à la version `0.0.1`. Le build (`./gradlew build`) **passe** — c'est aussi ce que
vérifie la CI.

## Ce qui est implémenté

- ✅ Bloc `eternia_crystal` + son item, hitbox 1×3×1, très résistant.
- ✅ Block entity avec PV persistants (100 par défaut) **et synchronisés vers le client**.
- ✅ Destruction du bloc et message « Game Over » à 0 PV.
- ✅ IA : les zombies convergent sur le cristal dans un rayon de 16 blocs et infligent 5 PV/s.
- ✅ Onglet créatif dédié.
- ✅ Renderer de barre de vie 3D au-dessus du cristal (API `submit` de 26.1).
- ✅ Modèle, blockstate, loot table, tags d'outil, traductions `en_us` et `fr_fr`.
- ✅ CI GitHub Actions.
- ✅ `neoforge.mods.toml` renseigné avec les vraies métadonnées (`mod_authors`,
  `mod_description` ajoutés à `replaceProperties` dans `build.gradle`).
- ✅ Bloc `spike_trap` + son item : 2 PV de dégâts à tout `Monster` qui marche dessus
  (`stepOn`), cooldown de 1 s par entité. Modèle, blockstate, loot table, tag `mineable/pickaxe`,
  traductions `en_us`/`fr_fr`, onglet créatif.
- ✅ Mana du joueur : data attachment `mana` (persistant, synchronisé), maximum par défaut de
  100, affiché en HUD via `ManaOverlay` — colonne verticale en bas à gauche de l'écran, très
  provisoire. Testable en jeu avec l'item `mana_test_wand` (clic droit = -10 mana).
- ✅ Vie du joueur : maximum vanilla porté de 20 à 100 (`ModEvents.onPlayerJoin`), affichée en
  HUD via `HealthOverlay` — colonne verticale juste à droite de celle du mana, même style.
- ✅ Expérience custom du joueur : data attachment `experience` (persistant, synchronisé),
  démarre à `0/100` (contrairement au mana/à la vie qui démarrent pleins), affichée en HUD
  via `ExperienceOverlay` — barre horizontale tout en bas, sous les colonnes mana/vie. Sans
  rapport avec l'XP vanilla. Le groupe des trois est décrit dans
  [02-gameplay.md](02-gameplay.md#le-groupe-bas-gauche--mana-vie-expérience).
- ✅ Vague en cours : data attachment `current_wave` sur la `Level` (persistant, synchronisé,
  démarre à 1), affichée en haut à droite (`Vague X/5`) via `WaveOverlay` — texte seul, pas de
  jauge. Aucun déroulement de vagues n'existe encore.
- ✅ Progression de la vague : `wave_enemies_killed`/`wave_enemies_total` (mêmes garanties que
  `current_wave`), affichée sur la même rangée, juste à gauche de `Vague X/Y`, via
  `WaveEnemiesOverlay` (jauge orange + texte `Ennemis : X/Y`, en miroir de
  `ExperienceOverlay`). Démarre à `0/10`, rien ne fait encore
  varier ni les tués ni le total.
- ✅ Phase de la partie : data attachment `game_phase` sur la `Level` (ordinal de l'enum
  `GamePhase` : `BUILD`/`COMBAT`), démarre en `BUILD`, affichée juste sous la rangée
  vague/ennemis via `PhaseOverlay` (`Phase : Construction`). Aucune transition n'existe
  encore.
- ✅ HUD vanilla masqué (cœurs, faim, expérience, hotbar) au profit d'une interface custom —
  voir [02-gameplay.md](02-gameplay.md#le-hud-vanilla-masqué).

## Corrections apportées

Les points suivants figuraient dans la première version de cette page et sont réglés.

| Problème | Correction |
|---|---|
| Le renderer ne compilait pas : `new EterniaCrystalBlockEntityRenderer()` sans argument | enregistrement via `EntityRenderersEvent.RegisterRenderers`, qui fournit le `Context` |
| L'interface `BlockEntityRenderer` avait changé (`render` → `submit`) | portage complet sur le trio `createRenderState` / `extractRenderState` / `submit` |
| `VertexConsumer.vertex(...).endVertex()` n'existe plus | `addVertex(pose, x, y, z).setColor(...)` via `submitCustomGeometry` |
| `RenderType.gui()` (type 2D) utilisé dans le monde | `RenderTypes.debugQuads()` — quads non texturés, translucides, non cullés |
| Barre de vie figée à 100 % côté client | `getUpdatePacket` + `getUpdateTag` + `sendBlockUpdated` |
| NPE potentiel : `this.level.players()` sans garde | sortie anticipée si `level == null` ou côté client |
| Chat inondé à chaque changement de PV | diffusion supprimée ; il ne reste que le message de destruction |
| PV pouvant descendre sous 0 | `Math.max(0, health)` dans le setter |
| `useWithoutItem` renvoyait toujours `SUCCESS` | `SUCCESS` côté client, `PASS` si le block entity est absent |
| Goals de zombie cumulés à chaque `EntityJoinLevelEvent` | test `anyMatch(... instanceof AttackEterniaCrystalGoal)` avant ajout |
| Cadence de frappe basée sur `mob.tickCount` | cooldown porté par le goal, remis à zéro quand le mob s'éloigne |
| Dégradé de couleur faux : jaune à 100 % de PV | `red = (1 - p) * 2` au-dessus de 50 % → vert pur à pleine vie |
| Aucun modèle, blockstate ni loot table | ajoutés ; le bloc se drope et se mine à la pioche en diamant |
| `en_us.json` ne contenait que les clés `examplemod` | remplacées par les vraies clés, plus un `fr_fr.json` |

Le goal des zombies a par ailleurs été sorti de `ModEvents` (classe anonyme) vers
`entity/ai/AttackEterniaCrystalGoal.java`, ce qui rend le test anti-doublon possible.

## Ce qui reste

### Pas de texture dédiée

`models/block/eternia_crystal.json` pointe sur `minecraft:block/diamond_block`. Le bloc est
donc visible et cohérent, mais ressemble à un bloc de diamant. Il faut créer
`textures/block/eternia_crystal.png` et mettre à jour le modèle — idéalement un modèle de
cristal, pas un cube plein, puisque la hitbox fait déjà 3 blocs de haut.

Même situation pour `models/block/spike_trap.json`, qui pointe sur
`minecraft:block/dripstone_block` en attendant `textures/block/spike_trap.png`.

### Le clic droit endommage encore le cristal

`useWithoutItem` retire 10 PV : c'est le harnais de test qui a servi à développer la
mécanique. Il est conservé volontairement (c'est le seul moyen simple de tester sans faire
spawner un zombie), mais il n'a rien à faire dans une version jouable.

### Le mana n'a pas de vraie utilité de gameplay

`ManaTestWandItem` retire 10 de mana au clic droit, mais c'est un harnais de test au même
titre que le clic droit sur le cristal : aucun sort ni capacité réelle ne consomme de mana,
et il n'y a pas de régénération, donc le mana ne remonte jamais une fois dépensé (à part en
se reconnectant, puisque l'attachment n'est pas remis à `MAX_MANA` ailleurs qu'à sa création).
Prochaines étapes logiques : une vraie capacité qui consomme du mana, une régénération
passive (tick côté serveur, borné à `MAX_MANA`), puis retrait de la baguette de test.

### Faim et hotbar masqués sans remplacement

`FOOD_LEVEL` et `HOTBAR` sont masqués (voir
[02-gameplay.md](02-gameplay.md#le-hud-vanilla-masqué)) mais rien ne les remplace encore : le
joueur ne voit plus sa faim, ni l'objet qu'il a en main/sa barre d'objets. Tant qu'un
équivalent custom n'existe pas, c'est une vraie perte d'information en jeu, pas seulement
esthétique — à garder en tête en testant (voir [06-a-tester.md](06-a-tester.md)).

### L'expérience custom n'a pas de vraie utilité de gameplay

Comme le mana à ses débuts : l'attachment `experience` existe et s'affiche, mais rien ne le
fait varier — pas de source de gain, pas de système de niveaux. Reste `0/100` en permanence
tant que ça n'existe pas.

### Les vagues ne se déroulent pas

`current_wave` existe et s'affiche (`1/5`), mais rien ne le fait avancer : pas de
déclenchement automatique/manuel, pas de condition pour passer à la vague suivante, pas de
victoire à la vague 5 ni de défaite si le cristal tombe avant. C'est un compteur statique pour
l'instant. Même chose pour `wave_enemies_killed`/`wave_enemies_total` : aucun mob n'est
généré pour une vague, rien n'incrémente les tués, le total (`10`) n'est jamais recalculé
selon la vague ou la difficulté. Idem pour `game_phase` : reste bloqué sur `BUILD`, rien ne
fait passer en `COMBAT` ni ne revient en `BUILD` entre deux vagues.

### `game_phase` stocke un ordinal d'enum, pas un nom stable

`ModAttachments.GAME_PHASE` sérialise `GamePhase.ordinal()` (0 pour `BUILD`, 1 pour
`COMBAT`). Si l'ordre des constantes de `GamePhase` change un jour (insertion d'une phase
avant `COMBAT`, par exemple), les sauvegardes existantes se retrouveront avec la mauvaise
phase au chargement. Pas un problème tant qu'on ajoute des valeurs à la fin de l'enum, mais à
garder en tête — voir [02-gameplay.md](02-gameplay.md#la-phase-de-la-partie--clientguiphaseoverlayjava).

### Le HUD du mana, de la vie et de l'expérience n'a pas de vrai visuel

`ManaOverlay`, `HealthOverlay` et `ExperienceOverlay` dessinent du texte et des rectangles
pleins (`guiGraphics.fill`), sans texture ni alignement avec le reste du HUD (hotbar, XP,
faim…) : c'est un placeholder assumé, à remplacer par des sprites une fois le reste de l'UI
défini. Ils sont aussi positionnés en `registerAboveAll` à des coordonnées fixes (bas gauche,
via les constantes de `HudLayout` — voir
[02-gameplay.md](02-gameplay.md#le-groupe-bas-gauche--mana-vie-expérience)), sans tenir
compte de `Gui.leftHeight`/`rightHeight` comme le fait le HUD vanilla pour empiler les barres
sans se chevaucher.

### Le rendu n'est pas interpolé

`extractRenderState` reçoit `partialTicks` mais ne s'en sert pas : la barre saute d'un palier
à l'autre à chaque coup encaissé. Une interpolation entre l'ancienne et la nouvelle valeur
rendrait l'effet plus lisible.

### Aucun gametest

Le run `gameTestServer` est configuré dans `build.gradle` mais aucun gametest n'existe : il
plantera au lancement. La CI ne l'exécute pas (`./gradlew build` seulement).

## Reliquats du template

| Fichier | Reliquat |
|---|---|
| `README.md` | encore le README du MDK NeoForge, ne parle pas du mod |
| `Config.java` | spec d'exemple (`logDirtBlock`, `magicNumber`…) jamais enregistrée via `registerConfig` |
| `DungeonDefendersModClient` | enregistre un `IConfigScreenFactory` pour une config inexistante au runtime |
| `TEMPLATE_LICENSE.txt` | licence du template, à ne pas confondre avec la licence du mod (`All Rights Reserved`) |
| `accesstransformer.cfg` | élargit trois méthodes de `Display` que plus aucune classe n'utilise depuis le retrait du code `TextDisplay` |

## Pistes prioritaires

1. Créer les textures et vrais modèles du cristal et du piège à pics.
2. Renseigner le `README.md` avec les vraies métadonnées (le `neoforge.mods.toml` est fait).
3. Externaliser les constantes de gameplay (`DEFAULT_HEALTH`, `DAMAGE_PER_HIT`,
   `SEARCH_RANGE`) dans `Config`, et enregistrer la spec.
4. Retirer le harnais de test du clic droit quand une autre source de dégâts existera.
5. Étendre l'IA au-delà des zombies (le goal n'exige qu'un `PathfinderMob`).
6. Donner une vraie utilité au mana (un sort/une capacité qui le consomme, une régénération
   passive), retirer `ManaTestWandItem`, puis remplacer le HUD provisoire de `ManaOverlay`,
   `HealthOverlay` et `ExperienceOverlay` par un vrai visuel, aligné sur le reste du HUD.
7. Concevoir et implémenter les remplacements custom de la faim et de la hotbar (masquées
   mais vides pour l'instant).
8. Définir un vrai système d'expérience/niveaux qui alimente `ModAttachments.EXPERIENCE`
   (aujourd'hui bloqué à `0/100`, comme le mana avant `ManaTestWandItem`).
9. Définir le déroulement des vagues (déclenchement, génération des ennemis, condition de
   passage à la suivante, victoire à la dernière vague, défaite si le cristal tombe avant) et
   faire avancer `ModAttachments.CURRENT_WAVE`/`WAVE_ENEMIES_TOTAL`/`WAVE_ENEMIES_KILLED`/
   `GAME_PHASE` en conséquence (transition `BUILD` → `COMBAT` au déclenchement d'une vague,
   retour à `BUILD` une fois la vague nettoyée).
