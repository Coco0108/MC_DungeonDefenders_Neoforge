package com.github.c0c0tier.dungeon_defenders.atelier;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

/**
 * Enregistre un événement sonore par entrée de {@code assets/dungeon_defenders/sounds.json} : ajouter un son ne demande
 * aucun code. Ils doivent être enregistrés : le gestionnaire de keyframes de son de GeckoLib les cherche dans le registre,
 * et le serveur envoie les sons de repos, de blessure et de mort d'un monstre par leur id de registre. Repris de
 * {@code entity/AtelierSounds.java} de l'Atelier.
 *
 * <p>Dungeon Defenders n'avait aucun son à lui avant l'Atelier : tout le sounds.json est enregistré ici. Un futur son
 * propre au mod passerait par ce même fichier, sans second registre (qui l'enregistrerait deux fois).
 */
public final class AtelierSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, DungeonDefendersMod.MODID);
    private static final Map<Identifier, DeferredHolder<SoundEvent, SoundEvent>> EVENTS = new TreeMap<>();

    private AtelierSounds() {
    }

    /** Après {@link AtelierLoader#load}, à la construction du mod (il lit aussi le sounds.json). */
    public static void register(IEventBus modEventBus) {
        for (String path : Atelier.sons()) {
            Identifier id = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, path);
            EVENTS.put(id, SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(id)));
        }
        SOUNDS.register(modEventBus);
    }

    /** L'événement enregistré, ou null s'il n'est pas déclaré (les fiches sont vérifiées au démarrage). */
    public static @Nullable SoundEvent get(Identifier id) {
        DeferredHolder<SoundEvent, SoundEvent> holder = EVENTS.get(id);
        return holder == null ? null : holder.get();
    }
}
