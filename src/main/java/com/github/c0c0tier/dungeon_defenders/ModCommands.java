package com.github.c0c0tier.dungeon_defenders;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import com.github.c0c0tier.dungeon_defenders.atelier.BlocAnimable;
import com.github.c0c0tier.dungeon_defenders.atelier.FicheBloc;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

import java.io.IOException;

// Les commandes du mod.
//
// - /dd_leave : harnais de test qui ramène tout le monde à la taverne. Doublon assumé avec le
//   bouton "Abandonner le niveau" du menu pause (voir PauseMenuClientEvents), qui est le chemin
//   destiné aux joueurs ; celle-ci reste pratique pour déboguer.
// - /dd_export : emballe les maps d'un pack dans un jar prêt à publier (voir MapExporter).
// - /dd_bloc_anim <pos> <role> : fait jouer un rôle au bloc animé visé (forge, mannequin, cristaux, décors dd_*), pour
//   vérifier les animations sans attendre leur déclencheur. Ne fait que l'animation : death ne détruit rien.
@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public class ModCommands {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(MapInstance.RETURN_COMMAND)
                .executes(ModCommands::executeLeave));

        // Niveau 2 : outil de créateur, pas une commande de joueur.
        event.getDispatcher().register(Commands.literal("dd_export")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("namespace", StringArgumentType.word())
                        .executes(ModCommands::executeExport)));

        event.getDispatcher().register(Commands.literal("dd_bloc_anim")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("role", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(FicheBloc.Anime.ROLES, builder))
                                .executes(ModCommands::executeBlocAnim))));
    }

    private static int executeBlocAnim(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        String role = StringArgumentType.getString(context, "role");
        if (!(source.getLevel().getBlockEntity(pos) instanceof BlocAnimable bloc)) {
            source.sendFailure(Component.translatable("dungeon_defenders.bloc_anim.not_animated"));
            return 0;
        }
        if (!bloc.jouer(role)) {
            source.sendFailure(Component.translatable("dungeon_defenders.bloc_anim.no_role", role));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("dungeon_defenders.bloc_anim.played", role), false);
        return 1;
    }

    private static int executeExport(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String namespace = StringArgumentType.getString(context, "namespace");
        try {
            MapExporter.Result result = MapExporter.export(source.getLevel(), namespace);
            source.sendSuccess(() -> Component.translatable(
                    "dungeon_defenders.export.success",
                    result.mapCount(), result.previewCount(), result.jar().toString()), true);
            // Maps sauvegardées avant le passage des ennemis en data-driven : leurs spawners ont été
            // réécrits au nouveau format dans le jar (détail par spawner dans le log serveur).
            if (result.convertedEnemyCount() > 0) {
                source.sendSuccess(() -> Component.translatable(
                        "dungeon_defenders.export.converted", result.convertedEnemyCount()), true);
            }
            return result.mapCount();
        } catch (IllegalArgumentException refused) {
            // Message métier destiné au joueur (namespace invalide, aucune map exportable).
            source.sendFailure(Component.translatable("dungeon_defenders.export." + refused.getMessage(), namespace));
            return 0;
        } catch (IOException failure) {
            LOGGER.warn("Export du pack {} impossible", namespace, failure);
            source.sendFailure(Component.translatable("dungeon_defenders.export.failed", failure.getMessage()));
            return 0;
        }
    }

    private static int executeLeave(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        MapInstance.returnToTavern(level);
        return 1;
    }
}
