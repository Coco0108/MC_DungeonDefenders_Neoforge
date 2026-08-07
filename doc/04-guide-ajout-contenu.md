# 4. Guide : ajouter du contenu

Recettes basées sur les conventions déjà en place dans le projet.

## Conventions

- Tout ID de registre est en `snake_case` minuscule (`eternia_crystal`).
- Les constantes `DeferredHolder` sont en `SCREAMING_SNAKE_CASE` et portent le même nom que
  l'ID (`ETERNIA_CRYSTAL`).
- Les registres vivent dans `init/` (`ModBlocks`), sauf `BLOCK_ENTITIES` et
  `CREATIVE_MODE_TABS` encore dans `DungeonDefendersMod`.
- Les classes de blocs vont dans `block/`, les block entities dans `block/entity/`, les
  goals d'IA dans `entity/ai/`.
- Les messages destinés aux joueurs passent par `Component.translatable` et une clé dans les
  fichiers de langue, jamais par `Component.literal`.

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

## Ajouter un piège (dégâts au contact)

Pour un bloc qui blesse une entité quand elle marche dessus (piège, sol dangereux…), pas
besoin de block entity : surcharger `stepOn` suffit. Exemple complet à recopier :
[`block/SpikeTrapBlock.java`](../src/main/java/com/github/c0c0tier/dungeon_defenders/block/SpikeTrapBlock.java).

```java
@Override
public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
    if (level.isClientSide()) {
        return;
    }
    if (entity instanceof Monster monster && monster.isAlive()) {
        // ... cooldown, puis monster.hurt(level.damageSources().xxx(), degats);
    }
    super.stepOn(level, pos, state, entity);
}
```

Points importants :

- `stepOn` se déclenche quand l'entité **marche sur** le bloc (comme `MagmaBlock`) —
  `entityInside` est réservé aux blocs avec lesquels l'entité **chevauche** le volume (cactus,
  ronces).
- Toujours sortir si `level.isClientSide()` : la logique de dégâts ne doit tourner que côté
  serveur.
- Pour un cooldown par entité (éviter que le piège déclenche à chaque tick), stocker le dernier
  `level.getGameTime()` de déclenchement dans un `WeakHashMap<Entity, Long>` en champ
  d'instance du bloc (le bloc est un singleton) ; comparer au tick courant avant de redéclencher.
  `WeakHashMap` évite de retenir des entités mortes/déchargées.
- Piocher la `DamageSource` la plus proche du thème dans `DamageSources`
  (`stalagmite()`, `hotFloor()`, `cactus()`…) plutôt que `generic()`.
- Filtrer avec `instanceof Monster` pour ne viser que les monstres hostiles ; élargir à
  `LivingEntity` pour toucher aussi le joueur.

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

Si l'état doit être visible côté client (barre de vie, animation…), ajouter aussi la
synchronisation — sans elle le client garde la valeur par défaut :

```java
@Override public Packet<ClientGamePacketListener> getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
}

@Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
    return this.saveWithoutMetadata(registries);
}
```

et appeler `level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL)` après chaque mutation.

## Ajouter un renderer de block entity

Depuis 26.1 un renderer ne voit plus le block entity au moment du rendu : il faut passer par
un état intermédiaire.

1. Créer un `XxxRenderState extends BlockEntityRenderState` qui porte les données à afficher.
   Ne pas l'annoter `@OnlyIn` : cette annotation est réservée à Minecraft et NeoForge, son
   effet n'existe plus au runtime et NeoForge log un avertissement pour tout mod qui
   l'utilise. La sûreté côté serveur vient du fait que la classe n'est référencée que depuis
   du code client.
2. Implémenter `BlockEntityRenderer<MonBlockEntity, XxxRenderState>` avec les trois méthodes :
   - `createRenderState()` → une instance neuve ;
   - `extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress)` —
     appeler `BlockEntityRenderer.super.extractRenderState(...)` puis remplir l'état ;
   - `submit(state, poseStack, collector, camera)` — soumettre la géométrie.
3. Le constructeur doit prendre un `BlockEntityRendererProvider.Context`.
4. Enregistrer dans `DungeonDefendersModClient` :

   ```java
   @SubscribeEvent
   static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
       event.registerBlockEntityRenderer(DungeonDefendersMod.MA_TOUR_BE.get(), MonRenderer::new);
   }
   ```

Pour de la géométrie brute, utiliser
`collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> ...)` ; le
`VertexConsumer` s'alimente avec `addVertex(pose, x, y, z).setColor(...)` — `endVertex()`
n'existe plus. Pour un billboard, `poseStack.mulPose(camera.orientation)` suffit (attention :
après cette rotation `+Y` pointe vers le bas).

Le renderer ne doit jamais être référencé depuis une classe chargée sur serveur dédié.

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

Un bloc en `RenderShape.MODEL` a besoin de :

```
src/main/resources/assets/dungeon_defenders/
├── blockstates/<id>.json    # { "variants": { "": { "model": "dungeon_defenders:block/<id>" } } }
├── models/block/<id>.json   # { "parent": "minecraft:block/cube_all", "textures": { "all": ... } }
├── items/<id>.json          # { "model": { "type": "minecraft:model", "model": "..." } }
└── textures/block/<id>.png
```

> Attention : depuis 1.21.4 le modèle d'item se déclare dans `assets/<ns>/items/<id>.json`
> (nouveau système de modèles d'items), et non plus dans `models/item/`.

Sans ces fichiers, le bloc s'affiche en damier noir/violet « missing model ».

Côté data pack :

```
src/main/resources/data/dungeon_defenders/
└── loot_table/blocks/<id>.json     # sinon le bloc ne drope rien
```

Noter le dossier `loot_table` au **singulier** depuis 1.21. Et comme le bloc utilise
`requiresCorrectToolForDrops()`, il faut l'ajouter à
`data/minecraft/tags/block/mineable/<outil>.json` et au tag de niveau d'outil correspondant
(`needs_diamond_tool`, `needs_iron_tool`…), avec `"replace": false`.

Tous ces fichiers peuvent aussi être générés par datagen (`./gradlew runData`, sortie dans
`src/generated/resources/`). `eternia_crystal` (bloc avec block entity) et `spike_trap`
(bloc simple) sont deux exemples complets à recopier.

## Ajouter une donnée persistante sur le joueur (data attachment)

Pour une ressource ou un état porté par le joueur (mana, stamina…) plutôt que par un bloc,
pas besoin de mixin ni de capability custom : les **data attachments** NeoForge suffisent.
Exemple complet à recopier : [`init/ModAttachments.java`](../src/main/java/com/github/c0c0tier/dungeon_defenders/init/ModAttachments.java)
(attachment `mana`).

```java
public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, DungeonDefendersMod.MODID);

public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> MA_RESSOURCE = ATTACHMENT_TYPES.register(
        "ma_ressource",
        () -> AttachmentType.builder(() -> VALEUR_PAR_DEFAUT)
                .serialize(Codec.INT.fieldOf("MaRessource"))
                .sync(ByteBufCodecs.VAR_INT)
                .build());
```

Points importants :

- Enregistrer `ATTACHMENT_TYPES` sur le bus du mod (méthode `register(IEventBus)`, appelée
  depuis `DungeonDefendersMod`, comme pour `ModBlocks`).
- `.serialize(MapCodec<T>)` rend l'attachment persistant (survit au rechargement) ; l'omettre
  pour une donnée purement transiente.
- `.sync(StreamCodec<...>)` pousse la valeur au client propriétaire — indispensable si un
  HUD ou un écran doit l'afficher. Après toute mutation côté serveur, appeler
  `entity.setData(ATTACHMENT, valeur)` **puis** `entity.syncData(ATTACHMENT)` (la synchro
  n'est pas automatique à chaque `setData`).
- Lecture : `entity.getData(ATTACHMENT)` (l'holder implémente `Supplier`, pas besoin de
  `.get()` sur l'attachment lui-même contrairement à un `DeferredBlock`).

## Ajouter une couche HUD (GuiLayer)

Pour afficher une info permanente à l'écran (barre de ressource, indicateur…), utiliser une
`GuiLayer` plutôt qu'un mixin sur `Gui`. Exemple complet :
[`client/gui/ManaOverlay.java`](../src/main/java/com/github/c0c0tier/dungeon_defenders/client/gui/ManaOverlay.java).

```java
public class MonOverlay implements GuiLayer {
    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        guiGraphics.text(minecraft.font, Component.translatable("..."), x, y, 0xFFFFFF);
        guiGraphics.fill(x, y, x + largeur, y + hauteur, couleurArgb);
    }
}
```

Puis l'enregistrer dans `DungeonDefendersModClient` (méthode `@SubscribeEvent` sur
`RegisterGuiLayersEvent`, qui part sur le bus du mod comme `RegisterRenderers`) :

```java
@SubscribeEvent
static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
    event.registerAboveAll(Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "mon_overlay"), new MonOverlay());
}
```

`registerAboveAll` est le plus simple (aucune dépendance d'ordre) ; `registerAbove` /
`registerBelow` permettent de se positionner par rapport à une couche vanilla précise
(voir les constantes de `VanillaGuiLayers`, ex. `HOTBAR`, `PLAYER_HEALTH`) si l'overlay doit
s'insérer dans l'empilement du HUD plutôt que flotter au-dessus.

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
