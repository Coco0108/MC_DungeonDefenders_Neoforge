package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.github.c0c0tier.dungeon_defenders.atelier.AnimationsBloc;
import com.github.c0c0tier.dungeon_defenders.atelier.BlocAnimable;
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
 * Un block entity du jeu dessiné par le modèle animé d'un bloc de décor de l'Atelier (la forge : {@code dd_forge_v2},
 * le mannequin : {@code dd_dummy}, les deux cristaux : {@code dd_cristal_v2}), comme le bloc emprunte déjà son modèle
 * fixe et sa forme ({@code block/FormeAtelier}). Les animations elles-mêmes sont dans {@link AnimationsBloc} ; le
 * renderer commun ({@code client/atelier/BlocAnimeRenderer}) les dessine. Synchronisé avec les clients (état ouvert).
 */
public abstract class AnimatedBlockEntity extends BlockEntity implements BlocAnimable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationsBloc animations;

    protected AnimatedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, String blocAtelier) {
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
