package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.MapDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// Porte les réglages d'une map : nom affiché, ordre dans son pack, nombre de vagues,
// multiplicateur de score. Posé DANS la map par son créateur, donc sauvegardé à l'intérieur du
// `.nbt` de la structure — copier le fichier emporte les réglages avec lui, il n'y a aucun JSON
// à écrire ni à transmettre à côté.
//
// Conséquence importante : ces valeurs se lisent **sans poser la map**. MapRegistry les extrait
// directement du template via StructureTemplate#filterBlocks, qui renvoie le NBT de chaque bloc
// trouvé — même technique que la recherche du marqueur player_spawn dans la taverne.
public class MapConfigBlockEntity extends BlockEntity {

    private static final String KEY_NAME = "MapName";
    private static final String KEY_ORDER = "MapOrder";
    private static final String KEY_WAVE_COUNT = "WaveCount";
    private static final String KEY_SCORE_MULTIPLIER = "ScoreMultiplier";
    private static final String KEY_FORMAT_VERSION = "FormatVersion";

    private String displayName = "";
    private int order = MapDefinition.DEFAULT_ORDER;
    private int waveCount = MapDefinition.DEFAULT_WAVE_COUNT;
    private float scoreMultiplier = MapDefinition.DEFAULT_SCORE_MULTIPLIER;

    public MapConfigBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.MAP_CONFIG_BE.get(), pos, state);
    }

    /**
     * Reconstruit une {@link MapDefinition} à partir du NBT d'un bloc de configuration trouvé
     * dans une structure. {@code tag} nul (aucun bloc de config dans la map) donne des valeurs
     * par défaut : décidé avec le joueur (2026-09-02), une map sans configuration reste jouable
     * plutôt que d'être masquée — on peut ainsi tester une map avant de l'avoir renseignée.
     */
    public static MapDefinition toDefinition(Identifier structureId, @Nullable CompoundTag tag, boolean fromWorld) {
        if (tag == null) {
            return new MapDefinition(structureId, "", MapDefinition.DEFAULT_ORDER,
                    MapDefinition.DEFAULT_WAVE_COUNT, MapDefinition.DEFAULT_SCORE_MULTIPLIER, fromWorld);
        }
        // Chaque champ a son propre repli : un pack publié avec une version de format plus
        // ancienne n'a simplement pas les clés ajoutées depuis, et reste lisible.
        return new MapDefinition(
                structureId,
                tag.getStringOr(KEY_NAME, ""),
                tag.getIntOr(KEY_ORDER, MapDefinition.DEFAULT_ORDER),
                Math.max(1, tag.getIntOr(KEY_WAVE_COUNT, MapDefinition.DEFAULT_WAVE_COUNT)),
                tag.getFloatOr(KEY_SCORE_MULTIPLIER, MapDefinition.DEFAULT_SCORE_MULTIPLIER),
                fromWorld);
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getOrder() {
        return this.order;
    }

    public int getWaveCount() {
        return this.waveCount;
    }

    public float getScoreMultiplier() {
        return this.scoreMultiplier;
    }

    public void applyConfig(String displayName, int order, int waveCount, float scoreMultiplier) {
        this.displayName = displayName;
        this.order = order;
        this.waveCount = Math.max(1, waveCount);
        this.scoreMultiplier = Math.max(0.0F, scoreMultiplier);
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString(KEY_NAME, this.displayName);
        output.putInt(KEY_ORDER, this.order);
        output.putInt(KEY_WAVE_COUNT, this.waveCount);
        output.putFloat(KEY_SCORE_MULTIPLIER, this.scoreMultiplier);
        output.putInt(KEY_FORMAT_VERSION, MapDefinition.FORMAT_VERSION);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.displayName = input.getStringOr(KEY_NAME, "");
        this.order = input.getIntOr(KEY_ORDER, MapDefinition.DEFAULT_ORDER);
        this.waveCount = Math.max(1, input.getIntOr(KEY_WAVE_COUNT, MapDefinition.DEFAULT_WAVE_COUNT));
        this.scoreMultiplier = input.getFloatOr(KEY_SCORE_MULTIPLIER, MapDefinition.DEFAULT_SCORE_MULTIPLIER);
    }

    // Synchronisé vers le client : l'écran de configuration lit la copie cliente du block
    // entity, comme SpawnerConfigScreen le fait pour le spawner.
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }
}
