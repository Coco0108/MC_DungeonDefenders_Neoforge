package com.github.c0c0tier.dungeon_defenders.client.gui.screen;

import com.github.c0c0tier.dungeon_defenders.init.GameDifficulty;
import com.github.c0c0tier.dungeon_defenders.init.MapDefinition;
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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Écran ouvert par TavernCrystalBlock. Trois colonnes, calquées sur le jeu de référence :
//
//   PACKS          |        LA MAP         |  DIFFICULTÉ
//   ▸ Campagne     |    ◀ [aperçu] ▶       |   Facile
//     Cavernes     |      Deeper Well      |   Normal
//     …            |         2 / 5         |   Difficile
//                                              [ Jouer ]
//
// **La liste des maps est reçue du serveur** (OpenMapSelectionPayload), elle n'est plus statique
// côté client : les maps sont découvertes parmi les structures disponibles, ce que seul le
// serveur peut faire (voir MapRegistry). Un pack tiers installé côté serveur apparaît donc ici
// sans que le client ait à le connaître.
//
// Le regroupement en packs se fait par **namespace** : `dungeon_defenders:map/*` est la campagne,
// `<autre>:map/*` un pack tiers. Aucune donnée à déclarer, l'identifiant de structure suffit.
public class MapSelectionScreen extends Screen {

    private static final int IMAGE_WIDTH = 128;
    private static final int IMAGE_HEIGHT = 72;
    private static final int ARROW_SIZE = 16;
    private static final int DIFFICULTY_BUTTON_WIDTH = 90;
    private static final int DIFFICULTY_BUTTON_HEIGHT = 16;
    private static final int DIFFICULTY_BUTTON_GAP = 6;
    private static final int PLAY_BUTTON_WIDTH = 100;
    private static final int PLAY_BUTTON_HEIGHT = 16;
    private static final int PACK_BUTTON_WIDTH = 110;
    private static final int PACK_BUTTON_HEIGHT = 16;
    private static final int PACK_BUTTON_GAP = 3;
    // Au-delà, la colonne des packs défile (molette) plutôt que de déborder de l'écran.
    private static final int MAX_VISIBLE_PACKS = 8;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    /** Maps groupées par pack, dans l'ordre reçu du serveur (campagne d'abord — voir MapRegistry). */
    private final Map<String, List<MapDefinition>> packs = new LinkedHashMap<>();
    private final List<String> packIds = new ArrayList<>();
    private final Map<GameDifficulty, Button> difficultyButtons = new EnumMap<>(GameDifficulty.class);
    private final List<Button> packButtons = new ArrayList<>();

    private int selectedPackIndex;
    private int selectedMapIndex;
    private int packScroll;
    private GameDifficulty selectedDifficulty;
    private int imageX;
    private int imageY;

    public MapSelectionScreen(List<MapDefinition> maps) {
        super(Component.translatable("dungeon_defenders.map_selection.title"));
        this.selectedDifficulty = resolveCurrentDifficulty();
        for (MapDefinition map : maps) {
            this.packs.computeIfAbsent(map.packId(), key -> new ArrayList<>()).add(map);
        }
        this.packIds.addAll(this.packs.keySet());
    }

    private static GameDifficulty resolveCurrentDifficulty() {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return GameDifficulty.NORMAL;
        }
        return GameDifficulty.values()[level.getData(ModAttachments.DIFFICULTY)];
    }

    private List<MapDefinition> currentPackMaps() {
        if (this.packIds.isEmpty()) {
            return List.of();
        }
        return this.packs.get(this.packIds.get(this.selectedPackIndex));
    }

    private MapDefinition currentMap() {
        return currentPackMaps().get(this.selectedMapIndex);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int packColumnCenterX = centerX - 180;
        int mapColumnCenterX = centerX - 20;
        int difficultyColumnCenterX = centerX + 140;
        int columnsTop = 42;

        buildPackButtons(packColumnCenterX, columnsTop);

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
        playButton.active = !this.packIds.isEmpty();
    }

    private void buildPackButtons(int columnCenterX, int top) {
        this.packButtons.clear();
        int visible = Math.min(this.packIds.size(), MAX_VISIBLE_PACKS);
        for (int i = 0; i < visible; i++) {
            int packIndex = this.packScroll + i;
            if (packIndex >= this.packIds.size()) {
                break;
            }
            int y = top + i * (PACK_BUTTON_HEIGHT + PACK_BUTTON_GAP);
            Button button = this.addRenderableWidget(Button.builder(
                            packLabel(packIndex), b -> onSelectPack(packIndex))
                    .bounds(columnCenterX - PACK_BUTTON_WIDTH / 2, y, PACK_BUTTON_WIDTH, PACK_BUTTON_HEIGHT)
                    .build());
            this.packButtons.add(button);
        }
    }

    // Le nom du pack vient de sa propre traduction si elle existe
    // (dungeon_defenders.map_pack.<namespace>, qu'un pack tiers peut fournir dans son fichier de
    // langue), sinon du namespace brut — voir MapDefinition#packDisplayName.
    private Component packLabel(int packIndex) {
        MapDefinition sample = this.packs.get(this.packIds.get(packIndex)).getFirst();
        Component base = sample.packDisplayName();
        return packIndex == this.selectedPackIndex
                ? Component.literal("▸ ").append(base)
                : Component.literal("  ").append(base);
    }

    private void onSelectPack(int packIndex) {
        if (packIndex == this.selectedPackIndex) {
            return;
        }
        this.selectedPackIndex = packIndex;
        this.selectedMapIndex = 0;
        this.rebuildWidgets();
    }

    // Molette sur la colonne des packs : ne sert que s'il y en a plus que MAX_VISIBLE_PACKS.
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.packIds.size() > MAX_VISIBLE_PACKS) {
            int max = this.packIds.size() - MAX_VISIBLE_PACKS;
            int next = Math.clamp(this.packScroll - (int) Math.signum(scrollY), 0, max);
            if (next != this.packScroll) {
                this.packScroll = next;
                this.rebuildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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

    // Le carrousel ne parcourt que les maps du pack sélectionné — c'est tout l'intérêt du
    // regroupement : on ne traverse plus la campagne entière pour atteindre la map d'un DLC.
    private void onPreviousMap() {
        List<MapDefinition> maps = currentPackMaps();
        if (maps.isEmpty()) {
            return;
        }
        this.selectedMapIndex = (this.selectedMapIndex - 1 + maps.size()) % maps.size();
    }

    private void onNextMap() {
        List<MapDefinition> maps = currentPackMaps();
        if (maps.isEmpty()) {
            return;
        }
        this.selectedMapIndex = (this.selectedMapIndex + 1) % maps.size();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        guiGraphics.centeredText(this.font, this.getTitle(), centerX, 16, TEXT_COLOR);

        List<MapDefinition> maps = currentPackMaps();
        if (maps.isEmpty()) {
            guiGraphics.centeredText(this.font, Component.translatable("dungeon_defenders.map_selection.none"),
                    this.imageX + IMAGE_WIDTH / 2, this.imageY + IMAGE_HEIGHT / 2, TEXT_COLOR);
            return;
        }

        MapDefinition currentMap = currentMap();
        guiGraphics.blit(currentMap.previewTexture(), this.imageX, this.imageY, IMAGE_WIDTH, IMAGE_HEIGHT, 0.0F, 0.0F, 1.0F, 1.0F);
        guiGraphics.centeredText(this.font, currentMap.mapDisplayName(),
                this.imageX + IMAGE_WIDTH / 2, this.imageY + IMAGE_HEIGHT + 8, TEXT_COLOR);
        // Position dans le pack : sans ça, rien n'indique qu'il y a d'autres maps derrière les
        // flèches quand un pack en contient plusieurs.
        guiGraphics.centeredText(this.font,
                Component.translatable("dungeon_defenders.map_selection.position",
                        this.selectedMapIndex + 1, maps.size()),
                this.imageX + IMAGE_WIDTH / 2, this.imageY + IMAGE_HEIGHT + 20, TEXT_COLOR);
    }

    private void onPlay() {
        List<MapDefinition> maps = currentPackMaps();
        if (maps.isEmpty()) {
            return;
        }
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            // Deux paquets distincts, envoyés l'un après l'autre sur la même connexion (donc
            // reçus et traités dans cet ordre côté serveur) : la difficulté d'abord, puis le
            // vrai déclenchement de la partie, qui porte la map choisie.
            connection.send(new SetDifficultyPayload(this.selectedDifficulty.ordinal()).toVanillaServerbound());
            connection.send(new StartGamePayload(currentMap().structureId()).toVanillaServerbound());
        }
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
