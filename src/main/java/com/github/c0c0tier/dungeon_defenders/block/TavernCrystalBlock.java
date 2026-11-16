package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.init.MapDefinition;
import com.github.c0c0tier.dungeon_defenders.init.MapPreviews;
import com.github.c0c0tier.dungeon_defenders.init.MapRegistry;
import com.github.c0c0tier.dungeon_defenders.network.MapPreviewPayload;
import com.github.c0c0tier.dungeon_defenders.network.OpenMapSelectionPayload;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// Le cristal de la taverne : contrairement à EterniaCrystalBlock (celui des maps, avec des PV,
// visé par les ennemis), celui-ci n'a aucune mécanique de combat — un simple bloc dont le clic
// droit ouvre l'écran de choix de map (voir MapSelectionScreen). Pas de PV, pas de block
// entity, rien à synchroniser : contrairement au spawner, cet écran n'a besoin d'aucune donnée
// propre à CE bloc (la liste des maps est statique, la difficulté vient d'un attachment de
// Level déjà synchronisé) — inutile de passer par le système de Menu/MenuProvider.
//
// L'ouverture passe quand même par un paquet (OpenMapSelectionPayload) et non par un appel
// direct à Minecraft.getInstance().setScreen(...) dans la branche cliente : cette classe est
// chargée par ModBlocks au démarrage du mod, y compris sur un serveur dédié, où toute mention
// d'une classe cliente fait planter le chargement — voir OpenMapSelectionPayload pour le
// détail du crash constaté (2026-08-30).
public class TavernCrystalBlock extends Block {

    public static final MapCodec<TavernCrystalBlock> CODEC = simpleCodec(TavernCrystalBlock::new);

    public TavernCrystalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // La découverte des maps ne peut se faire que côté serveur (le gestionnaire de
        // structures n'existe pas côté client) : la liste part donc avec le paquet qui ouvre
        // l'écran, plutôt que d'être reconstruite en face.
        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            List<MapDefinition> maps = MapRegistry.discover(serverLevel);
            // Les aperçus capturés en jeu partent AVANT le paquet qui ouvre l'écran, pour qu'ils
            // soient déjà chargés à son premier affichage. Seules les maps de la sauvegarde en
            // ont : celles d'un jar embarquent le leur comme une ressource ordinaire.
            for (MapDefinition map : maps) {
                if (map.fromWorld()) {
                    MapPreviews.load(serverLevel.getServer(), map.structureId()).ifPresent(png ->
                            serverPlayer.connection.send(new MapPreviewPayload(map.structureId(), png).toVanillaClientbound()));
                }
            }
            serverPlayer.connection.send(new OpenMapSelectionPayload(maps).toVanillaClientbound());
        }
        return InteractionResult.SUCCESS;
    }
}
