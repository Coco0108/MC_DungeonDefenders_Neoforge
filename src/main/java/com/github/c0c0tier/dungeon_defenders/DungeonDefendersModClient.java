package com.github.c0c0tier.dungeon_defenders;

import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntityRenderer;
import com.github.c0c0tier.dungeon_defenders.block.entity.SpawnerBlockEntityRenderer;
import com.github.c0c0tier.dungeon_defenders.block.entity.TowerHealthBarRenderer;
import com.github.c0c0tier.dungeon_defenders.client.ClientDisplayConfig;
import com.github.c0c0tier.dungeon_defenders.client.ModKeyMappings;
import com.github.c0c0tier.dungeon_defenders.client.gui.AbilitySlotsOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.CharacterOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.ExperienceOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.HealthOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.ManaOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.PhaseOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.ScoreGainOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.ScoreOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.WaveEnemiesOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.WaveOverlay;
import com.github.c0c0tier.dungeon_defenders.client.gui.screen.SpawnerConfigScreen;
import com.github.c0c0tier.dungeon_defenders.entity.MobHealthBarRenderer;
import com.github.c0c0tier.dungeon_defenders.init.ModEntities;
import com.github.c0c0tier.dungeon_defenders.init.ModMenus;
import com.github.c0c0tier.dungeon_defenders.init.ScoreSource;
import com.github.c0c0tier.dungeon_defenders.init.SpawnableEnemy;
import com.github.c0c0tier.dungeon_defenders.network.ScoreGainPayload;
import com.google.common.reflect.TypeToken;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.ExperienceOrbRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
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
import net.neoforged.neoforge.network.handling.IPayloadContext;

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

        // Type CLIENT (pas COMMON comme Config.SPEC dans DungeonDefendersMod) : préférences
        // d'affichage HUD, propres à ce joueur, jamais lues côté serveur — voir
        // ClientDisplayConfig pour le pourquoi et le patron pour en ajouter d'autres.
        container.registerConfig(ModConfig.Type.CLIENT, ClientDisplayConfig.SPEC);
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
    // vivante (coût négligeable), lue ensuite par MobHealthBarRenderer (RenderLivingEvent.Post,
    // pas un RenderLayer — voir cette classe pour le pourquoi) via ContextKey, limité là-bas au
    // zombie/squelette, seuls monstres du mod pour l'instant.
    @SubscribeEvent
    static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                (entity, state) -> {
                    state.setRenderData(MobHealthBarRenderer.HEALTH, entity.getHealth());
                    state.setRenderData(MobHealthBarRenderer.MAX_HEALTH, entity.getMaxHealth());
                    state.setRenderData(MobHealthBarRenderer.ENTITY_ID, entity.getId());
                });
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
                Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "score_gain_overlay"),
                ScoreGainOverlay.INSTANCE);
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

        // La barre d'armure n'a pas d'équivalent custom pour l'instant, mais masquée quand
        // même pour rester cohérent avec le reste du HUD vanilla retiré ci-dessous.
        event.replaceLayer(VanillaGuiLayers.ARMOR_LEVEL, HIDDEN);

        // HUD vanilla masqué au profit d'une interface custom (à construire) : faim,
        // expérience et barre d'inventaire. Voir doc/05-etat-et-problemes-connus.md.
        event.replaceLayer(VanillaGuiLayers.FOOD_LEVEL, HIDDEN);
        event.replaceLayer(VanillaGuiLayers.HOTBAR, HIDDEN);

        // EXPERIENCE_LEVEL ne masque que le numéro de niveau : la barre d'XP elle-même (le
        // rectangle vert) est rendue séparément par le "contextual info bar" de cette version
        // (Gui#nextContextualInfoState, ExperienceBarRenderer) — sans ces deux couches en plus,
        // la barre revient dès que le jeu décide d'afficher l'info contextuelle "expérience"
        // (essentiellement tout le temps en survie, hors monture/locator actifs).
        event.replaceLayer(VanillaGuiLayers.EXPERIENCE_LEVEL, HIDDEN);
        event.replaceLayer(VanillaGuiLayers.CONTEXTUAL_INFO_BAR, HIDDEN);
        event.replaceLayer(VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND, HIDDEN);
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SPAWNER_CONFIG.get(), SpawnerConfigScreen::new);
    }

    // Handler du seul paquet clientbound du mod (voir network/ScoreGainPayload.java) : le type
    // est enregistré côté partagé (ModNetworking, chargée des deux côtés), mais le handler
    // lui-même ne peut vivre qu'ici, une classe strictement client — il touche
    // ScoreGainOverlay, jamais chargée sur un serveur dédié.
    @SubscribeEvent
    static void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(ScoreGainPayload.TYPE, DungeonDefendersModClient::handleScoreGain);
    }

    private static void handleScoreGain(ScoreGainPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Pas de borne-check sur les ordinaux ici, contrairement à ModNetworking : ce
            // paquet vient du serveur (autoritaire dans ce mod co-op), pas d'un client — même
            // confiance que les autres ordinaux d'enum synchronisés par attachment (GamePhase,
            // GameDifficulty...), jamais revérifiés côté client non plus. NO_ENEMY (-1) reste un
            // cas à part : ce n'est pas un ordinal invalide, juste "aucun ennemi associé".
            SpawnableEnemy enemy = payload.enemyOrdinal() == ScoreGainPayload.NO_ENEMY
                    ? null
                    : SpawnableEnemy.values()[payload.enemyOrdinal()];
            ScoreGainOverlay.INSTANCE.addPopup(payload.amount(), ScoreSource.values()[payload.sourceOrdinal()], enemy);
        });
    }

    @SubscribeEvent
    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        ModKeyMappings.register(event);
    }

    // Plus de hotbar (HUD masqué, HOTBAR ci-dessus, et à terme un seul item par main plutôt
    // que 9 emplacements) : les touches 1-9 ne doivent plus rien faire — mais uniquement en
    // survie (décidé avec le joueur, 2026-08-26) : un créatif construit des maps et a besoin
    // de sa hotbar/molette normales pour changer d'item. Vidées avant que Minecraft#tick() ne
    // les lise lui-même (Pre se déclenche en tête de tick, avant handleKeybinds()) plutôt que
    // d'essayer de défaire le changement de slot après coup.
    @SubscribeEvent
    static void onClientTickPre(ClientTickEvent.Pre event) {
        if (!isSurvivalPlayer()) {
            return;
        }
        for (KeyMapping hotbarKey : Minecraft.getInstance().options.keyHotbarSlots) {
            while (hotbarKey.consumeClick()) {
                // Volontairement vide : on absorbe le clic sans rien faire.
            }
        }
    }

    // Même raisonnement pour la molette : plus de hotbar à faire défiler, en survie seulement.
    @SubscribeEvent
    static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (isSurvivalPlayer()) {
            event.setCanceled(true);
        }
    }

    private static boolean isSurvivalPlayer() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && !player.isCreative();
    }
}
