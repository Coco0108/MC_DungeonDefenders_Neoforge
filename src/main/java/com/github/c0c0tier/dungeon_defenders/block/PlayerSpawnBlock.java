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

// Marqueur posé par le créateur d'une map à l'endroit où les joueurs doivent apparaître : repéré
// et consommé par MapInstance#findAndConsumeSpawnMarker, toute la logique vit là-bas, aucun
// comportement au clic ici.
//
// Demandé en jeu (2026-08-30) : traité exactement comme SpawnerBlock — invisible, traversable,
// ciblable en créatif uniquement. C'est un marqueur d'édition, pas un élément de décor : il ne
// doit ni se voir ni gêner un déplacement pendant une partie. Ce n'était pas purement théorique
// même s'il s'auto-supprime au démarrage : findAndConsumeSpawnMarker ne consomme que le PREMIER
// marqueur trouvé, tous les autres restent en place et visibles, et le marqueur est de toute
// façon visible tant que la partie n'a pas démarré.
//
// Cette classe existe uniquement pour ces trois overrides ; le bloc était auparavant enregistré
// via BLOCKS.registerSimpleBlock (aucune classe dédiée).
public class PlayerSpawnBlock extends Block {

    public static final MapCodec<PlayerSpawnBlock> CODEC = simpleCodec(PlayerSpawnBlock::new);

    public PlayerSpawnBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    // Jamais rendu, pour personne. Même limite assumée que pour SpawnerBlock : cette méthode ne
    // reçoit que le BlockState, pas de niveau ni de joueur, donc impossible de la faire dépendre
    // de qui regarde (contrairement à getShape ci-dessous) — un créateur de map en créatif ne
    // voit donc pas non plus le bloc, il le retrouve par le contour de visée.
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // Toujours vide : lu par la résolution de collision physique — joueur comme monstre
    // traversent la position, quelle que soit la phase ou le mode de jeu.
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    // Pleine (ciblable/cliquable) uniquement pour un joueur en créatif : c'est cette forme, pas
    // getCollisionShape, que le jeu utilise pour le rayon de visée. Vide pour tout le reste
    // (survie, ou tout appel sans entité précise — CollisionContext.empty() laisse getEntity()
    // à null : génération de terrain, pathfinding, et le calcul d'occlusion/lumière fait à
    // l'initialisation du BlockState). Conséquence utile de ce dernier point : le bloc ne bloque
    // ni la lumière ni le rendu des faces voisines, sans avoir besoin de noOcclusion() dans ses
    // Properties — un bloc invisible qui occlurait laisserait un trou noir visible.
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
