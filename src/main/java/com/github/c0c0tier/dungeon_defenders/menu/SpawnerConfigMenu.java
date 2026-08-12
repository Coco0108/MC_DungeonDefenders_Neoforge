package com.github.c0c0tier.dungeon_defenders.menu;

import com.github.c0c0tier.dungeon_defenders.init.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

// Menu sans slot : sert uniquement de vecteur pour ouvrir SpawnerConfigScreen côté client à
// partir d'un clic droit serveur (voir SpawnerBlock) et pour lui transmettre la position du
// spawner concerné. La configuration elle-même est lue depuis la copie cliente, déjà
// synchronisée, de SpawnerBlockEntity — pas depuis ce menu (voir SpawnerConfigScreen).
public class SpawnerConfigMenu extends AbstractContainerMenu {

    private final BlockPos pos;

    public SpawnerConfigMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenus.SPAWNER_CONFIG.get(), containerId);
        this.pos = pos;
    }

    // Constructeur attendu côté client par IMenuTypeExtension.create(...) : reconstruit le
    // menu à partir du BlockPos écrit par SpawnerConfigMenuProvider#writeClientSideData.
    public SpawnerConfigMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
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
        // Pas de vérification de portée ici : SpawnerConfigPayload revérifie la portée et
        // l'existence du bloc côté serveur avant d'appliquer quoi que ce soit, ce qui suffit
        // puisqu'il n'y a ni slot ni item en jeu dans ce menu.
        return true;
    }
}
