package com.github.c0c0tier.dungeon_defenders.client;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.block.TavernCrystalBlock;
import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.EterniaCrystalBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

// Masque le contour noir de sélection (la "hitbox" visible quand on vise un bloc) sur les tours
// et les cristaux, purement pour l'immersion : ces blocs ont des modèles custom rendus par un
// BlockEntityRenderer, et la boîte filaire vanilla — alignée sur getShape, donc 1,5 bloc de haut
// pour une tour et 3 blocs pour le Cristal d'Eternia — flotte visiblement autour du modèle au
// lieu de l'épouser. Demandé en jeu (2026-08-30).
//
// Point important : ceci ne touche QUE le rendu du contour, jamais le ciblage. getShape reste
// inchangé, donc viser, clic droit (prêt sur le Cristal d'Eternia, choix de map sur celui de la
// taverne), casser à la pioche et le mode suppression de tour continuent de fonctionner
// exactement pareil — le bloc reste parfaitement cliquable, il n'est simplement plus souligné.
// C'est la différence avec l'approche "getShape renvoie Shapes.empty()" utilisée par SpawnerBlock
// (voir doc/02-gameplay.md) : là-bas le but était justement de rendre le bloc introuvable en
// survie, ce qui ici casserait toute interaction avec les tours et les cristaux.
//
// Le repérage visuel des tours ne dépend d'ailleurs pas de ce contour : le mode suppression
// dessine son propre contour orange (TowerRemovalClientEvents) et les tours affichent leur barre
// de vie (TowerHealthBarRenderer).
@EventBusSubscriber(modid = DungeonDefendersMod.MODID, value = Dist.CLIENT)
public final class BlockOutlineClientEvents {

    private BlockOutlineClientEvents() {
    }

    // ExtractBlockOutlineRenderStateEvent est annulable : annulé, aucun render state de contour
    // n'est soumis, donc rien n'est dessiné (RenderHighlightEvent des versions précédentes
    // n'existe plus dans cette version de NeoForge).
    @SubscribeEvent
    static void onExtractBlockOutline(ExtractBlockOutlineRenderStateEvent event) {
        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getBlockPos());
        if (hidesOutline(event.getBlockState(), blockEntity)) {
            event.setCanceled(true);
        }
    }

    // Les tours (Blockade comme Turret) et le Cristal d'Eternia se reconnaissent à leur block
    // entity, ce qui couvre automatiquement toute nouvelle tour ajoutée plus tard sans revenir
    // ici. Le cristal de la taverne n'a pas de block entity (aucun état à stocker) : il se
    // reconnaît à sa classe de bloc.
    private static boolean hidesOutline(BlockState state, BlockEntity blockEntity) {
        if (ClientDisplayConfig.SHOW_TOWER_BLOCK_OUTLINE.get()) {
            return false;
        }
        return blockEntity instanceof AbstractTowerBlockEntity
                || blockEntity instanceof EterniaCrystalBlockEntity
                || state.getBlock() instanceof TavernCrystalBlock;
    }
}
