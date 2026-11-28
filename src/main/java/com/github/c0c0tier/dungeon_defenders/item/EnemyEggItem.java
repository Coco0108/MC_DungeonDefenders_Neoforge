package com.github.c0c0tier.dungeon_defenders.item;

import com.github.c0c0tier.dungeon_defenders.init.EnemyDefinition;
import com.github.c0c0tier.dungeon_defenders.init.EnemyRegistry;
import com.github.c0c0tier.dungeon_defenders.init.ModBlocks;
import com.github.c0c0tier.dungeon_defenders.init.ModDataComponents;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * L'œuf d'un ennemi de Dungeon Defenders (onglet créatif « Monstres »). Un seul objet pour tous les ennemis : chaque
 * exemplaire porte l'id de son ennemi (composant {@code dungeon_defenders:enemy}) et prend l'icône de l'ennemi comme
 * apparence (composant {@code item_model}).
 *
 * <p>Pourquoi pas les œufs vanilla : les ennemis sont data-driven ({@code dungeon_defenders/enemy/*.json}) et plusieurs
 * partagent un même type de mob (la brute est un zombie) ; un œuf de zombie donne toujours le zombie. Celui-ci fait
 * apparaître l'ennemi exact, avec ses stats et son IA, comme un spawner ({@link EnemyDefinition#spawn}).
 */
public class EnemyEggItem extends Item {

    /** Un ennemi tel que l'onglet le montre : son id et l'objet qui lui sert d'icône. */
    public record Entry(Identifier id, Item icon) {
    }

    /**
     * Les ennemis à montrer dans l'onglet, dans l'ordre. Par défaut, ceux du serveur ({@link EnemyRegistry}) ; le client
     * le remplace par sa propre copie (reçue du serveur), sans que ce code commun ne nomme de classe cliente.
     */
    private static volatile Supplier<List<Entry>> enemies = () -> EnemyRegistry.sortedIds().stream()
            .map(id -> new Entry(id, Objects.requireNonNull(EnemyRegistry.byId(id)).icon()))
            .toList();

    public EnemyEggItem(Properties properties) {
        super(properties);
    }

    public static void setEnemies(Supplier<List<Entry>> source) {
        enemies = source;
    }

    public static List<Entry> enemies() {
        return enemies.get();
    }

    /** L'œuf de l'ennemi {@code id}, dessiné comme {@code icon}. */
    public static ItemStack of(Identifier id, Item icon) {
        ItemStack stack = new ItemStack(ModBlocks.ENEMY_EGG.get());
        stack.set(ModDataComponents.ENEMY.get(), id);
        stack.set(DataComponents.ITEM_MODEL, BuiltInRegistries.ITEM.getKey(icon));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        Identifier id = stack.get(ModDataComponents.ENEMY.get());
        return id == null ? super.getName(stack)
                : Component.translatable("item.dungeon_defenders.enemy_egg.named", Component.translatable(EnemyDefinition.translationKey(id)));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        Identifier id = stack.get(ModDataComponents.ENEMY.get());
        EnemyDefinition definition = id == null ? null : EnemyRegistry.byId(id);
        if (definition == null) {
            if (context.getPlayer() != null) {
                context.getPlayer().sendSystemMessage(Component.translatable("dungeon_defenders.enemy_egg.unknown",
                        id == null ? "?" : id.toString()));
            }
            return InteractionResult.FAIL;
        }
        // Comme un œuf vanilla : dans le bloc visé s'il est traversable, sinon devant la face cliquée.
        BlockPos pos = context.getClickedPos();
        BlockPos spawnPos = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                ? pos : pos.relative(context.getClickedFace());
        if (definition.spawn(level, id, spawnPos) == null) {
            return InteractionResult.FAIL;
        }
        stack.consume(1, context.getPlayer());
        level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, spawnPos);
        return InteractionResult.SUCCESS;
    }
}
