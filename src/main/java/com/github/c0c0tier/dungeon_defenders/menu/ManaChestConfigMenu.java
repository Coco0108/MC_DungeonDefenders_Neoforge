package com.github.c0c0tier.dungeon_defenders.menu;

import com.github.c0c0tier.dungeon_defenders.init.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

// Même principe que SpawnerConfigMenu : aucun slot, sert uniquement de vecteur pour ouvrir
// ManaChestConfigScreen côté client à partir d'un clic droit serveur (voir ManaChestBlock) et
// lui transmettre la position du coffre concerné.
public class ManaChestConfigMenu extends AbstractContainerMenu {

    private final BlockPos pos;

    public ManaChestConfigMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenus.MANA_CHEST_CONFIG.get(), containerId);
        this.pos = pos;
    }

    // Constructeur attendu côté client par IMenuTypeExtension.create(...) : reconstruit le
    // menu à partir du BlockPos écrit par ManaChestConfigMenuProvider#writeClientSideData.
    public ManaChestConfigMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
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
        // Pas de vérification de portée ici : ManaChestConfigPayload revérifie la portée et
        // l'existence du bloc côté serveur avant d'appliquer quoi que ce soit.
        return true;
    }
}
