package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.atelier.Atelier;
import com.github.c0c0tier.dungeon_defenders.atelier.BlocModeleOriente;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * La forme d'un bloc du mod qui emprunte le modèle d'un bloc de décor de l'Atelier (le cristal de la taverne, la forge,
 * le mannequin : modèles {@code dd_cristal_v2}, {@code dd_forge_v2}, {@code dd_dummy}). Elle vient du même fichier que
 * celle du bloc de décor ({@code atelier_formes_bloc/<id>.json}, déployé par l'Atelier), tournée comme lui pour un bloc
 * orienté (modèle dessiné devant au nord). Si l'Atelier n'a rien déployé, repli sur un bloc plein.
 *
 * <p>Calculée au premier usage : les formes de l'Atelier sont lues au démarrage du mod, avant tout appel de forme.
 */
public final class FormeAtelier {
    private final String blocAtelier;
    private volatile Map<Direction, VoxelShape> formes;

    public FormeAtelier(String blocAtelier) {
        this.blocAtelier = blocAtelier;
    }

    /** La forme du modèle tourné vers {@code facing} ({@link Direction#NORTH} pour un bloc non orienté). */
    public VoxelShape get(Direction facing) {
        Map<Direction, VoxelShape> current = formes;
        if (current == null) {
            current = new EnumMap<>(Direction.class);
            List<double[]> boites = Atelier.forme(blocAtelier);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                current.put(direction, boites.isEmpty() ? Shapes.block()
                        : boites.stream().map(b -> BlocModeleOriente.turn(b, direction))
                                .map(b -> Block.box(b[0], b[1], b[2], b[3], b[4], b[5]))
                                .reduce(Shapes.empty(), Shapes::or).optimize());
            }
            formes = current;
        }
        return current.get(facing);
    }
}
