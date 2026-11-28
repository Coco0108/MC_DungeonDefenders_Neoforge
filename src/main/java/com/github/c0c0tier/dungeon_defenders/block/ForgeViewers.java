package com.github.c0c0tier.dungeon_defenders.block;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.entity.ForgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Qui a l'écran de quelle Forge ouvert, côté serveur, pour son animation : {@code open} au premier joueur qui l'ouvre,
 * {@code close} quand le dernier le ferme. L'écran est un simple {@code Screen} client (pas un menu) : le serveur
 * l'apprend à l'ouverture ({@link ForgeBlock#useWithoutItem}) et à la fermeture ({@code ForgeActionPayload.CLOSE},
 * envoyé par {@code ForgeScreen#removed}). Un joueur qui se déconnecte ou change de dimension la referme aussi.
 */
@EventBusSubscriber(modid = DungeonDefendersMod.MODID)
public final class ForgeViewers {
    private record Forge(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private static final Map<UUID, Forge> OUVERTES = new ConcurrentHashMap<>();

    private ForgeViewers() {
    }

    /** {@code player} ouvre l'écran de la Forge en {@code pos} : elle s'ouvre si personne ne l'avait ouverte. */
    public static void open(ServerPlayer player, BlockPos pos) {
        open(player.getUUID(), player.level(), pos);
    }

    /** {@code player} ferme son écran de Forge : elle se referme si c'était le dernier. */
    public static void close(ServerPlayer player) {
        close(player.getUUID(), player.level().getServer());
    }

    /** Comme {@link #open(ServerPlayer, BlockPos)}, par l'UUID du joueur (gametests). */
    public static void open(UUID player, ServerLevel level, BlockPos pos) {
        close(player, level.getServer());
        Forge forge = new Forge(level.dimension(), pos.immutable());
        boolean premier = !OUVERTES.containsValue(forge);
        OUVERTES.put(player, forge);
        if (premier && level.getBlockEntity(pos) instanceof ForgeBlockEntity bloc) {
            bloc.jouer("open");
        }
    }

    /** Comme {@link #close(ServerPlayer)}, par l'UUID du joueur (gametests). */
    public static void close(UUID player, MinecraftServer server) {
        Forge forge = OUVERTES.remove(player);
        if (forge == null || OUVERTES.containsValue(forge)) {
            return;
        }
        ServerLevel level = server.getLevel(forge.dimension());
        if (level != null && level.isLoaded(forge.pos()) && level.getBlockEntity(forge.pos()) instanceof ForgeBlockEntity bloc) {
            bloc.jouer("close");
        }
    }

    /** Le nombre de joueurs qui ont l'écran de cette Forge ouvert (gametests). */
    public static long viewers(Level level, BlockPos pos) {
        Forge forge = new Forge(level.dimension(), pos);
        return OUVERTES.values().stream().filter(forge::equals).count();
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            close(player);
        }
    }

    @SubscribeEvent
    static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            close(player);
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        OUVERTES.clear();
    }
}
