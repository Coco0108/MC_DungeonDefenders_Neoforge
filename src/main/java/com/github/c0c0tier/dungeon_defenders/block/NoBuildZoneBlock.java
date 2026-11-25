package com.github.c0c0tier.dungeon_defenders.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Marqueur de zone interdite à la pose de tours, posé par le créateur d'une map.
//
// **Liste noire, pas liste blanche** (décidé avec le joueur, 2026-08-31) : le joueur peut poser
// partout SAUF là où le mappeur a marqué. Choisi exprès à l'inverse du jeu de référence (qui
// définit des zones autorisées) — c'est plus ouvert à la créativité du joueur, et un oubli du
// mappeur autorise une pose en trop plutôt que de rendre un endroit injouable.
//
// Un marqueur = une position interdite. On "peint" donc la zone à couvrir, typiquement avec un
// /fill en créatif ; le format structure les sauvegarde comme n'importe quel bloc.
//
// Invisible, traversable, ciblable en créatif seulement : c'est un marqueur d'édition, même
// traitement que SpawnerBlock, PlayerSpawnBlock et TrainingDummyBlock (voir ces classes pour le
// détail du raisonnement, identique ici).
//
// **Comment il bloque réellement la pose** : un bloc normal n'est pas `canBeReplaced()`, et
// c'est précisément ce que testent le ciblage côté client (TowerPlacementClientEvents) et
// l'autorité serveur (ModNetworking#handlePlaceTower) pour décider si une position est libre.
// Occuper la case suffit donc à interdire la pose, sans logique supplémentaire. Les deux
// endroits ajoutent quand même un test explicite du marqueur : uniquement pour pouvoir
// **expliquer** le refus au joueur (qui ne voit rien, le bloc étant invisible) au lieu d'un
// hologramme rouge sans raison apparente.
public class NoBuildZoneBlock extends Block {

    public static final MapCodec<NoBuildZoneBlock> CODEC = simpleCodec(NoBuildZoneBlock::new);

    public NoBuildZoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // Traversable pour tout le monde : un monstre doit pouvoir traverser une zone simplement
    // interdite à la construction, et le joueur aussi.
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    // Pleine (ciblable) uniquement pour un joueur en créatif — le seul moyen pour le mappeur de
    // retrouver ses marqueurs. Vide pour tout le reste, y compris les appels sans entité
    // précise : le calcul d'occlusion/lumière fait à l'initialisation du BlockState passe par
    // là, donc le marqueur n'occulte rien et ne fait pas d'ombre, sans avoir besoin de
    // noOcclusion() dans ses Properties.
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof Player player
                && player.isCreative()) {
            return Shapes.block();
        }
        return Shapes.empty();
    }
}
