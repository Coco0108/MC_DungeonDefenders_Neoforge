package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.Config;
import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.PhaseTransitions;
import com.github.c0c0tier.dungeon_defenders.atelier.Atelier;
import com.github.c0c0tier.dungeon_defenders.atelier.Impact;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// implements AiAttackTarget : priorité 30 (voir AiAttackTarget), entre les tours "corps à
// corps" (20) et les tourelles (40) — ciblé par entity/ai/AttackPriorityTargetGoal.java au
// même titre qu'une Blockade ou un Turret, mais le cristal n'a et n'aura pas de lien de code
// avec AbstractTowerBlockEntity (aucun coût en mana, aucune pose possible).
//
// Depuis le 2026-11-28, il prend le modèle animé dd_cristal_v2 de l'Atelier (AnimatedBlockEntity, comme le cristal de la
// taverne) et joue sa destruction : à 0 PV, l'animation death ; l'explosion tombe à son marqueur impact (sinon à sa
// moitié) ; le bloc est retiré et la partie perdue à la fin de l'animation. Sans modèle animé, tout se passe à
// l'instant, comme avant.
public class EterniaCrystalBlockEntity extends AnimatedBlockEntity implements AiAttackTarget {

    static final String BLOC_ATELIER = "dd_cristal_v2";

    private int crystalHealth = Config.DEFAULT_HEALTH.get();
    /** Ticks écoulés depuis le début de l'animation de destruction ; -1 tant que le cristal est debout. */
    private int deathTicks = -1;

    public EterniaCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.ETERNIA_CRYSTAL_BE.get(), pos, state, BLOC_ATELIER);
    }

    /** Vrai pendant l'animation de destruction (le cristal n'a plus de PV, la partie n'est pas encore perdue). */
    public boolean isDying() {
        return this.deathTicks >= 0;
    }

    /** Le tick de l'explosion, compté depuis le début de {@code death} : son marqueur {@code impact}, ou sa moitié. */
    public static int impactTick() {
        return Math.round(Atelier.impactBloc(BLOC_ATELIER, "death").secondes() * 20);
    }

    /**
     * Le tick où le bloc est retiré : un tick avant la fin de {@code death}, pour que le modèle ne revienne jamais à sa
     * boucle de repos sous les yeux des joueurs ; jamais avant l'explosion.
     */
    public static int removalTick() {
        Impact death = Atelier.impactBloc(BLOC_ATELIER, "death");
        return Math.max(impactTick(), Math.round(death.longueur() * 20) - 1);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EterniaCrystalBlockEntity crystal) {
        if (!crystal.isDying()) {
            return;
        }
        crystal.deathTicks++;
        if (crystal.deathTicks == impactTick()) {
            crystal.explode();
        }
        if (crystal.deathTicks >= removalTick()) {
            crystal.destroy();
        }
    }

    public int getCrystalHealth() {
        return this.crystalHealth;
    }

    // --- REGISTRE DE POSITION (voir ModAttachments.CRYSTAL_POS) ---
    // Même patron que SpawnerBlockEntity#setLevel/#setRemoved pour ACTIVE_SPAWNERS : permet à
    // entity/ai/SeekEterniaCrystalGoal.java de naviguer directement vers le cristal sans avoir
    // à le re-chercher.

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.setData(ModAttachments.CRYSTAL_POS, this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Ne retire la position que si elle pointait encore vers CE cristal : évite qu'un
        // (dé)chargement de chunk désordonné entre deux cristaux (cas normalement impossible,
        // un seul par map) n'efface la position d'un autre exemplaire plus récent.
        if (this.level instanceof ServerLevel serverLevel
                && this.worldPosition.equals(serverLevel.getData(ModAttachments.CRYSTAL_POS))) {
            // setData(..., null) est refusé par NeoForge (Objects.requireNonNull) : removeData
            // est la façon correcte d'effacer un attachment à valeur par défaut null.
            serverLevel.removeData(ModAttachments.CRYSTAL_POS);
        }
    }

    @Override
    public int getAiPriority() {
        return AiAttackTarget.PRIORITY_CRYSTAL;
    }

    /** Retire {@code amount} PV au cristal. */
    @Override
    public void damage(int amount) {
        if (isDying()) {
            return;
        }
        // Le cristal a été touché pendant cette vague : plus de multiplicateur "cristal intact"
        // sur le bonus de fin de vague (voir ScoreKeeper).
        if (amount > 0 && this.level != null && !this.level.isClientSide()) {
            this.level.setData(ModAttachments.WAVE_CRYSTAL_DAMAGED, true);
        }
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

        if (this.crystalHealth <= 0 && !isDying()) {
            if (jouer("death")) {
                // La suite au rythme de l'animation (serverTick) ; un marqueur à 0 s explose tout de suite.
                this.deathTicks = 0;
                if (impactTick() == 0) {
                    explode();
                }
                if (removalTick() <= 0) {
                    destroy();
                }
            } else {
                explode();
                destroy();
            }
        }
    }

    /**
     * L'explosion de la destruction : effet et son seulement, sans dégâts ni bloc cassé (la map est reposée à neuf à
     * chaque partie, et la défaite suffit).
     */
    private void explode() {
        if (this.level instanceof ServerLevel serverLevel) {
            double x = this.worldPosition.getX() + 0.5D;
            double y = this.worldPosition.getY() + 1.0D;
            double z = this.worldPosition.getZ() + 0.5D;
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z, 60, 0.6D, 0.8D, 0.6D, 0.25D);
            serverLevel.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, 0.9F);
        }
    }

    /** Retire le cristal et perd la partie. */
    private void destroy() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        this.deathTicks = -1;
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
        this.crystalHealth = input.getIntOr("CrystalHealth", Config.DEFAULT_HEALTH.get());
    }
}
