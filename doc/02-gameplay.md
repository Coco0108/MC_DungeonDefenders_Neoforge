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

`useWithoutItem` (clic droit à main nue) se comporte différemment selon la phase :

- **En Construction** : bascule "prêt" pour le joueur qui clique — voir "Le vote prêt" plus
  bas, c'est le vrai déclencheur du Combat.
- **En Combat** : retire **10 PV** au cristal et envoie un message au joueur — l'ancien
  harnais de test, gardé pour pouvoir déclencher la destruction du cristal sans attendre une
  vraie vague. Dans le jeu final, seuls les monstres devraient endommager le cristal.

Le client renvoie immédiatement `SUCCESS` (prédiction, animation de bras) ; la logique ne
tourne que côté serveur, et `PASS` est renvoyé si le block entity est absent (uniquement
pertinent pour la branche Combat, qui a besoin du block entity — la branche Construction n'en
a pas besoin).

### Le vote "prêt" — déclencheur du Combat

Pour passer de Construction à Combat, il faut que **tous les joueurs présents dans cette
Level** cliquent sur le cristal (pas tout le serveur : une future map/dimension différente
aura ses propres joueurs, voir 05-etat-et-problemes-connus.md). Chaque clic bascule l'état
"prêt" du joueur qui a cliqué (`ModAttachments.READY`, un attachment **joueur**, comme
`mana`/`experience` — pas persistant, se re-décider à chaque Construction est voulu), diffuse
la progression à tout le monde (`Prêt : 2/3`), et dès que tous sont prêts,
`PhaseTransitions.enterCombat(level)` se déclenche — qui remet aussitôt "prêt" à faux pour
tout le monde (voir plus bas), pour repartir propre à la Construction suivante. Un seul joueur
en solo se retrouve donc à devoir cliquer une fois pour lancer le combat (1/1).

Le harnais de test au clic droit du `SpawnerBlock` (shift + clic droit, voir plus bas) reste
disponible en parallèle pour basculer directement de phase sans passer par le vote — pratique
pour les tests, mais il passe aussi par `PhaseTransitions`, donc remet "prêt" à zéro pour tout
le monde comme le vote, pas de comportement divergent entre les deux déclencheurs.

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
    gras à tous les joueurs, et `PhaseTransitions.onDefeat(level)` (voir plus bas, "Victoire et
    défaite") pour remettre la partie à zéro.

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

### La base commune — `entity/ai/AbstractEterniaCrystalAttackGoal.java`

`AttackEterniaCrystalGoal` (corps à corps) et `RangedAttackEterniaCrystalGoal` (distance)
partagent tout ce qui concerne le **ciblage et le déplacement** — identique dans les deux cas
— via cette classe abstraite, qui étend `MoveToBlockGoal` :

| Membre | Rôle |
|---|---|
| Constructeur `(mob, speedModifier, acceptedDistance, damagePerHit)` | `speedModifier`/`acceptedDistance` passés à `MoveToBlockGoal` ; `damagePerHit` exposé aux sous-classes via `this.damagePerHit` |
| `isValidTarget` | `state.is(ModBlocks.ETERNIA_CRYSTAL)` — ne cible que le cristal |
| `getMoveToTarget` | `this.blockPos` — vise la **base** du cristal, pour éviter que le mob tente de grimper |
| `findCrystal()` | `protected final`, retrouve le `EterniaCrystalBlockEntity` à `this.blockPos` (ou `null` s'il a été cassé) |
| `onReachedTarget(crystal)` | abstrait, appelé chaque tick tant que le mob est à `acceptedDistance()` du cristal |
| `onTargetLost()` | appelé chaque tick tant qu'il ne l'est pas ; no-op par défaut |

`SEARCH_RANGE` (16 blocs) reste une constante interne à cette classe, pas exposée au
constructeur — contrairement aux dégâts, rien ne justifie encore qu'elle varie d'un ennemi à
l'autre. `acceptedDistance`, en revanche, est ce qui distingue fondamentalement le corps à
corps (une valeur fixe, faible) de la distance (une portée, potentiellement variable d'un
ennemi à l'autre) : chaque sous-classe la fixe à sa manière (voir plus bas).

> **Sous-classer `AbstractEterniaCrystalAttackGoal` directement** (plutôt que
> `AttackEterniaCrystalGoal`/`RangedAttackEterniaCrystalGoal`) n'a d'intérêt que pour un
> **nouveau style d'attaque** — une troisième famille, ni corps à corps ni tir à l'arc (une
> attaque de zone, par exemple). Pour un ennemi qui attaque comme les deux familles
> existantes mais avec d'autres chiffres, pas besoin de sous-classer quoi que ce soit : les
> deux constructeurs `(mob, damagePerHit, ticksBetweenX, ...)` couvrent déjà ce cas (voir
> ci-dessous).

### Le goal corps à corps — `entity/ai/AttackEterniaCrystalGoal.java`

```java
public AttackEterniaCrystalGoal(PathfinderMob mob)                              // 5 dégâts / 20 ticks (par défaut)
public AttackEterniaCrystalGoal(PathfinderMob mob, int damagePerHit, int ticksBetweenHits)
```

`SPEED_MODIFIER` (`1.2D`) et `ACCEPTED_DISTANCE` (`2.1D`, tolérance suffisante pour un mob au
sol contre une hitbox de 3 de haut) restent des constantes internes — c'est la cadence et les
dégâts qui varient d'un ennemi de mêlée à l'autre, pas la distance d'engagement (toujours
"collé au cristal" par définition du corps à corps).

Dans `onReachedTarget(crystal)`, une fois le cooldown écoulé : `crystal.damage(damagePerHit)`,
animation de bras (`mob.swing`), puis attend `ticksBetweenHits`. Le cooldown est un champ du
goal (remis à zéro dans `start()` et dans `onTargetLost()`), et non `mob.tickCount` : le
rythme reste correct si le mob quitte puis revient vers le cristal.

Avec 100 PV par défaut et les valeurs par défaut (5 dégâts/s), un zombie seul détruit le
cristal en 20 secondes.

### Le goal à distance — `entity/ai/RangedAttackEterniaCrystalGoal.java`

```java
public RangedAttackEterniaCrystalGoal(PathfinderMob mob)                                                    // 3 dégâts / 20 ticks / 10 blocs (par défaut)
public RangedAttackEterniaCrystalGoal(PathfinderMob mob, int damagePerHit, int ticksBetweenShots, double shootRange)
```

Contrairement au corps à corps, la **portée de tir** (`shootRange`, passée comme
`acceptedDistance` au parent) est elle aussi exposée au constructeur : un futur ennemi à
distance pourrait raisonnablement viser de plus près (un lanceur) ou de plus loin (un
tireur d'élite), pas seulement avoir des dégâts/une cadence différents. `DRAW_TICKS` (20,
temps de tension de l'arc) reste une constante interne : c'est un détail de timing
d'animation, pas un levier d'équilibrage entre archétypes.

Dans `onReachedTarget(crystal)`, une fois à portée : le mob se tourne vers le cristal
(`LookControl#setLookAt`, nécessaire une fois immobile — `MoveToBlockGoal` ne le fait plus
après l'approche), puis alterne tension (`mob.startUsingItem(MAIN_HAND)`, ce qui déclenche la
pose vanilla "arc tendu" puisque le squelette porte déjà un arc par défaut) et tir. Au tir,
`crystal.damage(damagePerHit)` est appliqué **directement** au cristal, sur le même principe
"harnais" que le corps à corps — la flèche réellement lancée (`spawnArrow`, une vraie entité
`Arrow`, avec le calcul d'arc `dy + distance × 0.2` repris du tir vanilla) n'est là que pour
le **visuel** du tir, ce n'est pas sa collision qui inflige les dégâts (le cristal n'étant pas
une entité, une flèche vanilla ne saurait pas le "toucher" toute seule).

### L'attribution — `ModEvents.onMonsterSpawn`

Écoute `EntityJoinLevelEvent` sur le bus de jeu. Pour chaque `Monster` rejoignant un monde
côté serveur, un goal est ajouté au `goalSelector` **en priorité 1** (donc au-dessus de la
plupart des objectifs vanilla) — lequel dépend du type : `AbstractSkeleton` (squelette, et
tout futur sous-type) reçoit `RangedAttackEterniaCrystalGoal`, tout le reste reçoit
`AttackEterniaCrystalGoal` (corps à corps).

> `Monster` plutôt que `PathfinderMob` : les deux goals n'exigent techniquement qu'un
> `PathfinderMob`, mais cette classe couvre aussi les mobs passifs (animaux, villageois...).
> `Monster` est la bonne frontière sémantique — tout ce qui est hostile, rien de passif.

`EntityJoinLevelEvent` se déclenche aussi au rechargement d'un chunk et au changement de
dimension. Le code vérifie donc d'abord qu'aucun des deux goals n'est déjà présent :

```java
monster.goalSelector.getAvailableGoals().stream()
        .anyMatch(wrapped -> wrapped.getGoal() instanceof AttackEterniaCrystalGoal
                || wrapped.getGoal() instanceof RangedAttackEterniaCrystalGoal)
```

Sans ce test, un même monstre cumulerait plusieurs exemplaires du goal et attaquerait le
cristal plusieurs fois par seconde.

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
`RegisterGuiLayersEvent#registerAboveAll`. Volontairement minimal, et fait partie du groupe
bas-gauche décrit dans [Le groupe bas-gauche](#le-groupe-bas-gauche--mana-vie-expérience)
ci-dessous :

- un losange (`DiamondGauge`, voir plus bas), en bas à gauche de l'écran : fond gris foncé sur
  toute sa hauteur, recouvert par le bas d'un losange bleu dont la hauteur est proportionnelle
  à `currentMana / maxMana` — la jauge se remplit du bas vers le haut, comme avant, mais dans
  une forme de losange plutôt qu'un rectangle. C'est le losange de **gauche** du groupe (vie à
  droite) ;
- au-dessus du losange, le texte `Mana: X/Y` (clé `dungeon_defenders.hud.mana`), centré
  horizontalement (`guiGraphics.centeredText`) sur son centre.

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
(même remarque que pour `onMonsterSpawn`). `setBaseValue` est idempotent (poser deux fois la
même valeur ne change rien), donc pas besoin de garde anti-doublon ici. Le seul piège est de
ne pas soigner gratuitement un joueur déjà blessé à chaque relog : le code ne remonte la vie
au nouveau maximum que si le joueur était **déjà** à son ancien maximum (cas du tout premier
join, où le joueur vient de spawn à 20/20 avant que l'attribut ne soit modifié).

### L'affichage — `client/gui/HealthOverlay.java`

Même structure que `ManaOverlay` (losange + texte `Health: X/Y`, clé
`dungeon_defenders.hud.health`), en rouge, positionné juste à **droite** du losange mana
(centré sur `HudLayout.MARGIN + DIAMOND_RADIUS * 3 + DIAMOND_GAP`), même taille. Lit
directement `player.getHealth()` / `player.getMaxHealth()` à chaque frame — pas besoin
d'attachment, ces valeurs sont déjà tenues à jour et synchronisées par le moteur.

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

Contrairement à `ManaOverlay`/`HealthOverlay`, reste une **barre horizontale** classique
(jauge + texte `Experience: X/Y` à sa droite, clé `dungeon_defenders.hud.experience`), en
vert, tout en bas de l'écran, sous les deux losanges. Comme rien ne fait encore varier
l'attachment, elle s'affiche `0/100` en permanence tant qu'aucun mécanisme n'alimente
`ModAttachments.EXPERIENCE`.

## Le groupe bas-gauche — mana, vie, expérience

`ManaOverlay`, `HealthOverlay` et `ExperienceOverlay` forment un groupe positionné dans le
coin bas-gauche de l'écran :

```
   Mana        Vie
    ◆           ◆     <- losanges, remplissage bas → haut (pointe basse → pointe haute)
   ▓█▓         ▓█▓        mana à gauche, vie à droite
  ▓███▓       ▓███▓
  ░░░░░       ▓▓▓▓▓
    ░           ▓
  [███░░░░░░░░░░░░░░] Experience: 0/100   <- barre horizontale, tout en bas
```

**Pourquoi des losanges et pas des rectangles ?** Le jeu qui a inspiré ce HUD (*Dungeon
Defenders* original) affiche le mana et la vie du joueur dans un widget en forme de
losange/triangle, pas des barres classiques. `DiamondGauge.render(...)` reproduit cette forme
sans texture : pour chaque bande horizontale d'1px de haut, la largeur croît puis décroît
linéairement (maximale au centre, nulle aux deux pointes) — un simple empilement de
`guiGraphics.fill(...)`, façon pixel art, en attendant de vraies textures (voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md)). `ManaOverlay` et
`HealthOverlay` appellent cette même méthode avec des couleurs différentes ; ils ne dessinent
plus rien eux-mêmes directement.

`client/gui/HudLayout.java` centralise les constantes de mise en page partagées par les trois
(marge, rayon/écart des losanges, écart avant la barre d'expérience) : sans ça, garder les
trois classes indépendantes alignées au pixel près demanderait de dupliquer les mêmes valeurs
magiques partout, avec le risque qu'elles divergent au premier ajustement.

Un quatrième overlay, `AbilitySlotsOverlay`, se greffe juste à droite de ce groupe (les 4
emplacements de compétences) — voir
[Les emplacements de compétences](#les-emplacements-de-compétences--clientguiabilityslotsoverlayjava)
plus bas.

`ExperienceOverlay` calcule sa position en premier (ancrée au bord bas de l'écran via
`guiGraphics.guiHeight()`) et expose `barTop(guiGraphics)`, une méthode package-visible que
`ManaOverlay`/`HealthOverlay` appellent pour savoir où s'arrête le bas de leurs colonnes
(`ExperienceOverlay.barTop(guiGraphics) - HudLayout.ROW_GAP`) — même principe de couplage
minimal que `WaveOverlay.waveText(level)` pour le groupe haut-droit.

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

**Centrée tout en haut de l'écran**, indépendante du reste du groupe vague/phase (qui reste
en haut à droite) : c'est l'information la plus visible du jeu de référence (*Dungeon
Defenders* original), une grosse barre en haut-centre, donc elle a sa propre place plutôt que
de se caser à côté du texte `Vague X/Y`.

Même style que `ExperienceOverlay` (jauge + texte `Ennemis : X/Y`, clé
`dungeon_defenders.hud.wave_enemies`), mais plus large (`BAR_WIDTH = 240`) et avec le texte
**superposé au centre de la jauge** plutôt qu'à côté — plus compact malgré la largeur,
également plus proche du rendu du jeu de référence. `X` = ennemis déjà tués
(`ModAttachments.WAVE_ENEMIES_KILLED`, vide par défaut), `Y` = ennemis total de la vague
(`ModAttachments.WAVE_ENEMIES_TOTAL`, `10` par défaut) ; la jauge orange se remplit à mesure
que `X` se rapproche de `Y`. Deux attachments sur la `Level`, même raisonnement que
`current_wave`.

### La phase de la partie — `client/gui/PhaseOverlay.java`

Juste en dessous de la rangée vague/ennemis (`WaveOverlay.ROW_Y + ROW_HEIGHT`), texte seul
comme `WaveOverlay` : `Phase : Construction` ou `Phase : Combat`, clé
`dungeon_defenders.hud.phase`. La partie démarre en `BUILD`. La seule façon de changer de
phase pour l'instant est le harnais de test au clic droit du `SpawnerBlock` (voir plus bas) —
aucun vrai déclencheur de combat n'existe encore.

`init/GamePhase.java` est un enum (`BUILD`, `COMBAT`) plutôt qu'une chaîne libre, pour garder
un ensemble de valeurs fermé et une traduction par valeur
(`dungeon_defenders.phase.build`/`.combat`, via `GamePhase.translationKey()`). L'attachment
`ModAttachments.GAME_PHASE` stocke cependant un simple `Integer` (l'ordinal de l'enum), comme
tous les autres compteurs du mod — pas de `Codec`/`StreamCodec` dédié à l'enum pour
l'instant, ça aurait été de la complexité en plus pour un gain nul tant qu'il n'y a que deux
valeurs. À revoir si la liste des phases grandit ou si l'ordre des constantes doit pouvoir
changer sans casser les sauvegardes existantes (l'ordinal n'est pas stable entre deux
réordonnancements de l'enum).

## Le Spawner — `block/SpawnerBlock.java`

Premier morceau de vraie mécanique de combat du mod (le reste n'était que du HUD affichant
des valeurs par défaut) : un bloc à poser dans la map qui fait apparaître des ennemis pendant
la phase de combat. L'algorithme vient de la feuille "Idées" du plan Excel du joueur, précisé
au fil d'une discussion : un accumulateur par type d'ennemi, incrémenté chaque contrôle de son
"nombre de base" ; dès qu'il atteint un seuil, un ennemi de ce type spawn et le seuil lui est
retiré. Le nombre de base sert **aussi** de plafond pour ce type : une fois atteint, ce type
est sauté (round-robin sur les types restants) jusqu'à ce que tous soient épuisés — c'est
exactement l'exemple du joueur ("on a au total 15 gobelins et 5 orcs", pas juste un ratio).

```java
// SpawnEntry.tickAndMaybeSpawn(...), une fois par entrée de la composition
if (spawned >= effectiveTotal) {
    return false;   // ce type est épuisé pour la vague, on le saute
}
accumulator += effectiveTotal;
if (accumulator < SPAWN_THRESHOLD) {   // 20
    return false;
}
accumulator -= SPAWN_THRESHOLD;
spawned++;
type.spawn(level, spawnPos, EntitySpawnReason.SPAWNER);
```

> Le seuil se déclenche à `>=`, pas `>` : l'exemple du joueur contenait une ligne
> ("Gobelin = 20 → spawn") qui ne fonctionne qu'avec `>=`. Détail d'implémentation, l'idée
> reste identique à ce qu'il avait écrit.

**La position de spawn** (`SpawnerBlockEntity#findSafeSpawnPos`) : avec un rayon supérieur à
0, la position tirée au hasard dans ce rayon peut tomber à l'intérieur d'un bloc plein (mur,
terrain irrégulier autour du spawner). `findSafeSpawnPos` essaie jusqu'à 8 positions
aléatoires, en ne retenant que celles où la position **et** celle juste au-dessus (place pour
les pieds et la tête) sont toutes les deux traversables (`BlockState#getCollisionShape(...)
.isEmpty()`) ; si aucune des 8 ne convient, replie sur `pos.above()` — la position par défaut
utilisée avant l'ajout du rayon, censée toujours être libre. Pas de vérification qu'il y a un
sol en dessous (un ennemi qui spawn au-dessus d'un trou tombe simplement, ce n'est pas un bug)
— seul l'enlisement dans un bloc plein est évité.

### L'état — `block/entity/SpawnerBlockEntity.java`

`BaseEntityBlock` + `BlockEntityTicker`, sur le même principe qu'`EterniaCrystalBlockEntity`
(codec, `newBlockEntity`) mais avec un tick serveur en plus — premier bloc du mod à en avoir
un. `serverTick(...)` :

1. Sort immédiatement si `ModAttachments.GAME_PHASE != COMBAT` — le spawner ne tourne qu'en
   combat.
2. Sort aussi si `CURRENT_WAVE` est en dehors de `[waveStart, waveEnd]` — un spawner peut être
   configuré pour ne s'activer que sur une plage de vagues.
3. Si une nouvelle session de combat a commencé depuis le dernier passage
   (`lastCombatSessionHandled` vs `ModAttachments.COMBAT_SESSION`, voir "Le déroulement d'une
   vague" plus bas), recalcule le plafond de chaque type (`resetForWave`, voir plus bas) et
   remet sa progression à zéro : une nouvelle session de combat, une nouvelle chance de spawn
   pour chaque type.
4. Ne fait tourner l'algorithme qu'une fois toutes les `intervalTicks` (20 par défaut, soit
   une seconde), pas à chaque tick, pour rester lisible.
5. Applique l'algorithme ci-dessus, une fois par entrée de la composition.

**La composition** (`List<SpawnEntry>`) est modifiable par spawner — deux entrées par défaut,
zombie (nombre de base 15) et squelette (nombre de base 5), reprenant exactement les chiffres
de l'exemple du joueur. Chaque `SpawnEntry` combine son ennemi (`init/SpawnableEnemy.java`,
plutôt qu'un `EntityType<?>` brut), son nombre de base et sa progression pour la vague en
cours (`spawned`, `accumulator`, `effectiveTotal`), le tout persistant via un `Codec` dédié
(`ValueOutput/ValueInput#list(...)`, la liste ayant une longueur variable contrairement aux
compteurs simples du reste du mod ; l'ennemi est stocké par ordinal, comme `GamePhase`).

`init/SpawnableEnemy.java` est la liste fermée des ennemis choisissables dans un spawner
(`ZOMBIE`, `SKELETON` pour l'instant). Il n'existe pas de tag vanilla générique "tout ce qui
est hostile" dans cette version de Minecraft (vérifié) : cet enum sert à la fois de source de
vérité réseau (transmis par ordinal, voir plus bas) et de liste pour le bouton "cycler le
type" du GUI. Ajouter un ennemi au jeu et vouloir le rendre choisissable dans un spawner se
résume à une ligne dans cet enum — rien d'autre à toucher côté GUI/réseau/persistance.

**Paramètres configurables par spawner**, en plus de la composition : `intervalTicks` (défaut
20), `spawnRadius` (défaut 0 — spawn pile au-dessus du bloc ; au-delà, une position aléatoire
dans ce rayon, pour éviter que tout s'empile sur la même case), `waveStart`/`waveEnd` (défaut
1 à `MAX_WAVE`). Tous persistants. Pas encore de GUI pour les éditer en jeu — en cours, voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md).

### Le multiplicateur de difficulté — `init/DifficultyScaling.java`

`effectiveTotal = round(baseCount × DifficultyScaling.getMultiplier(level))`, recalculé à
chaque nouvelle vague (pas à chaque tick — sinon un type déjà épuisé pourrait redevenir actif
en cours de vague si le multiplicateur changeait entre-temps). Le multiplicateur combine :

- **La difficulté** (`ModAttachments.DIFFICULTY`, enum `GameDifficulty` : `EASY` ×0.75,
  `NORMAL` ×1.0, `HARD` ×1.5) — censée être choisie au lancement de la map, mais aucun écran
  pour le faire n'existe encore : démarre toujours à `NORMAL`.
- **La vague courante** : +10 % par vague au-delà de la première (`1.0 + (wave - 1) × 0.1`).

`DifficultyScaling.getMultiplier(Level)` est le seul point de calcul, pensé pour accueillir
d'autres facteurs plus tard (nombre de joueurs, par exemple) sans changer les appelants.

Comme `effectiveTotal` sert à la fois de fréquence d'apparition (`accumulator +=
effectiveTotal`) et de plafond (`spawned >= effectiveTotal`), une difficulté plus élevée
rend un type d'ennemi **à la fois** plus nombreux et plus fréquent sur la vague — pas
seulement plus nombreux.

### Le harnais de test — clic droit (maj/shift = bascule de phase)

Comme pour le Cristal d'Eternia et `ManaTestWandItem`, un harnais de test plutôt qu'une vraie
mécanique : **shift + clic droit** sur le `SpawnerBlock` bascule `ModAttachments.GAME_PHASE`
entre `BUILD` et `COMBAT` directement, message système confirmant la nouvelle phase — sans
passer par le vote "prêt" du cristal (voir plus haut). Pratique pour tester rapidement sans
réunir tous les joueurs, mais ça fait aussi avancer `CURRENT_WAVE` comme n'importe quelle fin
de combat (voir [Le déroulement d'une
vague](#le-déroulement-dune-vague--initphasetransitionsjava-modeventsonmonsterdeath) plus bas)
— attention à l'utiliser en rafale en test, ça consomme les vagues vite.

Le passage effectif de phase (peu importe le déclencheur : vote "prêt", harnais de test, ou
fin de vague) passe toujours par `init/PhaseTransitions.java`, pas par un
`level.setData(GAME_PHASE, ...)` direct — ça centralise la remise à zéro des compteurs qui va
avec (voir plus bas), pour que les trois déclencheurs se comportent pareil.

Un clic droit **sans shift** ouvre l'écran de configuration (voir plus bas) — **réservé au
mode créatif**, comme un bloc de structure vanilla : la configuration d'un spawner est censée
être figée une fois la map construite (voir "Ce qui n'est PAS dans ce GUI" plus bas).

### L'écran de configuration — `menu/`, `network/`, `client/gui/screen/SpawnerConfigScreen.java`

Premier GUI custom du mod, **pas de slot ni d'item** — les paramètres décidés avec le joueur :
intervalle (ticks), rayon de spawn, vague de début, vague de fin, et une **liste dynamique**
de lignes de composition (une par ennemi choisi). Chaque ligne a un bouton qui affiche le nom
de l'ennemi et le fait cycler vers le suivant au clic (`SpawnableEnemy.next()`, en sautant les
ennemis déjà utilisés par une autre ligne), un champ pour son nombre de base, et un bouton "X"
pour la retirer (cachée s'il ne reste qu'une seule ligne — on garde toujours au moins un
ennemi). Un bouton "+ Ajouter" en bas de la liste, cachée une fois que toutes les valeurs de
`SpawnableEnemy` sont utilisées (la liste d'ennemis possibles est fermée). Pré-rempli avec la
configuration actuelle du spawner ciblé.

**Pourquoi l'état est gardé en mémoire, pas seulement dans les widgets** : ajouter ou retirer
une ligne change le nombre de lignes, donc décale tout ce qui suit (les lignes restantes, le
bouton Ajouter, le bouton Valider) — il faut reconstruire tous les widgets
(`Screen#rebuildWidgets()`). Mais reconstruire détruit les `EditBox`/`Button` existants, donc
leurs valeurs seraient perdues si elles n'étaient pas sauvegardées ailleurs. `SpawnerConfigScreen`
garde donc sa propre copie (`intervalText`, `radiusText`, ..., `rows: List<RowState>`) comme
source de vérité entre deux reconstructions, chargée depuis le `SpawnerBlockEntity` une seule
fois (`loadedFromSpawner`, à la toute première `init()`) pour ne pas écraser les modifications
en cours de l'utilisateur à chaque ajout/retrait. `syncFieldsToState()` recopie les valeurs des
widgets actuels dans cet état juste avant un rebuild. Cycler le type d'une ligne, en revanche,
ne touche **pas** au nombre de lignes : pas besoin de rebuild, juste
`AbstractWidget#setMessage(...)` sur le bouton concerné pour changer son libellé.

**Le trajet complet, dans l'ordre :**

1. **Clic droit** (sans shift) sur un `SpawnerBlock` → `SpawnerBlock.openConfigScreen(...)`
   appelle `player.openMenu(new SpawnerConfigMenuProvider(pos))` côté serveur.
2. **`SpawnerConfigMenuProvider`** (`record MenuProvider`) fournit `createMenu(...)` (le menu
   côté serveur, qui ne porte que le `BlockPos`) et surcharge
   `writeClientSideData(menu, buf)` — l'extension NeoForge de `MenuProvider` qui écrit des
   données supplémentaires dans le paquet d'ouverture. Ici : juste `buf.writeBlockPos(pos)`.
3. Côté client, `IMenuTypeExtension.create(SpawnerConfigMenu::new)` (voir
   `init/ModMenus.java`) reconstruit **le même** `SpawnerConfigMenu` à partir de ce
   `BlockPos` — c'est le rôle du deuxième constructeur de `SpawnerConfigMenu`, qui prend un
   `RegistryFriendlyByteBuf` et lit `extraData.readBlockPos()`.
4. `RegisterMenuScreensEvent` (voir `DungeonDefendersModClient`) associe ce `MenuType` à
   `SpawnerConfigScreen::new`, qui s'ouvre automatiquement.
5. **`SpawnerConfigScreen.init()`** ne lit **pas** le menu pour la configuration (le menu ne
   porte que le `BlockPos`) : il retrouve directement le `SpawnerBlockEntity` **côté client**
   via `Minecraft.getInstance().level.getBlockEntity(pos)` — cette copie cliente est déjà à
   jour grâce à la synchronisation ajoutée à `SpawnerBlockEntity`
   (`getUpdatePacket`/`getUpdateTag`, même mécanisme que pour les PV du cristal). L'état en
   mémoire (`intervalText`, ..., `rows`) est rempli depuis `getIntervalTicks()`,
   `getEntries()`, etc., avec un filtre n'acceptant que des chiffres sur les champs numériques
   (`EditBox#setFilter`).
6. Au clic sur **"Valider"**, le client construit un `SpawnerConfigPayload` (le `BlockPos` +
   4 valeurs scalaires + la liste `entries` (une paire ordinal d'ennemi / nombre de base par
   ligne), lue depuis l'état en mémoire) et l'envoie via
   `Minecraft.getInstance().getConnection().send(payload.toVanillaServerbound())`.
7. **`ModNetworking`** (enregistré via `RegisterPayloadHandlersEvent`, voir plus haut) reçoit
   le paquet côté serveur : revérifie que le bloc à cette position est toujours un
   `SpawnerBlockEntity` et que le joueur est encore à portée (8 blocs), reconstruit la liste
   de `SpawnEntry` en validant chaque ordinal d'ennemi reçu (`0 <= ordinal <
   SpawnableEnemy.values().length` — jamais faire confiance à une valeur reçue par le réseau
   pour indexer un tableau), puis appelle `spawner.applyConfig(...)`.

> **Pourquoi pas de vérification de portée dans `SpawnerConfigMenu#stillValid` ?** Parce que
> ce menu ne contient ni slot ni item : la seule action possible dessus est d'envoyer le
> paquet de config, et ce paquet revérifie déjà tout côté serveur avant d'agir. Une double
> vérification (menu **et** paquet) n'aurait rien apporté ici.

`SpawnerBlockEntity.applyConfig(...)` remplace entièrement la composition (`entries`) et
**applique tout de suite** les nouveaux plafonds : `resetForWave(...)` est appelé
immédiatement avec le multiplicateur de difficulté courant, sans attendre le prochain
changement de vague détecté par `serverTick`. C'est un choix volontaire différent de la toute
première version du spawner (qui attendait la vague suivante) : reconfigurer un spawner doit
se voir tout de suite, comme le reste du GUI. Une conséquence assumée : si on reconfigure en
plein milieu d'une vague, la progression de cette vague (`spawned`) repart de zéro pour les
nouvelles entrées.

**Ce qui n'est PAS dans ce GUI**, volontairement (voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md)) :

- Pas de choix de difficulté ici — c'est un réglage de partie (`ModAttachments.DIFFICULTY`),
  pas du spawner, voir plus haut.
- Pas de défilement (scroll) si la liste d'ennemis grandit au point de dépasser la hauteur de
  l'écran — non géré pour l'instant, acceptable tant que `SpawnableEnemy` ne contient que
  deux valeurs.
- **Pas accessible en survie** (`SpawnerBlock.openConfigScreen` vérifie `player.isCreative()`
  avant d'ouvrir l'écran, message système sinon) : l'idée à terme est que les maps soient des
  structures pré-construites (spawners déjà configurés en créatif, puis sauvegardées) posées
  au lancement d'une partie — pas un réglage que le joueur ferait pendant qu'il joue. Ce
  verrou permet aussi de garder simple le calcul de `wave_enemies_total` (voir plus bas) : pas
  besoin de le recalculer à chaque reconfiguration en pleine partie, puisque ça n'arrive plus.

### L'aperçu de composition en phase Construction — `SpawnerBlockEntityRenderer.java`

Comme dans le jeu de référence : en phase `BUILD`, chaque spawner affiche au-dessus de lui le
total d'ennemis à venir et le détail par type, **visible à travers les murs** — pour planifier
sa défense avant que le combat démarre. Disparaît en phase `COMBAT` (pas d'intérêt une fois la
vague lancée).

Techniquement, c'est un deuxième `BlockEntityRenderer` (même trio `createRenderState` /
`extractRenderState` / `submit` que `EterniaCrystalBlockEntityRenderer`), mais qui dessine du
**texte** plutôt que des quads de couleur — la barre de vie du cristal utilisait
`RenderTypes.debugQuads()` (quads non texturés, non cullés), qui ne convient pas au texte. Le
mécanisme "à travers les murs" ici est `Font.DisplayMode.SEE_THROUGH`, passé à
`SubmitNodeCollector#submitText(...)` : c'est le même render type que celui utilisé par
Minecraft pour les noms des mobs brillants (effet Glowing) au-dessus des blocs.

**Le calcul du total** réutilise exactement la formule de `SpawnEntry.resetForWave(...)`
(`max(1, round(baseCount × DifficultyScaling.getMultiplier(level)))`) pour chaque entrée de la
composition, sommée pour le total — l'aperçu affiché correspond donc exactement à ce que la
prochaine vague fera spawn, tant que la difficulté ne change pas entre-temps.

**Ce qui n'y est pas, volontairement** (voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md)) :

- Pas d'icône par type de monstre, texte seul pour l'instant ("Zombie : 12") — à ajouter plus
  tard si besoin, sans revoir cette classe puisque `SpawnableEnemy` porte déjà tout ce qu'il
  faut (`translationKey()`) pour brancher une icône dessus.
- Portée d'affichage plafonnée à 32 blocs (`MAX_DISTANCE_SQ`) pour éviter d'encombrer l'écran
  si beaucoup de spawners sont proches les uns des autres — valeur arbitraire, à ajuster si
  besoin une fois testée en jeu.

### Le compteur d'ennemis tués — `ModEvents.onMonsterDeath`

`WaveEnemiesOverlay` lisait déjà `ModAttachments.WAVE_ENEMIES_KILLED`, mais rien ne
l'incrémentait. Un handler `LivingDeathEvent` dans `ModEvents` : si l'entité qui meurt est un
`Monster` et que la phase est `COMBAT`, incrémente le compteur et le synchronise. Pas de
filtre sur "a été spawné par un `SpawnerBlockEntity`" — tout monstre mort en combat compte, ce
qui suffit au sens du HUD ("ennemis tués dans la vague").

### Le déroulement d'une vague — `init/PhaseTransitions.java`, `ModEvents.onMonsterDeath`

Ce qui manquait pour qu'une vague se déroule vraiment : un vrai déclencheur pour passer en
Combat (le vote "prêt", voir plus haut), un total juste (`WAVE_ENEMIES_TOTAL` était bloqué à
sa valeur par défaut), un retour automatique en Construction une fois ce total atteint, et
`CURRENT_WAVE` qui avance réellement d'une vague à l'autre.

**Le registre des spawners actifs** (`ModAttachments.ACTIVE_SPAWNERS`, un `Set<BlockPos>` sur
la `Level`) — nécessaire parce que calculer le total demande de connaître **tous** les
spawners de la carte, pas juste "moi-même" comme le fait déjà l'aperçu au-dessus de chaque
bloc (voir plus haut). Chaque `SpawnerBlockEntity` s'y ajoute dans `setLevel(...)` (appelé une
fois par instance, à la pose comme au chargement d'un chunk) et s'en retire dans
`setRemoved()`. Ni persistant ni synchronisé : usage strictement serveur, et un spawner non
chargé ne peut de toute façon pas spawn — l'exclure du registre est donc cohérent, pas un bug.
Ce registre ne reflète que "les spawners actuellement chargés" ; il deviendra pleinement fiable
une fois qu'un système force-chargera toute la zone de jeu pendant une partie (voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md)).

**Le calcul du total** (`PhaseTransitions.enterBuild(...)`) : à chaque entrée en Construction,
somme sur tous les spawners du registre (dont la vague courante tombe dans leur
`[waveStart, waveEnd]`) la même formule que l'aperçu déjà affiché au-dessus de chaque bloc
(`max(1, round(baseCount × multiplier))`). Recalculé **une seule fois**, à l'entrée en
Construction — pas en continu — parce que le GUI de config n'est plus accessible en pleine
partie (voir plus haut, verrou créatif) : rien ne peut changer la composition d'un spawner
une fois la Construction commencée, donc pas besoin de retour en arrière.

**La session de combat** (`ModAttachments.COMBAT_SESSION`, un compteur incrémenté à chaque
entrée en Combat) : chaque spawner relance sa propre progression de spawn (`resetForWave`)
au début de **chaque** session de combat, plutôt que seulement quand `CURRENT_WAVE` change
(remplace l'ancien déclencheur `lastWaveHandled`, désormais `lastCombatSessionHandled`). Cette
distinction reste utile même maintenant que `CURRENT_WAVE` avance : le harnais de test au clic
droit du `SpawnerBlock` (voir plus bas) permet toujours de rebasculer Combat → Construction →
Combat rapidement pour tester, ce qui fait aussi avancer la vague à chaque fois (voir plus
bas) — sans la session de combat comme déclencheur indépendant, un enchaînement de vagues très
rapide pourrait désynchroniser la remise à zéro de la progression de spawn.

**`PhaseTransitions.java`** centralise ces deux transitions (`enterCombat`/`enterBuild`) pour
que le vote "prêt", le harnais de test et le retour automatique de fin de vague passent tous
par le même code, plutôt que de dupliquer la remise à zéro des compteurs à trois endroits :

- `enterCombat(level)` : phase → `COMBAT`, incrémente `COMBAT_SESSION`, remet
  `WAVE_ENEMIES_KILLED` à 0, et remet "prêt" à faux pour tous les joueurs présents (voir plus
  haut, "Le vote prêt").
- `enterBuild(level)` : fait avancer `CURRENT_WAVE` de 1 (plafonné à `MAX_WAVE`, voir "Ce qui
  reste" plus bas), phase → `BUILD`, recalcule `WAVE_ENEMIES_TOTAL` à partir du registre pour
  la nouvelle vague.

**Le retour automatique** (`ModEvents.onMonsterDeath`) : après avoir incrémenté
`WAVE_ENEMIES_KILLED`, si `killed >= total` (et `total > 0`, pour ne pas basculer
immédiatement si aucun spawner n'a encore pu contribuer), regarde si la vague qu'on vient de
nettoyer était déjà `MAX_WAVE` (capturé **avant** d'appeler `enterBuild`/`onVictory`, puisque
les deux modifient `CURRENT_WAVE`) :

- Si ce n'était **pas** la dernière vague : `enterBuild(...)` comme avant, message
  `dungeon_defenders.spawner.wave_cleared`.
- Si c'**était** la dernière vague : `PhaseTransitions.onVictory(level)` à la place.

### Victoire et défaite — `PhaseTransitions.onVictory/onDefeat`

Deux nouvelles transitions, sur le même principe que `enterCombat`/`enterBuild`
(centralisées dans `PhaseTransitions`, pas dupliquées à chaque appelant) :

- **`onVictory(level)`** — appelée par `ModEvents.onMonsterDeath` quand la dernière vague
  vient d'être nettoyée. Diffuse `dungeon_defenders.game.victory` (vert, gras) à tous les
  joueurs, puis remet la partie à zéro.
- **`onDefeat(level)`** — appelée par `EterniaCrystalBlockEntity` juste après la destruction
  du bloc à 0 PV (à la suite du message `eternia_crystal.destroyed` déjà existant). Diffuse
  `dungeon_defenders.game.defeat` (rouge, gras), puis remet la partie à zéro.

Les deux passent par le même `resetGameState(level)` privé : `CURRENT_WAVE` → 1, phase →
`BUILD`, `WAVE_ENEMIES_KILLED` → 0, et `WAVE_ENEMIES_TOTAL` recalculé (réutilise
`recomputeWaveEnemiesTotal`, la même méthode privée qu'`enterBuild`) — pour que la partie soit
immédiatement prête à relancer une vague 1 propre, sans qu'un spawner continue à faire
apparaître des ennemis sur une partie déjà gagnée ou perdue.

**Ce qui n'est PAS fait ici**, volontairement — voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md) :

- Le Cristal d'Eternia détruit **n'est pas replacé automatiquement** : `resetGameState` remet
  les compteurs à zéro, mais le bloc lui-même reste absent tant que personne n'en repose un à
  la main. Remettre "le cristal" en jeu après une défaite fait partie de la future remise à
  neuf d'une map (structure reposée, tours retirées, PV du cristal restaurés), pas de ce
  morceau-ci.
- Pas d'écran dédié "Victoire"/"Game Over" (juste un message système) — l'idée d'un écran avec
  choix "rejouer/retour à la taverne" existe (vue dans le plan Excel du joueur), mais dépend du
  système de maps/structures (savoir où "rejouer" ou "la taverne" veulent dire concrètement),
  pas encore construit.
- Rien ne distingue encore une partie "terminée" (victoire/défaite) d'une simple pause entre
  deux vagues : les deux ramènent en phase `BUILD`, vague 1. Un joueur qui n'a pas vu le
  message peut ne pas remarquer que la partie a recommencé à zéro.

### Apparence

Modèle `cube_all` pointant **provisoirement** sur la texture vanilla
`minecraft:block/spawner` (la cage du spawner vanilla, cohérente thématiquement). Miné à la
pioche (tag `mineable/pickaxe`) et se drope lui-même via
`data/dungeon_defenders/loot_table/blocks/spawner.json`.

## Le score et le personnage — bas centre de l'écran

Deux dernières lignes de texte, centrées horizontalement tout en bas de l'écran (à droite de
la barre d'expérience, qui elle est en bas à gauche) :

```
        toto - niv 15
          Score : 0
```

### Le score — `client/gui/ScoreOverlay.java`

`ModAttachments.SCORE` est conceptuellement l'expérience gagnée **sur la carte en cours**,
par opposition à `ModAttachments.EXPERIENCE` qui est censée persister au-delà d'une carte.
C'est pourquoi ce n'est pas le même attachment, même si les deux valeurs pourraient un jour
augmenter ensemble (une capacité tuant un ennemi donnerait de l'XP *et* du score, un peu comme
la vue et le score au sens jeu vidéo classique). Comme `current_wave`, c'est un état de la
`Level` : "notre score" est partagé par la partie, pas individuel par joueur. Démarre à `0`,
rien ne l'alimente encore.

Affiché en texte seul (pas de jauge, un score n'a pas de maximum), clé
`dungeon_defenders.hud.score`, centré via `guiGraphics.centeredText(...)`. Expose
`rowY(guiGraphics)`, une méthode package-visible que `CharacterOverlay` utilise pour se
positionner juste au-dessus (même principe que `WaveOverlay.waveText(...)` ou
`ExperienceOverlay.barTop(...)`).

### Le personnage — `client/gui/CharacterOverlay.java`

Affiche `Nom - niv X` (clé `dungeon_defenders.hud.character`), juste au-dessus de
`ScoreOverlay`. Deux points à noter :

- **Le nom** (`ModAttachments.CHARACTER_NAME`) est un attachment `String`, distinct du pseudo
  Minecraft (`GameProfile.name()`) — c'est volontairement un champ à part, pour pouvoir
  diverger du compte du joueur. Sa valeur par défaut reprend le pseudo Minecraft (via
  `AttachmentType.builder(Function<IAttachmentHolder, T>)`, qui donne accès au holder — ici
  le `Player` — pour calculer la valeur initiale), juste pour ne pas afficher une chaîne vide
  tant qu'aucun nom n'a été choisi. Rien ne permet encore de le changer (pas de commande, pas
  d'écran de création de personnage) : voir
  [05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md).
- **Le niveau** (`ModAttachments.LEVEL`) est un attachment joueur (contrairement au score),
  démarre à `1`, persistant, synchronisé. Rien ne le fait encore monter — pas de formule
  d'XP → niveau, pas de notion de "monter de niveau".

## Les emplacements de compétences — `client/gui/AbilitySlotsOverlay.java`

Quatre ronds en bas à **gauche** de l'écran, juste à droite des losanges mana/vie et
au-dessus de la barre d'expérience — dans le prolongement du groupe bas-gauche décrit plus
haut, comme dans le jeu de référence, dans l'ordre gauche → droite :

```
   Mana        Vie
    ◆           ◆     ( ) ( ) ( ) ( )
   ▓█▓         ▓█▓     soin sort1 sort2 répare
  ▓███▓       ▓███▓
  ░░░░░       ▓▓▓▓▓
    ░           ▓
  [███░░░░░░░░░░░░░░] Experience: 0/100
```

1. Soin sur soi
2. Sort 1 du héros
3. Sort 2 du héros
4. Réparation de tour

**Purement visuel pour l'instant** : aucun clic, aucun cooldown, aucune consommation de mana,
aucune icône (elles arriveront plus tard, une par slot). C'est juste le fond des slots — un
rond avec une fine bordure, dessiné par `CircleSlot` (même philosophie que `DiamondGauge` :
`guiGraphics.fill()` empilés, bande par bande, largeur donnée par le théorème de Pythagore —
pas de texture ni de géométrie custom bas niveau).

Se positionne à partir de `HudLayout.MARGIN + DIAMOND_RADIUS * 4 + DIAMOND_GAP` (le bord droit
du losange vie), plus un petit écart (`GROUP_GAP`) — c'est le seul couplage avec le reste du
groupe bas-gauche, pour rester juste à côté des losanges plutôt que de dupliquer leur calcul
de position en dur.

`AbilitySlotsOverlay.SLOT_NAMES` est un tableau de 4 identifiants (`self_heal`,
`hero_spell_1`, `hero_spell_2`, `repair_tower`) qui ne sert encore à rien à l'exécution : il
documente juste l'ordre attendu, en attendant que chaque slot ait sa propre icône et sa propre
logique. Contrairement aux autres overlays, pas d'attachment ici non plus : il n'y a encore
aucun état à lire (pas de cooldown, pas de "sort débloqué ou non"), voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md).

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
