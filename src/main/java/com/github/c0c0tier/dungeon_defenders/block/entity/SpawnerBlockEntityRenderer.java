package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.init.DifficultyScaling;
import com.github.c0c0tier.dungeon_defenders.init.GamePhase;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// Aperçu de la composition du spawner (total + détail par type), en billboard au-dessus du
// bloc et visible à travers les murs — comme dans le jeu de référence, pour planifier sa
// défense pendant la phase de Construction, avant que le combat démarre. Caché en phase
// Combat (voir extractRenderState) : pas d'intérêt une fois la vague lancée, et évite de
// polluer l'écran pendant le combat.
//
// Icônes des ennemis : pas encore de sprite par type (voir 05-etat-et-problemes-connus.md),
// texte seul pour cette première version — SpawnableEnemy fournit déjà tout ce qu'il faut
// (translationKey) pour brancher une icône plus tard sans revoir cette classe.
public class SpawnerBlockEntityRenderer implements BlockEntityRenderer<SpawnerBlockEntity, SpawnerRenderState> {

    private static final float TEXT_Y = 1.8F;
    // Même échelle que les name tags vanilla (EntityRenderer.NAMETAG_SCALE = 0.025F), pour
    // une taille de texte cohérente avec le reste du jeu.
    private static final float TEXT_SCALE = 0.025F;
    private static final int LINE_HEIGHT = 10;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int BACKGROUND_COLOR = 0x60000000;
    // Lightmap "pleine luminosité" (0xF000F0) : constante vanilla utilisée pour les textes
    // qui doivent rester lisibles quel que soit l'éclairage ambiant (ex. noms des mobs
    // brillants), retrouvée dans le bytecode de AbstractSignRenderer faute de champ nommé
    // exposé dans cette version.
    private static final int FULL_BRIGHT_LIGHT = 15728880;
    // Au-delà de cette distance, l'aperçu se cache : sur une carte avec beaucoup de spawners,
    // les afficher tous en permanence à travers les murs deviendrait illisible.
    private static final double MAX_DISTANCE_SQ = 32.0 * 32.0;

    private final Font font;

    public SpawnerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
    }

    @Override
    public SpawnerRenderState createRenderState() {
        return new SpawnerRenderState();
    }

    @Override
    public void extractRenderState(
            SpawnerBlockEntity blockEntity,
            SpawnerRenderState state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        state.lines.clear();
        state.visible = false;

        Level level = blockEntity.getLevel();
        if (level == null || level.getData(ModAttachments.GAME_PHASE) != GamePhase.BUILD.ordinal()) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        double distanceSq = cameraPosition.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (distanceSq > MAX_DISTANCE_SQ) {
            return;
        }

        double multiplier = DifficultyScaling.getMultiplier(level);
        List<Component> entryLines = new ArrayList<>();
        int total = 0;
        for (SpawnerBlockEntity.SpawnEntry entry : blockEntity.getEntries()) {
            // Même formule que SpawnEntry.resetForWave(...), pour que l'aperçu affiché ici
            // corresponde exactement à ce que la prochaine vague fera spawn.
            int count = Math.max(1, (int) Math.round(entry.baseCount() * multiplier));
            total += count;
            entryLines.add(Component.translatable(
                    "dungeon_defenders.spawner.preview_entry",
                    Component.translatable(entry.enemy().translationKey()),
                    count));
        }

        state.visible = true;
        state.lines.add(Component.translatable("dungeon_defenders.spawner.preview_total", total));
        state.lines.addAll(entryLines);
    }

    @Override
    public void submit(
            SpawnerRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        if (!state.visible || state.lines.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, TEXT_Y, 0.5F);
        // Billboard, comme la barre de vie du cristal : orientation caméra + inversion de Y
        // (après mulPose, +Y va vers le bas, comme pour les name tags).
        poseStack.mulPose(camera.orientation);
        poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        for (int i = 0; i < state.lines.size(); i++) {
            FormattedCharSequence line = state.lines.get(i).getVisualOrderText();
            float x = -this.font.width(line) / 2.0F;
            float y = i * LINE_HEIGHT;
            // DisplayMode.SEE_THROUGH : rend le texte au travers des blocs, comme les noms
            // des mobs brillants (Glowing) — c'est le mécanisme qui traverse les murs ici,
            // pas debugQuads (réservé aux formes non texturées comme la barre de vie).
            collector.submitText(poseStack, x, y, line, false, Font.DisplayMode.SEE_THROUGH,
                    FULL_BRIGHT_LIGHT, TEXT_COLOR, BACKGROUND_COLOR, 0);
        }

        poseStack.popPose();
    }
}
