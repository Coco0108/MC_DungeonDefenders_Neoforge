package com.github.c0c0tier.dungeon_defenders;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// Commande de harnais (voir MapInstance.RETURN_COMMAND) : ramène tous les joueurs à la
// taverne. Utilisée à la fois comme commande tapable directement, et comme lien cliquable dans
// les messages de victoire/défaite (PhaseTransitions). Remplacera plus tard un vrai point de
// sortie posé dans chaque map — voir 05-etat-et-problemes-connus.md.
@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public class ModCommands {

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(MapInstance.RETURN_COMMAND)
                .executes(ModCommands::executeLeave));
    }

    private static int executeLeave(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        MapInstance.returnToTavern(level);
        return 1;
    }
}
