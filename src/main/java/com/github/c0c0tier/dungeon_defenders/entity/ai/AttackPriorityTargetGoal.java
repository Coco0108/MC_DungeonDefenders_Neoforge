package com.github.c0c0tier.dungeon_defenders.entity.ai;

import com.github.c0c0tier.dungeon_defenders.Config;
import com.github.c0c0tier.dungeon_defenders.block.entity.AiAttackTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;

// Remplace AttackBlockadeGoal + AttackEterniaCrystalGoal : un seul goal, générique à toute
// cible implémentant AiAttackTarget (Blockade, Turret, Cristal d'Eternia — et toute future
// catégorie), qui choisit la cible selon les paliers de priorité décidés avec le joueur (voir
// AiAttackTarget) plutôt que d'empiler une classe par palier.
//
// Pas de version à distance : un archer continue d'ignorer Blockade/Turret et de ne viser que
// le cristal (voir RangedAttackEterniaCrystalGoal, inchangé) — seuls les non-AbstractSkeleton
// reçoivent ce goal (ModEvents.onMonsterSpawn).
public class AttackPriorityTargetGoal extends MoveToBlockGoal {

    public static final int TICKS_BETWEEN_HITS = 20;

    // Paliers dans l'ordre croissant de priorité (10 = le plus prioritaire). Un palier plus
    // proche dans cet ordre l'emporte TOUJOURS sur un palier suivant, même si ce dernier a une
    // cible géométriquement plus proche du mob — c'est le point du système à paliers.
    private static final int[] TIERS_ASCENDING = {
            AiAttackTarget.PRIORITY_BLOCK,
            AiAttackTarget.PRIORITY_MELEE_TOWER,
            AiAttackTarget.PRIORITY_CRYSTAL,
            AiAttackTarget.PRIORITY_RANGED_TOWER
    };

    // Même portée que l'ancien AttackBlockadeGoal pour Block/Corps à corps/Tourelle (8 blocs,
    // pas encore externalisée en Config — seule la portée cristal est un vrai doublon entre
    // fichiers, voir Config.SEARCH_RANGE) ; la portée cristal (Config.SEARCH_RANGE) est
    // partagée avec AbstractEterniaCrystalAttackGoal (archers).
    private static final int SEARCH_RANGE_TOWER = 8;
    private static final int VERTICAL_SEARCH_RANGE = 1;

    private static final double SPEED_MODIFIER = 1.2D;
    private static final double ACCEPTED_DISTANCE = 2.1D;

    private int hitCooldown;

    public AttackPriorityTargetGoal(PathfinderMob mob) {
        // searchRange passé au super constructeur n'est jamais utilisé : findNearestBlock()
        // est entièrement réimplémenté ci-dessous (une passe par palier, chacune avec sa
        // propre portée).
        super(mob, SPEED_MODIFIER, Config.SEARCH_RANGE.get());
    }

    @Override
    protected boolean findNearestBlock() {
        for (int tier : TIERS_ASCENDING) {
            BlockPos found = findNearestMatch(searchRangeForTier(tier), tier);
            if (found != null) {
                this.blockPos = found;
                return true;
            }
        }
        return false;
    }

    private int searchRangeForTier(int tier) {
        return tier == AiAttackTarget.PRIORITY_CRYSTAL ? Config.SEARCH_RANGE.get() : SEARCH_RANGE_TOWER;
    }

    // Même algorithme en spirale que MoveToBlockGoal.findNearestBlock() (vanilla) : rejoué une
    // fois par palier, puisque vanilla ne l'expose pas comme méthode réutilisable/paramétrable.
    private BlockPos findNearestMatch(int horizontalSearch, int tier) {
        BlockPos mobPos = this.mob.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = 0; y <= VERTICAL_SEARCH_RANGE; y = y > 0 ? -y : 1 - y) {
            for (int r = 0; r < horizontalSearch; r++) {
                for (int x = 0; x <= r; x = x > 0 ? -x : 1 - x) {
                    for (int z = x < r && x > -r ? r : 0; z <= r; z = z > 0 ? -z : 1 - z) {
                        pos.setWithOffset(mobPos, x, y - 1, z);
                        if (this.mob.isWithinHome(pos) && matchesTier(pos, tier)) {
                            return pos.immutable();
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean matchesTier(BlockPos pos, int tier) {
        return this.mob.level().getBlockEntity(pos) instanceof AiAttackTarget target
                && target.getAiPriority() == tier;
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        // Appelé seulement par canContinueToUse(), pour vérifier que la cible déjà retenue
        // existe toujours — peu importe son palier exact : si elle a changé de nature entre
        // temps, le prochain canUse() referait une recherche complète par paliers de toute
        // façon.
        return level.getBlockEntity(pos) instanceof AiAttackTarget;
    }

    @Override
    protected BlockPos getMoveToTarget() {
        // Vise la position elle-même (pas .above()) : la cible est un bloc plein qu'on attaque
        // depuis le sol adjacent, jamais quelque chose sur lequel grimper.
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

        if (!(this.mob.level().getBlockEntity(this.blockPos) instanceof AiAttackTarget target)) {
            return;
        }

        target.damage(Config.DAMAGE_PER_HIT.get());
        this.mob.swing(InteractionHand.MAIN_HAND);
        this.hitCooldown = TICKS_BETWEEN_HITS;
    }
}
