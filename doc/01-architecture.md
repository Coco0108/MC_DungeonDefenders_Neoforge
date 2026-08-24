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
    │   ├── TavernSpawn.java                  # Point de spawn fixe + plateforme provisoire (monde vide)
    │   ├── MapInstance.java                  # "La map active" : emplacement partagé, placeholder, téléportation
    │   ├── ModCommands.java                  # /dd_leave (retour à la taverne, harnais de test)
    │   ├── Config.java                       # Spec de config (héritée du template, non branchée)
    │   ├── init/
    │   │   ├── ModBlocks.java                # DeferredRegister blocs + items
    │   │   ├── ModBlockTags.java              # TagKey<Block> BLOCKADES, ciblé par AttackBlockadeGoal
    │   │   ├── ModAttachments.java           # DeferredRegister des data attachments (mana, vagues, phase...)
    │   │   ├── ModMenus.java                 # DeferredRegister des MenuType (GUI de config)
    │   │   ├── GamePhase.java                # Enum des phases de partie (BUILD, COMBAT)
    │   │   ├── GameDifficulty.java           # Enum de difficulté (EASY, NORMAL, HARD)
    │   │   ├── DifficultyScaling.java        # Multiplicateur difficulté x vague, pour les spawners
    │   │   ├── SpawnableEnemy.java           # Liste fermée des ennemis choisissables dans un spawner
    │   │   ├── PhaseTransitions.java         # enterCombat/enterBuild : transitions de phase centralisées
    │   │   └── GameMap.java                  # Liste des maps proposées dans l'écran de choix (visible = false pour masquer une map en cours de conception)
    │   ├── menu/
    │   │   ├── SpawnerConfigMenu.java        # AbstractContainerMenu sans slot, transmet juste le BlockPos
    │   │   └── SpawnerConfigMenuProvider.java # MenuProvider ouvert par SpawnerBlock au clic droit
    │   ├── network/
    │   │   ├── SpawnerConfigPayload.java     # Paquet C2S (BlockPos + config du spawner)
    │   │   ├── SetDifficultyPayload.java     # Paquet C2S (difficulté choisie dans MapSelectionScreen)
    │   │   ├── StartGamePayload.java         # Paquet C2S (déclenche MapInstance.startGame, pas de champ)
    │   │   └── ModNetworking.java            # Enregistrement des paquets custom (RegisterPayloadHandlersEvent)
    │   ├── client/gui/screen/
    │   │   ├── SpawnerConfigScreen.java      # Écran de config du spawner (client uniquement)
    │   │   └── MapSelectionScreen.java       # Écran de choix de map + difficulté (client uniquement, pas de Menu)
    │   ├── client/gui/
    │   │   ├── HudLayout.java                # Constantes de mise en page du groupe bas-gauche (mana/vie/exp)
    │   │   ├── DiamondGauge.java             # Dessine une jauge en forme de losange (fill() empilés, sans texture)
    │   │   ├── CircleSlot.java               # Dessine un rond (fond + bordure) (fill() empilés, sans texture)
    │   │   ├── ManaOverlay.java              # Losange mana, bas gauche (client uniquement)
    │   │   ├── HealthOverlay.java            # Losange vie, bas gauche (client uniquement)
    │   │   ├── ExperienceOverlay.java        # Barre horizontale expérience custom, bas gauche (client uniquement)
    │   │   ├── WaveOverlay.java              # Couche HUD affichant la vague en cours (client uniquement)
    │   │   ├── WaveEnemiesOverlay.java       # Couche HUD affichant les ennemis tués/total, haut centre (client uniquement)
    │   │   ├── PhaseOverlay.java             # Couche HUD affichant la phase (construction/combat) (client uniquement)
    │   │   ├── ScoreOverlay.java             # Couche HUD affichant le score de la carte, bas centre (client uniquement)
    │   │   ├── CharacterOverlay.java         # Couche HUD affichant "Nom - niv X", bas centre (client uniquement)
    │   │   └── AbilitySlotsOverlay.java      # 4 emplacements de compétences, bas gauche, à côté des losanges (client uniquement)
    │   ├── entity/ai/
    │   │   ├── AbstractEterniaCrystalAttackGoal.java # Base commune : ciblage/déplacement vers le cristal
    │   │   ├── AttackEterniaCrystalGoal.java         # Goal : converger vers le cristal et le frapper (corps à corps)
    │   │   ├── RangedAttackEterniaCrystalGoal.java   # Goal : s'arrêter à portée de tir et tirer des flèches sur le cristal
    │   │   └── AttackBlockadeGoal.java               # Goal (priorité 0) : détourne un ennemi de mêlée vers tout bloc du tag blockades
    │   └── block/
    │       ├── EterniaCrystalBlock.java      # Le bloc : hitbox, interaction, codec
    │       ├── SpikeBlockadeBlock.java       # Premier tower "Blockade" : mur à PV qui pique au contact
    │       ├── SpawnerBlock.java             # Fait spawn des ennemis en combat ; clic droit = bascule phase (test)
    │       ├── TavernCrystalBlock.java       # Pas de PV : ouvre MapSelectionScreen au clic droit
    │       └── entity/
    │           ├── EterniaCrystalBlockEntity.java          # État persistant (PV) + synchro client
    │           ├── EterniaCrystalRenderState.java          # Instantané pour le rendu (client)
    │           ├── EterniaCrystalBlockEntityRenderer.java  # Barre de vie 3D (client)
    │           ├── AbstractBlockadeBlockEntity.java        # Base commune catégorie "Blockade" : PV, coût mana, dégâts de contact optionnels (voir 02-gameplay.md)
    │           ├── SpikeBlockadeBlockEntity.java           # Sous-classe : fixe les stats du Spike Blockade (voir 02-gameplay.md)
    │           ├── SpawnerBlockEntity.java                 # Algorithme de spawn pondéré (voir 02-gameplay.md)
    │           ├── SpawnerRenderState.java                 # Instantané pour le rendu (client)
    │           └── SpawnerBlockEntityRenderer.java         # Aperçu de composition en phase Construction, à travers les murs (client)
    ├── resources/
    │   ├── assets/dungeon_defenders/
    │   │   ├── lang/{en_us,fr_fr}.json                     # Traductions
    │   │   ├── blockstates/{eternia_crystal,spike_blockade,spawner,tavern_crystal}.json   # Variante unique par bloc
    │   │   ├── models/block/{eternia_crystal,spike_blockade,spawner,tavern_crystal}.json  # Modèles (texture vanilla provisoire)
    │   │   ├── items/{eternia_crystal,spike_blockade,spawner,tavern_crystal}.json         # Modèles d'item
    │   │   └── textures/gui/maps/<id>.png                  # Aperçu de chaque GameMap (une image par map)
    │   ├── data/dungeon_defenders/loot_table/blocks/{eternia_crystal,spike_blockade,spawner,tavern_crystal}.json
    │   ├── data/dungeon_defenders/tags/block/blockades.json # Blocs ciblés par AttackBlockadeGoal (spike_blockade pour l'instant)
    │   ├── data/minecraft/tags/block/             # mineable/pickaxe (+ needs_diamond_tool pour le cristal)
    │   ├── data/minecraft/dimension/overworld.json # Remplace le générateur de l'Overworld par "The Void"
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
- Enregistre les `BlockEntityType` `eternia_crystal`, `spawner` et `spike_blockade`, liés à
  `ModBlocks.ETERNIA_CRYSTAL`/`ModBlocks.SPAWNER`/`ModBlocks.SPIKE_BLOCKADE`.
- Enregistre l'onglet créatif `dungeon_defenders_tab`, dont l'icône et le seul contenu
  sont l'item du cristal.
- Le constructeur `DungeonDefendersMod(IEventBus modEventBus)` branche les cinq registres
  (`ModBlocks.register(...)`, `ModAttachments.register(...)`, `ModMenus.register(...)`,
  `BLOCK_ENTITIES`, `CREATIVE_MODE_TABS`) sur le bus du mod.

> `init/ModAttachments.java` et `init/ModMenus.java` suivent le même principe que
> `ModBlocks` : un `DeferredRegister` dédié (`NeoForgeRegistries.Keys.ATTACHMENT_TYPES` /
> `Registries.MENU`) avec sa propre méthode `register(IEventBus)`. Voir
> [02-gameplay.md](02-gameplay.md) pour le détail de l'attachment `mana`, et plus bas pour
> `ModMenus`.

> **`network/ModNetworking.java`** est une classe à part, ni dans `DungeonDefendersMod` ni
> dans `DungeonDefendersModClient` : elle écoute `RegisterPayloadHandlersEvent`
> (`@EventBusSubscriber(modid = MODID)`, sans `bus` — part sur le bus du mod comme les
> événements `IModBusEvent`), et **doit** rester dans du code commun : un serveur dédié a
> besoin de savoir décoder les paquets envoyés par ses clients, donc ce n'est pas du code
> client-only comme `DungeonDefendersModClient`.

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
- `onRegisterGuiLayers(RegisterGuiLayersEvent)` : enregistre `ManaOverlay`, `HealthOverlay`,
  `ExperienceOverlay`, `WaveOverlay`, `WaveEnemiesOverlay`, `PhaseOverlay`, `ScoreOverlay`,
  `CharacterOverlay` et `AbilitySlotsOverlay` via `event.registerAboveAll(...)`, au-dessus de
  toutes les autres couches du HUD, et masque les cœurs, la faim, l'expérience et la hotbar
  vanilla via
  `event.replaceLayer(...)` — voir [02-gameplay.md](02-gameplay.md#le-hud-vanilla-masqué).
- `onRegisterMenuScreens(RegisterMenuScreensEvent)` : associe `ModMenus.SPAWNER_CONFIG` à
  `SpawnerConfigScreen::new`. Contrairement à `RegisterGuiLayersEvent`, ce n'est pas
  `MenuScreens.register(...)` qu'on appelle directement (privée dans cette version) mais cet
  événement, sur le même principe.

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
        ├─ ModMenus.register(bus)         → MENU_TYPES
        ├─ BLOCK_ENTITIES.register(bus)
        └─ CREATIVE_MODE_TABS.register(bus)

Événements du bus mod
   ├─ RegisterEvent(BLOCK)             → eternia_crystal, spike_blockade, spawner, tavern_crystal
   ├─ RegisterEvent(ITEM)              → eternia_crystal, spike_blockade, spawner, tavern_crystal (BlockItem)
   ├─ RegisterEvent(ATTACHMENT_TYPE)   → mana, experience, current_wave,
   │                                      wave_enemies_total, wave_enemies_killed, game_phase,
   │                                      score, level, character_name, difficulty,
   │                                      combat_session, active_spawners, ready
   ├─ RegisterEvent(MENU)              → spawner_config (MenuType)
   ├─ RegisterEvent(BLOCK_ENTITY)      → eternia_crystal, spawner (BlockEntityType)
   ├─ RegisterEvent(CREATIVE_TAB)      → dungeon_defenders_tab
   ├─ RegisterPayloadHandlersEvent     → SpawnerConfigPayload, SetDifficultyPayload,
   │                                      StartGamePayload (C2S, ModNetworking — commun, pas client-only)
   ├─ RegisterRenderers [client]       → EterniaCrystalBlockEntityRenderer, SpawnerBlockEntityRenderer
   ├─ RegisterGuiLayersEvent [client]  → ManaOverlay, HealthOverlay, ExperienceOverlay,
   │                                      WaveOverlay, WaveEnemiesOverlay, PhaseOverlay,
   │                                      ScoreOverlay, CharacterOverlay, AbilitySlotsOverlay
   └─ RegisterMenuScreensEvent [client] → spawner_config -> SpawnerConfigScreen

Bus de jeu (NeoForge.EVENT_BUS)
   ├─ ModEvents.onMonsterSpawn(EntityJoinLevelEvent)
   ├─ ModEvents.onPlayerJoin(EntityJoinLevelEvent)
   ├─ ModEvents.onMonsterDeath(LivingDeathEvent)
   │    └─ si wave_enemies_killed >= wave_enemies_total :
   │         PhaseTransitions.enterBuild(level) (vague suivante)
   │         ou PhaseTransitions.onVictory(level) si c'était déjà MAX_WAVE
   ├─ TavernSpawn.onLevelLoad(LevelEvent.Load)
   │    └─ si Overworld : fixe le point de spawn (0,65,0) + pose la plateforme provisoire
   └─ ModCommands.onRegisterCommands(RegisterCommandsEvent)
        └─ enregistre /dd_leave -> MapInstance.returnToTavern(level)

EterniaCrystalBlockEntity#setCrystalHealth, à 0 PV :
   └─ level.destroyBlock(...) + message + PhaseTransitions.onDefeat(level)

Chaque SpawnerBlockEntity, en plus de ces événements :
   ├─ setLevel(...)/setRemoved()   → s'ajoute/se retire de ModAttachments.ACTIVE_SPAWNERS
   └─ BlockEntityTicker [serveur]  → SpawnerBlockEntity.serverTick(...), une fois par tick de bloc

Clic droit sur un SpawnerBlock, sans shift, en créatif uniquement (voir 02-gameplay.md) :
   └─ player.openMenu(SpawnerConfigMenuProvider)
        ├─ Serveur : SpawnerConfigMenuProvider#writeClientSideData -> écrit le BlockPos
        └─ Client : IContainerFactory reconstruit SpawnerConfigMenu, RegisterMenuScreensEvent
                     ouvre SpawnerConfigScreen -> au clic sur "Valider", envoie
                     SpawnerConfigPayload -> ModNetworking l'applique via
                     SpawnerBlockEntity.applyConfig(...)

Clic droit sur l'EterniaCrystalBlock, en Construction (voir 02-gameplay.md) :
   └─ EterniaCrystalBlock#toggleReady -> bascule ready (joueur), diffuse la progression
        └─ si tous les joueurs de la Level sont prêts : PhaseTransitions.enterCombat(level)

Clic droit sur un TavernCrystalBlock (voir 02-gameplay.md) :
   └─ Client uniquement : Minecraft.getInstance().setScreen(new MapSelectionScreen())
        └─ au clic sur "Jouer", envoie deux paquets (dans cet ordre) :
             SetDifficultyPayload -> ModNetworking l'applique à ModAttachments.DIFFICULTY
             StartGamePayload -> ModNetworking appelle MapInstance.startGame(level)
                  (nettoie l'emplacement de map, pose le placeholder, téléporte tout le monde)

Lien "Retour à la taverne" (messages de victoire/défaite, voir PhaseTransitions) :
   └─ ClickEvent.RunCommand("/dd_leave") -> ModCommands -> MapInstance.returnToTavern(level)
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
