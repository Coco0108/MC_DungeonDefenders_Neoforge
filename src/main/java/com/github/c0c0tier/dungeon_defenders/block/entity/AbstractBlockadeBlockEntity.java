package com.github.c0c0tier.dungeon_defenders.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.WeakHashMap;

// Base commune de la catégorie de tours "Blockade" (voir doc/05-etat-et-problemes-connus.md,
// section Système de tours) : un mur avec ses propres PV, qui bloque le passage gratuitement
// (un bloc plein bloque déjà naturellement) et que les monstres doivent détruire pour
// continuer (voir entity/ai/AttackBlockadeGoal.java, qui vise tout bloc du tag
// dungeon_defenders:blockades). Regroupe ce que la première version de la taxonomie séparait en
// "block passif" et "corps à corps" : un blockade passif est juste un sous-type avec
// dealsContactDamage=false.
public abstract class AbstractBlockadeBlockEntity extends BlockEntity {

    private final int maxHealth;
    // Coût en mana à la pose — stat réservée pour la future économie de mana (pas encore
    // consommée nulle part, voir 05-etat-et-problemes-connus.md).
    private final int manaCost;
    private final boolean dealsContactDamage;
    private final float contactDamage;
    private final long contactDamageIntervalTicks;
    private final double contactRange;

    private int health;

    // WeakHashMap : ne retient pas les entités mortes/déchargées, évite une fuite mémoire.
    private final Map<Monster, Long> lastContactDamageTick = new WeakHashMap<>();

    protected AbstractBlockadeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            int maxHealth, int manaCost, boolean dealsContactDamage,
            float contactDamage, long contactDamageIntervalTicks, double contactRange) {
        super(type, pos, state);
        this.maxHealth = maxHealth;
        this.manaCost = manaCost;
        this.dealsContactDamage = dealsContactDamage;
        this.contactDamage = contactDamage;
        this.contactDamageIntervalTicks = contactDamageIntervalTicks;
        this.contactRange = contactRange;
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

    /** Retire {@code amount} PV au blockade. */
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
            // Le false empêche le drop de l'item : un blockade détruit au combat ne se
            // récupère pas, comme le Cristal d'Eternia.
            this.level.destroyBlock(this.worldPosition, false);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractBlockadeBlockEntity blockEntity) {
        if (!blockEntity.dealsContactDamage) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        AABB contactArea = new AABB(pos).inflate(blockEntity.contactRange);
        long now = serverLevel.getGameTime();

        for (Monster monster : serverLevel.getEntitiesOfClass(Monster.class, contactArea)) {
            Long lastTrigger = blockEntity.lastContactDamageTick.get(monster);
            if (lastTrigger != null && now - lastTrigger < blockEntity.contactDamageIntervalTicks) {
                continue;
            }

            blockEntity.lastContactDamageTick.put(monster, now);
            monster.hurt(serverLevel.damageSources().stalagmite(), blockEntity.contactDamage);
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
