package com.github.c0c0tier.dungeon_defenders.client.atelier;

import com.geckolib.cache.GeckoLibResources;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.github.c0c0tier.dungeon_defenders.atelier.Atelier;
import com.github.c0c0tier.dungeon_defenders.atelier.BlocAnimable;
import com.github.c0c0tier.dungeon_defenders.atelier.FicheBloc;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Le renderer commun à tous les blocs animés de l'Atelier (décor {@code dd_*} et blocs du jeu qui empruntent leur
 * modèle) : le modèle GeckoLib de la fiche, tourné selon {@code facing} (dessiné devant au nord, comme le modèle
 * converti), et son calque lumineux {@code <texture>_glowmask.png} s'il existe. Repris de
 * {@code client/BlocAnimeRenderer.java} de l'Atelier. Client seulement : nommé uniquement par
 * {@code DungeonDefendersModClient}.
 */
public class BlocAnimeRenderer<T extends BlockEntity & BlocAnimable, R extends BlockEntityRenderState & GeoRenderState>
        extends GeoBlockRenderer<T, R> {
    public BlocAnimeRenderer(BlockEntityRendererProvider.Context context, FicheBloc.Anime anime) {
        super(context, new Model<>(anime));
        if (Atelier.aUnGlowmask(anime.texture())) {
            withRenderLayer(new AutoGlowingGeoLayer<>(this));
        }
    }

    private static class Model<T extends BlockEntity & BlocAnimable> extends GeoModel<T> {
        private final Identifier model;
        private final Identifier animation;
        private final Identifier texture;

        Model(FicheBloc.Anime anime) {
            // Même traitement que AtelierMonsterModel : GeckoLib range ses caches sous le chemin sans dossier ni extension.
            this.model = GeckoLibResources.stripPrefixAndSuffix(anime.modele());
            this.animation = GeckoLibResources.stripPrefixAndSuffix(anime.animations());
            this.texture = anime.texture();
        }

        @Override
        public Identifier getModelResource(GeoRenderState renderState) {
            return model;
        }

        @Override
        public Identifier getTextureResource(GeoRenderState renderState) {
            return texture;
        }

        @Override
        public Identifier getAnimationResource(T animatable) {
            return animation;
        }
    }
}
