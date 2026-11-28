package com.github.c0c0tier.dungeon_defenders.atelier;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Un objet par fiche d'objet ({@code data/dungeon_defenders/atelier_objets/<id>.json}) et un bloc, avec son objet, par
 * fiche de bloc ({@code atelier_blocs/<id>.json} ; un {@link BlocModele} pour un modèle converti, un
 * {@link BlocModeleOriente} s'il se tourne vers le joueur, un {@link RotatedPillarBlock} pour un cube dans un axe, la
 * classe vanilla du chêne pour une porte, une trappe, un escalier, une dalle, une barrière ou un portillon), tous dans l'onglet
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
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DungeonDefendersMod.MODID);
    /** Un type de block entity par bloc animé ({@code anime}), comme un type d'entité par monstre. */
    private static final Map<String, Supplier<BlockEntityType<BlocAnimeEntity>>> ANIMES = new HashMap<>();

    private AtelierObjets() {
    }

    /** Après {@link AtelierLoader#load}, à la construction du mod. */
    public static void register(IEventBus modEventBus) {
        for (FicheObjet objet : Atelier.objets().values()) {
            CONTENTS.add(ITEMS.registerSimpleItem(objet.id(), properties -> itemProperties(objet, properties)));
        }
        for (FicheBloc bloc : Atelier.blocs().values()) {
            var block = BLOCKS.registerBlock(bloc.id(), constructeur(bloc), properties -> blockProperties(bloc, properties));
            CONTENTS.add(ITEMS.registerSimpleBlockItem(block));
            if (anime(bloc)) {
                ANIMES.put(bloc.id(), BLOCK_ENTITIES.register(bloc.id(), () -> new BlockEntityType<>(
                        (pos, state) -> new BlocAnimeEntity(ANIMES.get(bloc.id()).get(), pos, state, bloc.id()), Set.of(block.get()))));
            }
        }
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(AtelierObjets::addToCreativeTab);
    }

    /** Les types de block entity des blocs animés, par id de bloc (pour leurs renderers, côté client). */
    public static Map<String, Supplier<BlockEntityType<BlocAnimeEntity>>> animes() {
        return Collections.unmodifiableMap(ANIMES);
    }

    /** Un bloc {@code modele} simple avec {@code anime} (le chargeur refuse {@code anime} ailleurs). */
    private static boolean anime(FicheBloc bloc) {
        return bloc.anime().isPresent() && bloc.modele() && bloc.type() == FicheBloc.Type.SIMPLE;
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == DungeonDefendersMod.DUNGEON_DEFENDERS_TAB.get()) {
            CONTENTS.forEach(item -> event.accept(new ItemStack(item.get())));
        }
    }

    /** La classe du bloc, d'après sa fiche (même choix que l'Atelier). */
    private static Function<BlockBehaviour.Properties, ? extends Block> constructeur(FicheBloc bloc) {
        if (bloc.type() != FicheBloc.Type.SIMPLE) {
            return menuiserie(bloc.type());
        }
        if (anime(bloc)) {
            List<double[]> forme = Atelier.forme(bloc.id());
            Supplier<BlockEntityType<BlocAnimeEntity>> type = () -> ANIMES.get(bloc.id()).get();
            return bloc.orientation() == FicheBloc.Orientation.HORIZONTALE
                    ? properties -> new BlocAnimeOriente(properties, forme, type)
                    : properties -> new BlocAnime(properties, forme, type);
        }
        if (bloc.modele()) {
            List<double[]> forme = Atelier.forme(bloc.id());
            return bloc.orientation() == FicheBloc.Orientation.HORIZONTALE
                    ? properties -> new BlocModeleOriente(properties, forme)
                    : properties -> new BlocModele(properties, forme);
        }
        return bloc.orientation() == FicheBloc.Orientation.AXE ? RotatedPillarBlock::new
                : bloc.transparent() ? TransparentBlock::new : Block::new;
    }

    /**
     * Une menuiserie ({@code type}) : la classe vanilla de son équivalent en chêne, pour qu'elle s'ouvre, se tourne, se
     * raccorde et se pose exactement comme lui (jeu du chêne : s'ouvre à la main, sons du chêne). Sa forme est celle du
     * vanilla ; ses modèles (un par état), états de bloc, butin et tags arrivent avec le déploiement.
     */
    private static Function<BlockBehaviour.Properties, ? extends Block> menuiserie(FicheBloc.Type type) {
        return switch (type) {
            case PORTE -> properties -> new DoorBlock(BlockSetType.OAK, properties);
            case TRAPPE -> properties -> new TrapDoorBlock(BlockSetType.OAK, properties);
            case ESCALIER -> properties -> new StairBlock(Blocks.OAK_PLANKS.defaultBlockState(), properties);
            case DALLE -> SlabBlock::new;
            case BARRIERE -> FenceBlock::new;
            case PORTILLON -> properties -> new FenceGateBlock(WoodType.OAK, properties);
            case SIMPLE -> throw new IllegalArgumentException("pas une menuiserie");
        };
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
        // Les propriétés de leurs équivalents en chêne qui ne sont pas dans la fiche.
        switch (bloc.type()) {
            case PORTE -> properties.noOcclusion().pushReaction(PushReaction.DESTROY);
            case TRAPPE -> properties.noOcclusion().isValidSpawn((state, level, pos, type) -> false);
            case BARRIERE, PORTILLON -> properties.forceSolidOn();
            default -> {
            }
        }
        if (bloc.transparent()) {
            properties.noOcclusion().isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false);
        }
        return properties;
    }
}
