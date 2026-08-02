# 5. État du projet & problèmes connus

État à la version `0.0.1`. Constats issus de la lecture du code — le projet **n'a pas été
compilé** pour rédiger cette page, les points marqués « probable » restent à vérifier par un
`./gradlew build`.

## Ce qui est implémenté

- ✅ Bloc `eternia_crystal` + son item, hitbox 1×3×1, très résistant.
- ✅ Block entity avec PV persistants (100 par défaut).
- ✅ Destruction du bloc et message « Game Over » à 0 PV.
- ✅ IA : les zombies convergent sur le cristal dans un rayon de 16 blocs et infligent 5 PV/s.
- ✅ Onglet créatif dédié.
- ✅ Renderer de barre de vie 3D au-dessus du cristal.
- ✅ CI GitHub Actions.

## Bloquants

### 1. Le renderer ne compile pas

`EterniaCrystalBlockEntityRenderer` ne déclare qu'un constructeur
`(BlockEntityRendererProvider.Context context)`, mais
[`DungeonDefendersModClient.java:31`](../src/main/java/com/github/c0c0tier/dungeon_defenders/DungeonDefendersModClient.java#L31)
l'instancie sans argument :

```java
new EterniaCrystalBlockEntityRenderer()
```

Il n'existe aucun constructeur sans paramètre → erreur de compilation.

Au passage, la voie normale d'enregistrement d'un `BlockEntityRenderer` sous NeoForge est
l'événement `EntityRenderersEvent.RegisterRenderers` (`event.registerBlockEntityRenderer(type, Renderer::new)`),
qui fournit justement le `Context` attendu, plutôt qu'un appel direct à
`getBlockEntityRenderDispatcher().register(...)` dans `FMLClientSetupEvent`.

### 2. API de rendu probablement obsolète

`renderBar` utilise `consumer.vertex(...).color(...).uv2(...).endVertex()`. Ce style
(`endVertex()` en particulier) a été supprimé des versions récentes du moteur de rendu.
`RenderType.gui()` est par ailleurs destiné à l'interface 2D, pas au rendu dans le monde —
un type comme `RenderType.debugQuads()` / un render type custom serait plus adapté.

### 3. Les PV ne sont pas synchronisés vers le client

`EterniaCrystalBlockEntity` ne surcharge ni `getUpdatePacket()` ni `getUpdateTag()`. Or les
PV ne sont modifiés que côté serveur, tandis que le renderer lit `getCrystalHealth()` côté
client : **la barre de vie restera bloquée à 100 %**. Il faut ajouter la synchro :

```java
@Override public Packet<ClientGamePacketListener> getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
}
@Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { ... }
```

et appeler `level.sendBlockUpdated(...)` après chaque changement de PV.

## Bugs & fragilités

### `setCrystalHealth` — NPE potentiel et spam chat

[`EterniaCrystalBlockEntity.java:39`](../src/main/java/com/github/c0c0tier/dungeon_defenders/block/entity/EterniaCrystalBlockEntity.java#L39) :

```java
this.level.players().forEach(...)   // pas de garde `level != null`
```

Le bloc suivant teste pourtant `this.level != null` — l'ordre est incohérent, et le premier
appel déréférence `level` sans vérification.

Ce même bloc envoie un message dans le chat de **tous les joueurs du monde** à chaque
changement de PV : avec un zombie qui frappe chaque seconde, le chat devient inutilisable.
C'est du code de debug à retirer (ou à passer en `LOGGER.debug`).

Il n'y a pas non plus de garde `isClientSide()` sur cette diffusion.

### Les goals de zombie s'accumulent

`ModEvents.onZombieSpawn` réagit à `EntityJoinLevelEvent`, qui se déclenche aussi au
rechargement d'un chunk ou au changement de dimension — pas seulement au spawn initial. Un
même zombie peut donc recevoir plusieurs fois le goal, et frapper le cristal plusieurs fois
par seconde. Envisager un `EntityJoinLevelEvent` couplé à un marqueur persistant, ou
`FinalizeSpawnEvent`, ou un test avant ajout.

### `useWithoutItem` retourne toujours `SUCCESS`

Le retour final est `InteractionResult.SUCCESS` même quand rien ne s'est passé (côté client,
ou block entity absent). `InteractionResult.PASS` / `CONSUME` selon les cas serait plus juste.

Plus fondamentalement : infliger 10 PV au clic droit à main nue est un harnais de test qui ne
devrait pas rester dans une version jouable.

### Pas de garde sur les PV négatifs

`setCrystalHealth(currentHealth - 5)` peut descendre sous 0 ; `healthPercent` est clampé côté
renderer, mais la valeur stockée ne l'est pas. Un `Math.max(0, health)` dans le setter
éviterait des états incohérents.

## Ressources manquantes

### Traductions non faites

`assets/dungeon_defenders/lang/en_us.json` contient encore **uniquement** les clés du
template `examplemod`. Manquent :

```json
"itemGroup.dungeon_defenders": "Dungeon Defenders",
"block.dungeon_defenders.eternia_crystal": "Eternia Crystal",
```

Les clés `examplemod.*` présentes ne servent à rien et peuvent être supprimées.

### Aucun modèle ni texture

Il n'existe ni `blockstates/`, ni `models/`, ni `textures/`. Le bloc étant en
`RenderShape.MODEL`, il s'affichera en damier noir/violet. Voir
[04-guide-ajout-contenu.md](04-guide-ajout-contenu.md#ressources-nécessaires-par-bloc).

### Aucune loot table

Pas de `data/dungeon_defenders/loot_table/blocks/eternia_crystal.json` : le bloc, qui plus
est en `requiresCorrectToolForDrops()`, ne dropera jamais rien. Aucun tag `mineable/*` non
plus.

## Reliquats du template

| Fichier | Reliquat |
|---|---|
| `README.md` | encore le README du MDK NeoForge, ne parle pas du mod |
| `src/main/templates/META-INF/neoforge.mods.toml` | `description = "Example mod description."`, `authors` commenté — alors que `mod_description` et `mod_authors` existent dans `gradle.properties` mais ne sont pas dans `replaceProperties` de `build.gradle` |
| `Config.java` | spec d'exemple (`logDirtBlock`, `magicNumber`…) jamais enregistrée via `registerConfig` |
| `DungeonDefendersModClient` | enregistre un `IConfigScreenFactory` pour une config inexistante côté runtime |
| `TEMPLATE_LICENSE.txt` | licence du template, à ne pas confondre avec la licence du mod (`All Rights Reserved`) |
| `accesstransformer.cfg` | élargit trois méthodes de `Display` utilisées uniquement par du code commenté |

Le run `gameTestServer` est configuré mais aucun gametest n'existe : il plantera au lancement.

## Pistes prioritaires

1. Corriger l'enregistrement du renderer (bloquant à la compilation).
2. Ajouter la synchronisation client des PV, sinon la barre de vie est décorative.
3. Ajouter modèle, texture, loot table et traductions.
4. Retirer le code de debug (messages chat, dégâts au clic droit).
5. Renseigner `neoforge.mods.toml` et le `README.md` avec les vraies métadonnées.
6. Externaliser les constantes de gameplay dans `Config`.
