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

// Sort 2 du Géomancien : "Peau de pierre". Tant que la touche est maintenue, le joueur encaisse
// 60 % de dégâts en moins (Résistance III) mais marche plus lentement (Lenteur I). Le pendant
// défensif de la Rage sanguinaire de l'Écuyer, construit de la même façon : effets vanilla très
// courts, rafraîchis à chaque tick, qui expirent seuls quand la canalisation s'arrête.
//
// Toutes les valeurs sont des valeurs de test, à ajuster en jeu.
public final class StoneSkinAbility implements ChannelAbility {

    public static final StoneSkinAbility INSTANCE = new StoneSkinAbility();

    private static final int MANA_COST_PER_TICK = 1;
    private static final int EFFECT_DURATION_TICKS = 5;
    private static final int RESISTANCE_AMPLIFIER = 2;
    private static final int SLOWNESS_AMPLIFIER = 0;

    private StoneSkinAbility() {
    }

    @Override
    public Component displayName() {
        return Component.translatable("dungeon_defenders.ability.stone_skin");
    }

    @Override
    public Item icon() {
        return Items.STONE;
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
        player.setData(ModAttachments.MANA, player.getData(ModAttachments.MANA) - MANA_COST_PER_TICK);
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, EFFECT_DURATION_TICKS, RESISTANCE_AMPLIFIER), null);
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, EFFECT_DURATION_TICKS, SLOWNESS_AMPLIFIER), null);
    }
}
