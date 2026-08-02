package com.github.c0c0tier.dungeon_defenders;

import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntityRenderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = DungeonDefendersMod.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = DungeonDefendersMod.MODID, value = Dist.CLIENT)
public class DungeonDefendersModClient {
    public DungeonDefendersModClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // C'est l'événement qui fournit le BlockEntityRendererProvider.Context attendu
        // par le constructeur du renderer.
        event.registerBlockEntityRenderer(
                DungeonDefendersMod.ETERNIA_CRYSTAL_BE.get(),
                EterniaCrystalBlockEntityRenderer::new
        );
    }
}
