package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.init.TowerDefinition;
import com.github.c0c0tier.dungeon_defenders.init.TowerStat;
import com.github.c0c0tier.dungeon_defenders.init.TowerStats;
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
//
// knockbackStrength (Bouncer Blockade, 2026-08-29) : 0 = aucun effet (Spike Blockade, Slice N
// Dice), > 0 = repousse en plus des dégâts, dans le même tick et sur les mêmes monstres que
// contactDamage (même AABB, même intervalle) — décidé avec le joueur : "fait aussi des dégâts",
// pas un mode exclusif l'un de l'autre.
public abstract class AbstractBlockadeBlockEntity extends AbstractTowerBlockEntity {

    // Comportement, pas un chiffre : reste décidé par le code de chaque tour (il fixe aussi la
    // priorité IA). Les dégâts, leur cadence, leur rayon et la repousse viennent des stats
    // data-driven de la tour (voir AbstractTowerBlockEntity#stats).
    private final boolean dealsContactDamage;

    // WeakHashMap : ne retient pas les entités mortes/déchargées, évite une fuite mémoire.
    private final Map<Monster, Long> lastContactDamageTick = new WeakHashMap<>();

    protected AbstractBlockadeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            TowerDefinition definition, boolean dealsContactDamage) {
        super(type, pos, state, definition);
        this.dealsContactDamage = dealsContactDamage;
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

        TowerStats stats = blockEntity.stats();
        float contactDamage = stats.floatValue(TowerStat.CONTACT_DAMAGE);
        long contactIntervalTicks = stats.intValue(TowerStat.CONTACT_INTERVAL_TICKS);
        // Repousse : seulement pour les tours qui déclarent la stat (Bouncer Blockade).
        float knockbackStrength = (float) stats.getOr(TowerStat.KNOCKBACK_STRENGTH, 0.0D);

        AABB contactArea = new AABB(pos).inflate(stats.get(TowerStat.CONTACT_RANGE));
        long now = serverLevel.getGameTime();

        for (Monster monster : serverLevel.getEntitiesOfClass(Monster.class, contactArea)) {
            Long lastTrigger = blockEntity.lastContactDamageTick.get(monster);
            if (lastTrigger != null && now - lastTrigger < contactIntervalTicks) {
                continue;
            }

            blockEntity.lastContactDamageTick.put(monster, now);
            monster.hurt(serverLevel.damageSources().stalagmite(), contactDamage);

            if (knockbackStrength > 0.0F) {
                // "Position de la source moins position du monstre" (PAS l'inverse) : le
                // vecteur passé à Entity#knockback pointe vers la source, la formule pousse
                // alors dans le sens opposé (voir son corps : deltaMovement - normalize(xd,zd)),
                // donc bien loin d'elle. Testé en jeu (2026-08-29) : la version précédente
                // (monstre moins source, en pensant reproduire LivingEntity#blockedByItem)
                // attirait les monstres AU LIEU de les repousser — signe inversé, corrigé ici
                // à partir du comportement observé plutôt que re-deviné depuis les sources.
                double dx = (pos.getX() + 0.5D) - monster.getX();
                double dz = (pos.getZ() + 0.5D) - monster.getZ();
                monster.knockback(knockbackStrength, dx, dz);
            }
        }
    }
}
