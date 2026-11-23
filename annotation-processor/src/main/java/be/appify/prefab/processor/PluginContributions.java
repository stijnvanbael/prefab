package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.OutputTarget;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * Applies {@link PrefabPlugin} builder hooks to one generated type and attributes every member a plugin adds.
 *
 * <p>Plugins run in the order of {@link PrefabContext#plugins()}, which is deterministic (see
 * {@link PrefabPlugin#EXECUTION_ORDER}). When a plugin adds a method, field or nested type that the generator or an
 * earlier plugin already contributed, a compiler error on the aggregate names both contributors. Without it the only
 * diagnostic would be javac's unattributed "already defined" error inside generated source, which still follows.
 */
final class PluginContributions {
    static final String GENERATOR = "Prefab";

    private final PrefabContext context;

    PluginContributions(PrefabContext context) {
        this.context = context;
    }

    /**
     * Runs {@code hook} for every plugin that is enabled for the aggregate and not routed to {@code excludedTarget}.
     *
     * @param aggregateType  the aggregate the generated type belongs to
     * @param builder        the generated type the plugins contribute to
     * @param excludedTarget output target whose plugins must not contribute to this type
     * @param hook           invokes the plugin hook on {@code builder}
     */
    void apply(TypeElement aggregateType, TypeSpec.Builder builder, OutputTarget excludedTarget,
            Consumer<PrefabPlugin> hook) {
        var owners = new HashMap<String, String>();
        var current = builder.build();
        memberKeys(current).forEach(key -> owners.putIfAbsent(key, GENERATOR));
        for (var plugin : context.plugins()) {
            if (context.isPluginEnabledFor(aggregateType, plugin.getClass())
                    && context.getOutputTargetFor(aggregateType, plugin.getClass()) != excludedTarget) {
                context.withPluginOutputTarget(aggregateType, plugin.getClass(), () -> hook.accept(plugin));
                var next = builder.build();
                claim(aggregateType, current, next, plugin, owners);
                current = next;
            }
        }
    }

    private void claim(TypeElement aggregateType, TypeSpec before, TypeSpec after, PrefabPlugin plugin,
            Map<String, String> owners) {
        var contributor = plugin.getClass().getName();
        addedMemberKeys(before, after).forEach(key -> {
            var owner = owners.putIfAbsent(key, contributor);
            if (owner != null) {
                reportConflict(aggregateType, after.name(), key, contributor, owner);
            }
        });
    }

    private void reportConflict(TypeElement aggregateType, String generatedType, String member, String contributor,
            String owner) {
        var message = ("Plugin %s adds %s to generated %s, but %s already contributes it. Disable one of them for "
                + "this aggregate with @Generate(plugin = ..., enabled = false) or give the member a distinct "
                + "signature.").formatted(contributor, member, generatedType, owner);
        context.processingEnvironment().getMessager().printMessage(Diagnostic.Kind.ERROR, message, aggregateType);
    }

    private static List<String> memberKeys(TypeSpec type) {
        return Stream.of(
                        type.methodSpecs().stream().map(PluginContributions::methodKey),
                        type.fieldSpecs().stream().map(PluginContributions::fieldKey),
                        type.typeSpecs().stream().map(PluginContributions::typeKey))
                .flatMap(keys -> keys)
                .toList();
    }

    /** Builder member lists are append-only, so the members a plugin added are the tail of each list. */
    private static List<String> addedMemberKeys(TypeSpec before, TypeSpec after) {
        return Stream.of(
                        tail(after.methodSpecs(), before.methodSpecs()).stream().map(PluginContributions::methodKey),
                        tail(after.fieldSpecs(), before.fieldSpecs()).stream().map(PluginContributions::fieldKey),
                        tail(after.typeSpecs(), before.typeSpecs()).stream().map(PluginContributions::typeKey))
                .flatMap(keys -> keys)
                .toList();
    }

    private static <T> List<T> tail(List<T> after, List<T> before) {
        return after.subList(before.size(), after.size());
    }

    private static String methodKey(MethodSpec method) {
        var parameters = method.parameters().stream()
                .map(ParameterSpec::type)
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        return "method %s(%s)".formatted(method.name(), parameters);
    }

    private static String fieldKey(FieldSpec field) {
        return "field " + field.name();
    }

    private static String typeKey(TypeSpec type) {
        return "nested type " + type.name();
    }
}
