package com.github.c0c0tier.dungeon_defenders.client.gui.screen;

import com.github.c0c0tier.dungeon_defenders.MapInstance;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.network.StartGamePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

// Ouvert côté client par GameOverPayload (voir PhaseTransitions.onVictory/onDefeat,
// ModNetworking, DungeonDefendersModClient#onRegisterClientPayloadHandlers) à la fin d'une
// partie. Deux boutons :
// - "Rejouer" envoie StartGamePayload, exactement comme le bouton "Jouer" de
//   MapSelectionScreen (MapInstance.startGame recompose la même zone et retéléporte) ;
// - "Retour à la taverne" exécute la commande de harnais MapInstance.RETURN_COMMAND, même
//   effet que le lien cliquable historique dans le chat (voir PhaseTransitions).
// Pas de Menu ni d'échange serveur pour l'affichage lui-même : victory/defeat est déjà connu
// au moment où le paquet arrive, rien à lire de plus.
public class GameOverScreen extends Screen {

    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;
    // Alpha explicite (0xFF______) obligatoire : GuiGraphicsExtractor#text (voir
    // extractRenderState ci-dessous) ignore silencieusement tout texte dont le canal alpha vaut
    // 0 (if (ARGB.alpha(color) != 0) avant d'ajouter le texte au render state, vérifié dans les
    // sources décompilées) — contrairement à l'ancien GuiGraphics.drawString, cette version ne
    // force plus alpha=0xFF par défaut pour une couleur écrite sans son octet de poids fort.
    // Un littéral 0xRRGGBB "nu" (comme ici avant correction, 2026-08-26) vaut donc 0x00RRGGBB
    // en pratique : texte soumis au rendu mais totalement transparent, donc invisible sans la
    // moindre erreur — exactement le bug signalé en jeu ("juste les boutons, pas le texte").
    private static final int TITLE_COLOR_VICTORY = 0xFF55FF55;
    private static final int TITLE_COLOR_DEFEAT = 0xFFFF5555;

    private final boolean victory;

    public GameOverScreen(boolean victory) {
        super(Component.translatable(victory
                ? "dungeon_defenders.game.victory"
                : "dungeon_defenders.game.defeat"));
        this.victory = victory;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int firstButtonY = this.height / 2;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.game_over.replay"), button -> onReplay())
                .bounds(centerX - BUTTON_WIDTH / 2, firstButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.game_over.return_to_tavern"), button -> onReturnToTavern())
                .bounds(centerX - BUTTON_WIDTH / 2, firstButtonY + BUTTON_HEIGHT + BUTTON_GAP, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.centeredText(this.font, this.getTitle(), this.width / 2, this.height / 2 - 40,
                this.victory ? TITLE_COLOR_VICTORY : TITLE_COLOR_DEFEAT);
    }

    // Relance LA MÊME map : son identifiant vient de l'attachment ModAttachments.CURRENT_MAP,
    // posé par MapInstance.startGame et synchronisé — l'écran de choix est fermé depuis
    // longtemps, le client n'a plus la liste des maps sous la main.
    private void onReplay() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        Identifier currentMap = minecraft.level == null
                ? null
                : Identifier.tryParse(minecraft.level.getData(ModAttachments.CURRENT_MAP));
        if (connection != null && currentMap != null) {
            connection.send(new StartGamePayload(currentMap).toVanillaServerbound());
        }
        this.onClose();
    }

    private void onReturnToTavern() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.sendCommand(MapInstance.RETURN_COMMAND);
        }
        this.onClose();
    }
}
