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
besoin de block entity : surcharger `stepOn` suffit. C'était le patron de l'ancien
`SpikeTrapBlock` (supprimé, remplacé par le Spike Blockade — un corps à corps attaquable,
catégorie 2 de la taxonomie des tours, voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md#système-de-tours-catégorie-blockade-démarrée)) ;
ce patron `stepOn` reste le bon point de départ pour les futurs **pièges de sol** (catégorie 5,
non attaquables) de cette taxonomie — aucun exemple actuel dans le code à recopier tel quel,
reconstruire à partir du squelette ci-dessous.

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

`ModEvents.onMonsterSpawn` filtre déjà sur `instanceof Monster` (toute hostilité vanilla ou
moddée) plutôt qu'un type précis comme `Zombie` — pas besoin de le généraliser davantage pour
un nouveau monstre standard. `PathfinderMob` (exigé par `MoveToBlockGoal`) serait plus large
mais couvrirait aussi des mobs passifs (animaux, villageois...), ce qui n'est pas voulu ici.

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
`src/generated/resources/`). `eternia_crystal`, `spike_blockade` et `harpoon_turret` (blocs
avec block entity) sont des exemples complets à recopier. Pour un nouveau membre d'une
catégorie de tour existante (mur à PV type "Blockade", ou tour à distance type "Turret" — voir
`05-etat-et-problemes-connus.md`), pas besoin de repartir de zéro :

- **Blockade** : étendre `block/entity/AbstractBlockadeBlockEntity.java` (stats seulement, voir
  `SpikeBlockadeBlockEntity`/`BouncerBlockadeBlockEntity`/`SliceNDiceBlockadeBlockEntity` comme
  modèles). Aucun tag à tenir à jour : `AttackPriorityTargetGoal` vise génériquement tout
  `AiAttackTarget` (donc tout `AbstractTowerBlockEntity`), pas une liste fermée — le tag
  `dungeon_defenders:blockades` et l'ancien `AttackBlockadeGoal` qui le lisait ont tous les deux
  été supprimés (voir 05-etat-et-problemes-connus.md, "Système de priorité IA").
- **Turret** : étendre `block/entity/AbstractTurretBlockEntity.java` (stats seulement, voir
  `HarpoonTurretBlockEntity` comme modèle — ou surcharger `fireAt` pour un comportement de tir
  différent d'"une flèche cosmétique + dégâts à une seule cible", voir
  `BowlingBallTurretBlockEntity`/`MortarTurretBlockEntity` pour deux exemples concrets, l'un
  avec une vraie collision perforante, l'autre avec des dégâts de zone) ; si la tour doit avoir
  une orientation, déclarer `BlockStateProperties.HORIZONTAL_FACING` sur le bloc (voir
  `HarpoonTurretBlock` — la rotation choisie dans la roue est déjà appliquée automatiquement par
  `ModNetworking.handlePlaceTower`, rien à faire côté réseau).
- Dans tous les cas, ajouter un membre à `init/TowerDefinition.java` pour que la tour
  apparaisse dans la roue (`TowerWheelScreen`) — c'est l'unique façon de la poser, voir
  `block/TowerBlockItem.java`.

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

## Ajouter un GUI de configuration (menu + écran + réseau)

Pour un écran ouvert par clic droit sur un bloc, qui édite l'état d'un `BlockEntity` (pas un
inventaire) : pas besoin de slots ni d'items, juste un menu-vecteur + un écran + un paquet
custom. Exemple complet à recopier : `menu/SpawnerConfigMenu.java`,
`menu/SpawnerConfigMenuProvider.java`, `network/SpawnerConfigPayload.java`,
`network/ModNetworking.java`, `client/gui/screen/SpawnerConfigScreen.java` (détaillé dans
[02-gameplay.md](02-gameplay.md)).

Les pièces, dans l'ordre où elles interviennent :

1. **`MenuProvider`** (un `record` suffit) : donné à `player.openMenu(...)` côté serveur dans
   le `useWithoutItem`/`use` du bloc. Porte les données nécessaires pour retrouver le bloc
   côté client (typiquement un `BlockPos`) et les écrit dans
   `writeClientSideData(menu, buf)` — la surcharge NeoForge de `MenuProvider`, pas une
   méthode vanilla.
2. **`AbstractContainerMenu`** minimal : deux constructeurs, un pour le serveur (reçoit les
   données directement), un pour le client (reçoit un `RegistryFriendlyByteBuf` et les
   relit). `quickMoveStack` peut renvoyer `ItemStack.EMPTY` et `stillValid` renvoyer `true`
   sans risque tant qu'il n'y a ni slot ni item — la vraie vérification se fait dans le
   handler du paquet (point 5).
3. **`MenuType`** : `IMenuTypeExtension.create(MonMenu::new)` (le constructeur "buffer"),
   enregistré via un `DeferredRegister<MenuType<?>>` sur `Registries.MENU`, même principe que
   `ModBlocks`/`ModAttachments`.
4. **Écran** : `class MonEcran extends Screen implements MenuAccess<MonMenu>` — pas besoin
   d'étendre `AbstractContainerScreen` si le menu n'a pas de slot, ça évite d'hériter du
   rendu du panneau d'inventaire vanilla. Widgets standards (`EditBox`, `Button.builder(...)`)
   ajoutés via `addRenderableWidget(...)` dans `init()`. **Attention au renommage de cette
   version** : le point d'entrée du rendu n'est pas `render(GuiGraphics, ...)` mais
   `extractRenderState(GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick)` —
   appeler `super.extractRenderState(...)` en premier dessine le fond et les widgets déjà
   ajoutés, comme le faisait `super.render(...)` avant. Enregistré côté client via
   `RegisterMenuScreensEvent` (pas `MenuScreens.register(...)`, privée dans cette version).
5. **Paquet C2S** : un `record` implémentant `CustomPacketPayload` (`type()`, un
   `Type<T>` déclaré avec `Identifier.fromNamespaceAndPath(...)`, un `StreamCodec` via
   `StreamCodec.composite(...)` — jusqu'à 7 champs avec les surcharges de cette version). Pour
   un champ de longueur variable (une liste), un des champs peut être
   `ByteBufCodecs.collection(ArrayList::new, elementCodec)` — voir `SpawnerConfigPayload.Entry`
   pour un exemple d'élément avec son propre petit `StreamCodec.composite`. Toujours **revalider
   les indices/ordinaux reçus** avant de s'en servir pour indexer un tableau côté serveur (voir
   `ModNetworking.handleSpawnerConfig`) — un client est une source non fiable.
   Enregistré dans une classe à part (**pas** dans la classe client) via
   `RegisterPayloadHandlersEvent` + `PayloadRegistrar#playToServer(type, codec, handler)` :
   un serveur dédié doit savoir décoder ce que ses clients lui envoient, donc cet
   enregistrement doit rester du code commun.
6. **Envoi depuis l'écran** : `Minecraft.getInstance().getConnection().send(payload.toVanillaServerbound())`
   — pas de helper `PacketDistributor.sendToServer(...)`, ces méthodes sont toutes serveur →
   client dans cette version.
7. **Handler côté serveur** : revérifier l'existence du bloc à la position reçue et la
   portée du joueur avant d'appliquer quoi que ce soit (voir `ModNetworking`) — le client est
   toujours considéré non fiable.

> **Règle absolue quel que soit le chemin choisi** : le bloc (chargé aussi sur un serveur
> dédié) ne doit **jamais** nommer l'écran, ni `Minecraft`, ni aucune classe
> `net.minecraft.client.*` — même dans une branche `if (level.isClientSide())` qui ne
> s'exécutera jamais côté serveur. Le mod refuserait de se charger sur un serveur dédié, voir
> [01-architecture.md](01-architecture.md#la-règle-clientserveur--nommer-une-classe-cliente-suffit-à-casser-un-serveur-dédié).
> Vérifier avec `python3 tools/verifier-dist.py build/libs/dungeon_defenders-<version>.jar`.

**Écran sans donnée propre au bloc** : si l'écran n'a besoin d'aucune information de *ce*
bloc-là (liste statique, attachment déjà synchronisé...), tout le mécanisme
`Menu`/`MenuProvider`/`MenuType` ci-dessus est inutile — un simple paquet **clientbound** sans
champ suffit : `StreamCodec.unit(INSTANCE)`, `registrar.playToClient(TYPE, STREAM_CODEC)` dans
`ModNetworking`, `event.register(TYPE, ...)` dans `DungeonDefendersModClient`, et le bloc fait
`serverPlayer.connection.send(MonPayload.INSTANCE.toVanillaClientbound())`. Exemple complet :
`network/OpenMapSelectionPayload.java` + `block/TavernCrystalBlock.java`.

**Nombre de lignes variable dans l'écran (ajouter/retirer)** : si l'écran doit permettre
d'ajouter/retirer des lignes de widgets (pas juste des champs fixes), garder l'état
(valeurs actuelles) dans des champs Java ordinaires de l'écran (pas seulement dans les
widgets), et reconstruire avec `Screen#clearWidgets()`/`rebuildWidgets()` à chaque
ajout/retrait — les widgets sont détruits à la reconstruction, donc tout ce qui doit survivre
doit déjà être recopié ailleurs juste avant (voir `SpawnerConfigScreen.syncFieldsToState()` et
la section correspondante de [02-gameplay.md](02-gameplay.md)). Un changement qui ne modifie
pas le nombre de lignes (ex. cycler la valeur d'un bouton) n'a pas besoin de tout reconstruire :
`AbstractWidget#setMessage(...)` suffit pour changer son libellé en place.

**Ajouter un ennemi choisissable dans le spawner** : une seule ligne dans
`init/SpawnableEnemy.java` (nom + `EntityType`), plus sa clé de traduction
(`dungeon_defenders.enemy.<nom>`) dans les deux fichiers de lang. Rien d'autre à toucher : le
GUI, le réseau et la persistance passent déjà par cet enum (voir 02-gameplay.md).

**Donner à un nouvel ennemi une attaque sur le cristal** : dans la grande majorité des cas,
**pas besoin d'écrire de nouvelle classe**. `AttackEterniaCrystalGoal` (corps à corps) et
`RangedAttackEterniaCrystalGoal` (distance) ont toutes les deux un constructeur
`(mob, damagePerHit, ticksBetweenX, ...)` — il suffit de brancher le bon type dans
`ModEvents.onMonsterSpawn` avec les chiffres voulus (voir
[02-gameplay.md](02-gameplay.md#ia-des-ennemis)) :

```java
if (monster instanceof AbstractSkeleton) {
    monster.goalSelector.addGoal(1, new RangedAttackEterniaCrystalGoal(monster, 3, 20, 10.0D));
} else {
    monster.goalSelector.addGoal(1, new AttackEterniaCrystalGoal(monster));
}
```

Sous-classer `AbstractEterniaCrystalAttackGoal` directement n'est nécessaire que pour un
**nouveau style d'attaque** (une attaque de zone, un effet de poison au contact, etc.) — dans
ce cas, implémenter `onReachedTarget(EterniaCrystalBlockEntity crystal)` (appelé chaque tick
tant que le mob est à portée) et éventuellement `onTargetLost()` (remise à zéro d'un état en
cours, comme l'annulation d'une tension d'arc dans la version à distance).

## Ajouter une option de configuration

[`Config.java`](../src/main/java/com/github/c0c0tier/dungeon_defenders/Config.java) est une
vraie spec (plus l'exemple du template), enregistrée dans le constructeur du mod :

```java
public DungeonDefendersMod(IEventBus modEventBus, ModContainer container) {
    ...
    container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
}
```

Génère `config/dungeon_defenders-common.toml` au premier lancement. Trois valeurs y sont déjà
externalisées : `defaultHealth` (PV du Cristal d'Eternia, 100 par défaut), `damagePerHit`
(dégâts de mêlée d'`AttackPriorityTargetGoal`, 5 par défaut) et `searchRange` (portée de
détection du cristal, mêlée et distance confondues, 16 par défaut). Pour en ajouter une :
déclarer le champ dans `Config.java` (`BUILDER.comment(...).defineInRange(...)` ou
`.define(...)` selon le type), puis lire sa valeur via `Config.MA_VALEUR.get()` là où c'était
une constante en dur.

**Cette config (`COMMON`) est chargée des deux côtés** (client et serveur dédié) : réservée aux
valeurs de gameplay, jamais à une préférence purement visuelle — voir la recette suivante pour
ça.

## Ajouter une option d'affichage HUD facultative (masquer/afficher un élément)

Pour une préférence propre à **ce joueur** (masquer un overlay, désactiver une animation...),
pas la config de gameplay ci-dessus : [`client/ClientDisplayConfig.java`](../src/main/java/com/github/c0c0tier/dungeon_defenders/client/ClientDisplayConfig.java)
est le spec dédié, type `CLIENT` plutôt que `COMMON` — un fichier **local**
(`config/dungeon_defenders-client.toml`), jamais lu ni synchronisé côté serveur. Exemple
existants : `showScoreGainPopup` (affichage du popup de `ScoreGainOverlay`) et
`showTowerBlockOutline` (contour de sélection sur les tours/cristaux, voir la recette « Masquer
le contour de sélection d'un bloc » plus bas — l'étape 3 y est différente, ce n'est pas un
`GuiLayer`).

1. Déclarer le champ dans `ClientDisplayConfig.java` :

   ```java
   public static final ModConfigSpec.BooleanValue SHOW_MON_TRUC = BUILDER
           .comment("Description affichée en tooltip dans l'écran de config.")
           .define("showMonTruc", true);
   ```

2. Clé de lang pour le libellé de l'option (sinon NeoForge affiche le nom du champ brut) :

   ```json
   "dungeon_defenders.configuration.showMonTruc": "Afficher mon truc"
   ```

3. Dans le `GuiLayer` concerné, tout en haut de `render(...)`, **après** les gardes existantes
   (`minecraft.level == null`, `minecraft.options.hideGui`...) mais avant tout calcul de
   dessin :

   ```java
   if (!ClientDisplayConfig.SHOW_MON_TRUC.get()) {
       return;
   }
   ```

   Si l'overlay fait aussi du travail utile indépendamment de l'affichage (ex.
   `ScoreGainOverlay.purgeExpired()`, qui doit tourner même masqué pour ne pas laisser une
   liste grossir indéfiniment), placer ce travail **avant** le `if`, pour qu'il continue de
   s'exécuter même quand l'option est décochée — seul le dessin doit être sauté.

**Rien à enregistrer côté réseau** : contrairement à `Config.SPEC`, ce spec ne concerne qu'un
seul client, aucun autre joueur de la partie ne le voit ni n'en dépend. Déjà branché dans le
constructeur de `DungeonDefendersModClient`
(`container.registerConfig(ModConfig.Type.CLIENT, ClientDisplayConfig.SPEC)`) — pas la peine d'y
retoucher pour une nouvelle option, seulement pour la toute première fois que ce fichier a été
créé.

## Masquer le contour de sélection d'un bloc (sans casser son ciblage)

Le contour noir filaire dessiné autour du bloc visé se supprime **au rendu**, dans
[`client/BlockOutlineClientEvents.java`](../src/main/java/com/github/c0c0tier/dungeon_defenders/client/BlockOutlineClientEvents.java) :
il suffit d'ajouter une condition à `hidesOutline(...)` (reconnaissance par block entity de
préférence — générique à toute une famille de blocs — sinon par classe de bloc).

> **Ne surtout pas passer par `getShape` → `Shapes.empty()`** pour ça. `getShape` est la forme
> lue par le rayon de visée du joueur : la vider ne masque pas seulement le contour, elle rend
> le bloc **impossible à viser et à cliquer** (`useWithoutItem` ne se déclenche plus, le mode
> suppression de tour ne le reconnaît plus). C'est un choix délibéré et documenté pour
> `SpawnerBlock` (introuvable en survie, voir [02-gameplay.md](02-gameplay.md)), mais c'est un
> bug pour tout bloc avec lequel le joueur doit encore interagir.

L'événement à utiliser est `ExtractBlockOutlineRenderStateEvent`, annulable — annulé, aucun
render state de contour n'est soumis. **`RenderHighlightEvent` n'existe plus** dans cette
version de NeoForge : les tutoriels qui le mentionnent sont périmés.

## Livrer la structure de la taverne (ou d'une map)

Côté **constructeur** (dans le jeu, en créatif) :

1. Construire la taverne **n'importe où sauf** autour de `MapInstance.MAP_POS`
   (10000, 65, 0) : cette zone est rasée à chaque démarrage de partie.
2. Poser le `tavern_crystal` dedans — sans lui, aucun accès à l'écran de choix de map.
3. Poser (optionnel) **un seul** `player_spawn` à l'endroit exact où les joueurs doivent
   arriver. Sans marqueur, l'arrivée retombe sur `TavernSpawn.SPAWN_POS`, c'est-à-dire le
   centre horizontal de la structure, un bloc au-dessus de sa couche la plus basse.
4. Encadrer le tout avec un **bloc de structure** vanilla en mode « Sauvegarder », puis
   récupérer le fichier écrit dans `<monde>/generated/minecraft/structures/<nom>.nbt`.

Côté **dépôt** : déposer le fichier en
`src/main/resources/data/dungeon_defenders/structure/tavern.nbt` — c'est le chemin que
`TavernSpawn.TAVERN_STRUCTURE` (`dungeon_defenders:tavern`) résout. Rien d'autre à changer :
si le fichier est là il est chargé, sinon le mod retombe sur sa plateforme de repli.

Deux contraintes à respecter à la construction :

- **Prévoir des murs ou un garde-corps** : le monde est vide, rien ne retient dans le vide en
  dehors de la structure.
- **La taille est libre**, la zone nettoyée avant de poser se calcule depuis
  `template.getSize()` — mais elle est reposée **entièrement** à chaque chargement du monde,
  blocs **et entités** : tout ce qu'un joueur y aurait modifié ou laissé traîner est écrasé au
  redémarrage. Les entités décoratives (cadres, supports à armure, tableaux) sont bien posées.

## Créer et publier une map

Tout se fait **en jeu**, sans toucher au code ni écrire de JSON.

1. Construire la map en créatif, **loin de (0, 65, 0) et de (10000, 65, 0)** — ces deux zones
   sont rasées, l'une à chaque chargement du monde, l'autre au lancement d'une partie.
2. Y poser : un `eternia_crystal` (l'objectif), au moins un `spawner` configuré, un
   `player_spawn` (point d'arrivée, consommé au démarrage), des marqueurs `no_build_zone` où
   la pose de tours doit être interdite, et un **`map_config`** (nom, ordre dans le pack,
   nombre de vagues, multiplicateur de score).
3. Encadrer le tout avec un **bloc de structure** vanilla en mode « Sauvegarder », en la
   nommant **`<namespace>:map/<id>`** — le namespace est l'identité du pack
   (`dungeon_defenders` pour la campagne, le sien pour une extension).
4. C'est fini : la map apparaît dans l'écran de choix du cristal de la taverne, sans
   redémarrage. Le fichier est écrit dans `<monde>/generated/<namespace>/structure/map/<id>.nbt`.

> **Le nom de sauvegarde est le seul identifiant.** Le bloc de configuration ne le connaît pas
> et ne peut donc pas l'afficher — c'est pour ça que la suppression d'une map demande de le
> saisir à la main dans son écran.

### Publier

Une map créée en jeu vit dans la sauvegarde du monde : elle ne part pas avec le mod. Pour la
distribuer, copier son `.nbt` dans les ressources d'un jar :

```
mon_pack.jar
├── META-INF/neoforge.mods.toml      (dépendance à dungeon_defenders)
├── data/mon_pack/structure/map/ma_map.nbt
└── assets/mon_pack/textures/gui/maps/ma_map.png     (aperçu 128x72)
```

**Aucun code dans le jar.** Il est découvert automatiquement (voir `MapRegistry`). Le nom
affiché du pack peut venir d'un `assets/mon_pack/lang/fr_fr.json` contenant
`"dungeon_defenders.map_pack.mon_pack": "Mon Pack"` ; sans ça c'est le namespace qui s'affiche.

Un datapack seul fonctionne pour la structure mais ne peut pas embarquer l'image d'aperçu.

> **Piège** : une structure du monde est cherchée **avant** celle d'un jar. Une map publiée puis
> re-sauvegardée en jeu sous le même identifiant est remplacée par la version locale tant qu'on
> ne la supprime pas de la sauvegarde.
