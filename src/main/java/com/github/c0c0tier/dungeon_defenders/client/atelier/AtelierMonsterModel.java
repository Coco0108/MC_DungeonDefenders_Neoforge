package com.github.c0c0tier.dungeon_defenders.client.atelier;

import com.geckolib.cache.GeckoLibResources;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.github.c0c0tier.dungeon_defenders.atelier.AtelierMonster;
import com.github.c0c0tier.dungeon_defenders.atelier.FicheMonstre;
import net.minecraft.resources.Identifier;

/**
 * Le modèle GeckoLib d'un monstre de l'Atelier : modèle, animations et texture pris dans sa fiche. Repris de
 * {@code client/MonsterGeoModel.java} de l'Atelier, avec en plus la texture de la VARIANTE de chaque monstre, passée
 * de l'entité à son état de rendu par {@link #VARIANTE}.
 */
public class AtelierMonsterModel extends GeoModel<AtelierMonster> {
    /** Index de variante de l'entité dessinée (-1 : texture principale). */
    public static final DataTicket<Integer> VARIANTE = DataTicket.create("dungeon_defenders_variante", Integer.class);

    private final FicheMonstre fiche;
    private final Identifier model;
    private final Identifier animation;

    public AtelierMonsterModel(FicheMonstre fiche) {
        this.fiche = fiche;
        // Les fiches donnent des chemins d'assets complets (dungeon_defenders:geckolib/models/entity/<x>.geo.json) ;
        // GeckoLib range ses caches sous le chemin sans le préfixe geckolib/models|animations/ ni l'extension, ce que
        // stripPrefixAndSuffix fait exactement.
        this.model = GeckoLibResources.stripPrefixAndSuffix(fiche.modele());
        this.animation = GeckoLibResources.stripPrefixAndSuffix(fiche.animations());
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return this.model;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return this.fiche.texture(renderState.getOrDefaultGeckolibData(VARIANTE, -1));
    }

    @Override
    public Identifier getAnimationResource(AtelierMonster animatable) {
        return this.animation;
    }

    @Override
    public void addAdditionalStateData(AtelierMonster animatable, Object relatedObject, GeoRenderState renderState) {
        super.addAdditionalStateData(animatable, relatedObject, renderState);
        renderState.addGeckolibData(VARIANTE, animatable.variante());
    }
}
