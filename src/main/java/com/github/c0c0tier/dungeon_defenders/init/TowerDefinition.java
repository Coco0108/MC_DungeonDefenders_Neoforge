package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.block.entity.SpikeBlockadeBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

// Catalogue des tours posables via la roue (TowerWheelScreen) — un membre par tour, commun au
// client (affichage de la roue/de l'aperçu) et au serveur (validation de PlaceTowerPayload). Un
// seul membre pour l'instant : pas de filtrage par héros, ce système n'existe pas encore.
//
// Le coût en mana n'est PAS dupliqué ici : il référence directement la constante du block
// entity concerné (source d'autorité unique, déjà utilisée par ModEvents.onBlockadePlace), ce
// catalogue ne fait que la réexposer pour l'affichage.
public enum TowerDefinition {

    SPIKE_BLOCKADE(
            "spike_blockade",
            () -> ModBlocks.SPIKE_BLOCKADE.get(),
            () -> ModBlocks.SPIKE_BLOCKADE_ITEM.get(),
            SpikeBlockadeBlockEntity.MANA_COST,
            // Pas de portée : c'est un mur au contact, pas une tour à distance. 0 = pas de
            // cercle de portée affiché par TowerPlacementClientEvents.
            0.0D);

    private final String id;
    private final Supplier<Block> block;
    private final Supplier<Item> icon;
    private final int manaCost;
    private final double range;

    TowerDefinition(String id, Supplier<Block> block, Supplier<Item> icon, int manaCost, double range) {
        this.id = id;
        this.block = block;
        this.icon = icon;
        this.manaCost = manaCost;
        this.range = range;
    }

    public String id() {
        return this.id;
    }

    public Block block() {
        return this.block.get();
    }

    public Item icon() {
        return this.icon.get();
    }

    public int manaCost() {
        return this.manaCost;
    }

    public double range() {
        return this.range;
    }

    public Component displayName() {
        return Component.translatable("dungeon_defenders.tower." + this.id);
    }
}
