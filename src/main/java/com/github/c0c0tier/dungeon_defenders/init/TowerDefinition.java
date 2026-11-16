package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

// Catalogue des tours posables via la roue (TowerWheelScreen) — un membre par tour, commun au
// client (affichage de la roue/de l'aperçu) et au serveur (validation de PlaceTowerPayload). Quel
// héros peut poser quelle tour : voir HeroDefinition.
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
                    .build()),

    // --- Roster de l'Alchimiste (2026-11-16) : tours de conception propre au mod. ---

    FROST_STILL(
            "frost_still",
            () -> ModBlocks.FROST_STILL.get(),
            () -> ModBlocks.FROST_STILL_ITEM.get(),
            // Aucun dégât : ralentit tout ce qui passe dans son rayon.
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 20)
                    .with(TowerStat.MANA_COST, 40)
                    .with(TowerStat.RANGE, 5.0D)
                    .with(TowerStat.EFFECT_LEVEL, 2)
                    .build()),

    ACID_SPRAYER(
            "acid_sprayer",
            () -> ModBlocks.ACID_SPRAYER.get(),
            () -> ModBlocks.ACID_SPRAYER_ITEM.get(),
            // Cône court et large, tous les monstres touchés à la fois : dégâts faibles, mais
            // +30 % de dégâts subis de toutes les sources pendant 3 s.
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 20)
                    .with(TowerStat.MANA_COST, 60)
                    .with(TowerStat.RANGE, 6.0D)
                    .with(TowerStat.CONE_ANGLE_DEGREES, 90.0D)
                    .with(TowerStat.DAMAGE, 1.0D)
                    .with(TowerStat.ATTACK_INTERVAL_TICKS, 20)
                    .with(TowerStat.VULNERABILITY_PERCENT, 30.0D)
                    .build()),

    VOLATILE_VAT(
            "volatile_vat",
            () -> ModBlocks.VOLATILE_VAT.get(),
            () -> ModBlocks.VOLATILE_VAT_ITEM.get(),
            // Mur fragile : c'est sa destruction qui paie (explosion). Pas de stat RANGE, donc
            // pas de cercle à la pose, comme les autres murs.
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 15)
                    .with(TowerStat.MANA_COST, 35)
                    .with(TowerStat.DAMAGE, 12.0D)
                    .with(TowerStat.SPLASH_RADIUS, 3.0D)
                    .build()),

    CATALYST_PYLON(
            "catalyst_pylon",
            () -> ModBlocks.CATALYST_PYLON.get(),
            () -> ModBlocks.CATALYST_PYLON_ITEM.get(),
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 15)
                    .with(TowerStat.MANA_COST, 80)
                    .with(TowerStat.RANGE, 4.0D)
                    .with(TowerStat.BOOST_PERCENT, 25.0D)
                    .build()),

    MANA_CONDENSER(
            "mana_condenser",
            () -> ModBlocks.MANA_CONDENSER.get(),
            () -> ModBlocks.MANA_CONDENSER_ITEM.get(),
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 15)
                    .with(TowerStat.MANA_COST, 60)
                    .with(TowerStat.RANGE, 5.0D)
                    .with(TowerStat.BONUS_MANA, 5)
                    .build()),

    FLASK_LAUNCHER(
            "flask_launcher",
            () -> ModBlocks.FLASK_LAUNCHER.get(),
            () -> ModBlocks.FLASK_LAUNCHER_ITEM.get(),
            // Omnidirectionnel (aucune stat de cône). Dégâts PAR passage de la flaque, deux fois
            // par seconde pendant linger_ticks.
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 20)
                    .with(TowerStat.MANA_COST, 65)
                    .with(TowerStat.RANGE, 10.0D)
                    .with(TowerStat.DAMAGE, 2.0D)
                    .with(TowerStat.ATTACK_INTERVAL_TICKS, 80)
                    .with(TowerStat.SPLASH_RADIUS, 2.0D)
                    .with(TowerStat.LINGER_TICKS, 60)
                    .build()),

    // --- Roster de l'Ingénieur (2026-11-16) : tours de conception propre au mod. ---

    IRON_BARRICADE(
            "iron_barricade",
            () -> ModBlocks.IRON_BARRICADE.get(),
            () -> ModBlocks.IRON_BARRICADE_ITEM.get(),
            // Mur passif très solide, qui se répare seul (heal_amount PV par intervalle).
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 60)
                    .with(TowerStat.MANA_COST, 45)
                    .with(TowerStat.HEAL_AMOUNT, 1)
                    .with(TowerStat.ATTACK_INTERVAL_TICKS, 40)
                    .build()),

    REPEATER_TURRET(
            "repeater_turret",
            () -> ModBlocks.REPEATER_TURRET.get(),
            () -> ModBlocks.REPEATER_TURRET_ITEM.get(),
            // Cadence très élevée, petits dégâts, portée moyenne.
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 20)
                    .with(TowerStat.MANA_COST, 55)
                    .with(TowerStat.RANGE, 9.0D)
                    .with(TowerStat.CONE_ANGLE_DEGREES, 60.0D)
                    .with(TowerStat.DAMAGE, 1.5D)
                    .with(TowerStat.ATTACK_INTERVAL_TICKS, 6)
                    .build()),

    LONGSHOT_TURRET(
            "longshot_turret",
            () -> ModBlocks.LONGSHOT_TURRET.get(),
            () -> ModBlocks.LONGSHOT_TURRET_ITEM.get(),
            // Cône très étroit, très longue portée, gros dégâts, cadence lente.
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 15)
                    .with(TowerStat.MANA_COST, 75)
                    .with(TowerStat.RANGE, 24.0D)
                    .with(TowerStat.CONE_ANGLE_DEGREES, 20.0D)
                    .with(TowerStat.DAMAGE, 18.0D)
                    .with(TowerStat.ATTACK_INTERVAL_TICKS, 80)
                    .build()),

    MAGNET_COIL(
            "magnet_coil",
            () -> ModBlocks.MAGNET_COIL.get(),
            () -> ModBlocks.MAGNET_COIL_ITEM.get(),
            // Attire les monstres vers elle (knockback_strength = force de l'attraction).
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 25)
                    .with(TowerStat.MANA_COST, 50)
                    .with(TowerStat.RANGE, 5.0D)
                    .with(TowerStat.ATTACK_INTERVAL_TICKS, 10)
                    .with(TowerStat.KNOCKBACK_STRENGTH, 0.35D)
                    .build()),

    REPAIR_STATION(
            "repair_station",
            () -> ModBlocks.REPAIR_STATION.get(),
            () -> ModBlocks.REPAIR_STATION_ITEM.get(),
            // Répare les tours voisines (heal_amount PV par intervalle).
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 20)
                    .with(TowerStat.MANA_COST, 70)
                    .with(TowerStat.RANGE, 4.0D)
                    .with(TowerStat.HEAL_AMOUNT, 2)
                    .with(TowerStat.ATTACK_INTERVAL_TICKS, 40)
                    .build()),

    PISTON_SLAMMER(
            "piston_slammer",
            () -> ModBlocks.PISTON_SLAMMER.get(),
            () -> ModBlocks.PISTON_SLAMMER_ITEM.get(),
            // Onde de choc tout autour : dégâts et forte repousse.
            TowerStats.builder()
                    .with(TowerStat.MAX_HEALTH, 30)
                    .with(TowerStat.MANA_COST, 60)
                    .with(TowerStat.RANGE, 3.0D)
                    .with(TowerStat.DAMAGE, 4.0D)
                    .with(TowerStat.ATTACK_INTERVAL_TICKS, 50)
                    .with(TowerStat.KNOCKBACK_STRENGTH, 1.0D)
                    .build());

    /**
     * Distance maximale (blocs, depuis les yeux) à laquelle un joueur vise une case ou une tour
     * pour la poser, la retirer, l'inspecter ou l'améliorer. Partagée par le client (raycasts) et
     * le serveur (ModNetworking), qui refusait auparavant en silence au-delà de 8 blocs alors que
     * le client laissait viser jusqu'à 20.
     */
    public static final double REACH = 20.0D;

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

    /** Une phrase qui dit à quoi sert la tour, affichée dans la roue (issue #15). */
    public Component description() {
        return Component.translatable("dungeon_defenders.tower." + this.id + ".description");
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
