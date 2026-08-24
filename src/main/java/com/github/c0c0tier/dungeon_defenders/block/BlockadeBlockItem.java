package com.github.c0c0tier.dungeon_defenders.block;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

// Item d'une tour de la catégorie "Blockade" — décidé avec le joueur : la roue
// (TowerWheelScreen) est désormais l'UNIQUE façon de poser une tour, plus d'item posable à la
// main (ni depuis l'onglet créatif, ni depuis un item récupéré en jeu, ex. un drop). useOn ne
// fait donc plus rien ; l'item reste un simple objet (drop à la casse, inventaire), sans action
// de pose. Base commune à toutes les futures blockades, pas seulement Spike Blockade.
public class BlockadeBlockItem extends BlockItem {

    public BlockadeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }
}
