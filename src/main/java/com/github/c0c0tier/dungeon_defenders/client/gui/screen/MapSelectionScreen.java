package com.github.c0c0tier.dungeon_defenders.client.gui.screen;

import com.github.c0c0tier.dungeon_defenders.init.GameDifficulty;
import com.github.c0c0tier.dungeon_defenders.init.GameMap;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.network.SetDifficultyPayload;
import com.github.c0c0tier.dungeon_defenders.network.StartGamePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Écran ouvert par TavernCrystalBlock : choix d'une map (carrousel, flèches ◀ ▶, aperçu image
// + nom) et de la difficulté (Facile/Moyen/Difficile, un seul sélectionné à la fois). Pas de
// Menu ni de réseau pour LIRE l'état de cet écran (la liste des maps est statique côté client,
// la difficulté actuelle vient de ModAttachments.DIFFICULTY, déjà synchronisé) — seul le clic
// sur "Jouer" envoie quelque chose au serveur.
//
// Le choix de map précis n'a pour l'instant aucun effet : une seule map "placeholder"
// générique existe (voir MapInstance), donc "Jouer" lance toujours la même chose quelle que
// soit la map sélectionnée dans le carrousel — le vrai chargement d'une structure par map
// reste à faire, voir 05-etat-et-problemes-connus.md.
public class MapSelectionScreen extends Screen {

    private static final int IMAGE_WIDTH = 128;
    private static final int IMAGE_HEIGHT = 72;
    private static final int ARROW_SIZE = 16;
    private static final int DIFFICULTY_BUTTON_WIDTH = 90;
    private static final int DIFFICULTY_BUTTON_HEIGHT = 16;
    private static final int DIFFICULTY_BUTTON_GAP = 6;
    private static final int PLAY_BUTTON_WIDTH = 100;
    private static final int PLAY_BUTTON_HEIGHT = 16;
    private static final int TEXT_COLOR = 0xFFFFFF;

    private final List<GameMap> maps = GameMap.visibleMaps();
    private final Map<GameDifficulty, Button> difficultyButtons = new EnumMap<>(GameDifficulty.class);

    private int selectedMapIndex;
    private GameDifficulty selectedDifficulty;
    private int imageX;
    private int imageY;

    public MapSelectionScreen() {
        super(Component.translatable("dungeon_defenders.map_selection.title"));
        this.selectedDifficulty = resolveCurrentDifficulty();
    }

    private static GameDifficulty resolveCurrentDifficulty() {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return GameDifficulty.NORMAL;
        }
        return GameDifficulty.values()[level.getData(ModAttachments.DIFFICULTY)];
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int mapColumnCenterX = centerX - 90;
        int difficultyColumnCenterX = centerX + 90;
        int columnsTop = 42;

        this.imageX = mapColumnCenterX - IMAGE_WIDTH / 2;
        this.imageY = columnsTop;

        int arrowY = this.imageY + (IMAGE_HEIGHT - ARROW_SIZE) / 2;
        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> onPreviousMap())
                .bounds(this.imageX - ARROW_SIZE - 4, arrowY, ARROW_SIZE, ARROW_SIZE)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> onNextMap())
                .bounds(this.imageX + IMAGE_WIDTH + 4, arrowY, ARROW_SIZE, ARROW_SIZE)
                .build());

        this.difficultyButtons.clear();
        GameDifficulty[] difficulties = GameDifficulty.values();
        for (int i = 0; i < difficulties.length; i++) {
            GameDifficulty difficulty = difficulties[i];
            int y = columnsTop + i * (DIFFICULTY_BUTTON_HEIGHT + DIFFICULTY_BUTTON_GAP);
            Button button = this.addRenderableWidget(Button.builder(
                            difficultyLabel(difficulty), b -> onSelectDifficulty(difficulty))
                    .bounds(difficultyColumnCenterX - DIFFICULTY_BUTTON_WIDTH / 2, y, DIFFICULTY_BUTTON_WIDTH, DIFFICULTY_BUTTON_HEIGHT)
                    .build());
            this.difficultyButtons.put(difficulty, button);
        }

        int columnsHeight = Math.max(
                IMAGE_HEIGHT + 20,
                difficulties.length * (DIFFICULTY_BUTTON_HEIGHT + DIFFICULTY_BUTTON_GAP));
        int playY = columnsTop + columnsHeight + 12;
        Button playButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("dungeon_defenders.map_selection.play"), button -> onPlay())
                .bounds(centerX - PLAY_BUTTON_WIDTH / 2, playY, PLAY_BUTTON_WIDTH, PLAY_BUTTON_HEIGHT)
                .build());
        playButton.active = !this.maps.isEmpty();
    }

    private Component difficultyLabel(GameDifficulty difficulty) {
        Component base = Component.translatable(difficulty.translationKey());
        return difficulty == this.selectedDifficulty
                ? Component.literal("> ").append(base).append(Component.literal(" <"))
                : base;
    }

    private void onSelectDifficulty(GameDifficulty difficulty) {
        this.selectedDifficulty = difficulty;
        for (Map.Entry<GameDifficulty, Button> entry : this.difficultyButtons.entrySet()) {
            entry.getValue().setMessage(difficultyLabel(entry.getKey()));
        }
    }

    private void onPreviousMap() {
        if (this.maps.isEmpty()) {
            return;
        }
        this.selectedMapIndex = (this.selectedMapIndex - 1 + this.maps.size()) % this.maps.size();
    }

    private void onNextMap() {
        if (this.maps.isEmpty()) {
            return;
        }
        this.selectedMapIndex = (this.selectedMapIndex + 1) % this.maps.size();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        guiGraphics.centeredText(this.font, this.getTitle(), centerX, 16, TEXT_COLOR);

        if (this.maps.isEmpty()) {
            guiGraphics.centeredText(this.font, Component.translatable("dungeon_defenders.map_selection.none"),
                    this.imageX + IMAGE_WIDTH / 2, this.imageY + IMAGE_HEIGHT / 2, TEXT_COLOR);
            return;
        }

        GameMap currentMap = this.maps.get(this.selectedMapIndex);
        guiGraphics.blit(currentMap.previewTexture(), this.imageX, this.imageY, IMAGE_WIDTH, IMAGE_HEIGHT, 0.0F, 0.0F, 1.0F, 1.0F);
        guiGraphics.centeredText(this.font, currentMap.displayName(),
                this.imageX + IMAGE_WIDTH / 2, this.imageY + IMAGE_HEIGHT + 8, TEXT_COLOR);
    }

    private void onPlay() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            // Deux paquets distincts, envoyés l'un après l'autre sur la même connexion (donc
            // reçus et traités dans cet ordre côté serveur) : la difficulté d'abord, puis le
            // vrai déclenchement de la partie.
            connection.send(new SetDifficultyPayload(this.selectedDifficulty.ordinal()).toVanillaServerbound());
            connection.send(new StartGamePayload().toVanillaServerbound());
        }
        this.onClose();
    }
}
