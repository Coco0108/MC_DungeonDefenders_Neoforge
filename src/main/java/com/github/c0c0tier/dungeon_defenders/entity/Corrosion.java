package com.github.c0c0tier.dungeon_defenders.entity;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

// "Corrosion" : un monstre arrosé par le Projecteur d'acide subit plus de dégâts, de TOUTES les
// sources (tours, joueurs, sorts), pendant quelques secondes. C'est ce qui fait du Projecteur
// une tour de soutien plus qu'une tour de dégâts.
//
// En mémoire uniquement (WeakHashMap : un monstre mort ou déchargé s'oublie seul) : l'effet
// dure trois secondes, il n'a pas à survivre à un redémarrage. Appliqué par ModEvents
// (LivingIncomingDamageEvent). Les corrosions ne se cumulent pas : la plus forte l'emporte.
public final class Corrosion {

    private record Mark(double factor, long untilGameTime) {
    }

    private static final Map<LivingEntity, Mark> MARKS = new WeakHashMap<>();

    private Corrosion() {
    }

    /** Marque {@code entity} : dégâts subis multipliés par {@code factor} jusqu'au tick donné. */
    public static void apply(LivingEntity entity, double factor, long untilGameTime) {
        Mark current = MARKS.get(entity);
        long now = entity.level().getGameTime();
        if (current != null && now < current.untilGameTime() && factor < current.factor()) {
            return;
        }
        MARKS.put(entity, new Mark(factor, untilGameTime));
    }

    /** Multiplicateur de dégâts subis en vigueur pour {@code entity} : 1 si elle n'est pas corrodée. */
    public static double factor(LivingEntity entity) {
        Mark mark = MARKS.get(entity);
        if (mark == null) {
            return 1.0D;
        }
        if (entity.level().getGameTime() >= mark.untilGameTime()) {
            MARKS.remove(entity);
            return 1.0D;
        }
        return mark.factor();
    }
}
