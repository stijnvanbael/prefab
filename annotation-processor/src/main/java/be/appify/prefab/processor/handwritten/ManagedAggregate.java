package be.appify.prefab.processor.handwritten;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.rest.AggregateEndpoints;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static be.appify.prefab.processor.CaseUtil.toSnakeCase;

/**
 * The part of an aggregate that the hand-written code checks need.
 *
 * @param simpleName
 *         the aggregate's simple name
 * @param packageName
 *         the aggregate's package
 * @param ownTypeNames
 *         qualified names of the aggregate's own types (the root and, for polymorphic aggregates, its subtypes)
 * @param endpoints
 *         the endpoints Prefab exposes for it
 */
record ManagedAggregate(
        String simpleName,
        String packageName,
        Set<String> ownTypeNames,
        List<AggregateEndpoints.Endpoint> endpoints
) {

    static ManagedAggregate of(ClassManifest manifest) {
        return new ManagedAggregate(manifest.simpleName(), manifest.packageName(),
                Set.of(manifest.qualifiedName()), AggregateEndpoints.of(manifest));
    }

    static ManagedAggregate of(PolymorphicAggregateManifest manifest) {
        var names = Stream.concat(
                Stream.of(manifest.packageName() + "." + manifest.simpleName()),
                manifest.subtypes().stream().map(ClassManifest::qualifiedName)).collect(Collectors.toSet());
        return new ManagedAggregate(manifest.simpleName(), manifest.packageName(), names, List.of());
    }

    /** Whether the given name, as written in an entity annotation, designates this aggregate's table or collection. */
    boolean isMappedTo(String mappedName) {
        var wanted = comparable(toSnakeCase(simpleName));
        return comparable(mappedName).equals(wanted);
    }

    /** Whether the given hand-written type name is the class Prefab would generate with this suffix. */
    boolean isGeneratedName(String typeSimpleName, String suffix) {
        return typeSimpleName.equals(simpleName + suffix);
    }

    private static String comparable(String name) {
        return name.replace("\"", "").replace("`", "").replace("_", "").toLowerCase(Locale.ROOT);
    }
}
