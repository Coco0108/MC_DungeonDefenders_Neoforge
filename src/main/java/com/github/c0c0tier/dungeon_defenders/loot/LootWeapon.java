package com.github.c0c0tier.dungeon_defenders.loot;

import com.github.c0c0tier.dungeon_defenders.init.HeroDefinition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// Une arme de butin : l'arme d'un héros précis, avec une rareté et des bonus tirés au hasard.
// C'est une DONNÉE, pas un objet d'inventaire vanilla : elle vit dans l'inventaire de la Forge
// ou dans l'emplacement d'arme du joueur (attachments, voir Equipment), et n'existe sous forme
// d'ItemStack que posée au sol, en attendant d'être ramassée (toStack/fromStack).
//
//   hero         : identifiant du héros qui peut l'équiper (HeroDefinition#id)
//   rarity       : nom d'un LootRarity
//   damage       : dégâts infligés par le joueur, en fraction (0.12 = +12 %)
//   health       : PV max en plus
//   speed        : vitesse de déplacement, en fraction
//   towerDamage  : dégâts des tours posées par le joueur, en fraction
//
// Toujours au moins le bonus de dégâts ; les trois autres valent 0 quand l'arme ne les a pas.
public record LootWeapon(String hero, String rarity, double damage, double health, double speed, double towerDamage) {

    /** Clé de la donnée personnalisée qui marque un ItemStack au sol comme butin. */
    private static final String STACK_KEY = "dd_loot";

    public static final Codec<LootWeapon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("Hero").forGetter(LootWeapon::hero),
            Codec.STRING.fieldOf("Rarity").forGetter(LootWeapon::rarity),
            Codec.DOUBLE.optionalFieldOf("Damage", 0.0D).forGetter(LootWeapon::damage),
            Codec.DOUBLE.optionalFieldOf("Health", 0.0D).forGetter(LootWeapon::health),
            Codec.DOUBLE.optionalFieldOf("Speed", 0.0D).forGetter(LootWeapon::speed),
            Codec.DOUBLE.optionalFieldOf("TowerDamage", 0.0D).forGetter(LootWeapon::towerDamage)
    ).apply(instance, LootWeapon::new));

    public static final StreamCodec<ByteBuf, LootWeapon> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, LootWeapon::hero,
            ByteBufCodecs.STRING_UTF8, LootWeapon::rarity,
            ByteBufCodecs.DOUBLE, LootWeapon::damage,
            ByteBufCodecs.DOUBLE, LootWeapon::health,
            ByteBufCodecs.DOUBLE, LootWeapon::speed,
            ByteBufCodecs.DOUBLE, LootWeapon::towerDamage,
            LootWeapon::new);

    public LootRarity lootRarity() {
        return LootRarity.byName(this.rarity);
    }

    /** Le héros qui peut équiper cette arme, ou null si son identifiant n'existe plus. */
    public @Nullable HeroDefinition heroDefinition() {
        for (HeroDefinition candidate : HeroDefinition.values()) {
            if (candidate.id().equals(this.hero)) {
                return candidate;
            }
        }
        return null;
    }

    /** "Sacoche de fioles (Rare)", dans la couleur de la rareté. */
    public MutableComponent displayName() {
        HeroDefinition owner = heroDefinition();
        Component base = owner == null ? Component.literal(this.hero) : owner.weapon().getName(new ItemStack(owner.weapon()));
        return Component.translatable("dungeon_defenders.loot.name", base, lootRarity().displayName())
                .withStyle(lootRarity().textColor());
    }

    /** Une ligne par bonus non nul, dans un ordre fixe. */
    public List<Component> statLines() {
        List<Component> lines = new ArrayList<>();
        if (this.damage > 0.0D) {
            lines.add(Component.translatable("dungeon_defenders.loot.stat.damage", percent(this.damage)));
        }
        if (this.health > 0.0D) {
            lines.add(Component.translatable("dungeon_defenders.loot.stat.health", String.format(Locale.ROOT, "%.0f", this.health)));
        }
        if (this.speed > 0.0D) {
            lines.add(Component.translatable("dungeon_defenders.loot.stat.speed", percent(this.speed)));
        }
        if (this.towerDamage > 0.0D) {
            lines.add(Component.translatable("dungeon_defenders.loot.stat.tower_damage", percent(this.towerDamage)));
        }
        return lines;
    }

    private static String percent(double fraction) {
        return String.format(Locale.ROOT, "%.0f", fraction * 100.0D);
    }

    // --- FORME "OBJET AU SOL" ---

    /** L'objet posé au sol : l'item de l'arme du héros, nommé et porteur de ces données. */
    public ItemStack toStack() {
        HeroDefinition owner = heroDefinition();
        ItemStack stack = new ItemStack(owner == null ? HeroDefinition.DEFAULT.weapon() : owner.weapon());
        CompoundTag tag = new CompoundTag();
        CODEC.encodeStart(NbtOps.INSTANCE, this).result().ifPresent(encoded -> tag.put(STACK_KEY, encoded));
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
        stack.set(DataComponents.CUSTOM_NAME, displayName());
        return stack;
    }

    /** Relit un butin depuis un objet au sol ; vide si ce n'en est pas un. Valable des deux côtés. */
    public static Optional<LootWeapon> fromStack(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) {
            return Optional.empty();
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(STACK_KEY)) {
            return Optional.empty();
        }
        return CODEC.parse(NbtOps.INSTANCE, tag.get(STACK_KEY)).result();
    }
}
