# 1. Architecture du projet

## Arborescence

```
MC_DungeonDefenders_Neoforge/
├── build.gradle                  # Plugin NeoGradle "moddev", toolchain Java 25, run configs
├── settings.gradle               # Résolution de toolchain (foojay)
├── gradle.properties             # Versions MC/NeoForge + métadonnées du mod (mod_id, version…)
├── .github/workflows/build.yml   # CI : ./gradlew build sur push et PR
├── doc/                          # ← cette documentation
└── src/main/
    ├── java/com/github/c0c0tier/dungeon_defenders/
    │   ├── DungeonDefendersMod.java          # Point d'entrée @Mod (commun)
    │   ├── DungeonDefendersModClient.java    # Point d'entrée @Mod côté CLIENT uniquement
    │   ├── ModEvents.java                    # Événements de jeu (IA des zombies)
    │   ├── Config.java                       # Spec de config (héritée du template, non branchée)
    │   ├── init/
    │   │   └── ModBlocks.java                # DeferredRegister blocs + items
    │   └── block/
    │       ├── EterniaCrystalBlock.java      # Le bloc : hitbox, interaction, codec
    │       └── entity/
    │           ├── EterniaCrystalBlockEntity.java          # État persistant (PV)
    │           └── EterniaCrystalBlockEntityRenderer.java  # Barre de vie 3D (client)
    ├── resources/
    │   ├── assets/dungeon_defenders/lang/en_us.json   # Traductions (encore celles du template)
    │   └── META-INF/accesstransformer.cfg             # Access Transformers
    └── templates/META-INF/neoforge.mods.toml          # Métadonnées, expansées par Gradle
```

## Les deux points d'entrée

NeoForge autorise plusieurs classes `@Mod` pour le même `modId`, différenciées par `dist`.

### `DungeonDefendersMod` (commun — client + serveur dédié)

- Déclare la constante `MODID = "dungeon_defenders"`.
- Possède deux `DeferredRegister` :
  - `BLOCK_ENTITIES` (`Registries.BLOCK_ENTITY_TYPE`)
  - `CREATIVE_MODE_TABS` (`Registries.CREATIVE_MODE_TAB`)
- Enregistre le `BlockEntityType` `eternia_crystal`, lié à `ModBlocks.ETERNIA_CRYSTAL`.
- Enregistre l'onglet créatif `dungeon_defenders_tab`, dont l'icône et le seul contenu
  sont l'item du cristal.
- Le constructeur `DungeonDefendersMod(IEventBus modEventBus)` branche les trois registres
  (`ModBlocks.register(...)`, `BLOCK_ENTITIES`, `CREATIVE_MODE_TABS`) sur le bus du mod.

> **Pourquoi les blocs sont-ils dans `init/ModBlocks` et pas ici ?**
> Pour casser la dépendance circulaire : le `BlockEntityType` a besoin d'une référence au
> bloc au moment de sa construction. En sortant les blocs dans une classe séparée, l'ordre
> d'initialisation des `static final` reste résoluble.

### `DungeonDefendersModClient` (`dist = Dist.CLIENT`)

Cette classe n'est jamais chargée sur un serveur dédié — le code client peut donc y être
référencé sans risque.

- Constructeur : enregistre le `IConfigScreenFactory` (`ConfigurationScreen::new`) pour que
  NeoForge génère un écran de config depuis l'écran « Mods ».
- `@EventBusSubscriber(value = Dist.CLIENT)` + `onClientSetup(FMLClientSetupEvent)` :
  enregistre le renderer de block entity du cristal.

## Chaîne d'enregistrement

```
Chargement FML
   └─ new DungeonDefendersMod(modEventBus)
        ├─ ModBlocks.register(bus)   → BLOCKS + ITEMS
        ├─ BLOCK_ENTITIES.register(bus)
        └─ CREATIVE_MODE_TABS.register(bus)

Événements du bus mod
   ├─ RegisterEvent(BLOCK)          → eternia_crystal (EterniaCrystalBlock)
   ├─ RegisterEvent(ITEM)           → eternia_crystal (BlockItem)
   ├─ RegisterEvent(BLOCK_ENTITY)   → eternia_crystal (BlockEntityType)
   ├─ RegisterEvent(CREATIVE_TAB)   → dungeon_defenders_tab
   └─ FMLClientSetupEvent [client]  → EterniaCrystalBlockEntityRenderer

Bus de jeu (NeoForge.EVENT_BUS)
   └─ ModEvents.onZombieSpawn(EntityJoinLevelEvent)
```

`ModEvents` est annoté `@EventBusSubscriber(modid = MODID)` sans `bus` explicite : il
s'abonne donc au **bus de jeu**, celui des événements runtime (`EntityJoinLevelEvent`).
`DungeonDefendersModClient` utilise le même mécanisme mais pour un événement de cycle de vie.

## Métadonnées & build

`gradle.properties` est la source de vérité unique. `build.gradle` définit une tâche
`generateModMetadata` qui copie `src/main/templates/` vers
`build/generated/sources/modMetadata/` en substituant les placeholders `${...}`
(`mod_id`, `mod_version`, `neo_version`, `minecraft_version_range`, `mod_license`,
`mod_name`). Le résultat est ajouté aux ressources de `sourceSets.main`.

**Conséquence pratique :** ne jamais éditer un `neoforge.mods.toml` généré dans `build/` —
éditer `src/main/templates/META-INF/neoforge.mods.toml`.

## Access Transformers

`src/main/resources/META-INF/accesstransformer.cfg` élargit la visibilité de trois méthodes
de `Display` / `Display.TextDisplay` :

```
public net.minecraft.world.entity.Display$TextDisplay setText(...)
public net.minecraft.world.entity.Display setBillboardConstraints(...)
public net.minecraft.world.entity.Display setViewRange(F)V
```

Elles servaient à l'affichage des PV via une entité `TextDisplay`. Ce code est aujourd'hui
commenté au profit d'un rendu custom (voir [02-gameplay.md](02-gameplay.md)) ; l'AT reste en
place. NeoForge détecte automatiquement ce fichier, aucune déclaration Gradle n'est requise.
