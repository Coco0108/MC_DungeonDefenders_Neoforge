package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.PhaseTransitions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EterniaCrystalBlockEntity extends BlockEntity {

    public static final int DEFAULT_HEALTH = 100;
    private int crystalHealth = DEFAULT_HEALTH;

    public EterniaCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.ETERNIA_CRYSTAL_BE.get(), pos, state);
    }

    public int getCrystalHealth() {
        return this.crystalHealth;
    }

    /** Retire {@code amount} PV au cristal. */
    public void damage(int amount) {
        this.setCrystalHealth(this.crystalHealth - amount);
    }

    public void setCrystalHealth(int health) {
        // Les PV ne descendent jamais sous 0 : le renderer clampe déjà l'affichage,
        // mais la valeur stockée doit rester cohérente.
        int clamped = Math.max(0, health);
        if (clamped == this.crystalHealth) {
            return;
        }

        this.crystalHealth = clamped;
        setChanged();

        if (this.level == null || this.level.isClientSide()) {
            return;
        }

        // Pousse les PV vers les clients : la barre de vie lit getCrystalHealth()
        // côté client, sans ça elle resterait figée à 100 %.
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);

        if (this.crystalHealth <= 0) {
            // Le false empêche le drop de l'item.
            this.level.destroyBlock(this.worldPosition, false);

            this.level.players().forEach(player -> player.sendSystemMessage(
                    Component.translatable("dungeon_defenders.eternia_crystal.destroyed")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));

            // Remet la partie à zéro (vague, phase) pour que les spawners arrêtent de faire
            // apparaître des ennemis sur une partie déjà perdue. Le cristal détruit n'est pas
            // replacé automatiquement, voir 05-etat-et-problemes-connus.md.
            PhaseTransitions.onDefeat(this.level);
        }
    }

    // --- SYNCHRONISATION CLIENT ---

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // Réutilise saveAdditional : le client reçoit donc CrystalHealth.
        return this.saveWithoutMetadata(registries);
    }

    // --- PERSISTANCE ---

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("CrystalHealth", this.crystalHealth);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // getIntOr renvoie la valeur sauvegardée, ou la valeur par défaut si absente
        this.crystalHealth = input.getIntOr("CrystalHealth", DEFAULT_HEALTH);
    }
}
