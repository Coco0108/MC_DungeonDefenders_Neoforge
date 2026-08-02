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
icône et unique entrée : l'item du cristal.

## Apparence du bloc

Le bloc utilise un modèle `cube_all` standard, mais pointe **provisoirement** sur la texture
vanilla `minecraft:block/diamond_block` : il n'y a pas encore de texture dédiée. Voir
[05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md).

Le bloc est miné à la pioche en diamant (tags `mineable/pickaxe` et `needs_diamond_tool`) et
se drope lui-même via `data/dungeon_defenders/loot_table/blocks/eternia_crystal.json`.
