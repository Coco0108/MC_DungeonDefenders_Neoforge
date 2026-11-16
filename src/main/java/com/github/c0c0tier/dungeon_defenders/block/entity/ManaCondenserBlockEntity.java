package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

// "Condenseur de mana" (Alchimiste) : tour d'économie. Un monstre qui meurt dans son rayon lâche
// un cristal de mana en plus (ModEvents#onMonsterDeath). Elle n'attaque pas et ne ralentit
// rien : c'est un pari, du mana dépensé maintenant pour en gagner plus tard, à poser là où les
// monstres meurent vraiment. Les condenseurs ne se cumulent pas : un monstre ne compte que pour
// le meilleur de ceux qui le couvrent.
//
// La mort d'un monstre n'a aucun moyen bon marché de retrouver les condenseurs alentour ; ils
// s'inscrivent donc eux-mêmes dans une table par monde quand ils reçoivent leur Level, et s'en
// retirent à leur suppression (WeakHashMap et ensemble faible : un monde déchargé s'oublie seul).
public class ManaCondenserBlockEntity extends AbstractTowerBlockEntity {

    private static final Map<Level, Set<ManaCondenserBlockEntity>> ACTIVE = new WeakHashMap<>();

    public ManaCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonDefendersMod.MANA_CONDENSER_BE.get(), pos, state, TowerDefinition.MANA_CONDENSER);
    }

    @Override
    public int getAiPriority() {
        return AiAttackTarget.PRIORITY_RANGED_TOWER;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (!level.isClientSide()) {
            ACTIVE.computeIfAbsent(level, key -> Collections.newSetFromMap(new WeakHashMap<>())).add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null) {
            Set<ManaCondenserBlockEntity> condensers = ACTIVE.get(this.level);
            if (condensers != null) {
                condensers.remove(this);
            }
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (this.level != null && !this.level.isClientSide()) {
            ACTIVE.computeIfAbsent(this.level, key -> Collections.newSetFromMap(new WeakHashMap<>())).add(this);
        }
    }

    /**
     * Le mana en plus que doit lâcher un monstre mort à {@code position} : celui du meilleur
     * condenseur qui le couvre, 0 s'il n'y en a aucun.
     */
    public static int bonusManaAt(Level level, Vec3 position) {
        Set<ManaCondenserBlockEntity> condensers = ACTIVE.get(level);
        if (condensers == null) {
            return 0;
        }
        int best = 0;
        for (ManaCondenserBlockEntity condenser : condensers) {
            if (condenser.isRemoved()) {
                continue;
            }
            double range = condenser.stats().get(TowerStat.RANGE);
            Vec3 center = Vec3.atCenterOf(condenser.worldPosition);
            double dx = position.x - center.x;
            double dz = position.z - center.z;
            if (dx * dx + dz * dz <= range * range && Math.abs(position.y - center.y) <= range) {
                // Le bonus monte avec le niveau d'amélioration, comme les dégâts d'une tour offensive.
                best = Math.max(best, (int) Math.round(
                        condenser.stats().intValue(TowerStat.BONUS_MANA) * condenser.levelMultiplier()));
            }
        }
        return best;
    }
}
