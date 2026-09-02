package com.github.c0c0tier.dungeon_defenders.menu;

import com.github.c0c0tier.dungeon_defenders.init.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

// Menu sans slot, calqué sur SpawnerConfigMenu : sert uniquement à ouvrir MapConfigScreen côté
// client depuis un clic droit serveur, en lui transmettant la position du bloc. Les valeurs
// affichées sont lues depuis la copie cliente de MapConfigBlockEntity, déjà synchronisée.
public class MapConfigMenu extends AbstractContainerMenu {

    private final BlockPos pos;

    public MapConfigMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenus.MAP_CONFIG.get(), containerId);
        this.pos = pos;
    }

    public MapConfigMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    public BlockPos pos() {
        return this.pos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        // Comme SpawnerConfigMenu : la portée et l'existence du bloc sont revérifiées côté
        // serveur par MapConfigPayload, ce qui suffit puisqu'il n'y a ni slot ni item ici.
        return true;
    }
}
