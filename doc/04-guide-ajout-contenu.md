# 4. Guide : ajouter du contenu

Recettes basées sur les conventions déjà en place dans le projet.

## Conventions

- Tout ID de registre est en `snake_case` minuscule (`eternia_crystal`).
- Les constantes `DeferredHolder` sont en `SCREAMING_SNAKE_CASE` et portent le même nom que
  l'ID (`ETERNIA_CRYSTAL`).
- Les registres vivent dans `init/` (`ModBlocks`), sauf `BLOCK_ENTITIES` et
  `CREATIVE_MODE_TABS` encore dans `DungeonDefendersMod`.
- Les classes de blocs vont dans `block/`, les block entities dans `block/entity/`.

## Ajouter un bloc simple

Dans [`init/ModBlocks.java`](../src/main/java/com/github/c0c0tier/dungeon_defenders/init/ModBlocks.java) :

```java
public static final DeferredBlock<Block> MA_TOUR = BLOCKS.registerBlock("ma_tour",
        Block::new,
        properties -> properties
                .destroyTime(3.0F)
                .requiresCorrectToolForDrops());

public static final DeferredItem<BlockItem> MA_TOUR_ITEM =
        ITEMS.registerSimpleBlockItem("ma_tour", MA_TOUR);
```

> `registerBlock` et `registerSimpleBlockItem` appliquent automatiquement l'ID de registre sur
> les `Properties`, ce qui est **obligatoire depuis 1.21.3**. Ne pas construire les
> `BlockBehaviour.Properties` à la main.

Puis l'ajouter à l'onglet créatif dans `DungeonDefendersMod.DUNGEON_DEFENDERS_TAB` :

```java
.displayItems((parameters, output) -> {
    output.accept(ModBlocks.ETERNIA_CRYSTAL_ITEM.get());
    output.accept(ModBlocks.MA_TOUR_ITEM.get());
})
```

Enfin, créer les ressources (voir [Ressources](#ressources-nécessaires-par-bloc) plus bas).

## Ajouter un block entity

1. Créer la classe dans `block/entity/`, étendant `BlockEntity`, avec un constructeur
   `(BlockPos, BlockState)` qui passe le `BlockEntityType` via le holder :

   ```java
   super(DungeonDefendersMod.MA_TOUR_BE.get(), pos, state);
   ```

2. Implémenter la persistance avec l'API `ValueOutput` / `ValueInput` :

   ```java
   @Override protected void saveAdditional(ValueOutput output) {
       super.saveAdditional(output);
       output.putInt("MaValeur", this.maValeur);
   }

   @Override protected void loadAdditional(ValueInput input) {
       super.loadAdditional(input);
       this.maValeur = input.getIntOr("MaValeur", VALEUR_PAR_DEFAUT);
   }
   ```

   Appeler `setChanged()` après chaque mutation, sinon l'état n'est pas sauvegardé.

3. Faire étendre `BaseEntityBlock` au bloc, exposer un `MapCodec` via `simpleCodec(...)` et
   surcharger `newBlockEntity`.

4. Enregistrer le type dans `DungeonDefendersMod` :

   ```java
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MaTourBlockEntity>> MA_TOUR_BE =
           BLOCK_ENTITIES.register("ma_tour", () -> new BlockEntityType<>(
                   MaTourBlockEntity::new,
                   ModBlocks.MA_TOUR.get()
           ));
   ```

   ⚠️ Le bloc doit être déclaré dans `ModBlocks` (et pas dans `DungeonDefendersMod`) pour
   éviter la boucle d'initialisation statique décrite en [01-architecture.md](01-architecture.md).

## Ajouter un renderer de block entity

Créer la classe dans `block/entity/`, implémenter `BlockEntityRenderer`, puis l'enregistrer
côté client uniquement, dans `DungeonDefendersModClient`. Le renderer ne doit jamais être
référencé depuis une classe chargée sur serveur dédié.

## Ajouter un comportement sur les entités

Ajouter une méthode `static` annotée `@SubscribeEvent` dans
[`ModEvents.java`](../src/main/java/com/github/c0c0tier/dungeon_defenders/ModEvents.java).
La classe est déjà abonnée au bus de jeu via `@EventBusSubscriber(modid = MODID)`.

Toujours filtrer sur le côté serveur pour la logique de gameplay :

```java
if (!event.getLevel().isClientSide()) { ... }
```

Pour cibler d'autres monstres que les zombies, généraliser le test
`event.getEntity() instanceof Zombie` en `instanceof Monster` (ou `PathfinderMob` pour un
`MoveToBlockGoal`).

## Ajouter une traduction

Dans `src/main/resources/assets/dungeon_defenders/lang/en_us.json`. Clés attendues :

| Clé | Pour |
|---|---|
| `block.dungeon_defenders.<id>` | nom d'un bloc |
| `item.dungeon_defenders.<id>` | nom d'un item |
| `itemGroup.dungeon_defenders` | titre de l'onglet créatif |
| `dungeon_defenders.configuration.<option>` | libellé d'une option de config |

Pour ajouter une langue, créer `fr_fr.json` dans le même dossier.

## Ressources nécessaires par bloc

Le projet n'en contient **aucune** pour l'instant. Un bloc en `RenderShape.MODEL` a besoin de :

```
src/main/resources/assets/dungeon_defenders/
├── blockstates/<id>.json
├── models/block/<id>.json
├── models/item/<id>.json
└── textures/block/<id>.png
```

Sans eux, le bloc s'affiche en damier noir/violet « missing model ». Ces fichiers peuvent
être écrits à la main ou générés par datagen (`./gradlew runData`, sortie dans
`src/generated/resources/`).

Côté data pack, prévoir aussi :

```
src/main/resources/data/dungeon_defenders/
└── loot_table/blocks/<id>.json     # sinon le bloc ne drope rien
```

et, comme le bloc utilise `requiresCorrectToolForDrops()`, une entrée dans les tags
`minecraft:mineable/<outil>` et le tag de niveau d'outil correspondant.

## Ajouter une option de configuration

[`Config.java`](../src/main/java/com/github/c0c0tier/dungeon_defenders/Config.java) contient
la spec d'exemple du template. Pour l'utiliser réellement, il faut l'enregistrer dans le
constructeur du mod — ce n'est **pas** fait aujourd'hui :

```java
public DungeonDefendersMod(IEventBus modEventBus, ModContainer modContainer) {
    ...
    modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
}
```

Une bonne première option serait d'externaliser les constantes actuellement en dur
(`DEFAULT_HEALTH = 100`, dégâts de `5`, portée de recherche `16`).
