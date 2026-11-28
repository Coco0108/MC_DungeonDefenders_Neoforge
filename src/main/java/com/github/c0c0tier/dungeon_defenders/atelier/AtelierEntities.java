package com.github.c0c0tier.dungeon_defenders.atelier;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un type d'entité par fiche de monstre chargée, {@code dungeon_defenders:<id>}, et son œuf d'apparition
 * {@code dungeon_defenders:<id>_spawn_egg} (onglets « Œufs d'apparition » et « Monstres »). Repris de
 * {@code entity/AtelierEntities.java} et {@code entity/AtelierItems.java} de l'Atelier, sans apparition naturelle ni
 * projectiles.
 *
 * <p>L'apparence de l'œuf (définition d'objet {@code items/<id>_spawn_egg.json}) et son nom sont des fichiers que
 * l'Atelier fabrique déjà pour lui-même : ils doivent arriver avec le monstre (voir doc/ATELIER.md).
 */
public final class AtelierEntities {
    private static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(DungeonDefendersMod.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DungeonDefendersMod.MODID);
    private static final Map<String, DeferredHolder<EntityType<?>, EntityType<AtelierMonster>>> TYPES = new LinkedHashMap<>();
    private static final Map<String, DeferredItem<SpawnEggItem>> EGGS = new LinkedHashMap<>();

    private AtelierEntities() {
    }

    /** Après {@link AtelierLoader#load}, à la construction du mod. */
    public static void register(IEventBus modEventBus) {
        for (FicheMonstre fiche : Atelier.monstres().values()) {
            // MONSTER : c'est la catégorie des ennemis de Dungeon Defenders ; ils n'apparaissent jamais naturellement
            // (aucun placement de spawn n'est enregistré), seulement par les spawners des maps, un œuf ou /summon.
            DeferredHolder<EntityType<?>, EntityType<AtelierMonster>> type = ENTITIES.registerEntityType(fiche.id(),
                    AtelierMonster::new, MobCategory.MONSTER,
                    builder -> builder.sized(fiche.hitbox().largeur(), fiche.hitbox().hauteur())
                            .eyeHeight(fiche.hitbox().hauteur() * 0.85F)
                            .clientTrackingRange(10));
            TYPES.put(fiche.id(), type);
            EGGS.put(fiche.id(), ITEMS.registerItem(fiche.id() + "_spawn_egg", SpawnEggItem::new,
                    properties -> properties.spawnEgg(type.get())));
        }
        ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(AtelierEntities::createAttributes);
        modEventBus.addListener(AtelierEntities::addToCreativeTabs);
    }

    public static Map<String, DeferredHolder<EntityType<?>, EntityType<AtelierMonster>>> types() {
        return TYPES;
    }

    private static void createAttributes(EntityAttributeCreationEvent event) {
        TYPES.forEach((id, type) -> event.put(type.get(), AtelierMonster.createAttributes(Atelier.monstre(id)).build()));
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS || event.getTab() == DungeonDefendersMod.MONSTERS_TAB.get()) {
            EGGS.values().forEach(event::accept);
        }
    }
}
