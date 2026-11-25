package com.github.c0c0tier.dungeon_defenders.atelier;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Un objet par fiche d'objet ({@code data/dungeon_defenders/atelier_objets/<id>.json}) et un bloc, avec son objet, par
 * fiche de bloc ({@code atelier_blocs/<id>.json} ; un {@link BlocModele} pour un modèle converti), tous dans l'onglet
 * « Dungeon Defenders ». Aucun code par objet ni par bloc : tout vient de la fiche. Repris de
 * {@code objet/AtelierObjets.java} de l'Atelier, sans les blocs-cachettes ni l'onglet « Atelier ».
 *
 * <p>Les fichiers natifs (états de bloc, modèles et définitions d'objet, butin du bloc, tags d'outil, noms) ne sont pas
 * écrits ici : ils arrivent avec le déploiement (voir doc/ATELIER.md). Ce code ne fait qu'ENREGISTRER.
 */
public final class AtelierObjets {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DungeonDefendersMod.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DungeonDefendersMod.MODID);
    /** Objets puis blocs, dans l'ordre de l'onglet créatif. */
    private static final List<Supplier<? extends Item>> CONTENTS = new ArrayList<>();

    private AtelierObjets() {
    }

    /** Après {@link AtelierLoader#load}, à la construction du mod. */
    public static void register(IEventBus modEventBus) {
        for (FicheObjet objet : Atelier.objets().values()) {
            CONTENTS.add(ITEMS.registerSimpleItem(objet.id(), properties -> itemProperties(objet, properties)));
        }
        for (FicheBloc bloc : Atelier.blocs().values()) {
            var block = BLOCKS.registerBlock(bloc.id(),
                    bloc.modele() ? properties -> new BlocModele(properties, Atelier.forme(bloc.id()))
                            : bloc.transparent() ? TransparentBlock::new : Block::new,
                    properties -> blockProperties(bloc, properties));
            CONTENTS.add(ITEMS.registerSimpleBlockItem(block));
        }
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(AtelierObjets::addToCreativeTab);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == DungeonDefendersMod.DUNGEON_DEFENDERS_TAB.get()) {
            CONTENTS.forEach(item -> event.accept(new ItemStack(item.get())));
        }
    }

    private static Item.Properties itemProperties(FicheObjet objet, Item.Properties properties) {
        properties.stacksTo(objet.pile()).rarity(objet.rarete().rarity);
        if (objet.brillant()) {
            properties.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        objet.nourriture().ifPresent(nourriture -> {
            FoodProperties.Builder food = new FoodProperties.Builder().nutrition(nourriture.faim())
                    .saturationModifier(nourriture.saturation());
            if (nourriture.toujours()) {
                food.alwaysEdible();
            }
            properties.food(food.build());
        });
        return properties;
    }

    /**
     * Dureté, résistance, sons et lumière de la fiche. Un {@code niveau} ne le fait tomber qu'avec le bon outil (les tags
     * qui disent quel outil arrivent avec le déploiement). Transparent : ne cache rien derrière lui, rien n'y apparaît, on
     * n'y étouffe pas, comme le verre. Un bloc modèle ne cache jamais les faces voisines : il ne remplit pas son cube.
     */
    private static BlockBehaviour.Properties blockProperties(FicheBloc bloc, BlockBehaviour.Properties properties) {
        properties.strength(bloc.durete(), bloc.resistance().orElse(bloc.durete())).sound(bloc.son().sound);
        if (bloc.lumiere() > 0) {
            int light = bloc.lumiere();
            properties.lightLevel(state -> light);
        }
        if (bloc.niveau() != FicheBloc.Niveau.AUCUN) {
            properties.requiresCorrectToolForDrops();
        }
        if (bloc.modele()) {
            properties.noOcclusion();
        }
        if (bloc.transparent()) {
            properties.noOcclusion().isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false);
        }
        return properties;
    }
}
