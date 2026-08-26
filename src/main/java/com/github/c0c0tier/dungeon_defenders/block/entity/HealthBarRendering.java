package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;

// Barre de vie en billboard générique (segment plein dégradé vert -> jaune -> rouge + segment
// gris restant) : dessine le quad, appelant n'a qu'à fournir taille et ratio, le poseStack
// devant déjà être positionné/orienté (translate + mulPose(camera.orientation) + scale(1,-1,1)).
// Extraite quand TowerHealthBarRenderer a fait apparaître une vraie duplication avec
// EterniaCrystalBlockEntityRenderer — même geste, juste une taille/portée différente.
public final class HealthBarRendering {
    private HealthBarRendering() {
    }

    public static void render(
            PoseStack poseStack, SubmitNodeCollector collector, float width, float height, float healthPercent) {
        // debugQuads : quads non texturés, translucides et non cullés — exactement ce qu'il
        // faut pour une barre de vie, et visible sous tous les angles.
        collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(),
                (pose, buffer) -> {
                    float left = -width / 2.0F;
                    float right = left + width;
                    float split = left + width * healthPercent;

                    // Les deux segments sont juxtaposés, jamais superposés : ce render type
                    // trie les quads par distance à la caméra (sortOnUpload), et deux quads
                    // coplanaires donneraient un ordre instable — la barre apparaissait alors
                    // entièrement grise par intermittence.
                    if (healthPercent > 0.0F) {
                        addSegment(pose, buffer, left, split, height, red(healthPercent), green(healthPercent), 0.0F);
                    }
                    if (healthPercent < 1.0F) {
                        addSegment(pose, buffer, split, right, height, 0.3F, 0.3F, 0.3F);
                    }
                });
    }

    /** Ajoute un quad allant de {@code x0} à {@code x1}, de 0 à {@code height}. */
    private static void addSegment(
            PoseStack.Pose pose, VertexConsumer buffer, float x0, float x1, float height, float r, float g, float b) {
        buffer.addVertex(pose, x0, 0.0F, 0.0F).setColor(r, g, b, 1.0F);
        buffer.addVertex(pose, x1, 0.0F, 0.0F).setColor(r, g, b, 1.0F);
        buffer.addVertex(pose, x1, height, 0.0F).setColor(r, g, b, 1.0F);
        buffer.addVertex(pose, x0, height, 0.0F).setColor(r, g, b, 1.0F);
    }

    // Dégradé vert -> jaune -> rouge : au-dessus de 50 % le rouge monte, en dessous le vert
    // descend.
    private static float red(float healthPercent) {
        return healthPercent > 0.5F ? (1.0F - healthPercent) * 2.0F : 1.0F;
    }

    private static float green(float healthPercent) {
        return healthPercent > 0.5F ? 1.0F : healthPercent * 2.0F;
    }
}
