package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractBlockadeBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractPulseTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTurretBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BiFunction;

// Bloc de tour générique, introduit avec le roster de l'Alchimiste (2026-11-16) : les six tours
// de l'Écuyer ont chacune leur classe de bloc, toutes identiques à une ligne près (le block
// entity créé). Ici le block entity est passé au constructeur, une seule classe sert donc à
// toutes les tours sans orientation ; DirectionalTowerBlock ajoute HORIZONTAL_FACING.
//
// Même hitbox de 1,5 bloc de haut que les autres tours (anti-escalade des monstres).
public class SimpleTowerBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.5D, 1.0D);

    private final BiFunction<BlockPos, BlockState, ? extends BlockEntity> blockEntityFactory;
    private final MapCodec<SimpleTowerBlock> codec;

    public SimpleTowerBlock(Properties properties, BiFunction<BlockPos, BlockState, ? extends BlockEntity> blockEntityFactory) {
        super(properties);
        this.blockEntityFactory = blockEntityFactory;
        this.codec = simpleCodec(this::recreate);
    }

    /** Le même bloc avec d'autres propriétés : ce que le codec doit savoir reconstruire. */
    protected SimpleTowerBlock recreate(Properties properties) {
        return new SimpleTowerBlock(properties, this.blockEntityFactory);
    }

    protected BiFunction<BlockPos, BlockState, ? extends BlockEntity> blockEntityFactory() {
        return this.blockEntityFactory;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return this.codec;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.blockEntityFactory.apply(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // Pas de BlockEntityType à comparer (le bloc ne le connaît pas) : le ticker reconnaît la
    // catégorie de la tour (impulsions, Turret, Blockade) et ne fait rien pour celles qui
    // n'agissent pas à chaque tick (Cuve instable, Condenseur).
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof AbstractPulseTowerBlockEntity tower && tickLevel instanceof ServerLevel serverLevel) {
                tower.serverTick(serverLevel);
            } else if (blockEntity instanceof AbstractTurretBlockEntity turret) {
                AbstractTurretBlockEntity.serverTick(tickLevel, pos, tickState, turret);
            } else if (blockEntity instanceof AbstractBlockadeBlockEntity blockade) {
                AbstractBlockadeBlockEntity.serverTick(tickLevel, pos, tickState, blockade);
            }
        };
    }
}
