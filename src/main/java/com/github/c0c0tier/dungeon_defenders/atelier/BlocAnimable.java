package com.github.c0c0tier.dungeon_defenders.atelier;

import com.geckolib.animatable.GeoBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Un block entity dessiné par le modèle animé d'un bloc de l'Atelier (voir {@link AnimationsBloc}) : le renderer
 * commun ({@code client/atelier/BlocAnimeRenderer}) le dessine, le serveur lui fait jouer ses rôles.
 */
public interface BlocAnimable extends GeoBlockEntity {
    AnimationsBloc animations();

    /** Joue un rôle pour tous les joueurs autour (côté serveur) ; faux si la fiche ne l'a pas. */
    default boolean jouer(String role) {
        return animations().jouer((BlockEntity) this, role);
    }
}
