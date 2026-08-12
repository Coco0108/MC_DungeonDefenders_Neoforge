package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.menu.SpawnerConfigMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, DungeonDefendersMod.MODID);

    // IMenuTypeExtension.create(...) lit le BlockPos écrit côté serveur (voir
    // SpawnerConfigMenu.SpawnerConfigMenuProvider#writeClientSideData) pour reconstruire le
    // menu côté client.
    public static final DeferredHolder<MenuType<?>, MenuType<SpawnerConfigMenu>> SPAWNER_CONFIG = MENU_TYPES.register(
            "spawner_config",
            () -> IMenuTypeExtension.create(SpawnerConfigMenu::new));

    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}
