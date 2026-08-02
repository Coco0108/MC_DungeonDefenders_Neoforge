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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // On s'assure d'exécuter le code côté serveur (là où la logique s'exécute)
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof EterniaCrystalBlockEntity crystal) {
                // 1. On récupère les PV actuels
                int currentHealth = crystal.getCrystalHealth();

                // 2. On lui retire 10 PV pour tester la modification
                int newHealth = currentHealth - 10;
                crystal.setCrystalHealth(newHealth);

                // 3. On envoie un message au joueur dans le chat
                player.sendSystemMessage(Component.literal("Aïe ! Le cristal perd 10 PV. PV restants : " + newHealth));

                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.SUCCESS;
    }
}