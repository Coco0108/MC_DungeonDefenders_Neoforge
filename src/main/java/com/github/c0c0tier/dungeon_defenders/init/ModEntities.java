package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.entity.ManaCrystalEntity;
import com.github.c0c0tier.dungeon_defenders.entity.TrainingDummyEntity;
import com.github.c0c0tier.dungeon_defenders.entity.WeaponProjectileEntity;
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

    // Mannequin d'entraînement : cible immobile et indestructible des tours, invoquée par
    // TrainingDummyBlock. MobCategory.MISC (comme le cristal de mana) et non MONSTER, pour qu'il
    // reste hors de toute règle de spawn naturel — seul le bloc décide de son existence. La
    // taille est celle d'un zombie, dont il réutilise le modèle (voir TrainingDummyEntity).
    public static final DeferredHolder<EntityType<?>, EntityType<TrainingDummyEntity>> TRAINING_DUMMY =
            ENTITY_TYPES.registerEntityType("training_dummy", TrainingDummyEntity::new, MobCategory.MISC,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(8));

    // Projectile des armes à distance des héros (fiole, rivet, éclat de pierre), voir
    // WeaponProjectileEntity. Taille et suivi d'une boule de neige vanilla.
    public static final DeferredHolder<EntityType<?>, EntityType<WeaponProjectileEntity>> WEAPON_PROJECTILE =
            ENTITY_TYPES.registerEntityType("weapon_projectile", WeaponProjectileEntity::new, MobCategory.MISC,
                    builder -> builder.sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).noSave());

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
