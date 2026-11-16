package com.github.c0c0tier.dungeon_defenders.ability;

import com.github.c0c0tier.dungeon_defenders.block.entity.AbstractTowerBlockEntity;
import com.github.c0c0tier.dungeon_defenders.block.entity.TowerScan;
import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

// Sort 2 de l'Alchimiste : "Catalyse". Tant que la touche est maintenue, toutes les tours à 8
// blocs du joueur frappent 50 % plus fort, au prix d'un drain de mana rapide. Là où la Rage
// sanguinaire de l'Écuyer renforce le joueur, la Catalyse renforce ses tours : l'Alchimiste se
// tient près de sa défense plutôt que dans la mêlée.
//
// Même mécanisme que le Pylône catalyseur (AbstractTowerBlockEntity#applyDamageBoost) : les deux
// ne se cumulent pas, la tour garde le bonus le plus fort. Le bonus est posé pour quelques ticks
// et renouvelé : il s'éteint seul quand la canalisation s'arrête, sans code de nettoyage.
//
// Toutes les valeurs sont des valeurs de test, à ajuster en jeu.
public final class CatalysisAbility implements ChannelAbility {

    public static final CatalysisAbility INSTANCE = new CatalysisAbility();

    private static final int MANA_COST_PER_TICK = 2;
    private static final double RADIUS = 8.0D;
    private static final double BOOST_FACTOR = 1.5D;
    // Les tours sont recherchées deux fois par seconde seulement ; le bonus posé dure un peu
    // plus longtemps que cet intervalle pour ne pas clignoter.
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final int BOOST_DURATION_TICKS = 15;

    private CatalysisAbility() {
    }

    @Override
    public Component displayName() {
        return Component.translatable("dungeon_defenders.ability.catalysis");
    }

    @Override
    public Item icon() {
        return Items.GLOWSTONE_DUST;
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

        ServerLevel level = player.level();
        long now = level.getGameTime();
        if (now % SCAN_INTERVAL_TICKS != 0) {
            return;
        }
        for (AbstractTowerBlockEntity tower : TowerScan.towersNear(level, player.position(), RADIUS)) {
            tower.applyDamageBoost(BOOST_FACTOR, now + BOOST_DURATION_TICKS);
            BlockPos pos = tower.getBlockPos();
            level.sendParticles(ParticleTypes.WITCH,
                    pos.getX() + 0.5D, pos.getY() + 1.7D, pos.getZ() + 0.5D, 3, 0.2D, 0.1D, 0.2D, 0.0D);
        }
    }
}
