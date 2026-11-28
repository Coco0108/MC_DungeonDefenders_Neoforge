package com.github.c0c0tier.dungeon_defenders.atelier;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Un objet par fiche d'objet ({@code data/dungeon_defenders/atelier_objets/<id>.json}) et un bloc, avec son objet, par
 * fiche de bloc ({@code atelier_blocs/<id>.json} ; un {@link BlocModele} pour un modèle converti, un
 * {@link BlocModeleOriente} s'il se tourne vers le joueur, un {@link RotatedPillarBlock} pour un cube dans un axe, la
 * classe vanilla du chêne pour une porte, une trappe, un escalier, une dalle, une barrière ou un portillon). Les objets vont
 * dans l'onglet « Dungeon Defenders », les blocs dans un onglet par série (une par map : {@code taverne_*},
 * {@code puits_*}…, voir {@link #registerTabs}). Aucun code par objet ni par bloc : tout vient de la fiche. Repris de
 * {@code objet/AtelierObjets.java} de l'Atelier, sans les blocs-cachettes ni l'onglet « Atelier ».
 *
 * <p>Les fichiers natifs (états de bloc, modèles et définitions d'objet, butin du bloc, tags d'outil, noms) ne sont pas
 * écrits ici : ils arrivent avec le déploiement (voir doc/ATELIER.md). Ce code ne fait qu'ENREGISTRER.
 */
public final class AtelierObjets {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DungeonDefendersMod.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DungeonDefendersMod.MODID);
    /** Les objets, dans l'ordre de l'onglet créatif « Dungeon Defenders ». */
    private static final List<Supplier<? extends Item>> CONTENTS = new ArrayList<>();
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, DungeonDefendersMod.MODID);
    /**
     * Les séries de blocs connues, dans l'ordre des onglets (la taverne, puis les maps de la campagne), avec le bloc qui
     * sert d'icône. Une série inconnue a quand même son onglet, après celles-ci (icône : son premier bloc).
     */
    private static final Map<String, String> SERIES = new LinkedHashMap<>();

    static {
        SERIES.put("taverne", "taverne_tonneau");
        SERIES.put("puits", "puits_margelle");
        SERIES.put("fonderie", "fonderie_enclume");
    }

    /** Les décors {@code dd_*} (cristal, forge, mannequin) sont des meubles de la taverne. */
    private static final Map<String, String> SERIE_DE_PREFIXE = Map.of("dd", "taverne");
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
        Map<String, List<Supplier<? extends Item>>> parSerie = new LinkedHashMap<>();
        SERIES.keySet().forEach(serie -> parSerie.put(serie, new ArrayList<>()));
        Map<String, Supplier<? extends Item>> icones = new HashMap<>();
        for (FicheBloc bloc : Atelier.blocs().values()) {
            var block = BLOCKS.registerBlock(bloc.id(), constructeur(bloc), properties -> blockProperties(bloc, properties));
            var item = ITEMS.registerSimpleBlockItem(block);
            String serie = serie(bloc.id());
            parSerie.computeIfAbsent(serie, s -> new ArrayList<>()).add(item);
            if (bloc.id().equals(SERIES.get(serie))) {
                icones.put(serie, item);
            }
            if (anime(bloc)) {
                ANIMES.put(bloc.id(), BLOCK_ENTITIES.register(bloc.id(), () -> new BlockEntityType<>(
                        (pos, state) -> new BlocAnimeEntity(ANIMES.get(bloc.id()).get(), pos, state, bloc.id()), Set.of(block.get()))));
            }
        }
        registerTabs(parSerie, icones);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        TABS.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(AtelierObjets::addToCreativeTab);
    }

    /** La série d'un bloc : le début de son id jusqu'au premier {@code _} ({@code puits_margelle} → {@code puits}). */
    static String serie(String id) {
        int fin = id.indexOf('_');
        String prefixe = fin < 0 ? id : id.substring(0, fin);
        return SERIE_DE_PREFIXE.getOrDefault(prefixe, prefixe);
    }

    /** L'onglet créatif d'une série de blocs, s'il existe ({@code taverne}, {@code puits}…). */
    public static java.util.Optional<CreativeModeTab> onglet(String serie) {
        return java.util.Optional.ofNullable(net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getValue(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "blocs_" + serie)));
    }

    /**
     * Un onglet créatif par série de blocs (une par map, plus la taverne), après l'onglet « Monstres », dans
     * l'ordre de {@link #SERIES}. Titre : {@code itemGroup.dungeon_defenders.blocs_<serie>} des fichiers de langue du mod,
     * sinon le nom de la série.
     */
    private static void registerTabs(Map<String, List<Supplier<? extends Item>>> parSerie, Map<String, Supplier<? extends Item>> icones) {
        ResourceKey<CreativeModeTab> precedent = DungeonDefendersMod.MONSTERS_TAB.getKey();
        for (var entry : parSerie.entrySet()) {
            String serie = entry.getKey();
            List<Supplier<? extends Item>> items = entry.getValue();
            if (items.isEmpty()) {
                continue;
            }
            Supplier<? extends Item> icone = icones.getOrDefault(serie, items.getFirst());
            ResourceKey<CreativeModeTab> avant = precedent;
            var tab = TABS.register("blocs_" + serie, () -> CreativeModeTab.builder()
                    .title(Component.translatableWithFallback("itemGroup." + DungeonDefendersMod.MODID + ".blocs_" + serie,
                            Character.toUpperCase(serie.charAt(0)) + serie.substring(1)))
                    .icon(() -> new ItemStack(icone.get()))
                    .withTabsBefore(avant)
                    .displayItems((parameters, output) -> items.forEach(item -> output.accept(item.get())))
                    .build());
            precedent = tab.getKey();
        }
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
