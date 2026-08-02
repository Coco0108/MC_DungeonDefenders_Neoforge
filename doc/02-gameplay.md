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

Le code est gardé par `if (!level.isClientSide())` pour ne s'exécuter que côté serveur.

### L'état — `block/entity/EterniaCrystalBlockEntity.java`

```java
public static final int DEFAULT_HEALTH = 100;
private int crystalHealth = DEFAULT_HEALTH;
```

- `getCrystalHealth()` / `setCrystalHealth(int)` : accès aux PV.
- `setCrystalHealth` appelle `setChanged()` (marque le chunk à sauvegarder), diffuse les PV
  restants à **tous les joueurs du monde** dans le chat, puis, si les PV tombent à ≤ 0 :
  - `level.destroyBlock(worldPosition, false)` — le `false` empêche le drop de l'item ;
  - message `§c§lLe cristal a été détruit !! Game Over !` à tous les joueurs.

**Persistance.** `saveAdditional` / `loadAdditional` utilisent l'API `ValueOutput` /
`ValueInput` (le remplaçant des `CompoundTag` bruts) :

```java
output.putInt("CrystalHealth", this.crystalHealth);
this.crystalHealth = input.getIntOr("CrystalHealth", DEFAULT_HEALTH);
```

Les PV survivent donc au rechargement du monde.

**Code désactivé.** `updateTextDisplay()` / `removeTextDisplay()` créent une entité
`Display.TextDisplay` flottante au-dessus du cristal (Y + 3.2), en billboard face au joueur,
portée 30 blocs, avec un texte coloré selon les PV (vert > 50, orange > 20, rouge sinon).
Tous les appels sont **commentés** — remplacés par le renderer custom ci-dessous. C'est ce
code qui justifie l'access transformer ; les méthodes sont conservées, prêtes à être
réactivées.

## Rendu de la barre de vie — `EterniaCrystalBlockEntityRenderer.java`

Renderer client (`BlockEntityRenderer`), enregistré dans `DungeonDefendersModClient.onClientSetup`.

Fonctionnement :

1. Translation à `(0.5, 3.2, 0.5)` — centré au-dessus de la hitbox de 3 blocs.
2. Billboard manuel : rotation inverse du yaw/pitch du joueur local, pour que la barre soit
   toujours face à la caméra.
3. `healthPercent = clamp(currentHealth / DEFAULT_HEALTH, 0, 1)`.
4. Deux quads dessinés via `renderBar` :
   - le fond, gris (`0.3, 0.3, 0.3`), largeur fixe `2.0`, de `x = -1` à `x = +1` ;
   - la jauge, largeur `2.0 * healthPercent`.
5. Couleur de la jauge : dégradé **vert → jaune → rouge** calculé par `getRed`/`getGreen`/`getBlue`.
   - Au-dessus de 50 % : rouge monte de 0 → 1, vert reste à 1 (vert → jaune).
   - En dessous : rouge reste à 1, vert descend de 1 → 0 (jaune → rouge).
   - Bleu toujours 0.

> ⚠️ Ce renderer utilise l'API `VertexConsumer.vertex(...).endVertex()` et un constructeur
> prenant un `BlockEntityRendererProvider.Context` qui n'est pas celui appelé côté client.
> Voir [05-etat-et-problemes-connus.md](05-etat-et-problemes-connus.md).

## IA des ennemis — `ModEvents.java`

Écoute `EntityJoinLevelEvent` sur le bus de jeu. Pour chaque `Zombie` rejoignant un monde
côté serveur, un `MoveToBlockGoal` anonyme est ajouté au `goalSelector` **en priorité 1**
(donc au-dessus de la plupart des objectifs vanilla).

| Paramètre | Valeur | Rôle |
|---|---|---|
| Vitesse | `1.2D` | multiplicateur de vitesse de déplacement |
| Rayon de recherche | `16` | blocs autour du zombie |
| `isValidTarget` | `state.is(ModBlocks.ETERNIA_CRYSTAL)` | ne cible que le cristal |
| `getMoveToTarget` | `this.blockPos` | vise la **base** du cristal, pour éviter que le zombie tente de grimper |
| `acceptedDistance` | `2.1D` | tolérance suffisante pour un zombie au sol contre une hitbox de 3 de haut |

Dans `tick()`, si la cible est atteinte (`isReachedTarget()`), le zombie frappe **toutes les
20 ticks (1 seconde)**, condition `mob.tickCount % 20 == 0` :

- `crystal.setCrystalHealth(currentHealth - 5)` — **5 dégâts par seconde par zombie** ;
- `mob.swing(InteractionHand.MAIN_HAND)` — animation de bras.

Avec 100 PV par défaut, un zombie seul détruit le cristal en 20 secondes.

> Le compteur utilise `mob.tickCount` (âge de l'entité), pas un compteur propre au goal : le
> rythme de frappe est donc désynchronisé d'un zombie à l'autre, ce qui est plutôt souhaitable
> visuellement.

## Onglet créatif

`dungeon_defenders_tab`, titre `Component.translatable("itemGroup.dungeon_defenders")`,
icône et unique entrée : l'item du cristal. La clé de traduction n'existe pas encore dans
`en_us.json` — voir [05](05-etat-et-problemes-connus.md).
