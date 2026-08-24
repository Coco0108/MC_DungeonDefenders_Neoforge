package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.ManaChestBlock;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// Meuble de map (posé par le créateur, pas par un joueur en jeu — voir ManaChestBlock) qui
// donne du mana au clic droit, une fois par vague : "se remplit" à chaque nouvelle
// Construction plutôt qu'un vrai minuteur, en comparant lastOpenedWave à CURRENT_WAVE (déjà
// incrémenté à chaque entrée en Construction par PhaseTransitions#enterBuild). Visuellement,
// un coffre ouvert devient invisible/traversable (ManaChestBlock#OPENED) jusqu'à la vague
// suivante — voir ManaChestBlock#respawnAll, qui a besoin d'ACTIVE_MANA_CHESTS pour savoir
// quels coffres existent, même principe qu'ACTIVE_SPAWNERS pour les spawners.
// Distribuera aussi des armes plus tard (feuille "Idées" du plan Excel du joueur) : hors
// scope pour l'instant, voir doc/05-etat-et-problemes-connus.md.
public class ManaChestBlockEntity extends BlockEntity {

    private static final int DEFAULT_MANA_AMOUNT = 25;

    // Configurable par map (voir ManaChestConfigScreen) : la quantité dépend de la taille/
    // difficulté de la map, décidé avec le joueur — pas de valeur fixe globale.
    private int manaAmount = DEFAULT_MANA_AMOUNT;
    // 0 = jamais ouvert. Comparé à CURRENT_WAVE plutôt que stocké comme un simple booléen :
    // se "recharge" tout seul à chaque nouvelle vague sans qu'aucun code n'ait besoin de le
    // remettre à zéro explicitement (contrairement à WAVE_ENEMIES_KILLED, par exemple).
    private int lastOpenedWave;

    public ManaChestBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.MANA_CHEST_BE.get(), pos, state);
    }

    // --- REGISTRE DES COFFRES ACTIFS ---
    // Même principe que SpawnerBlockEntity#setLevel/#setRemoved et ACTIVE_SPAWNERS : permet à
    // ManaChestBlock#respawnAll de savoir quels coffres existent sans parcourir tous les
    // chunks chargés.

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel) {
            level.getData(ModAttachments.ACTIVE_MANA_CHESTS).add(this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level instanceof ServerLevel) {
            this.level.getData(ModAttachments.ACTIVE_MANA_CHESTS).remove(this.worldPosition);
        }
    }

    public int getManaAmount() {
        return this.manaAmount;
    }

    /** Appliqué par le créateur de map via ManaChestConfigScreen (menu créatif seulement, voir ManaChestBlock). */
    public void applyConfig(int manaAmount) {
        this.manaAmount = Math.max(0, manaAmount);
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    /**
     * Tente de donner {@link #manaAmount} à {@code player}. Ne fait rien et renvoie
     * {@code false} si déjà ouvert pour {@code currentWave} — toute la logique "coffre" vit
     * ici plutôt que dans le bloc, comme SpawnerBlockEntity porte son propre algorithme.
     */
    public boolean tryOpen(Player player, int currentWave) {
        if (this.lastOpenedWave == currentWave) {
            return false;
        }

        this.lastOpenedWave = currentWave;
        setChanged();

        int currentMana = player.getData(ModAttachments.MANA);
        int newMana = Math.min(ModAttachments.MAX_MANA, currentMana + this.manaAmount);
        player.setData(ModAttachments.MANA, newMana);
        player.syncData(ModAttachments.MANA);

        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS);
            // Devient invisible/traversable jusqu'à la prochaine Construction (voir
            // ManaChestBlock#respawnAll) — le bloc entity, lui, n'est pas touché : même
            // instance, même config, seule la propriété OPENED du blockstate change.
            this.level.setBlock(
                    this.worldPosition, this.getBlockState().setValue(ManaChestBlock.OPENED, true), Block.UPDATE_ALL);
        }

        return true;
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
        output.putInt("ManaAmount", this.manaAmount);
        output.putInt("LastOpenedWave", this.lastOpenedWave);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.manaAmount = input.getIntOr("ManaAmount", DEFAULT_MANA_AMOUNT);
        this.lastOpenedWave = input.getIntOr("LastOpenedWave", 0);
    }
}
