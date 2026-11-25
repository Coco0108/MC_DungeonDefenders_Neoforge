package com.github.c0c0tier.dungeon_defenders.entity.ai;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

// Comble le trou que AttackPriorityTargetGoal/RangedAttackEterniaCrystalGoal laissent : ces
// deux-là (des MoveToBlockGoal) ne s'activent QUE si une cible existe déjà dans un rayon local
// court (Config.SEARCH_RANGE pour le cristal, 8 blocs fixes pour les tours) — tant qu'un
// monstre spawné loin n'est pas par hasard entré dans ce rayon, il n'a aucune raison de s'en
// approcher et retombe sur l'errance aléatoire vanilla. Sur une grande map, ça peut ne jamais
// arriver.
//
// Ce Goal-ci n'a pas cette limite : il navigue en ligne directe vers ModAttachments.CRYSTAL_POS
// (la position réelle du cristal, mémorisée une fois pour toutes par EterniaCrystalBlockEntity
// plutôt que re-recherchée) via le vrai pathfinder Minecraft (contourne les murs/obstacles),
// quelle que soit la distance — voir ModEvents.onMonsterSpawn pour le relèvement de l'attribut
// FOLLOW_RANGE qui l'accompagne, sans quoi le pathfinder vanilla plafonnerait la recherche de
// chemin bien avant d'atteindre le cristal sur une grande map.
//
// Priorité toujours AU-DESSUS des goals de paliers (AttackPriorityTargetGoal/
// RangedAttackEterniaCrystalGoal, ajoutés à une priorité plus basse dans ModEvents) : les deux
// partagent Flag.MOVE, donc dès qu'un palier trouve une cible à sa portée locale (un
// Bloc/Corps-à-corps si le monstre passe à moins de 8 blocs d'une tour, par exemple), il reprend
// la main sur le déplacement — puis, une fois cette cible détruite/hors de portée, ce Goal-ci
// redevient le seul éligible et reprend naturellement la direction du cristal. C'est exactement
// le comportement "checkpoint" demandé par le joueur (2026-09-12), obtenu gratuitement par la
// hiérarchie des Goals plutôt qu'un vrai système de points de passage.
public class SeekEterniaCrystalGoal extends Goal {

    private static final double SPEED_MODIFIER = 1.0D;

    // Le monstre n'atteint jamais littéralement la position du cristal (bloc plein, la
    // navigation s'arrête juste avant) : une fois la navigation "terminée" (arrivée ou chemin
    // impossible à calculer), retenter tout de suite en boucle serait un travail de pathfinding
    // gratuit chaque tick pour rien — ce délai borne le coût au pire cas.
    private static final int RETRY_INTERVAL_TICKS = 40;

    private final PathfinderMob mob;
    private int retryCooldown;

    public SeekEterniaCrystalGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private @Nullable BlockPos crystalPos() {
        return this.mob.level().getData(ModAttachments.CRYSTAL_POS);
    }

    @Override
    public boolean canUse() {
        return crystalPos() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        this.retryCooldown = 0;
        moveToCrystal();
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!this.mob.getNavigation().isDone()) {
            return;
        }
        if (this.retryCooldown > 0) {
            this.retryCooldown--;
            return;
        }
        this.retryCooldown = RETRY_INTERVAL_TICKS;
        moveToCrystal();
    }

    private void moveToCrystal() {
        BlockPos pos = crystalPos();
        if (pos != null) {
            this.mob.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, SPEED_MODIFIER);
        }
    }
}
