package com.github.c0c0tier.dungeon_defenders.init;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

// Un ennemi, tel que décrit par un JSON de data/<namespace>/dungeon_defenders/enemy/<nom>.json
// (voir EnemyRegistry pour le chargement, doc/data-driven/ennemis.md pour le format). Remplace
// l'ex-enum SpawnableEnemy : son identifiant (<namespace>:<nom>) est ce que les spawners
// sauvegardent désormais, à la place d'un ordinal.
public record EnemyDefinition(
        EntityType<?> entityType,
        Item icon,
        int xpValue,
        int order,
        EnemyBehavior behavior) {

    // Codecs STRICTS plutôt que BuiltInRegistries.X.byNameCodec() : ces deux registres sont des
    // DefaultedRegistry, dont le codec standard remplace silencieusement un identifiant inconnu
    // par la valeur par défaut (un cochon pour ENTITY_TYPE, l'air pour ITEM). Une faute de
    // frappe dans un JSON donnerait un ennemi-cochon sans le moindre message ; ici, c'est une
    // erreur de parse, donc un fichier ignoré et loggé.
    private static final Codec<EntityType<?>> STRICT_ENTITY_TYPE = strict(BuiltInRegistries.ENTITY_TYPE, "entity_type");
    private static final Codec<Item> STRICT_ITEM = strict(BuiltInRegistries.ITEM, "icon");

    public static final Codec<EnemyDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            STRICT_ENTITY_TYPE.fieldOf("entity_type").forGetter(EnemyDefinition::entityType),
            STRICT_ITEM.fieldOf("icon").forGetter(EnemyDefinition::icon),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("xp_value").forGetter(EnemyDefinition::xpValue),
            Codec.INT.optionalFieldOf("order", 0).forGetter(EnemyDefinition::order),
            EnemyBehavior.CODEC.fieldOf("behavior").forGetter(EnemyDefinition::behavior)
    ).apply(instance, EnemyDefinition::new));

    /**
     * Clé de traduction du nom affiché, déduite de l'identifiant : {@code <namespace>.enemy.<nom>}.
     * Donne exactement les clés d'avant la migration ({@code dungeon_defenders.enemy.zombie}).
     */
    public static String translationKey(Identifier id) {
        return id.getNamespace() + ".enemy." + id.getPath().replace('/', '.');
    }

    private static <T> Codec<T> strict(Registry<T> registry, String field) {
        // containsKey d'abord : c'est le seul test qui ne passe jamais par la valeur par défaut
        // d'un DefaultedRegistry, quelle que soit la façon dont getOptional/getValue y sont
        // redéfinis.
        return Identifier.CODEC.comapFlatMap(
                id -> registry.containsKey(id)
                        ? DataResult.success(registry.getValue(id))
                        : DataResult.error(() -> "\"" + field + "\" inconnu : " + id),
                registry::getKey);
    }
}
