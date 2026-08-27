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
    │   ├── Config.java                       # Spec de config COMMON (PV cristal, dégâts, portée), branchée
    │   ├── init/
    │   │   ├── ModBlocks.java                # DeferredRegister blocs + items
    │   │   ├── ModAttachments.java           # DeferredRegister des data attachments (mana, vagues, phase...)
    │   │   ├── ModMenus.java                 # DeferredRegister des MenuType (GUI de config)
    │   │   ├── GamePhase.java                # Enum des phases de partie (BUILD, COMBAT)
    │   │   ├── GameDifficulty.java           # Enum de difficulté (EASY, NORMAL, HARD)
    │   │   ├── DifficultyScaling.java        # Multiplicateur difficulté x vague, pour les spawners
    │   │   ├── SpawnableEnemy.java           # Liste fermée des ennemis choisissables dans un spawner
    │   │   ├── PhaseTransitions.java         # enterCombat/enterBuild : transitions de phase centralisées
    │   │   ├── GameMap.java                  # Liste des maps proposées dans l'écran de choix (visible = false pour masquer une map en cours de conception)
    │   │   ├── TowerDefinition.java          # Catalogue des tours posables via la roue (voir client/gui/screen/TowerWheelScreen.java)
    │   │   ├── ManaCrystalType.java           # Paliers de cristaux de mana (un seul pour l'instant, extensible)
    │   │   └── ModEntities.java               # DeferredRegister.Entities (premier Entity custom du mod : le cristal de mana)
    │   ├── menu/
    │   │   ├── SpawnerConfigMenu.java        # AbstractContainerMenu sans slot, transmet juste le BlockPos
    │   │   └── SpawnerConfigMenuProvider.java # MenuProvider ouvert par SpawnerBlock au clic droit
    │   ├── network/
    │   │   ├── SpawnerConfigPayload.java     # Paquet C2S (BlockPos + config du spawner)
    │   │   ├── SetDifficultyPayload.java     # Paquet C2S (difficulté choisie dans MapSelectionScreen)
    │   │   ├── StartGamePayload.java         # Paquet C2S (déclenche MapInstance.startGame, pas de champ)
    │   │   ├── PlaceTowerPayload.java        # Paquet C2S (tour + position + rotation, confirmation finale de la roue)
    │   │   └── ModNetworking.java            # Enregistrement des paquets custom (RegisterPayloadHandlersEvent)
    │   ├── client/
    │   │   ├── ModKeyMappings.java           # Touches roue des tours + rotation (RegisterKeyMappingsEvent)
    │   │   ├── TowerPlacementState.java      # État transitoire du mode pose (AIMING/ORIENTING, tour choisie, rotation)
    │   │   ├── TowerPlacementRenderState.java # Instantané pour le rendu de l'hologramme (ContextKey sur LevelRenderState)
    │   │   ├── TowerPlacementClientEvents.java # Ouverture roue, rayon de visée, rotation, confirmation, rendu hologramme/portée
    │   │   └── ClientDisplayConfig.java      # Spec de config CLIENT (options d'affichage HUD facultatives), branchée dans DungeonDefendersModClient
    │   ├── client/gui/screen/
    │   │   ├── SpawnerConfigScreen.java      # Écran de config du spawner (client uniquement)
    │   │   ├── MapSelectionScreen.java       # Écran de choix de map + difficulté (client uniquement, pas de Menu)
    │   │   └── TowerWheelScreen.java         # Roue radiale de sélection des tours (client uniquement, pas de Menu)
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
    │   ├── entity/
    │   │   ├── ManaCrystalEntity.java         # extends ExperienceOrb : drop de mana ramassable au sol, pas un item d'inventaire
    │   │   ├── MobHealthBarRenderer.java      # RenderLivingEvent.Post : barre de vie zombie/squelette, cachée à PV pleins/hors portée (client)
    │   │   └── ai/
    │   │       ├── AbstractEterniaCrystalAttackGoal.java # Base commune : ciblage/déplacement vers le cristal (un seul sous-classeur : la version à distance)
    │   │       ├── RangedAttackEterniaCrystalGoal.java   # Goal : s'arrêter à portée de tir et tirer des flèches sur le cristal (archers, ignorent Blockade/Turret)
    │   │       └── AttackPriorityTargetGoal.java         # Goal unique des monstres de mêlée : choisit Block > Corps à corps > Cristal > Tourelle selon AiAttackTarget
    │   ├── gametest/
    │   │   ├── DungeonDefendersGameTests.java # Fonctions de test + enregistrement (RegisterGameTestsEvent)
    │   │   └── ModGameTestInstance.java       # GameTestInstance custom (contourne Registries.TEST_FUNCTION, hors d'atteinte d'un mod)
    │   └── block/
    │       ├── EterniaCrystalBlock.java      # Le bloc : hitbox, interaction, codec
    │       ├── SpikeBlockadeBlock.java       # Premier tower "Blockade" : mur à PV qui pique au contact
    │       ├── HarpoonTurretBlock.java       # Premier tower "Turret" : HORIZONTAL_FACING + tir en cône
    │       ├── TowerBlockItem.java           # Item commun à toute tour (Blockade, Turret...) : useOn() ne pose plus rien (roue uniquement)
    │       ├── SpawnerBlock.java             # Fait spawn des ennemis en combat ; clic droit = bascule phase (test)
    │       ├── TavernCrystalBlock.java       # Pas de PV : ouvre MapSelectionScreen au clic droit
    │       └── entity/
    │           ├── EterniaCrystalBlockEntity.java          # État persistant (PV) + synchro client + AiAttackTarget (priorité cristal)
    │           ├── EterniaCrystalRenderState.java          # Instantané pour le rendu (client)
    │           ├── EterniaCrystalBlockEntityRenderer.java  # Barre de vie 3D, toujours affichée (client)
    │           ├── AiAttackTarget.java                     # Interface : contrat + paliers de priorité IA (Block/Corps à corps/Cristal/Tourelle)
    │           ├── AbstractTowerBlockEntity.java           # Base commune à TOUTE catégorie de tour : PV, coût mana, persistance, sync, AiAttackTarget (voir 02-gameplay.md)
    │           ├── AbstractBlockadeBlockEntity.java        # Catégorie "Blockade" : dégâts de contact optionnels, priorité selon dealsContactDamage
    │           ├── SpikeBlockadeBlockEntity.java           # Sous-classe : fixe les stats du Spike Blockade (voir 02-gameplay.md)
    │           ├── AbstractTurretBlockEntity.java          # Catégorie "Turret" : portée + cône + tir (scan/tir par tick, pas de Goal)
    │           ├── HarpoonTurretBlockEntity.java           # Sous-classe : fixe les stats du Harpoon Turret (voir 02-gameplay.md)
    │           ├── TowerHealthBarRenderState.java          # Instantané pour le rendu (client)
    │           ├── TowerHealthBarRenderer.java             # Barre de vie 3D générique à toute tour, cachée à PV pleins/hors portée (client)
    │           ├── HealthLerp.java                         # Animation temps réel d'un ratio de PV (client, partagée cristal/tours)
    │           ├── HealthBarRendering.java                 # Dessin du quad de barre de vie (client, partagée cristal/tours)
    │           ├── SpawnerBlockEntity.java                 # Algorithme de spawn pondéré (voir 02-gameplay.md)
    │           ├── SpawnerRenderState.java                 # Instantané pour le rendu (client)
    │           └── SpawnerBlockEntityRenderer.java         # Aperçu de composition en phase Construction, à travers les murs (client)
    ├── resources/
    │   ├── assets/dungeon_defenders/
    │   │   ├── lang/{en_us,fr_fr}.json                     # Traductions
    │   │   ├── blockstates/{eternia_crystal,spike_blockade,spawner,tavern_crystal}.json   # Variante unique par bloc
    │   │   ├── blockstates/harpoon_turret.json             # 4 variantes facing=north/east/south/west (comme la furnace vanilla)
    │   │   ├── models/block/{eternia_crystal,spike_blockade,spawner,tavern_crystal}.json  # Modèles (texture vanilla provisoire)
    │   │   ├── models/block/harpoon_turret.json             # parent minecraft:block/orientable, textures furnace (placeholder directionnel)
    │   │   ├── items/{eternia_crystal,spike_blockade,spawner,tavern_crystal,harpoon_turret}.json # Modèles d'item
    │   │   └── textures/gui/maps/<id>.png                  # Aperçu de chaque GameMap (une image par map)
    │   ├── data/dungeon_defenders/loot_table/blocks/{eternia_crystal,spike_blockade,spawner,tavern_crystal,harpoon_turret}.json
    │   ├── data/dungeon_defenders/structure/gametest/empty.nbt  # Gabarit 3x3x3 sans bloc, partagé par les gametests
    │   ├── data/minecraft/tags/block/             # mineable/pickaxe (+ needs_diamond_tool pour le cristal)
    │   └── data/minecraft/dimension/overworld.json # Remplace le générateur de l'Overworld par "The Void"
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
  NeoForge génère un écran de config depuis l'écran « Mods », **et**
  `container.registerConfig(CLIENT, ClientDisplayConfig.SPEC)` — contrairement à `Config.SPEC`
  (COMMON) enregistré depuis `DungeonDefendersMod`, ce spec-là ne doit jamais être chargé sur un
  serveur dédié, d'où l'enregistrement ici plutôt que là-bas.
- `@EventBusSubscriber(value = Dist.CLIENT)` + `onRegisterRenderers(EntityRenderersEvent.RegisterRenderers)` :
  enregistre le renderer de block entity du cristal.
- `onRegisterGuiLayers(RegisterGuiLayersEvent)` : enregistre `ManaOverlay`, `HealthOverlay`,
  `ExperienceOverlay`, `WaveOverlay`, `WaveEnemiesOverlay`, `PhaseOverlay`, `ScoreOverlay`,
  `ScoreGainOverlay`, `CharacterOverlay` et `AbilitySlotsOverlay` via
  `event.registerAboveAll(...)`, au-dessus de toutes les autres couches du HUD, et masque les
  cœurs, la faim, l'expérience et la hotbar vanilla via `event.replaceLayer(...)` — voir
  [02-gameplay.md](02-gameplay.md#le-hud-vanilla-masqué).
- `onRegisterMenuScreens(RegisterMenuScreensEvent)` : associe `ModMenus.SPAWNER_CONFIG` à
  `SpawnerConfigScreen::new`. Contrairement à `RegisterGuiLayersEvent`, ce n'est pas
  `MenuScreens.register(...)` qu'on appelle directement (privée dans cette version) mais cet
  événement, sur le même principe.
- `onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent)` : associe le handler de
  `ScoreGainPayload` (paquet clientbound) à `ScoreGainOverlay.INSTANCE` — le type/codec est
  enregistré côté partagé (`ModNetworking`), mais le handler ne peut vivre qu'ici, voir
  [02-gameplay.md](02-gameplay.md#le-gain-de-score-flottant--clientguiscoregainoverlayjava-networkscoregainpayloadjava).

> `@EventBusSubscriber` n'a pas de paramètre `bus` dans cette version : les événements qui
> implémentent `IModBusEvent` (comme `RegisterRenderers`) partent automatiquement sur le bus
> du mod, les autres sur `NeoForge.EVENT_BUS`. C'est ce qui permet à `ModEvents` et à cette
> classe d'utiliser la même annotation pour des bus différents.

## Chaîne d'enregistrement

```
Chargement FML
   └─ new DungeonDefendersMod(modEventBus, container)
        ├─ ModBlocks.register(bus)        → BLOCKS + ITEMS
        ├─ ModAttachments.register(bus)   → ATTACHMENT_TYPES
        ├─ ModMenus.register(bus)         → MENU_TYPES
        ├─ BLOCK_ENTITIES.register(bus)
        ├─ CREATIVE_MODE_TABS.register(bus)
        └─ container.registerConfig(COMMON, Config.SPEC)

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
   ├─ RegisterRenderers [client]       → EterniaCrystalBlockEntityRenderer, SpawnerBlockEntityRenderer, TowerHealthBarRenderer (Blockade + Turret)
   ├─ RegisterRenderStateModifiersEvent [client] → HEALTH/MAX_HEALTH/ENTITY_ID sur tout LivingEntityRenderState
   │                                                (lu par MobHealthBarRenderer, bus de jeu ci-dessous — pas
   │                                                un RenderLayer/AddLayers, voir 02-gameplay.md pour le pourquoi)
   ├─ RegisterGuiLayersEvent [client]  → ManaOverlay, HealthOverlay, ExperienceOverlay,
   │                                      WaveOverlay, WaveEnemiesOverlay, PhaseOverlay,
   │                                      ScoreOverlay, CharacterOverlay, AbilitySlotsOverlay
   ├─ RegisterMenuScreensEvent [client] → spawner_config -> SpawnerConfigScreen
   └─ RegisterGameTestsEvent           → eternia_crystal_damage, phase_transitions
                                          (DungeonDefendersGameTests, voir 05-etat-et-problemes-connus.md)

Bus de jeu (NeoForge.EVENT_BUS)
   ├─ MobHealthBarRenderer.onRenderLiving(RenderLivingEvent.Post) [client] → billboard de vie
   │    filtré sur zombie/squelette, lit HEALTH/MAX_HEALTH/ENTITY_ID posés ci-dessus
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

Aucun pour l'instant. `src/main/resources/META-INF/accesstransformer.cfg` élargissait la
visibilité de trois méthodes de `Display`/`Display.TextDisplay`, utilisées par une première
version (retirée) de l'affichage des PV basée sur une entité `TextDisplay` — supprimé le
2026-08-24, plus aucune classe du mod ne les utilisait (rendu custom depuis, voir
[02-gameplay.md](02-gameplay.md)). Si un futur besoin d'AT se présente : NeoForge détecte
automatiquement `src/main/resources/META-INF/accesstransformer.cfg`, aucune déclaration Gradle
n'est requise (voir `build.gradle`, ligne commentée `accessTransformers = ...`).
