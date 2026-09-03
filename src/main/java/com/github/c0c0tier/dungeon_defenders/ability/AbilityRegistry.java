package com.github.c0c0tier.dungeon_defenders.ability;

import com.github.c0c0tier.dungeon_defenders.init.AbilitySlot;
import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

// Résout quelle compétence concrète correspond à un emplacement pour un joueur donné : HEAL et
// REPAIR sont génériques (mêmes singletons pour tout héros, voir HealAbility/RepairAbility) ;
// SPELL_2 vient du héros du joueur ; SPELL_1 n'est jamais une canalisation (c'est une salve,
// voir BurstAbility), null ici est le signal qu'aucune compétence maintenue ne correspond.
//
// Partagé entre ModNetworking (démarrage d'une canalisation) et ModEvents (tick de la
// canalisation en cours) pour ne pas dupliquer ce switch à deux endroits.
public final class AbilityRegistry {

    private AbilityRegistry() {
    }

    public static @Nullable ChannelAbility resolveChannel(ServerPlayer player, AbilitySlot slot) {
        return switch (slot) {
            case HEAL -> HealAbility.INSTANCE;
            case REPAIR -> RepairAbility.INSTANCE;
            case SPELL_2 -> HeroDefinition.of(player).spell2();
            case SPELL_1 -> null;
        };
    }
}
