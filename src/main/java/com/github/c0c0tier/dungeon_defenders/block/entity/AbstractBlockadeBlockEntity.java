package com.github.c0c0tier.dungeon_defenders.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.WeakHashMap;

// Catégorie de tours "Blockade" (voir doc/05-etat-et-problemes-connus.md, section Système de
// tours) : un mur qui bloque le passage gratuitement (un bloc plein bloque déjà naturellement)
// et que les monstres doivent détruire pour continuer (voir
// entity/ai/AttackPriorityTargetGoal.java). Regroupe ce que la première version de la
// taxonomie séparait en "block passif" et "corps à corps" : un blockade passif est juste un
// sous-type avec dealsContactDamage=false. PV/coût mana/persistance/sync viennent de
// AbstractTowerBlockEntity, commun à toutes les catégories de tours.
//
// dealsContactDamage détermine aussi la priorité IA (voir AiAttackTarget) : "block" pur
// (false) est priorité 10, "corps à corps" (true, le cas de Spike Blockade) est priorité 20 —
// aucun nouveau champ, décidé avec le joueur pour réutiliser ce qui existait déjà.
public abstract class AbstractBlockadeBlockEntity extends AbstractTowerBlockEntity {

    private final boolean dealsContactDamage;
    private final float contactDamage;
    private final long contactDamageIntervalTicks;
    private final double contactRange;

    // WeakHashMap : ne retient pas les entités mortes/déchargées, évite une fuite mémoire.
    private final Map<Monster, Long> lastContactDamageTick = new WeakHashMap<>();

    protected AbstractBlockadeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            int maxHealth, int manaCost, boolean dealsContactDamage,
            float contactDamage, long contactDamageIntervalTicks, double contactRange) {
        super(type, pos, state, maxHealth, manaCost);
        this.dealsContactDamage = dealsContactDamage;
        this.contactDamage = contactDamage;
        this.contactDamageIntervalTicks = contactDamageIntervalTicks;
        this.contactRange = contactRange;
    }

    @Override
    public int getAiPriority() {
        return this.dealsContactDamage ? AiAttackTarget.PRIORITY_MELEE_TOWER : AiAttackTarget.PRIORITY_BLOCK;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractBlockadeBlockEntity blockEntity) {
        if (!blockEntity.dealsContactDamage) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        AABB contactArea = new AABB(pos).inflate(blockEntity.contactRange);
        long now = serverLevel.getGameTime();

        for (Monster monster : serverLevel.getEntitiesOfClass(Monster.class, contactArea)) {
            Long lastTrigger = blockEntity.lastContactDamageTick.get(monster);
            if (lastTrigger != null && now - lastTrigger < blockEntity.contactDamageIntervalTicks) {
                continue;
            }

            blockEntity.lastContactDamageTick.put(monster, now);
            monster.hurt(serverLevel.damageSources().stalagmite(), blockEntity.contactDamage);
        }
    }
}
