package com.github.c0c0tier.dungeon_defenders.client.atelier;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.github.c0c0tier.dungeon_defenders.atelier.Atelier;
import com.github.c0c0tier.dungeon_defenders.atelier.AtelierMonster;
import com.github.c0c0tier.dungeon_defenders.atelier.FicheMonstre;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Le renderer commun à tous les monstres de l'Atelier, réglé par sa fiche : modèle, texture de la variante, échelle,
 * ombre. Repris de {@code client/MonsterRenderer.java} de l'Atelier, avec :
 * <ul>
 *   <li>le calque lumineux GeckoLib ({@code <texture>_glowmask.png}) quand l'une de ses textures en a un, dessiné
 *       seulement pour les textures qui en ont un (sinon rien) ;</li>
 *   <li>pas de basculement sur le côté à la mort quand la fiche a une animation {@code death} : c'est elle qui joue.</li>
 * </ul>
 */
public class AtelierMonsterRenderer extends GeoEntityRenderer<AtelierMonster, LivingEntityRenderState> {
    private final FicheMonstre fiche;

    public AtelierMonsterRenderer(EntityRendererProvider.Context context, FicheMonstre fiche) {
        super(context, new AtelierMonsterModel(fiche));
        this.fiche = fiche;
        withScale(fiche.echelle());
        this.shadowRadius = fiche.hitbox().largeur() * 0.5F * fiche.echelle();
        boolean glowmask = Atelier.aUnGlowmask(fiche.texture())
                || fiche.variantes().stream().anyMatch(variante -> Atelier.aUnGlowmask(variante.texture()));
        if (glowmask) {
            addGlowmaskLayer();
        }
    }

    @Override
    protected float getDeathMaxRotation(GeoRenderState renderState) {
        return this.fiche.animation("death").isPresent() ? 0.0F : super.getDeathMaxRotation(renderState);
    }

    // LivingEntityRenderState n'implémente GeoRenderState qu'à l'exécution (mixin de GeckoLib) : les
    // types génériques du calque ne se rejoignent pas à la compilation, d'où ces conversions.
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addGlowmaskLayer() {
        withRenderLayer((GeoRenderLayer) new GlowmaskLayer((GeoRenderer) this));
    }

    /** Le calque lumineux de GeckoLib, sauté pour une variante dont la texture n'a pas de glowmask. */
    private final class GlowmaskLayer extends AutoGlowingGeoLayer<AtelierMonster, Void, GeoRenderState> {
        GlowmaskLayer(GeoRenderer<AtelierMonster, Void, GeoRenderState> renderer) {
            super(renderer);
        }

        @Override
        public void submitRenderTask(RenderPassInfo<GeoRenderState> pass, SubmitNodeCollector collector) {
            int variante = pass.renderState().getOrDefaultGeckolibData(AtelierMonsterModel.VARIANTE, -1);
            if (Atelier.aUnGlowmask(fiche.texture(variante))) {
                super.submitRenderTask(pass, collector);
            }
        }
    }
}
