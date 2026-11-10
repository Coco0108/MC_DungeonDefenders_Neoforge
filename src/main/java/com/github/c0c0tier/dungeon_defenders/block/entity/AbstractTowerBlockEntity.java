package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.init.BalanceValue;
import com.github.c0c0tier.dungeon_defenders.init.GameBalance;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import com.github.c0c0tier.dungeon_defenders.init.TowerStats;
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

    // Quelle tour du catalogue est ce block entity : c'est par elle qu'il lit ses chiffres
    // (PV max, coût, dégâts, portée...), data-driven depuis le 2026-11-10 — voir
    // init/TowerStatsRegistry. Jamais recopiés dans des champs : un /reload s'applique ainsi aux
    // tours déjà posées, sans les reposer.
    private final TowerDefinition definition;

    private int health;
    // Niveau d'amélioration, 1 à la pose (voir upgrade()). Persistant et synchronisé : le client
    // en a besoin pour les PV max de la barre de vie et pour l'affichage de l'inspection.
    private int towerLevel = 1;

    protected AbstractTowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            TowerDefinition definition) {
        super(type, pos, state);
        this.definition = definition;
        this.health = getMaxHealth();
    }

    public TowerDefinition definition() {
        return this.definition;
    }

    /**
     * Les chiffres en vigueur pour cette tour, du bon côté : la copie client si ce block entity
     * vit dans un monde client, celle du serveur sinon (y compris avant d'avoir une Level, le
     * temps de sa construction).
     */
    protected TowerStats stats() {
        return this.definition.stats(this.level != null && this.level.isClientSide());
    }

    public int getHealth() {
        return this.health;
    }

    public int getMaxHealth() {
        return (int) Math.round(stats().intValue(TowerStat.MAX_HEALTH) * levelMultiplier());
    }

    /** Coût en mana à la pose, consommé par ModEvents.onTowerPlace (générique à toute catégorie). */
    public int getManaCost() {
        return stats().intValue(TowerStat.MANA_COST);
    }

    // --- AMÉLIORATION ---
    // Idée du backlog ("4 niveaux par tour, comme le jeu de référence"), faite le 2026-11-10.
    // Une tour posée est niveau 1 ; chaque niveau coûte une part de son coût de pose et ajoute
    // un bonus à ses PV max et à ses dégâts. Les trois réglages sont data-driven
    // (balance/towers.json : max_level, upgrade_cost_ratio, upgrade_stat_bonus). Portée et
    // cadence ne changent pas avec le niveau.

    public int getTowerLevel() {
        return this.towerLevel;
    }

    private boolean isClientSide() {
        return this.level != null && this.level.isClientSide();
    }

    public int getMaxTowerLevel() {
        return Math.max(1, GameBalance.getInt(BalanceValue.TOWER_MAX_LEVEL, isClientSide()));
    }

    public boolean canUpgrade() {
        return this.towerLevel < getMaxTowerLevel();
    }

    /** Multiplicateur de PV max et de dégâts au niveau actuel : x1 au niveau 1. */
    public double levelMultiplier() {
        return 1.0D + (this.towerLevel - 1) * GameBalance.get(BalanceValue.TOWER_UPGRADE_STAT_BONUS, isClientSide());
    }

    /** Prix en mana du prochain niveau : une part fixe du coût de pose, quel que soit le niveau. */
    public int getUpgradeCost() {
        return (int) Math.round(getManaCost() * GameBalance.get(BalanceValue.TOWER_UPGRADE_COST_RATIO, isClientSide()));
    }

    /** Tout le mana investi dans cette tour (pose + améliorations) : la base du remboursement. */
    public int getInvestedMana() {
        return getManaCost() + (this.towerLevel - 1) * getUpgradeCost();
    }

    /**
     * Passe au niveau suivant (serveur). Les PV gagnés en PV max sont aussi ajoutés aux PV
     * actuels : améliorer une tour entamée ne la soigne pas entièrement, mais ne la laisse pas
     * non plus plus abîmée qu'avant en proportion.
     *
     * @return faux si la tour est déjà au niveau maximum.
     */
    public boolean upgrade() {
        if (!canUpgrade()) {
            return false;
        }
        int previousMaxHealth = getMaxHealth();
        this.towerLevel++;
        this.health = Math.min(getMaxHealth(), this.health + (getMaxHealth() - previousMaxHealth));
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
        return true;
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
            // Une tour détruite par les monstres pendant la vague : plus de multiplicateur
            // "aucune tour perdue" sur le bonus de fin de vague (voir ScoreKeeper). Un retrait
            // volontaire par le joueur ne passe jamais par ici.
            this.level.setData(ModAttachments.WAVE_TOWER_LOST, true);
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
        output.putInt("TowerLevel", this.towerLevel);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // Le niveau d'abord : il entre dans le calcul des PV max, donc du repli des PV.
        this.towerLevel = Math.max(1, input.getIntOr("TowerLevel", 1));
        this.health = input.getIntOr("Health", getMaxHealth());
    }
}
