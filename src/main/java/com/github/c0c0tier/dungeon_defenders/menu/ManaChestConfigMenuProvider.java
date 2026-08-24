package com.github.c0c0tier.dungeon_defenders.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Donné à {@code player.openMenu(...)} par ManaChestBlock ; transmet la position du coffre au client. */
public record ManaChestConfigMenuProvider(BlockPos pos) implements MenuProvider {

    @Override
    public Component getDisplayName() {
        return Component.translatable("dungeon_defenders.mana_chest.config_title");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ManaChestConfigMenu(containerId, playerInventory, this.pos);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }
}
