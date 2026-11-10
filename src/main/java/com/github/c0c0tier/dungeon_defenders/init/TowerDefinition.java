package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

// Catalogue des tours posables via la roue (TowerWheelScreen) — un membre par tour, commun au
// client (affichage de la roue/de l'aperçu) et au serveur (validation de PlaceTowerPayload). Pas
// de filtrage par héros, ce système n'existe pas encore.
//
// **Chiffres data-driven depuis le 2026-11-10** (doc/data-driven/tours.md) : PV, coût en mana,
// dégâts, portée, cadence... vivent dans data/<namespace>/dungeon_defenders/tower/<id>.json,
// relus à chaque /reload (init/TowerStatsRegistry). Les valeurs écrites ICI sont les valeurs PAR
// DÉFAUT : celles d'avant la migration, reprises à l'identique par les JSON livrés, et utilisées
// telles quelles si le JSON d'une tour manque ou est invalide — une tour a donc toujours des
// stats. Elles déclarent aussi QUELLES stats une tour utilise (un JSON qui en cite une autre est
// refusé).
//
// La liste des tours et leur comportement restent en Java : une tour est un bloc + un block
// entity enregistrés par le mod.
public enum TowerDefinition {

    SPIKE_BLOCKADE(
            "spike_blockade",
            () -> ModBlocks.SPIKE_BLOCKADE.get(),
            () -> ModBlocks.SPIKE_BLOCKADE_ITEM.get(),
            // Pas de portée : c'est un mur au contact, pas une tour à distance (aucun cercle de
            // portée affiché par TowerPlacementClientEvents sans stat RANGE).
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 30)
                    .with(TowerStat.MANA_COST, 30)
                    .with(TowerStat.CONTACT_DAMAGE, 2.0D)
                    .with(TowerStat.CONTACT_INTERVAL_TICKS, 20)
                    .with(TowerStat.CONTACT_RANGE, 1.0D)
                    .build()),

    HARPOON_TURRET(
            "harpoon_turret",
            () -> ModBlocks.HARPOON_TURRET.get(),
            () -> ModBlocks.HARPOON_TURRET_ITEM.get(),
            // Première tour avec une vraie portée : cône de 45°, pas omnidirectionnel — voir
            // AbstractTurretBlockEntity pour la logique de ciblage correspondante.
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 20)
                    .with(TowerStat.MANA_COST, 50)
                    .with(TowerStat.RANGE, 12.0D)
                    .with(TowerStat.CONE_ANGLE_DEGREES, 45.0D)
                    .with(TowerStat.DAMAGE, 6.0D)
                    .with(TowerStat.ATTACK_INTERVAL_TICKS, 30)
                    .build()),

    // Reste du roster de l'Écuyer (2026-08-29) — voir doc/02-gameplay.md pour le détail de
    // chaque comportement.
    BOUNCER_BLOCKADE(
            "bouncer_blockade",
            () -> ModBlocks.BOUNCER_BLOCKADE.get(),
            () -> ModBlocks.BOUNCER_BLOCKADE_ITEM.get(),
            // Moins de PV/coût/dégâts que Spike Blockade : sa valeur vient de la repousse, pas
            // d'être un mur durable.
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 25)
                    .with(TowerStat.MANA_COST, 25)
                    .with(TowerStat.CONTACT_DAMAGE, 1.0D)
                    .with(TowerStat.CONTACT_INTERVAL_TICKS, 10)
                    .with(TowerStat.CONTACT_RANGE, 1.0D)
                    .with(TowerStat.KNOCKBACK_STRENGTH, 0.8D)
                    .build()),

    SLICE_N_DICE_BLOCKADE(
            "slice_n_dice_blockade",
            () -> ModBlocks.SLICE_N_DICE_BLOCKADE.get(),
            () -> ModBlocks.SLICE_N_DICE_BLOCKADE_ITEM.get(),
            // Dégâts faibles mais très rapprochés, dans un rayon un peu plus large.
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 35)
                    .with(TowerStat.MANA_COST, 40)
                    .with(TowerStat.CONTACT_DAMAGE, 1.0D)
                    .with(TowerStat.CONTACT_INTERVAL_TICKS, 5)
                    .with(TowerStat.CONTACT_RANGE, 1.5D)
                    .build()),

    BOWLING_BALL_TURRET(
            "bowling_ball_turret",
            () -> ModBlocks.BOWLING_BALL_TURRET.get(),
            () -> ModBlocks.BOWLING_BALL_TURRET_ITEM.get(),
            // Même cône que Harpoon Turret (45°) : orientée comme lui via HORIZONTAL_FACING,
            // pas de raison de la rendre omnidirectionnelle. La boule continue 4 blocs au-delà
            // de la portée de ciblage avant de disparaître.
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 20)
                    .with(TowerStat.MANA_COST, 55)
                    .with(TowerStat.RANGE, 12.0D)
                    .with(TowerStat.CONE_ANGLE_DEGREES, 45.0D)
                    .with(TowerStat.DAMAGE, 5.0D)
                    .with(TowerStat.ATTACK_INTERVAL_TICKS, 40)
                    .with(TowerStat.PROJECTILE_VELOCITY, 1.5D)
                    .with(TowerStat.PROJECTILE_EXTRA_DISTANCE, 4.0D)
                    .build()),

    MORTAR_TURRET(
            "mortar_turret",
            () -> ModBlocks.MORTAR_TURRET.get(),
            () -> ModBlocks.MORTAR_TURRET_ITEM.get(),
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 20)
                    .with(TowerStat.MANA_COST, 70)
                    .with(TowerStat.RANGE, 14.0D)
                    .with(TowerStat.CONE_ANGLE_DEGREES, 45.0D)
                    .with(TowerStat.DAMAGE, 8.0D)
                    .with(TowerStat.ATTACK_INTERVAL_TICKS, 60)
                    .with(TowerStat.SPLASH_RADIUS, 2.0D)
                    .build());

    /** Au-delà (ou sans stat CONE_ANGLE_DEGREES) : omnidirectionnel, cercle complet à l'aperçu. */
    public static final double FULL_CIRCLE_DEGREES = 360.0D;

    private final String id;
    private final Identifier identifier;
    private final Supplier<Block> block;
    private final Supplier<Item> icon;
    private final TowerStats defaultStats;

    TowerDefinition(String id, Supplier<Block> block, Supplier<Item> icon, TowerStats defaultStats) {
        this.id = id;
        this.identifier = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, id);
        this.block = block;
        this.icon = icon;
        this.defaultStats = defaultStats;
    }

    public String id() {
        return this.id;
    }

    /** `dungeon_defenders:<id>` : c'est aussi le nom du fichier de stats, tower/<id>.json. */
    public Identifier identifier() {
        return this.identifier;
    }

    public Block block() {
        return this.block.get();
    }

    public Item icon() {
        return this.icon.get();
    }

    /** Les valeurs du code, utilisées si le JSON de cette tour manque ou est invalide. */
    public TowerStats defaultStats() {
        return this.defaultStats;
    }

    /**
     * Les chiffres en vigueur pour cette tour. {@code clientSide} choisit la copie : celle du
     * serveur (lue dans les JSON) ou celle du client (reçue du serveur), voir TowerStatsRegistry.
     */
    public TowerStats stats(boolean clientSide) {
        return TowerStatsRegistry.get(this, clientSide);
    }

    public Component displayName() {
        return Component.translatable("dungeon_defenders.tower." + this.id);
    }

    public static @Nullable TowerDefinition byIdentifier(Identifier identifier) {
        for (TowerDefinition tower : values()) {
            if (tower.identifier.equals(identifier)) {
                return tower;
            }
        }
        return null;
    }
}
