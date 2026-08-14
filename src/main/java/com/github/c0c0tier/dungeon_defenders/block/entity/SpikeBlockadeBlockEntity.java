package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.WeakHashMap;

// Premier "vrai" tower du mod (voir doc/02-gameplay.md) : un mur avec ses propres PV — bloque
// le passage (un bloc plein bloque déjà naturellement, rien à faire pour ça), les monstres
// doivent le détruire pour continuer (voir entity/ai/AttackBlockadeGoal.java), et pique
// automatiquement tout monstre à son contact à intervalle régulier — même mécanique de
// cooldown par entité que l'ancien SpikeTrapBlock (piège au sol, supprimé), mais déclenchée par
// la proximité plutôt que par stepOn.
public class SpikeBlockadeBlockEntity extends BlockEntity {

    public static final int DEFAULT_HEALTH = 30;
    private static final float CONTACT_DAMAGE = 2.0F;
    private static final long CONTACT_DAMAGE_INTERVAL_TICKS = 20L;
    // Distance sur laquelle le blockade "gonfle" sa propre boîte pour détecter un contact —
    // approximation grossière de la portée de mêlée, pas une vraie détection de collision.
    private static final double CONTACT_RANGE = 1.0D;

    private int health = DEFAULT_HEALTH;

    // WeakHashMap : ne retient pas les entités mortes/déchargées, évite une fuite mémoire.
    private final Map<Monster, Long> lastContactDamageTick = new WeakHashMap<>();

    public SpikeBlockadeBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.SPIKE_BLOCKADE_BE.get(), pos, state);
    }

    public int getHealth() {
        return this.health;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpikeBlockadeBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        AABB contactArea = new AABB(pos).inflate(CONTACT_RANGE);
        long now = serverLevel.getGameTime();

        for (Monster monster : serverLevel.getEntitiesOfClass(Monster.class, contactArea)) {
            Long lastTrigger = blockEntity.lastContactDamageTick.get(monster);
            if (lastTrigger != null && now - lastTrigger < CONTACT_DAMAGE_INTERVAL_TICKS) {
                continue;
            }

            blockEntity.lastContactDamageTick.put(monster, now);
            monster.hurt(serverLevel.damageSources().stalagmite(), CONTACT_DAMAGE);
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
        this.health = input.getIntOr("Health", DEFAULT_HEALTH);
    }
}
