package com.github.c0c0tier.dungeon_defenders.init;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

// Un ennemi, tel que décrit par un JSON de data/<namespace>/dungeon_defenders/enemy/<nom>.json
// (voir EnemyRegistry pour le chargement, doc/data-driven/ennemis.md pour le format). Remplace
// l'ex-enum SpawnableEnemy : son identifiant (<namespace>:<nom>) est ce que les spawners
// sauvegardent désormais, à la place d'un ordinal.
public record EnemyDefinition(
        EntityType<?> entityType,
        Item icon,
        int xpValue,
        int order,
        EnemyBehavior behavior,
        // Stats optionnelles (2026-11-10) : absentes, l'attribut vanilla du mob n'est pas touché —
        // c'est le cas du zombie et du squelette livrés, donc rien ne change pour eux.
        Optional<Double> maxHealth,
        Optional<Double> movementSpeed,
        Optional<Double> scale) {

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
            EnemyBehavior.CODEC.fieldOf("behavior").forGetter(EnemyDefinition::behavior),
            Codec.doubleRange(1.0D, 1024.0D).optionalFieldOf("max_health").forGetter(EnemyDefinition::maxHealth),
            Codec.doubleRange(0.01D, 2.0D).optionalFieldOf("movement_speed").forGetter(EnemyDefinition::movementSpeed),
            Codec.doubleRange(0.1D, 8.0D).optionalFieldOf("scale").forGetter(EnemyDefinition::scale)
    ).apply(instance, EnemyDefinition::new));

    /**
     * Fait apparaître une instance de cet ennemi, marquée de son identifiant AVANT de rejoindre
     * le monde : ModEvents.onMonsterSpawn (EntityJoinLevelEvent, déclenché pendant l'ajout) lit
     * ce marquage pour choisir l'IA, ce qui serait trop tard une fois le mob déjà ajouté. Les
     * stats optionnelles sont appliquées au même moment.
     */
    public @Nullable Entity spawn(ServerLevel level, Identifier id, BlockPos pos) {
        return spawnTyped(this.entityType, level, id, pos);
    }

    private <T extends Entity> @Nullable T spawnTyped(EntityType<T> type, ServerLevel level, Identifier id, BlockPos pos) {
        return type.spawn(level, entity -> {
            entity.setData(ModAttachments.ENEMY_ID, id.toString());
            if (entity instanceof LivingEntity living) {
                applyStats(living);
            }
        }, pos, EntitySpawnReason.SPAWNER, false, false);
    }

    /** Applique les stats optionnelles de cet ennemi (PV max, vitesse, taille) à un mob. */
    public void applyStats(LivingEntity living) {
        this.maxHealth.ifPresent(value -> {
            setBase(living, Attributes.MAX_HEALTH, value);
            living.setHealth(living.getMaxHealth());
        });
        this.movementSpeed.ifPresent(value -> setBase(living, Attributes.MOVEMENT_SPEED, value));
        this.scale.ifPresent(value -> setBase(living, Attributes.SCALE, value));
    }

    private static void setBase(LivingEntity living, Holder<Attribute> attribute, double value) {
        AttributeInstance instance = living.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

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
