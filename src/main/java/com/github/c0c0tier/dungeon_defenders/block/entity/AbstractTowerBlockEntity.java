package com.github.c0c0tier.dungeon_defenders.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// Base commune à TOUTES les catégories de tours (Blockade, Turret, et les suivantes) : PV,
// coût en mana à la pose, persistance et synchronisation client. Extraite quand une deuxième
// catégorie concrète (Turret, voir AbstractTurretBlockEntity) a fait apparaître une vraie
// duplication avec AbstractBlockadeBlockEntity — pas devinée à l'avance, voir
// doc/05-etat-et-problemes-connus.md. Chaque catégorie ajoute ensuite ce qui est spécifique à
// sa façon d'agir (dégâts de contact pour Blockade, portée + cône + tir pour Turret).
//
// implements AiAttackTarget : damage(int) est déjà ce qu'il faut, getAiPriority() reste
// abstrait — chaque catégorie décide de sa propre priorité (voir AiAttackTarget pour le
// détail des paliers).
public abstract class AbstractTowerBlockEntity extends BlockEntity implements AiAttackTarget {

    private final int maxHealth;
    // Coût en mana à la pose, consommé par ModEvents.onTowerPlace (générique à toute la
    // catégorie, pas seulement Blockade).
    private final int manaCost;

    private int health;

    protected AbstractTowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            int maxHealth, int manaCost) {
        super(type, pos, state);
        this.maxHealth = maxHealth;
        this.manaCost = manaCost;
        this.health = maxHealth;
    }

    public int getHealth() {
        return this.health;
    }

    public int getMaxHealth() {
        return this.maxHealth;
    }

    public int getManaCost() {
        return this.manaCost;
    }

    @Override
    public abstract int getAiPriority();

    /** Retire {@code amount} PV à la tour. */
    @Override
    public void damage(int amount) {
        setHealth(this.health - amount);
    }

    public void setHealth(int health) {
        int clamped = Math.max(0, health);
        if (clamped == this.health) {
            return;
        }

        this.health = clamped;
        setChanged();

        if (this.level == null || this.level.isClientSide()) {
            return;
        }

        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);

        if (this.health <= 0) {
            // Le false empêche le drop de l'item : une tour détruite au combat ne se
            // récupère pas, comme le Cristal d'Eternia.
            this.level.destroyBlock(this.worldPosition, false);
        }
    }

    // --- SYNCHRONISATION CLIENT ---

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    // --- PERSISTANCE ---

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Health", this.health);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.health = input.getIntOr("Health", this.maxHealth);
    }
}
