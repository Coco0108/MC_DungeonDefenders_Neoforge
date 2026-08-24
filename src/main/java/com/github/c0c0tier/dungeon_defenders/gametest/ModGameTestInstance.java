package com.github.c0c0tier.dungeon_defenders.gametest;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

// GameTestInstance custom plutôt que la FunctionGameTestInstance vanilla : celle-ci résout sa
// fonction via Registries.TEST_FUNCTION, un registre "BuiltInRegistries" bootstrapé une seule
// fois au chargement de la classe (BuiltInRegistries.TEST_FUNCTION = registerSimple(...,
// BuiltinTestFunctions::bootstrap)) — bien avant que RegisterGameTestsEvent (côté mod) ait la
// moindre chance de s'exécuter, donc pas de point d'accroche exploitable pour y ajouter les
// fonctions de ce mod. RegisterGameTestsEvent#registerTest accepte en revanche n'importe quel
// GameTestInstance construit directement en code : cette classe stocke elle-même l'association
// nom -> Consumer<GameTestHelper> (petite table statique propre au mod), sans dépendre du
// registre vanilla.
public class ModGameTestInstance extends GameTestInstance {
    private static final Map<Identifier, Consumer<GameTestHelper>> FUNCTIONS = new HashMap<>();

    public static final MapCodec<ModGameTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC.fieldOf("function").forGetter(ModGameTestInstance::functionId),
                    TestData.CODEC.forGetter(ModGameTestInstance::info))
            .apply(instance, ModGameTestInstance::new));

    private final Identifier functionId;

    private ModGameTestInstance(Identifier functionId, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.functionId = functionId;
    }

    /** Enregistre {@code function} sous {@code functionId} et construit l'instance qui la référence. */
    public static ModGameTestInstance create(
            Identifier functionId, Consumer<GameTestHelper> function, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        FUNCTIONS.put(functionId, function);
        return new ModGameTestInstance(functionId, info);
    }

    private Identifier functionId() {
        return this.functionId;
    }

    @Override
    public void run(GameTestHelper helper) {
        Consumer<GameTestHelper> function = FUNCTIONS.get(this.functionId);
        if (function == null) {
            throw new IllegalStateException("Fonction de gametest inconnue : " + this.functionId);
        }
        function.accept(helper);
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal(DungeonDefendersMod.MODID);
    }
}
