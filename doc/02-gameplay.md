# 2. Gameplay & mécaniques

Boucle de jeu visée : le joueur pose un **Cristal d'Eternia**, les monstres convergent vers
lui et le frappent, le joueur doit les en empêcher. À 0 PV, partie perdue.

## Le Cristal d'Eternia

### Le bloc — `block/EterniaCrystalBlock.java`

Étend `BaseEntityBlock` (bloc porteur d'un block entity).

| Aspect | Implémentation |
|---|---|
| Codec | `simpleCodec(EterniaCrystalBlock::new)` — obligatoire depuis 1.20.5 |
| Block entity | `newBlockEntity` → `new EterniaCrystalBlockEntity(pos, state)` |
| Render shape | `RenderShape.MODEL` (modèle JSON classique) |
| Collision | `Shapes.box(0, 0, 0, 1, 3, 1)` — 1×3×1, le cristal est infranchissable sur 3 blocs |
| Sélection | même boîte 1×3×1 (contour de visée aligné sur la collision) |

Propriétés définies à l'enregistrement dans `ModBlocks` :

- `destroyTime(50.0F)` — très long à miner à la main
- `explosionResistance(1200.0F)` — quasi immunisé aux explosions (comparable à l'obsidienne)
- `requiresCorrectToolForDrops()` — pas de drop sans le bon outil

### Interaction joueur

`useWithoutItem` (clic droit à main nue) retire **10 PV** au cristal et envoie un message au
joueur. C'est un harnais de test, pas une mécanique définitive : dans le jeu final, seuls les
monstres devraient endommager le cristal.

Le client renvoie immédiatement `SUCCESS` (prédiction, animation de bras) ; la logique ne
tourne que côté serveur, et `PASS` est renvoyé si le block entity est absent.

### L'état — `block/entity/EterniaCrystalBlockEntity.java`

```java
public static final int DEFAULT_HEALTH = 100;
private int crystalHealth = DEFAULT_HEALTH;
```

- `getCrystalHealth()` : lecture des PV.
- `damage(int)` : raccourci pour retirer des PV — c'est l'entrée utilisée par l'IA et par
  l'interaction joueur.
- `setCrystalHealth(int)` :
  - clampe la valeur à 0 minimum et sort immédiatement si les PV n'ont pas changé ;
  - appelle `setChanged()` (marque le chunk à sauvegarder) ;
  - sort si `level` est nul ou côté client ;
  - appelle `level.sendBlockUpdated(...)` pour pousser les nouveaux PV vers les clients ;
  - si les PV tombent à ≤ 0 : `level.destroyBlock(worldPosition, false)` — le `false` empêche
    le drop de l'item — puis message `dungeon_defenders.eternia_crystal.destroyed` en rouge
    gras à tous les joueurs.

**Persistance.** `saveAdditional` / `loadAdditional` utilisent l'API `ValueOutput` /
`ValueInput` (le remplaçant des `CompoundTag` bruts) :

```java
output.putInt("CrystalHealth", this.crystalHealth);
this.crystalHealth = input.getIntOr("CrystalHealth", DEFAULT_HEALTH);
```

Les PV survivent donc au rechargement du monde.

**Synchronisation client.** Les PV ne sont modifiés que côté serveur, alors que le renderer
les lit côté client. Deux surcharges assurent la propagation :

```java
getUpdatePacket() → ClientboundBlockEntityDataPacket.create(this)
getUpdateTag(registries) → saveWithoutMetadata(registries)   // réutilise saveAdditional
```

`getUpdateTag` couvre l'envoi initial (chargement du chunk), `getUpdatePacket` +
`sendBlockUpdated` couvrent les mises à jour.

## Rendu de la barre de vie — `EterniaCrystalBlockEntityRenderer.java`

Renderer client, enregistré via `EntityRenderersEvent.RegisterRenderers` dans
`DungeonDefendersModClient`.

Depuis 26.1, un `BlockEntityRenderer` ne voit plus le block entity au moment du rendu. Le
cycle est en trois temps :

1. **`createRenderState()`** → un `EterniaCrystalRenderState` (sous-classe de
   `BlockEntityRenderState` portant un simple `float healthPercent`).
2. **`extractRenderState(...)`** — appelé côté extraction, avec accès au block entity :
   remplit `healthPercent = clamp(getCrystalHealth() / DEFAULT_HEALTH, 0, 1)`.
3. **`submit(state, poseStack, collector, camera)`** — ne voit que l'état :
   - translation à `(0.5, 3.2, 0.5)`, au-dessus de la hitbox de 3 blocs ;
   - billboard via `poseStack.mulPose(camera.orientation)`. Après cette rotation `+X` va vers
     la droite et **`+Y` vers le bas** (même convention que les name tags vanilla), d'où le
     `scale(1, -1, 1)` qui rétablit des coordonnées naturelles ;
   - `collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), ...)` — la
     géométrie est *soumise*, plus dessinée immédiatement ; le lambda reçoit un
     `PoseStack.Pose` et un `VertexConsumer` au moment du rendu réel.

Deux quads sont émis via `addSegment`, **juxtaposés et jamais superposés** : la jauge
colorée occupe la portion pleine (`healthPercent`), le gris (`0.3, 0.3, 0.3`) le reste. Un
segment de largeur nulle n'est pas émis du tout.

> C'est volontaire. `debugQuads` est déclaré avec `sortOnUpload()`, un tri de transparence
> par distance à la caméra. Avec un fond pleine largeur recouvert par la jauge, les deux
> quads étaient coplanaires (`z = 0` tous les deux) et l'ordre de tri devenait instable : la
> barre apparaissait entièrement grise par intermittence, selon la position de la caméra.
> Juxtaposer les segments supprime le problème à la racine plutôt que de jouer sur le Z.

Le dégradé **vert → jaune → rouge** vient de `red()` / `green()` (le bleu est toujours 0) :

- au-dessus de 50 % : `red = (1 - p) * 2`, `green = 1` → vert pur à 100 %, jaune à 50 % ;
- en dessous : `red = 1`, `green = p * 2` → jaune à 50 %, rouge pur à 0 %.

> **Pourquoi `debugQuads` ?** C'est le seul render type public qui combine ce dont une barre
> de vie a besoin : quads non texturés (`POSITION_COLOR`), mélange translucide, test de
> profondeur, et surtout `withCull(false)` — donc visible quel que soit le sens du quad, ce
> qui évite tout problème d'orientation après le `scale` négatif.

## IA des ennemis

### Le goal — `entity/ai/AttackEterniaCrystalGoal.java`

Étend `MoveToBlockGoal`. Constantes et surcharges :

| Paramètre | Valeur | Rôle |
|---|---|---|
| `SPEED_MODIFIER` | `1.2D` | multiplicateur de vitesse de déplacement |
| `SEARCH_RANGE` | `16` | blocs autour du mob |
| `DAMAGE_PER_HIT` | `5` | PV retirés par coup |
| `TICKS_BETWEEN_HITS` | `20` | 1 seconde entre deux coups |
| `isValidTarget` | `state.is(ModBlocks.ETERNIA_CRYSTAL)` | ne cible que le cristal |
| `getMoveToTarget` | `this.blockPos` | vise la **base** du cristal, pour éviter que le mob tente de grimper |
| `acceptedDistance` | `2.1D` | tolérance suffisante pour un mob au sol contre une hitbox de 3 de haut |

Dans `tick()`, si la cible est atteinte (`isReachedTarget()`), le mob appelle
`crystal.damage(5)` et joue l'animation de bras (`mob.swing`), puis attend
`TICKS_BETWEEN_HITS`. Le cooldown est un champ du goal (remis à zéro dans `start()` et dès
que le mob s'éloigne), et non `mob.tickCount` : le rythme reste correct si le mob quitte puis
revient vers le cristal.

Avec 100 PV par défaut, un zombie seul détruit le cristal en 20 secondes.

### L'attribution — `ModEvents.java`

Écoute `EntityJoinLevelEvent` sur le bus de jeu. Pour chaque `Zombie` rejoignant un monde
côté serveur, le goal est ajouté au `goalSelector` **en priorité 1** (donc au-dessus de la
plupart des objectifs vanilla).

`EntityJoinLevelEvent` se déclenche aussi au rechargement d'un chunk et au changement de
dimension. Le code vérifie donc d'abord qu'aucun `AttackEterniaCrystalGoal` n'est déjà
présent :

```java
zombie.goalSelector.getAvailableGoals().stream()
        .anyMatch(wrapped -> wrapped.getGoal() instanceof AttackEterniaCrystalGoal)
```

Sans ce test, un même zombie cumulerait plusieurs exemplaires du goal et frapperait le
cristal plusieurs fois par seconde.

> Pour étendre l'IA à d'autres monstres, il suffit d'élargir le test `instanceof Zombie` :
> le goal n'exige qu'un `PathfinderMob`.

## Onglet créatif

`dungeon_defenders_tab`, titre `Component.translatable("itemGroup.dungeon_defenders")`,
contient l'item du cristal et celui du piège à pics.

## Apparence du Cristal d'Eternia

Le bloc utilise un modèle `cube_all` standard, mais pointe **provisoirement** sur la texture
vanilla `minecraft:block/diamond_block` : il n'y a pas encore de texture dédiée. Voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md).

Le bloc est miné à la pioche en diamant (tags `mineable/pickaxe` et `needs_diamond_tool`) et
se drope lui-même via `data/dungeon_defenders/loot_table/blocks/eternia_crystal.json`.

## Le Piège à Pics — `block/SpikeTrapBlock.java`

Bloc de défense simple, sans block entity : c'est un premier piège pour ralentir/blesser les
monstres avant qu'ils n'atteignent le cristal.

| Aspect | Implémentation |
|---|---|
| Classe | `Block` (pas de `BaseEntityBlock`, aucun état à persister) |
| Hook de déclenchement | `stepOn(Level, BlockPos, BlockState, Entity)` — appelé quand une entité **marche sur** le bloc (même mécanisme que `MagmaBlock` pour la lave/feu, à ne pas confondre avec `entityInside`, utilisé par le cactus pour un chevauchement latéral) |
| Cible | `entity instanceof Monster` (tout monstre hostile vanilla ou modded) |
| Dégâts | `2.0F`, via `level.damageSources().stalagmite()` |
| Cooldown | `20` ticks (1 s) **par entité**, pas par bloc : un `WeakHashMap<Entity, Long>` en champ d'instance stocke le dernier `level.getGameTime()` de déclenchement pour chaque monstre. `WeakHashMap` évite de retenir indéfiniment des entités mortes/déchargées |
| Garde côté client | `level.isClientSide()` sort immédiatement — toute la logique (dégâts + cooldown) tourne uniquement côté serveur |

Propriétés définies à l'enregistrement dans `ModBlocks` : `strength(2.0F)` (dureté/résistance
aux explosions comparables à la pierre), pas de `requiresCorrectToolForDrops()` — n'importe
quel outil (ou la main) suffit à le récupérer.

### Apparence

Modèle `cube_all` pointant **provisoirement** sur la texture vanilla
`minecraft:block/dripstone_block` (choisie pour son aspect visuellement "pointu"). Miné à la
pioche (tag `mineable/pickaxe` uniquement, pas de tag de niveau d'outil) et se drope lui-même
via `data/dungeon_defenders/loot_table/blocks/spike_trap.json`.

## Le mana du joueur

Ressource pensée pour alimenter de futurs sorts/capacités (aucun n'existe encore : pour
l'instant le mana ne se dépense ni ne se régénère, il reste affiché plein).

### L'état — `init/ModAttachments.java`

Le mana courant est un **data attachment** NeoForge posé sur l'entité joueur, pas un champ
custom : c'est le mécanisme standard pour attacher un état persistant et synchronisable à une
entité vanilla sans la sous-classer.

```java
public static final int MAX_MANA = 100;

public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> MANA = ATTACHMENT_TYPES.register(
        "mana",
        () -> AttachmentType.builder(() -> MAX_MANA)
                .serialize(Codec.INT.fieldOf("Mana"))
                .sync(ByteBufCodecs.VAR_INT)
                .build());
```

- `serialize(...)` : le mana survit à une sauvegarde/rechargement (comme les PV du cristal).
- `sync(...)` : NeoForge pousse automatiquement la valeur au client propriétaire dès qu'un
  code serveur fait `player.setData(ModAttachments.MANA, valeur)` puis
  `player.syncData(ModAttachments.MANA)` — même logique de synchro explicite que
  `sendBlockUpdated` pour le cristal, mais côté entité.
- `MAX_MANA = 100` est la valeur par défaut : c'est aussi la valeur de retour du
  `defaultValueSupplier`, donc un joueur sans mana explicitement défini est considéré plein.

### L'affichage — `client/gui/ManaOverlay.java`

Couche HUD (`GuiLayer`) enregistrée dans `DungeonDefendersModClient` via
`RegisterGuiLayersEvent#registerAboveAll`. Volontairement minimal :

- une jauge unie en haut à gauche de l'écran (`guiGraphics.fill`, pas de sprite) : fond gris
  foncé pleine largeur, recouvert par un rectangle bleu dont la largeur est proportionnelle à
  `currentMana / maxMana` ;
- juste à droite de la jauge, le texte `Mana: X/Y` (clé `dungeon_defenders.hud.mana`) pour
  lire la valeur exacte, centré verticalement sur la hauteur de la jauge.

Lit `player.getData(ModAttachments.MANA)` à chaque frame ; pas d'état côté overlay lui-même.
Voir [05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md) pour ce qu'il reste à
faire (texture, régénération).

### La baguette de test — `item/ManaTestWandItem.java`

Item simple (pas de bloc) enregistré dans `ModBlocks` via `ITEMS.registerItem(...)` — même
`DeferredRegister.Items` que les `BlockItem`, il n'existe pas encore de classe `ModItems`
séparée. Sert uniquement à vérifier le HUD en jeu, sur le même principe que le harnais de
clic droit du Cristal d'Eternia :

- clic droit : retire 10 de mana (`MANA_COST`) au joueur, via
  `player.setData(ModAttachments.MANA, ...)` puis `player.syncData(...)` pour pousser la
  nouvelle valeur au client ;
- message système confirmant le nouveau mana, ou `dungeon_defenders.mana_test_wand.empty` si
  le mana est déjà à 0 ;
- toute la logique est côté serveur (`level.isClientSide()` en sortie anticipée) ; le client
  reçoit juste `InteractionResult.SUCCESS` pour l'animation de bras.

Modèle provisoire : texture vanilla `minecraft:item/blaze_rod`, pas de modèle dédié.

## La vie du joueur

Contrairement au mana, la vie n'est pas une ressource inventée pour le mod : c'est l'attribut
vanilla `Attributes.MAX_HEALTH` / `LivingEntity.getHealth()`, déjà persistant et déjà
synchronisé par le jeu. Le mod se contente d'en changer le maximum par défaut.

### Le maximum à 100 — `ModEvents.onPlayerJoin`

```java
private static final double PLAYER_MAX_HEALTH = 100.0D;

@SubscribeEvent
public static void onPlayerJoin(EntityJoinLevelEvent event) {
    if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Player player)) {
        return;
    }

    AttributeInstance maxHealthAttribute = player.getAttribute(Attributes.MAX_HEALTH);
    if (maxHealthAttribute == null) {
        return;
    }

    boolean wasAtPreviousMax = player.getHealth() >= maxHealthAttribute.getValue();
    maxHealthAttribute.setBaseValue(PLAYER_MAX_HEALTH);
    if (wasAtPreviousMax) {
        player.setHealth((float) maxHealthAttribute.getValue());
    }
}
```

`EntityJoinLevelEvent` se redéclenche à chaque connexion, respawn et changement de dimension
(même remarque que pour `onZombieSpawn`). `setBaseValue` est idempotent (poser deux fois la
même valeur ne change rien), donc pas besoin de garde anti-doublon ici. Le seul piège est de
ne pas soigner gratuitement un joueur déjà blessé à chaque relog : le code ne remonte la vie
au nouveau maximum que si le joueur était **déjà** à son ancien maximum (cas du tout premier
join, où le joueur vient de spawn à 20/20 avant que l'attribut ne soit modifié).

### L'affichage — `client/gui/HealthOverlay.java`

Même structure que `ManaOverlay` (jauge + texte `Health: X/Y`, clé
`dungeon_defenders.hud.health`), en rouge, positionnée juste en dessous grâce aux constantes
`ManaOverlay.ROW_Y` / `ROW_HEIGHT`. Lit directement `player.getHealth()` /
`player.getMaxHealth()` à chaque frame — pas besoin d'attachment, ces valeurs sont déjà
tenues à jour et synchronisées par le moteur.

Les cœurs vanilla (`VanillaGuiLayers.PLAYER_HEALTH`) sont masqués dans
`DungeonDefendersModClient.onRegisterGuiLayers` via `event.replaceLayer(..., HIDDEN)` :
avec 100 PV ils s'étaleraient sur plusieurs rangées de cœurs (le rendu vanilla est pensé pour
20 PV, pas 100) et feraient de toute façon doublon avec `HealthOverlay`.

## L'expérience custom du joueur

**Rien à voir avec l'XP vanilla** (`EXPERIENCE_LEVEL`/`getExperienceLevel()`) : c'est une
ressource propre au mod, pensée pour un futur système de progression/niveaux (pas encore
défini — rien ne la fait varier pour l'instant, elle démarre à 0).

### L'état — `init/ModAttachments.java`

Même mécanisme que le mana : un data attachment, mais qui démarre **vide** plutôt que plein,
puisqu'une expérience se gagne au lieu de se dépenser.

```java
public static final int MAX_EXPERIENCE = 100;

public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> EXPERIENCE = ATTACHMENT_TYPES.register(
        "experience",
        () -> AttachmentType.builder(() -> 0)
                .serialize(Codec.INT.fieldOf("Experience"))
                .sync(ByteBufCodecs.VAR_INT)
                .build());
```

`MAX_EXPERIENCE = 100` est une valeur provisoire : sans système de niveaux défini, il n'y a
pas encore de vraie notion de "maximum", c'est surtout ce qui donne son échelle à la jauge.

### L'affichage — `client/gui/ExperienceOverlay.java`

Même structure que `ManaOverlay`/`HealthOverlay` (jauge + texte `Experience: X/Y`, clé
`dungeon_defenders.hud.experience`), en vert, positionnée juste en dessous de la vie grâce à
`HealthOverlay.ROW_Y + ManaOverlay.ROW_HEIGHT`. Comme rien ne fait encore varier l'attachment,
elle s'affiche `0/100` en permanence tant qu'aucun mécanisme n'alimente `ModAttachments.EXPERIENCE`.

## La vague en cours

Compteur affiché en haut à droite (`Vague X/Y`), pour un futur déroulement en vagues de
monstres. Aucune mécanique de déclenchement, de victoire ou de défaite n'existe encore : la
valeur ne bouge jamais toute seule.

### L'état — `init/ModAttachments.java`

Contrairement au mana/à la vie/à l'expérience, la vague **n'est pas un état du joueur** :
c'est un état de la partie, donc de la `Level`. `Level`/`ServerLevel` implémentent aussi
`IAttachmentHolder` (comme `Entity`), le même mécanisme de data attachment s'applique donc
directement à l'échelle du monde plutôt que par joueur.

```java
public static final int MAX_WAVE = 5;

public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> CURRENT_WAVE = ATTACHMENT_TYPES.register(
        "current_wave",
        () -> AttachmentType.builder(() -> 1)
                .serialize(Codec.INT.fieldOf("CurrentWave"))
                .sync(ByteBufCodecs.VAR_INT)
                .build());
```

Commence à `1` (pas `0`, une partie démarre à la vague 1). `ServerLevel` expose le même
`syncData(AttachmentType<?>)` qu'`Entity`, poussé à tous les joueurs qui suivent ce monde.

### L'affichage — `client/gui/WaveOverlay.java`

Contrairement aux trois autres overlays, **pas de jauge** : juste le texte `Vague X/Y` (clé
`dungeon_defenders.hud.wave`), en haut à droite de l'écran. La position est calculée avec
`guiGraphics.guiWidth() - MARGIN - font.width(texte)` pour rester collée au bord droit quel
que soit le nombre de chiffres. Lit `Minecraft.getInstance().level.getData(...)` — `level`,
pas `player`, puisque l'état appartient au monde.

### La progression de la vague — `client/gui/WaveEnemiesOverlay.java`

Sur la **même rangée** que `WaveOverlay` (pas en dessous, pour lire les deux informations
d'un coup d'œil), même style que `ExperienceOverlay` (jauge + texte `Ennemis : X/Y`, clé
`dungeon_defenders.hud.wave_enemies`) mais **en miroir et ancré sur `WaveOverlay`** : dans
l'ordre, de droite à gauche, `Vague X/Y` puis un espace, la jauge (`BAR_WIDTH = 60`, plus
étroite que les autres jauges du HUD pour que la ligne ne déborde pas), puis le texte
`Ennemis : X/Y`. `X` = ennemis déjà tués (`ModAttachments.WAVE_ENEMIES_KILLED`, vide par
défaut), `Y` = ennemis total de la vague (`ModAttachments.WAVE_ENEMIES_TOTAL`, `10` par
défaut) ; la jauge orange se remplit à mesure que `X` se rapproche de `Y`. Deux attachments
sur la `Level`, même raisonnement que `current_wave`.

`WaveEnemiesOverlay` appelle `WaveOverlay.waveText(level)` (méthode package-visible) pour
calculer où commence le texte `Vague X/Y` et se positionner juste à sa gauche — c'est le seul
couplage entre les deux classes, pour ne pas dupliquer la logique de lecture de
`CURRENT_WAVE`/`MAX_WAVE`.

## Le HUD vanilla masqué

Le mod vise une interface entièrement custom : plusieurs couches du HUD vanilla sont donc
masquées dans `DungeonDefendersModClient.onRegisterGuiLayers`, via
`event.replaceLayer(identifiant, HIDDEN)` où `HIDDEN` est une `GuiLayer` dont `render(...)` ne
fait rien.

| Couche masquée | Identifiant `VanillaGuiLayers` | Remplacée par |
|---|---|---|
| Cœurs de vie | `PLAYER_HEALTH` | `HealthOverlay` |
| Faim | `FOOD_LEVEL` | *(rien pour l'instant)* |
| Expérience vanilla | `EXPERIENCE_LEVEL` | `ExperienceOverlay` (expérience **custom**, sans rapport avec l'XP vanilla) |
| Barre d'inventaire (hotbar) | `HOTBAR` | *(rien pour l'instant)* |

> `replaceLayer` ne fait que vider le contenu d'une couche existante, sans la retirer de la
> liste : l'ordre d'affichage et les couches enregistrées relativement à elle (via
> `registerAbove`/`registerBelow` côté vanilla, par exemple l'armure au-dessus de la hotbar)
> restent inchangés, elles dessinent juste dans le vide.

Faim et hotbar n'ont pas encore d'équivalent custom : tant que ce n'est pas fait, le joueur ne
voit ni sa faim, ni l'objet sélectionné/sa barre d'objets. Voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md).
