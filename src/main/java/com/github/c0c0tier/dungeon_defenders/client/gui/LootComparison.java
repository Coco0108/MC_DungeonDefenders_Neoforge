package com.github.c0c0tier.dungeon_defenders.client.gui;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.loot.LootWeapon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

// Les bonus d'une arme de butin comparés à ceux de l'arme que son héros porte déjà : une ligne
// par bonus (« Dégâts : +18 % (+6) »), l'écart en vert s'il est meilleur, en rouge s'il est
// moins bon. Sert à l'écran de la Forge et à l'encadré du butin au sol.
//
// On compare toujours à l'arme équipée par le HÉROS DE L'ARME (pas forcément le héros actuel) :
// c'est elle que l'arme remplacerait. Sans arme équipée, la référence est l'arme de base, sans
// aucun bonus.
public final class LootComparison {

    private record Bonus(String key, ToDoubleFunction<LootWeapon> value, boolean percent) {
    }

    private static final List<Bonus> BONUSES = List.of(
            new Bonus("damage", LootWeapon::damage, true),
            new Bonus("health", LootWeapon::health, false),
            new Bonus("speed", LootWeapon::speed, true),
            new Bonus("tower_damage", LootWeapon::towerDamage, true));

    private LootComparison() {
    }

    /** L'arme que porte déjà le héros de {@code weapon}, ou null (arme de base). */
    public static @Nullable LootWeapon reference(Player player, LootWeapon weapon) {
        return player.getData(ModAttachments.EQUIPPED_WEAPONS).get(weapon.hero());
    }

    /**
     * Une ligne par bonus présent sur l'une ou l'autre arme. {@code reference} null = arme de
     * base ; {@code compare} faux = les valeurs seules, sans écart (l'arme comparée à elle-même).
     */
    public static List<Component> lines(LootWeapon weapon, @Nullable LootWeapon reference, boolean compare) {
        List<Component> lines = new ArrayList<>();
        for (Bonus bonus : BONUSES) {
            double value = bonus.value().applyAsDouble(weapon);
            double previous = reference == null ? 0.0D : bonus.value().applyAsDouble(reference);
            if (value <= 0.0D && (!compare || previous <= 0.0D)) {
                continue;
            }
            MutableComponent line = Component.translatable("dungeon_defenders.loot.bonus." + bonus.key(),
                    value <= 0.0D ? "—" : format(value, bonus.percent(), true));
            line.withStyle(value <= 0.0D ? ChatFormatting.DARK_GRAY : ChatFormatting.WHITE);
            double delta = value - previous;
            if (compare && Math.abs(delta) >= (bonus.percent() ? 0.005D : 0.5D)) {
                line.append(Component.literal(" (" + format(delta, bonus.percent(), true).replace(" ", "") + ")")
                        .withStyle(delta > 0.0D ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
            lines.add(line);
        }
        return lines;
    }

    private static String format(double value, boolean percent, boolean signed) {
        String sign = signed && value > 0.0D ? "+" : "";
        if (percent) {
            return sign + String.format(Locale.ROOT, "%.0f %%", value * 100.0D);
        }
        return sign + String.format(Locale.ROOT, "%.0f", value);
    }
}
