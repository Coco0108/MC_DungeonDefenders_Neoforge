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
    │   ├── ModEvents.java                    # Événements de jeu (attribution de l'IA, vie max du joueur)
    │   ├── Config.java                       # Spec de config (héritée du template, non branchée)
    │   ├── init/
    │   │   ├── ModBlocks.java                # DeferredRegister blocs + items
    │   │   └── ModAttachments.java           # DeferredRegister des data attachments (mana du joueur)
    │   ├── client/gui/
    │   │   ├── ManaOverlay.java              # Couche HUD affichant le mana (client uniquement)
    │   │   └── HealthOverlay.java            # Couche HUD affichant la vie (client uniquement)
    │   ├── entity/ai/
    │   │   └── AttackEterniaCrystalGoal.java # Goal : converger vers le cristal et le frapper
    │   └── block/
    │       ├── EterniaCrystalBlock.java      # Le bloc : hitbox, interaction, codec
    │       ├── SpikeTrapBlock.java           # Piège : dégâts au contact (stepOn) + cooldown
    │       └── entity/
    │           ├── EterniaCrystalBlockEntity.java          # État persistant (PV) + synchro client
    │           ├── EterniaCrystalRenderState.java          # Instantané pour le rendu (client)
    │           └── EterniaCrystalBlockEntityRenderer.java  # Barre de vie 3D (client)
    ├── resources/
    │   ├── assets/dungeon_defenders/
    │   │   ├── lang/{en_us,fr_fr}.json                     # Traductions
    │   │   ├── blockstates/{eternia_crystal,spike_trap}.json   # Variante unique par bloc
    │   │   ├── models/block/{eternia_crystal,spike_trap}.json  # Modèles (texture vanilla provisoire)
    │   │   └── items/{eternia_crystal,spike_trap}.json         # Modèles d'item
    │   ├── data/dungeon_defenders/loot_table/blocks/{eternia_crystal,spike_trap}.json
    │   ├── data/minecraft/tags/block/             # mineable/pickaxe (+ needs_diamond_tool pour le cristal)
    │   └── META-INF/accesstransformer.cfg         # Access Transformers
    └── templates/META-INF/neoforge.mods.toml      # Métadonnées, expansées par Gradle
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
- Le constructeur `DungeonDefendersMod(IEventBus modEventBus)` branche les quatre registres
  (`ModBlocks.register(...)`, `ModAttachments.register(...)`, `BLOCK_ENTITIES`,
  `CREATIVE_MODE_TABS`) sur le bus du mod.

> `init/ModAttachments.java` suit le même principe que `ModBlocks` : un `DeferredRegister`
> dédié (ici `NeoForgeRegistries.Keys.ATTACHMENT_TYPES`) avec sa propre méthode
> `register(IEventBus)`. Voir [02-gameplay.md](02-gameplay.md) pour le détail de l'attachment
> `mana`.

> **Pourquoi les blocs sont-ils dans `init/ModBlocks` et pas ici ?**
> Pour casser la dépendance circulaire : le `BlockEntityType` a besoin d'une référence au
> bloc au moment de sa construction. En sortant les blocs dans une classe séparée, l'ordre
> d'initialisation des `static final` reste résoluble.

### `DungeonDefendersModClient` (`dist = Dist.CLIENT`)

Cette classe n'est jamais chargée sur un serveur dédié — le code client peut donc y être
référencé sans risque.

- Constructeur : enregistre le `IConfigScreenFactory` (`ConfigurationScreen::new`) pour que
  NeoForge génère un écran de config depuis l'écran « Mods ».
- `@EventBusSubscriber(value = Dist.CLIENT)` + `onRegisterRenderers(EntityRenderersEvent.RegisterRenderers)` :
  enregistre le renderer de block entity du cristal.
- `onRegisterGuiLayers(RegisterGuiLayersEvent)` : enregistre `ManaOverlay` et `HealthOverlay`
  via `event.registerAboveAll(...)`, au-dessus de toutes les autres couches du HUD.

> `@EventBusSubscriber` n'a pas de paramètre `bus` dans cette version : les événements qui
> implémentent `IModBusEvent` (comme `RegisterRenderers`) partent automatiquement sur le bus
> du mod, les autres sur `NeoForge.EVENT_BUS`. C'est ce qui permet à `ModEvents` et à cette
> classe d'utiliser la même annotation pour des bus différents.

## Chaîne d'enregistrement

```
Chargement FML
   └─ new DungeonDefendersMod(modEventBus)
        ├─ ModBlocks.register(bus)        → BLOCKS + ITEMS
        ├─ ModAttachments.register(bus)   → ATTACHMENT_TYPES
        ├─ BLOCK_ENTITIES.register(bus)
        └─ CREATIVE_MODE_TABS.register(bus)

Événements du bus mod
   ├─ RegisterEvent(BLOCK)             → eternia_crystal (EterniaCrystalBlock)
   ├─ RegisterEvent(ITEM)              → eternia_crystal (BlockItem)
   ├─ RegisterEvent(ATTACHMENT_TYPE)   → mana
   ├─ RegisterEvent(BLOCK_ENTITY)      → eternia_crystal (BlockEntityType)
   ├─ RegisterEvent(CREATIVE_TAB)      → dungeon_defenders_tab
   ├─ RegisterRenderers [client]       → EterniaCrystalBlockEntityRenderer
   └─ RegisterGuiLayersEvent [client]  → ManaOverlay, HealthOverlay

Bus de jeu (NeoForge.EVENT_BUS)
   ├─ ModEvents.onZombieSpawn(EntityJoinLevelEvent)
   └─ ModEvents.onPlayerJoin(EntityJoinLevelEvent)
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

Elles servaient à une première version de l'affichage des PV, basée sur une entité
`TextDisplay`. Ce code a été retiré au profit d'un rendu custom (voir
[02-gameplay.md](02-gameplay.md)) : **l'AT n'est plus utilisé par aucune classe du mod**. Il
est conservé pour ne pas fermer la porte à cette approche, mais il peut être supprimé sans
conséquence. NeoForge détecte automatiquement ce fichier, aucune déclaration Gradle n'est
requise.
