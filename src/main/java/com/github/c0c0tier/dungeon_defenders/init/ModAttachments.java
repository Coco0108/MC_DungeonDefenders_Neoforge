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

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
