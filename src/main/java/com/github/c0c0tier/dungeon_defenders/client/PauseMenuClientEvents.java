package com.github.c0c0tier.dungeon_defenders.client;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.network.LeaveMapPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

// Ajoute un bouton "Abandonner le niveau" au menu pause vanilla.
//
// Choisi (2026-09-02) plutôt qu'un bloc de sortie à poser dans chaque map : ça marche sur
// TOUTES les maps sans que le mappeur ait quoi que ce soit à placer — y compris celles où il
// aurait oublié une sortie — et c'est là où un joueur cherche naturellement à quitter. La
// commande /dd_leave reste en parallèle comme harnais de test.
//
// Passe par ScreenEvent.Init.Post, qui permet d'ajouter des widgets à un écran vanilla déjà
// initialisé (`addListener`). Le bouton n'apparaît **que pendant une partie** : proposer
// d'abandonner depuis la taverne n'a aucun sens.
@EventBusSubscriber(modid = DungeonDefendersMod.MODID, value = Dist.CLIENT)
public final class PauseMenuClientEvents {

    private static final int BUTTON_WIDTH = 204;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;

    private PauseMenuClientEvents() {
    }

    @SubscribeEvent
    static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof PauseScreen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || !GamePhase.of(level).isInGame()) {
            return;
        }

        Component label = Component.translatable("dungeon_defenders.pause.abandon")
                .withStyle(ChatFormatting.RED);

        event.addListener(Button.builder(label, button -> openConfirmation(screen))
                .bounds(screen.width / 2 - BUTTON_WIDTH / 2, bottomOf(event, screen), BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    // Placé sous le widget le plus bas déjà présent, plutôt qu'à une position calculée à partir
    // de la mise en page vanilla : celle-ci change d'une version à l'autre, et d'autres mods
    // peuvent aussi avoir ajouté des boutons ici. Repli en bas de l'écran si l'écran est vide
    // (ne devrait pas arriver).
    private static int bottomOf(ScreenEvent.Init event, Screen screen) {
        int lowest = Integer.MIN_VALUE;
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof AbstractWidget widget) {
                lowest = Math.max(lowest, widget.getY() + widget.getHeight());
            }
        }
        int y = lowest == Integer.MIN_VALUE ? screen.height - BUTTON_HEIGHT - BUTTON_GAP : lowest + BUTTON_GAP;
        // Ne jamais déborder hors de l'écran, même si la liste vanilla est très longue (petite
        // résolution, GUI scale élevé...).
        return Math.min(y, screen.height - BUTTON_HEIGHT - BUTTON_GAP);
    }

    // Confirmation demandée par le joueur : le menu pause s'ouvre par réflexe, et un clic à côté
    // ferait perdre la partie en cours sans retour possible. "Non" ramène au menu pause, pas au
    // jeu — on repasse par le même écran, dont init() est rejoué par setScreen.
    private static void openConfirmation(Screen pauseScreen) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (!confirmed) {
                        minecraft.setScreen(pauseScreen);
                        return;
                    }
                    leaveMap(minecraft);
                },
                Component.translatable("dungeon_defenders.pause.abandon_confirm_title"),
                Component.translatable("dungeon_defenders.pause.abandon_confirm_message")));
    }

    private static void leaveMap(Minecraft minecraft) {
        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null) {
            connection.send(new LeaveMapPayload().toVanillaServerbound());
        }
        // Referme tout et rend la souris au jeu : la téléportation vers la taverne arrive juste
        // après, côté serveur.
        minecraft.setScreen(null);
        minecraft.mouseHandler.grabMouse();
    }
}
