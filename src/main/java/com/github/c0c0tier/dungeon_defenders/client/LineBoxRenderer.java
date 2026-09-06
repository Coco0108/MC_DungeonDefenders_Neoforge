package com.github.c0c0tier.dungeon_defenders.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

// Dessine le contour filaire d'une forme (une boîte, en pratique toujours Shapes.block()) pour
// RenderTypes.lines(). Extrait ici (2026-09-06) parce que TowerPlacementClientEvents et
// TowerRemovalClientEvents en avaient chacun leur propre copie — assumé à l'époque
// ("pas assez de logique partagée pour justifier une extraction commune vu la taille des deux
// classes") — mais MarkerOverlayClientEvents en aurait fait une troisième : la duplication
// devient le vrai coût à ce point-là.
final class LineBoxRenderer {

    private LineBoxRenderer() {
    }

    static void renderBoxOutline(PoseStack.Pose pose, VertexConsumer buffer, VoxelShape shape, int color, float width) {
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            Vector3f normal = new Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
            buffer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(color).setNormal(pose, normal).setLineWidth(width);
            buffer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(color).setNormal(pose, normal).setLineWidth(width);
        });
    }
}
