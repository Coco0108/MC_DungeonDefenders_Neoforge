package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.entity.ManaCrystalEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {

    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(DungeonDefendersMod.MODID);

    // Cristal de mana : drop des monstres (voir ModEvents.onMonsterDeath), ramassage direct
    // (pas un item d'inventaire), voir ManaCrystalEntity pour le détail.
    public static final DeferredHolder<EntityType<?>, EntityType<ManaCrystalEntity>> MANA_CRYSTAL =
            ENTITY_TYPES.registerEntityType("mana_crystal", ManaCrystalEntity::new, MobCategory.MISC,
                    builder -> builder.sized(0.5F, 0.5F).clientTrackingRange(6));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
