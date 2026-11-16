package com.github.c0c0tier.dungeon_defenders.item;

import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Garantit qu'un joueur porte l'arme de SON héros, et aucune autre. Appelé par ModEvents et
// ModNetworking à chaque moment où ça peut cesser d'être vrai : connexion, réapparition,
// changement de héros, tentative de jeter l'arme.
//
// L'arme n'est donc pas un objet qu'on trouve ou qu'on perd : elle fait partie du héros, comme
// ses tours. Le butin (armes aux stats variables) viendra plus tard par-dessus, s'il vient.
public final class HeroWeapons {

    private HeroWeapons() {
    }

    /** Vrai si {@code item} est l'arme d'un héros, quel qu'il soit. */
    public static boolean isHeroWeapon(Item item) {
        for (HeroDefinition hero : HeroDefinition.values()) {
            if (hero.weapon() == item) {
                return true;
            }
        }
        return false;
    }

    /** Retire les armes des autres héros et donne celle du héros du joueur s'il ne l'a pas. */
    public static void ensure(Player player) {
        Item own = HeroDefinition.of(player).weapon();
        Inventory inventory = player.getInventory();
        boolean hasOwn = false;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !isHeroWeapon(stack.getItem())) {
                continue;
            }
            if (stack.getItem() == own && !hasOwn) {
                hasOwn = true;
                // Jamais plus d'un exemplaire.
                stack.setCount(1);
            } else {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }

        if (!hasOwn) {
            inventory.add(new ItemStack(own));
        }
    }
}
