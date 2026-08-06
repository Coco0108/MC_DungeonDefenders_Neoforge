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
- ✅ Bloc `spike_trap` + son item : 2 PV de dégâts à tout `Monster` qui marche dessus
  (`stepOn`), cooldown de 1 s par entité. Modèle, blockstate, loot table, tag `mineable/pickaxe`,
  traductions `en_us`/`fr_fr`, onglet créatif.

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
| `src/main/templates/META-INF/neoforge.mods.toml` | `description = "Example mod description."`, `authors` commenté — alors que `mod_description` et `mod_authors` existent dans `gradle.properties` mais ne sont pas dans `replaceProperties` de `build.gradle` |
| `Config.java` | spec d'exemple (`logDirtBlock`, `magicNumber`…) jamais enregistrée via `registerConfig` |
| `DungeonDefendersModClient` | enregistre un `IConfigScreenFactory` pour une config inexistante au runtime |
| `TEMPLATE_LICENSE.txt` | licence du template, à ne pas confondre avec la licence du mod (`All Rights Reserved`) |
| `accesstransformer.cfg` | élargit trois méthodes de `Display` que plus aucune classe n'utilise depuis le retrait du code `TextDisplay` |

## Pistes prioritaires

1. Créer les textures et vrais modèles du cristal et du piège à pics.
2. Renseigner `neoforge.mods.toml` et le `README.md` avec les vraies métadonnées.
3. Externaliser les constantes de gameplay (`DEFAULT_HEALTH`, `DAMAGE_PER_HIT`,
   `SEARCH_RANGE`) dans `Config`, et enregistrer la spec.
4. Retirer le harnais de test du clic droit quand une autre source de dégâts existera.
5. Étendre l'IA au-delà des zombies (le goal n'exige qu'un `PathfinderMob`).
