package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.block.entity.HarpoonTurretBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.SpikeBlockadeBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

// Catalogue des tours posables via la roue (TowerWheelScreen) — un membre par tour, commun au
// client (affichage de la roue/de l'aperçu) et au serveur (validation de PlaceTowerPayload). Pas
// de filtrage par héros, ce système n'existe pas encore.
//
// Le coût en mana n'est PAS dupliqué ici : il référence directement la constante du block
// entity concerné (source d'autorité unique, déjà utilisée par ModEvents.onTowerPlace), ce
// catalogue ne fait que la réexposer pour l'affichage.
public enum TowerDefinition {

    SPIKE_BLOCKADE(
            "spike_blockade",
            () -> ModBlocks.SPIKE_BLOCKADE.get(),
            () -> ModBlocks.SPIKE_BLOCKADE_ITEM.get(),
            SpikeBlockadeBlockEntity.MANA_COST,
            // Pas de portée : c'est un mur au contact, pas une tour à distance. 0 = pas de
            // cercle de portée affiché par TowerPlacementClientEvents. coneAngleDegrees sans
            // effet tant que range() vaut 0.
            0.0D, 360.0D),

    HARPOON_TURRET(
            "harpoon_turret",
            () -> ModBlocks.HARPOON_TURRET.get(),
            () -> ModBlocks.HARPOON_TURRET_ITEM.get(),
            HarpoonTurretBlockEntity.MANA_COST,
            // Première tour avec une vraie portée : cône de 45°, pas omnidirectionnel — voir
            // AbstractTurretBlockEntity pour la logique de ciblage correspondante.
            12.0D, 45.0D);

    private final String id;
    private final Supplier<Block> block;
    private final Supplier<Item> icon;
    private final int manaCost;
    private final double range;
    // >= 360 = omnidirectionnel (pas de filtre d'angle, cercle complet à l'aperçu).
    private final double coneAngleDegrees;

    TowerDefinition(String id, Supplier<Block> block, Supplier<Item> icon, int manaCost,
            double range, double coneAngleDegrees) {
        this.id = id;
        this.block = block;
        this.icon = icon;
        this.manaCost = manaCost;
        this.range = range;
        this.coneAngleDegrees = coneAngleDegrees;
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

    public double coneAngleDegrees() {
        return this.coneAngleDegrees;
    }

    public Component displayName() {
        return Component.translatable("dungeon_defenders.tower." + this.id);
    }
}
