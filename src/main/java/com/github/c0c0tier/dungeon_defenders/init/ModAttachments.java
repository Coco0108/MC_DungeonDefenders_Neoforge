package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModAttachments {
    // Mana maximal par défaut d'un joueur. À externaliser dans Config si des sorts
    // ou upgrades doivent le faire varier.
    public static final int MAX_MANA = 100;

    // Expérience custom maximale par défaut (rien à voir avec l'XP vanilla). Valeur
    // provisoire tant que la façon d'en gagner/monter de niveau n'est pas définie.
    public static final int MAX_EXPERIENCE = 100;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, DungeonDefendersMod.MODID);

    // Mana courant du joueur : plein par défaut, persistant, synchronisé au client
    // pour l'affichage HUD.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> MANA = ATTACHMENT_TYPES.register(
            "mana",
            () -> AttachmentType.builder(() -> MAX_MANA)
                    .serialize(Codec.INT.fieldOf("Mana"))
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    // Expérience custom du joueur : vide par défaut (contrairement au mana/à la vie, elle se
    // gagne au lieu de se dépenser), persistante, synchronisée au client pour le HUD.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> EXPERIENCE = ATTACHMENT_TYPES.register(
            "experience",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT.fieldOf("Experience"))
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
