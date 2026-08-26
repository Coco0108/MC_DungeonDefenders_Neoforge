package com.github.c0c0tier.dungeon_defenders;

import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntityRenderer;
import com.github.c0c0tier.dungeon_defenders.block.entity.SpawnerBlockEntityRenderer;
import com.github.c0c0tier.dungeon_defenders.block.entity.TowerHealthBarRenderer;
import com.github.c0c0tier.dungeon_defenders.client.ModKeyMappings;
import com.github.c0c0tier.dungeon_defenders.client.gui.AbilitySlotsOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.CharacterOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.ExperienceOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.HealthOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.ManaOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.PhaseOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.ScoreOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.WaveEnemiesOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.WaveOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.screen.GameOverScreen;
import com.github.c0c0tier.dungeon_defenders.client.gui.screen.ManaChestConfigScreen;
import com.github.c0c0tier.dungeon_defenders.client.gui.screen.SpawnerConfigScreen;
import com.github.c0c0tier.dungeon_defenders.entity.MobHealthBarLayer;
import com.github.c0c0tier.dungeon_defenders.init.ModEntities;
import com.github.c0c0tier.dungeon_defenders.init.ModMenus;
import com.github.c0c0tier.dungeon_defenders.network.GameOverPayload;
import com.google.common.reflect.TypeToken;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.ExperienceOrbRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = DungeonDefendersMod.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = DungeonDefendersMod.MODID, value = Dist.CLIENT)
public class DungeonDefendersModClient {
    // Couche vide réutilisée pour masquer des éléments du HUD vanilla : pas de contenu, une
    // seule instance suffit pour tous.
    private static final GuiLayer HIDDEN = (guiGraphics, deltaTracker) -> {};

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
        event.registerBlockEntityRenderer(
                DungeonDefendersMod.SPAWNER_BE.get(),
                SpawnerBlockEntityRenderer::new
        );
        // TowerHealthBarRenderer est générique sur AbstractTowerBlockEntity : un seul renderer
        // pour toute catégorie de tour, enregistré une fois par BlockEntityType concret (voir
        // TowerHealthBarRenderer, doc/05-etat-et-problemes-connus.md).
        event.registerBlockEntityRenderer(
                DungeonDefendersMod.SPIKE_BLOCKADE_BE.get(),
                TowerHealthBarRenderer::new
        );
        event.registerBlockEntityRenderer(
                DungeonDefendersMod.HARPOON_TURRET_BE.get(),
                TowerHealthBarRenderer::new
        );
        // Réutilise tel quel le renderer vanilla de l'orbe d'XP (pas final, paramétré sur
        // ExperienceOrb) : le cristal de mana aura donc l'air d'une orbe d'XP verte/jaune, pas
        // encore de couleur "mana" dédiée — voir doc/05-etat-et-problemes-connus.md.
        event.registerEntityRenderer(
                ModEntities.MANA_CRYSTAL.get(),
                ExperienceOrbRenderer::new
        );
    }

    // La vie n'existe pas nativement sur un EntityRenderState vanilla (voir
    // doc/05-etat-et-problemes-connus.md) : ce modificateur l'y ajoute pour toute entité
    // vivante (coût négligeable), lue ensuite par MobHealthBarLayer via ContextKey — la
    // couche elle-même n'est branchée que sur zombie/squelette (voir onAddLayers ci-dessous),
    // seuls monstres du mod pour l'instant.
    @SubscribeEvent
    static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                (entity, state) -> {
                    state.setRenderData(MobHealthBarLayer.HEALTH, entity.getHealth());
                    state.setRenderData(MobHealthBarLayer.MAX_HEALTH, entity.getMaxHealth());
                    state.setRenderData(MobHealthBarLayer.ENTITY_ID, entity.getId());
                });
    }

    @SubscribeEvent
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        LivingEntityRenderer<Zombie, ZombieRenderState, ZombieModel<ZombieRenderState>> zombieRenderer =
                event.getRenderer(EntityType.ZOMBIE);
        if (zombieRenderer != null) {
            zombieRenderer.addLayer(new MobHealthBarLayer<>(zombieRenderer));
        }

        LivingEntityRenderer<Skeleton, SkeletonRenderState, SkeletonModel<SkeletonRenderState>> skeletonRenderer =
                event.getRenderer(EntityType.SKELETON);
        if (skeletonRenderer != null) {
            skeletonRenderer.addLayer(new MobHealthBarLayer<>(skeletonRenderer));
        }
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "mana_overlay"),
                new ManaOverlay());
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "health_overlay"),
                new HealthOverlay());
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "experience_overlay"),
                new ExperienceOverlay());
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "wave_overlay"),
                new WaveOverlay());
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "wave_enemies_overlay"),
                new WaveEnemiesOverlay());
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "phase_overlay"),
                new PhaseOverlay());
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "score_overlay"),
                new ScoreOverlay());
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "character_overlay"),
                new CharacterOverlay());
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "ability_slots_overlay"),
                new AbilitySlotsOverlay());

        // Les cœurs vanilla ne sont pas prévus pour 100 PV (ils s'étalent sur plusieurs
        // rangées) et feraient doublon avec HealthOverlay : on les masque plutôt que de
        // les laisser déborder.
        event.replaceLayer(VanillaGuiLayers.PLAYER_HEALTH, HIDDEN);

        // HUD vanilla masqué au profit d'une interface custom (à construire) : faim,
        // expérience et barre d'inventaire. Voir doc/05-etat-et-problemes-connus.md.
        event.replaceLayer(VanillaGuiLayers.FOOD_LEVEL, HIDDEN);
        event.replaceLayer(VanillaGuiLayers.EXPERIENCE_LEVEL, HIDDEN);
        event.replaceLayer(VanillaGuiLayers.HOTBAR, HIDDEN);
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SPAWNER_CONFIG.get(), SpawnerConfigScreen::new);
        event.register(ModMenus.MANA_CHEST_CONFIG.get(), ManaChestConfigScreen::new);
    }

    @SubscribeEvent
    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        ModKeyMappings.register(event);
    }

    // Plus de hotbar (HUD masqué, HOTBAR ci-dessus, et à terme un seul item par main plutôt
    // que 9 emplacements) : les touches 1-9 ne doivent plus rien faire. Vidées avant que
    // Minecraft#tick() ne les lise lui-même (Pre se déclenche en tête de tick, avant
    // handleKeybinds()) plutôt que d'essayer de défaire le changement de slot après coup.
    @SubscribeEvent
    static void onClientTickPre(ClientTickEvent.Pre event) {
        for (KeyMapping hotbarKey : Minecraft.getInstance().options.keyHotbarSlots) {
            while (hotbarKey.consumeClick()) {
                // Volontairement vide : on absorbe le clic sans rien faire.
            }
        }
    }

    // Même raisonnement pour la molette : plus de hotbar à faire défiler.
    @SubscribeEvent
    static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        event.setCanceled(true);
    }

    // Handler du seul paquet clientbound du mod (voir ModNetworking, qui n'enregistre que le
    // TYPE/STREAM_CODEC de GameOverPayload, sans handler) : ouvre GameOverScreen sur le thread
    // principal. Vit ici (classe client-only) plutôt que dans ModNetworking pour ne jamais
    // charger Minecraft/Screen sur un serveur dédié.
    @SubscribeEvent
    static void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(GameOverPayload.TYPE, (payload, context) ->
                context.enqueueWork(() -> Minecraft.getInstance().setScreen(new GameOverScreen(payload.victory()))));
    }
}
