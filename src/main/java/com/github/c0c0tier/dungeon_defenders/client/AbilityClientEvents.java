package com.github.c0c0tier.dungeon_defenders.client;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.AbilitySlot;
import com.github.c0c0tier.dungeon_defenders.network.ActivateBurstAbilityPayload;
import com.github.c0c0tier.dungeon_defenders.network.StartChannelAbilityPayload;
import com.github.c0c0tier.dungeon_defenders.network.StopChannelAbilityPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

// Fait vivre les quatre touches de compétence (voir ModKeyMappings.ABILITY_KEYS). Trois d'entre
// elles sont MAINTENUES (Heal, Spell 2, Repair — voir ChannelAbility) : ce client-ci ne fait
// qu'annoncer "je commence"/"j'arrête" au serveur, qui est seul juge de la durée réelle
// (ModEvents.onPlayerTick appelle ChannelAbility#canContinue à chaque tick, peut interrompre de
// son propre chef). La quatrième (Spell 1, une salve — voir BurstAbility) se déclenche en un
// seul paquet à l'appui, comme n'importe quel autre bouton du mod.
@EventBusSubscriber(modid = DungeonDefendersMod.MODID, value = Dist.CLIENT)
public final class AbilityClientEvents {

    // Même portée que le mode suppression de tour (TowerRemovalClientEvents) : Repair vise une
    // tour par le même mécanisme de raycast.
    private static final double MAX_REACH = 20.0D;

    // Suivi manuel de l'état "maintenue" de chaque touche canalisée — premier idiome de ce
    // genre dans le mod (tout le reste n'utilise que consumeClick(), pour des appuis simples).
    private static boolean healActive;
    private static boolean spell2Active;
    // Repair a besoin de deux drapeaux distincts : repairKeyWasDown pour détecter le
    // relâchement même si aucune canalisation n'a démarré (viser dans le vide au moment de
    // l'appui), et repairChannelActive pour ne renvoyer un Stop que si un Start est réellement
    // parti.
    private static boolean repairKeyWasDown;
    private static boolean repairChannelActive;

    private AbilityClientEvents() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        // Un écran qui s'ouvre (menu pause, roue des tours...) interrompt toute canalisation en
        // cours plutôt que de la laisser continuer hors de vue du joueur — capturer une touche
        // pendant qu'un écran est ouvert n'aurait de toute façon aucun sens.
        if (minecraft.screen != null) {
            interruptAll();
            return;
        }

        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return;
        }

        boolean healDown = ModKeyMappings.ABILITY_HEAL.isDown();
        if (healDown != healActive) {
            healActive = healDown;
            if (healDown) {
                sendStart(AbilitySlot.HEAL, null);
            } else {
                sendStop();
            }
        }

        boolean spell2Down = ModKeyMappings.ABILITY_SPELL_2.isDown();
        if (spell2Down != spell2Active) {
            spell2Active = spell2Down;
            if (spell2Down) {
                sendStart(AbilitySlot.SPELL_2, null);
            } else {
                sendStop();
            }
        }

        tickRepair(player, level);

        if (ModKeyMappings.ABILITY_SPELL_1.consumeClick()) {
            sendBurst(AbilitySlot.SPELL_1);
        }
    }

    private static void tickRepair(LocalPlayer player, Level level) {
        boolean down = ModKeyMappings.ABILITY_REPAIR.isDown();
        if (down && !repairKeyWasDown) {
            BlockPos target = raycastTowerTarget(player, level);
            if (target != null) {
                repairChannelActive = true;
                sendStart(AbilitySlot.REPAIR, target);
            }
            // Aucune tour visée à l'instant de l'appui : pas de canalisation, il faudra
            // relâcher puis rappuyer en visant correctement — pas de "rattrapage" en cours de
            // maintien, pour une sémantique simple et prévisible.
        } else if (!down && repairKeyWasDown && repairChannelActive) {
            repairChannelActive = false;
            sendStop();
        }
        repairKeyWasDown = down;
    }

    // Même mécanisme que TowerRemovalClientEvents#updateTargetFromRaycast (raycast OUTLINE,
    // 20 blocs) : Repair vise une tour exactement comme le mode suppression.
    private static @Nullable BlockPos raycastTowerTarget(LocalPlayer player, Level level) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(MAX_REACH));

        BlockHitResult hit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos pos = hit.getBlockPos();
        return level.getBlockEntity(pos) instanceof AbstractTowerBlockEntity ? pos : null;
    }

    private static void interruptAll() {
        boolean anyActive = healActive || spell2Active || repairChannelActive;
        healActive = false;
        spell2Active = false;
        repairChannelActive = false;
        repairKeyWasDown = false;
        if (anyActive) {
            sendStop();
        }
    }

    private static void sendStart(AbilitySlot slot, @Nullable BlockPos target) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new StartChannelAbilityPayload(slot.ordinal(), Optional.ofNullable(target)).toVanillaServerbound());
        }
    }

    private static void sendStop() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new StopChannelAbilityPayload().toVanillaServerbound());
        }
    }

    private static void sendBurst(AbilitySlot slot) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ActivateBurstAbilityPayload(slot.ordinal()).toVanillaServerbound());
        }
    }
}
