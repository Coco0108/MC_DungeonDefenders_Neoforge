package com.github.c0c0tier.dungeon_defenders.block;

import com.github.c0c0tier.dungeon_defenders.init.MapDefinition;
import com.github.c0c0tier.dungeon_defenders.block.entity.TavernCrystalBlockEntity;
import com.github.c0c0tier.dungeon_defenders.atelier.AnimationsBloc;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.MapPreviews;
import com.github.c0c0tier.dungeon_defenders.init.MapRegistry;
import com.github.c0c0tier.dungeon_defenders.network.MapPreviewPayload;
import com.github.c0c0tier.dungeon_defenders.network.OpenMapSelectionPayload;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
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
//
// Depuis le 2026-11-28, un block entity (TavernCrystalBlockEntity) porte seulement son modèle animé dd_cristal_v2.
public class TavernCrystalBlock extends BaseEntityBlock {

    public static final MapCodec<TavernCrystalBlock> CODEC = simpleCodec(TavernCrystalBlock::new);

    // Modèle et forme du bloc de décor dd_cristal_v2 de l'Atelier (2026-11-27).
    private static final FormeAtelier FORME = new FormeAtelier("dd_cristal_v2");

    public TavernCrystalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FORME.get(Direction.NORTH);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TavernCrystalBlockEntity(pos, state);
    }

    // Dessiné par GeckoLib quand l'Atelier a déployé le modèle animé de dd_cristal_v2, sinon par son modèle fixe.
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return AnimationsBloc.dessineParGeckoLib("dd_cristal_v2") ? RenderShape.INVISIBLE : RenderShape.MODEL;
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
