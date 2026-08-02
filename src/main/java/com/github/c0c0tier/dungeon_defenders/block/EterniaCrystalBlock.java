package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EterniaCrystalBlock extends BaseEntityBlock {

    // La bonne syntaxe pour déclarer le codec d'un bloc simple
    public static final MapCodec<EterniaCrystalBlock> CODEC = simpleCodec(EterniaCrystalBlock::new);

    public EterniaCrystalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EterniaCrystalBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Crée une boîte qui fait 1 bloc de large (de 0 à 16) mais 3 blocs de haut (de 0 à 48)
        return Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 3.0D, 1.0D);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // La hitbox de sélection (le contour noir quand on le regarde) s'aligne aussi sur les 3 blocs de haut
        return Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 3.0D, 1.0D);
    }

    /** Dégâts infligés au clic droit à main nue. Harnais de test. */
    private static final int DEBUG_DAMAGE_ON_USE = 10;

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // Le client se contente de prédire le succès ; la logique tourne côté serveur.
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof EterniaCrystalBlockEntity crystal)) {
            return InteractionResult.PASS;
        }

        crystal.damage(DEBUG_DAMAGE_ON_USE);
        player.sendSystemMessage(Component.translatable(
                "dungeon_defenders.eternia_crystal.damaged", DEBUG_DAMAGE_ON_USE, crystal.getCrystalHealth()));

        return InteractionResult.SUCCESS;
    }
}