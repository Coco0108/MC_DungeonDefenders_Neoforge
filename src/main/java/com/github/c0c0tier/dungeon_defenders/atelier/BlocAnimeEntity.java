package com.github.c0c0tier.dungeon_defenders.atelier;

import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Le block entity d'un bloc de décor animé de l'Atelier ({@link BlocAnime}, {@link BlocAnimeOriente}) : un type par
 * bloc, comme un type d'entité par monstre. Il ne fait que tenir ses animations ({@link AnimationsBloc}) ; au repos il
 * boucle {@code idle}, les autres rôles se jouent par la commande {@code /dd_bloc_anim}.
 */
public class BlocAnimeEntity extends BlockEntity implements BlocAnimable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationsBloc animations;

    public BlocAnimeEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, String blocAtelier) {
        super(type, pos, state);
        this.animations = new AnimationsBloc(blocAtelier);
    }

    @Override
    public AnimationsBloc animations() {
        return animations;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        animations.registerControllers(controllers);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        animations.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        animations.load(input);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
