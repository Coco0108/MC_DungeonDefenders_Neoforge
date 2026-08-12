package com.github.c0c0tier.dungeon_defenders;

import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.SpawnerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.ModBlocks;
import com.github.c0c0tier.dungeon_defenders.init.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(DungeonDefendersMod.MODID)
public class DungeonDefendersMod {
    public static final String MODID = "dungeon_defenders";
    private static final Logger LOGGER = LogUtils.getLogger();

    // 1. Déclarations des Registres (On garde uniquement ce qui n'est pas dans ModBlocks)
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // 2. Le Block Entity (Il fait référence au bloc stocké dans ModBlocks pour casser la boucle)
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EterniaCrystalBlockEntity>> ETERNIA_CRYSTAL_BE =
            BLOCK_ENTITIES.register("eternia_crystal", () -> new BlockEntityType<>(
                    EterniaCrystalBlockEntity::new,
                    ModBlocks.ETERNIA_CRYSTAL.get()
            ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpawnerBlockEntity>> SPAWNER_BE =
            BLOCK_ENTITIES.register("spawner", () -> new BlockEntityType<>(
                    SpawnerBlockEntity::new,
                    ModBlocks.SPAWNER.get()
            ));

    // 3. L'onglet Créatif
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DUNGEON_DEFENDERS_TAB = CREATIVE_MODE_TABS.register("dungeon_defenders_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.dungeon_defenders"))
                    .icon(() -> ModBlocks.ETERNIA_CRYSTAL_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.ETERNIA_CRYSTAL_ITEM.get());
                        output.accept(ModBlocks.SPIKE_TRAP_ITEM.get());
                        output.accept(ModBlocks.MANA_TEST_WAND.get());
                        output.accept(ModBlocks.SPAWNER_ITEM.get());
                    })
                    .build());

    public DungeonDefendersMod(IEventBus modEventBus) {
        // On enregistre tes deux fichiers de registres sur le bus
        ModBlocks.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModMenus.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        LOGGER.info("Le mod Dungeon Defenders est initialisé proprement !");
    }
}