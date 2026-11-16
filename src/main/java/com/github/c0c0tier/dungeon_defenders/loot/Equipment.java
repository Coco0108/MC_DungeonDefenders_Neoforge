package com.github.c0c0tier.dungeon_defenders.loot;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// L'équipement d'un joueur : son inventaire de la Forge et l'arme équipée de chacun de ses
// héros. Règles fixées avec le joueur (2026-11-16) :
//   - le joueur n'a pas d'inventaire portatif : tout le butin est rangé dans l'inventaire de la
//     Forge (attachment FORGE_INVENTORY), consultable seulement au bloc Forge ;
//   - un objet au sol se RANGE (va dans l'inventaire) ou s'ÉQUIPE directement ;
//   - équiper remplace l'arme en place, qui part dans l'inventaire.
//
// Une arme de butin n'est équipable que par son héros. Chaque héros garde donc sa propre arme
// équipée (attachment EQUIPPED_WEAPONS, par identifiant de héros) : changer de héros ne
// déséquipe rien. Sans arme de butin, le héros a simplement son arme de base, sans bonus.
//
// Toutes les méthodes qui modifient quelque chose sont à appeler côté serveur.
public final class Equipment {

    /** Cinq pages de vingt cases dans l'écran de la Forge. */
    public static final int PAGE_SIZE = 20;
    public static final int CAPACITY = 5 * PAGE_SIZE;

    /** Distance maximale à laquelle on ramasse un objet au sol. */
    public static final double PICKUP_RANGE = 3.0D;

    private static final Identifier HEALTH_MODIFIER = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "loot_health");
    private static final Identifier SPEED_MODIFIER = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "loot_speed");

    private Equipment() {
    }

    // --- LECTURE ---

    public static List<LootWeapon> inventory(Player player) {
        return player.getData(ModAttachments.FORGE_INVENTORY);
    }

    /** L'arme de butin équipée par le héros ACTUEL du joueur, ou null s'il n'en a pas. */
    public static @Nullable LootWeapon equipped(Player player) {
        return player.getData(ModAttachments.EQUIPPED_WEAPONS).get(HeroDefinition.of(player).id());
    }

    /** Multiplicateur de dégâts du joueur dû à son arme : x1 sans arme de butin. */
    public static double damageMultiplier(Player player) {
        LootWeapon weapon = equipped(player);
        return weapon == null ? 1.0D : 1.0D + weapon.damage();
    }

    /** Bonus (fraction) aux dégâts des tours que le joueur pose : 0 sans arme de butin. */
    public static double towerDamageBonus(Player player) {
        LootWeapon weapon = equipped(player);
        return weapon == null ? 0.0D : weapon.towerDamage();
    }

    // --- ACTIONS ---

    /**
     * Ramasse un objet de butin au sol : le range, ou l'équipe directement.
     *
     * @return faux (objet laissé au sol, message au joueur) s'il est trop loin, si ce n'est pas
     *         du butin, si l'inventaire est plein, ou s'il s'agit de l'arme d'un autre héros
     *         qu'on voulait équiper.
     */
    public static boolean pickUp(Player player, ItemEntity entity, boolean equip) {
        if (!entity.isAlive() || entity.distanceToSqr(player) > PICKUP_RANGE * PICKUP_RANGE) {
            return false;
        }
        Optional<LootWeapon> loot = LootWeapon.fromStack(entity.getItem());
        if (loot.isEmpty()) {
            return false;
        }
        LootWeapon weapon = loot.get();

        boolean done = equip ? equip(player, weapon) : store(player, weapon);
        if (done) {
            entity.discard();
        }
        return done;
    }

    /** Range {@code weapon} dans l'inventaire de la Forge. Faux (avec message) s'il est plein. */
    public static boolean store(Player player, LootWeapon weapon) {
        List<LootWeapon> items = new ArrayList<>(inventory(player));
        if (items.size() >= CAPACITY) {
            player.sendSystemMessage(Component.translatable("dungeon_defenders.loot.inventory_full"));
            return false;
        }
        items.add(weapon);
        setInventory(player, items);
        player.sendSystemMessage(Component.translatable("dungeon_defenders.loot.stored", weapon.displayName()));
        return true;
    }

    /**
     * Équipe {@code weapon} (une arme qui n'est PAS dans l'inventaire : butin au sol). L'arme en
     * place part dans l'inventaire ; s'il est plein, rien ne change.
     */
    public static boolean equip(Player player, LootWeapon weapon) {
        if (!canEquip(player, weapon)) {
            return false;
        }
        LootWeapon previous = equipped(player);
        if (previous != null) {
            List<LootWeapon> items = new ArrayList<>(inventory(player));
            if (items.size() >= CAPACITY) {
                player.sendSystemMessage(Component.translatable("dungeon_defenders.loot.inventory_full"));
                return false;
            }
            items.add(previous);
            setInventory(player, items);
        }
        setEquipped(player, weapon);
        player.sendSystemMessage(Component.translatable("dungeon_defenders.loot.equipped", weapon.displayName()));
        return true;
    }

    /** Équipe l'arme de la case {@code index} de l'inventaire ; l'arme en place prend sa case. */
    public static boolean equipFromInventory(Player player, int index) {
        List<LootWeapon> items = new ArrayList<>(inventory(player));
        if (index < 0 || index >= items.size()) {
            return false;
        }
        LootWeapon weapon = items.get(index);
        if (!canEquip(player, weapon)) {
            return false;
        }
        LootWeapon previous = equipped(player);
        if (previous == null) {
            items.remove(index);
        } else {
            items.set(index, previous);
        }
        setInventory(player, items);
        setEquipped(player, weapon);
        return true;
    }

    /** Retire l'arme équipée du héros actuel et la range. Faux si aucune, ou inventaire plein. */
    public static boolean unequip(Player player) {
        LootWeapon current = equipped(player);
        if (current == null) {
            return false;
        }
        List<LootWeapon> items = new ArrayList<>(inventory(player));
        if (items.size() >= CAPACITY) {
            player.sendSystemMessage(Component.translatable("dungeon_defenders.loot.inventory_full"));
            return false;
        }
        items.add(current);
        setInventory(player, items);
        setEquipped(player, null);
        return true;
    }

    /** Détruit l'arme de la case {@code index} de l'inventaire. */
    public static boolean discard(Player player, int index) {
        List<LootWeapon> items = new ArrayList<>(inventory(player));
        if (index < 0 || index >= items.size()) {
            return false;
        }
        items.remove(index);
        setInventory(player, items);
        return true;
    }

    private static boolean canEquip(Player player, LootWeapon weapon) {
        HeroDefinition owner = weapon.heroDefinition();
        if (owner == HeroDefinition.of(player)) {
            return true;
        }
        player.sendSystemMessage(Component.translatable("dungeon_defenders.loot.wrong_hero",
                owner == null ? Component.literal(weapon.hero()) : owner.displayName()));
        return false;
    }

    private static void setInventory(Player player, List<LootWeapon> items) {
        player.setData(ModAttachments.FORGE_INVENTORY, List.copyOf(items));
        player.syncData(ModAttachments.FORGE_INVENTORY);
    }

    private static void setEquipped(Player player, @Nullable LootWeapon weapon) {
        Map<String, LootWeapon> equipped = new HashMap<>(player.getData(ModAttachments.EQUIPPED_WEAPONS));
        String hero = HeroDefinition.of(player).id();
        if (weapon == null) {
            equipped.remove(hero);
        } else {
            equipped.put(hero, weapon);
        }
        player.setData(ModAttachments.EQUIPPED_WEAPONS, Map.copyOf(equipped));
        player.syncData(ModAttachments.EQUIPPED_WEAPONS);
        applyAttributes(player);
    }

    /**
     * Pose les modificateurs de PV max et de vitesse de l'arme équipée du héros actuel. À
     * rappeler à la connexion, à la réapparition et au changement de héros, comme
     * HeroStat#applyAttributes : ce sont des modificateurs transitoires.
     */
    public static void applyAttributes(Player player) {
        LootWeapon weapon = equipped(player);
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.addOrUpdateTransientModifier(new AttributeModifier(
                    HEALTH_MODIFIER, weapon == null ? 0.0D : weapon.health(), AttributeModifier.Operation.ADD_VALUE));
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(
                    SPEED_MODIFIER, weapon == null ? 0.0D : weapon.speed(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }
}
