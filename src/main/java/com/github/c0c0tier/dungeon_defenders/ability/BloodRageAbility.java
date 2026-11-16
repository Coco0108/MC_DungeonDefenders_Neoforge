package com.github.c0c0tier.dungeon_defenders.ability;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

// Sort 2 de l'Écuyer (emplacement SPELL_2) — le second des deux sorts confirmés du jeu de
// référence : augmente fortement dégâts et vitesse tant qu'elle est active, en drainant le
// mana rapidement.
//
// Implémentée via les effets vanilla Speed + Strength (MobEffects.SPEED/STRENGTH — PAS
// MOVEMENT_SPEED/DAMAGE_BOOST, des noms plausibles mais faux, vérifiés dans les sources avant
// d'écrire cette classe) plutôt qu'un modificateur d'attribut ou un hook de dégâts custom :
// c'est le mécanisme vanilla déjà testé pour "buff de vitesse/dégâts", pas de raison de
// réinventer la logique de calcul de dégâts au corps à corps.
//
// Aucun onStop explicite à la fin de la canalisation : la durée de l'effet appliqué à chaque
// tick est volontairement courte (EFFECT_DURATION_TICKS) et rafraîchie tant que la touche est
// tenue — dès que la canalisation s'arrête (mana épuisé ou touche relâchée), l'effet expire de
// lui-même en quelques ticks, sans code de nettoyage séparé.
public final class BloodRageAbility implements ChannelAbility {

    public static final BloodRageAbility INSTANCE = new BloodRageAbility();

    // "Draine le mana très rapidement" (jeu de référence, valeur exacte non documentée) :
    // volontairement plus cher que le soin (1/tick) — valeur de test.
    private static final int MANA_COST_PER_TICK = 2;
    private static final int EFFECT_DURATION_TICKS = 5;
    private static final int EFFECT_AMPLIFIER = 1;

    private BloodRageAbility() {
    }

    @Override
    public Component displayName() {
        return Component.translatable("dungeon_defenders.ability.blood_rage");
    }

    @Override
    public Item icon() {
        return Items.BLAZE_POWDER;
    }

    @Override
    public int manaCostPerTick() {
        return MANA_COST_PER_TICK;
    }

    @Override
    public boolean canContinue(ServerPlayer player, @Nullable BlockPos target) {
        return player.getData(ModAttachments.MANA) >= MANA_COST_PER_TICK;
    }

    @Override
    public void applyTick(ServerPlayer player, @Nullable BlockPos target) {
        int currentMana = player.getData(ModAttachments.MANA);
        player.setData(ModAttachments.MANA, currentMana - MANA_COST_PER_TICK);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER), null);
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER), null);
    }
}
