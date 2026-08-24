package com.github.c0c0tier.dungeon_defenders.entity.ai;

import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractBlockadeBlockEntity;
import com.github.c0c0tier.dungeon_defenders.init.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;

// Détourne un ennemi de mêlée vers n'importe quel bloc de la catégorie "Blockade" (tag
// dungeon_defenders:blockades — voir ModBlockTags) plutôt que le Cristal d'Eternia —
// "premier rempart" : tant qu'un blockade lui bloque le passage à courte portée, il doit le
// détruire avant de pouvoir continuer. Enregistré à une priorité plus haute (numéro plus
// petit) qu'AttackEterniaCrystalGoal dans ModEvents.onMonsterSpawn, donc préféré quand les deux
// sont utilisables ; une fois le blockade détruit, ce goal ne trouve plus de cible et le mob
// retombe sur le goal du cristal. Le ciblage par tag (plutôt qu'un bloc précis en dur) permet
// à toute future blockade (Bouncer, Slice N Dice, etc.) d'hériter de ce comportement sans
// toucher au goal — il suffit de l'ajouter au tag.
//
// Pas de version à distance pour l'instant (voir doc/02-gameplay.md) : un archer peut tirer
// par-dessus/à côté d'un blockade sans avoir besoin de le détruire, contrairement à un ennemi
// de mêlée qui doit littéralement passer au travers.
//
// N'étend pas AbstractEterniaCrystalAttackGoal : la structure (convergence + cooldown de coups)
// se ressemble, mais ce goal a une responsabilité en plus que celui du cristal n'a pas
// (viser un tag de bloc plutôt qu'un cristal unique sur la carte) — voir le commentaire de
// AbstractEterniaCrystalAttackGoal : forcer une base commune maintenant, avant d'avoir une
// deuxième catégorie de tour au comportement de convergence comparable, serait deviner une
// forme partagée plutôt que la constater.
public class AttackBlockadeGoal extends MoveToBlockGoal {

    public static final int DAMAGE_PER_HIT = 5;
    public static final int TICKS_BETWEEN_HITS = 20;
    // Plus court que la portée de détection du cristal (16) : ne détourne l'ennemi que si un
    // blockade est vraiment sur son chemin, pas n'importe où sur la carte.
    private static final int SEARCH_RANGE = 8;
    private static final double SPEED_MODIFIER = 1.2D;
    private static final double ACCEPTED_DISTANCE = 2.1D;

    private int hitCooldown;

    public AttackBlockadeGoal(PathfinderMob mob) {
        super(mob, SPEED_MODIFIER, SEARCH_RANGE);
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlockTags.BLOCKADES);
    }

    @Override
    protected BlockPos getMoveToTarget() {
        return this.blockPos;
    }

    @Override
    public double acceptedDistance() {
        return ACCEPTED_DISTANCE;
    }

    @Override
    public void start() {
        super.start();
        this.hitCooldown = 0;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.isReachedTarget()) {
            this.hitCooldown = 0;
            return;
        }

        if (this.hitCooldown > 0) {
            this.hitCooldown--;
            return;
        }

        if (!(this.mob.level().getBlockEntity(this.blockPos) instanceof AbstractBlockadeBlockEntity blockade)) {
            return;
        }

        blockade.damage(DAMAGE_PER_HIT);
        this.mob.swing(InteractionHand.MAIN_HAND);
        this.hitCooldown = TICKS_BETWEEN_HITS;
    }
}
