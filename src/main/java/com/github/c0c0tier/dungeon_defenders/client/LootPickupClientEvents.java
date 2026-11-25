package com.github.c0c0tier.dungeon_defenders.client;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.loot.Equipment;
import com.github.c0c0tier.dungeon_defenders.loot.LootWeapon;
import com.github.c0c0tier.dungeon_defenders.network.LootPickupPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

// Ramassage du butin au sol (décidé avec le joueur, 2026-11-16) : près d'un objet, une touche le
// RANGE dans l'inventaire de la Forge, une autre l'ÉQUIPE directement. Par défaut E et F.
//
// **E et F sont déjà des touches vanilla** (inventaire, échange de main). D'où deux précautions :
//   - on travaille en début de tick (ClientTickEvent.Pre), AVANT que le jeu ne traite ses
//     propres touches ;
//   - quand un objet est à portée et que notre touche partage la sienne avec la touche vanilla,
//     on consomme aussi les appuis de la touche vanilla : l'inventaire ne s'ouvre pas, l'arme ne
//     passe pas en main gauche. Sans objet à portée, on ne touche à rien : E ouvre l'inventaire
//     comme d'habitude.
// Les deux touches restent reconfigurables ; si le joueur leur donne une touche libre, les
// touches vanilla ne sont jamais interceptées.
//
// La cible est l'objet de butin le PLUS PROCHE dans le rayon de ramassage, sans avoir à le
// viser : en plein combat on ne cherche pas un petit objet au viseur.
@EventBusSubscriber(modid = DungeonDefendersMod.MODID, value = Dist.CLIENT)
public final class LootPickupClientEvents {

    /** L'objet à portée et son contenu, pour LootPromptOverlay. */
    public record Target(int entityId, LootWeapon weapon) {
    }

    private static @Nullable Target target;

    private LootPickupClientEvents() {
    }

    public static @Nullable Target target() {
        return target;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            target = null;
            return;
        }

        target = findTarget(minecraft);
        if (target == null) {
            return;
        }

        boolean store = pressed(ModKeyMappings.LOOT_STORE, minecraft.options.keyInventory);
        boolean equip = pressed(ModKeyMappings.LOOT_EQUIP, minecraft.options.keySwapOffhand);
        if (!store && !equip) {
            return;
        }

        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null) {
            // Les deux à la fois : équiper l'emporte.
            connection.send(new LootPickupPayload(target.entityId(), equip).toVanillaServerbound());
        }
    }

    // Vrai si notre touche a été pressée depuis le dernier tick. Deux touches liées à la même
    // touche physique ne reçoivent pas forcément l'appui toutes les deux : on regarde donc aussi
    // la touche vanilla jumelle, et on vide ses appuis pour que le jeu ne les traite pas.
    private static boolean pressed(KeyMapping ours, KeyMapping vanillaTwin) {
        boolean pressed = false;
        while (ours.consumeClick()) {
            pressed = true;
        }
        if (ours.same(vanillaTwin)) {
            while (vanillaTwin.consumeClick()) {
                pressed = true;
            }
        }
        return pressed;
    }

    private static @Nullable Target findTarget(Minecraft minecraft) {
        Target nearest = null;
        double nearestDistSq = Equipment.PICKUP_RANGE * Equipment.PICKUP_RANGE;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity itemEntity) || !itemEntity.isAlive()) {
                continue;
            }
            double distSq = itemEntity.distanceToSqr(minecraft.player);
            if (distSq > nearestDistSq) {
                continue;
            }
            Optional<LootWeapon> weapon = LootWeapon.fromStack(itemEntity.getItem());
            if (weapon.isPresent()) {
                nearestDistSq = distSq;
                nearest = new Target(itemEntity.getId(), weapon.get());
            }
        }
        return nearest;
    }
}
