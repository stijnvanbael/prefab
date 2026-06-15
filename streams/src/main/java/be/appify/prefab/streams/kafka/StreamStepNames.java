package be.appify.prefab.streams.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates unique, stable, and representative processor names for each DSL step within a
 * single Kafka Streams topology.
 *
 * <p>Names encode the operator type and the simple class names of the value types involved,
 * converted to kebab-case (e.g. {@code filter-incoming-order},
 * {@code join-incoming-order-shipping-update}).  When the same base name would be generated
 * more than once in the same topology a numeric suffix starting at {@code -2} is appended.
 */
public final class StreamStepNames {
    private final Map<String, AtomicInteger> usageCountByName = new HashMap<>();

    String nextBranchName(Class<?> inputType) {
        return reserveName("branch", inputType);
    }

    String nextBranchSubtypeName(Class<?> subtype) {
        return reserveName("branch-subtype", subtype);
    }

    String nextFilterName(Class<?> inputType) {
        return reserveName("filter", inputType);
    }

    String nextFlatMapName(Class<?> inputType) {
        return reserveName("flat-map", inputType);
    }

    String nextJoinName(Class<?> leftType, Class<?> rightType) {
        return reserveName("join", leftType, rightType);
    }

    String nextMapName(Class<?> inputType) {
        return reserveName("map", inputType);
    }

    String nextMergeName(Class<?> leftType, Class<?> rightType) {
        return reserveName("merge", leftType, rightType);
    }

    String nextProcessName(Class<?> inputType) {
        return reserveName("process", inputType);
    }

    private String reserveName(String operatorPrefix, Class<?>... types) {
        var baseName = buildBaseName(operatorPrefix, types);
        var count = usageCountByName.computeIfAbsent(baseName, k -> new AtomicInteger(0))
                .incrementAndGet();
        return count == 1 ? baseName : baseName + "-" + count;
    }

    private static String buildBaseName(String operatorPrefix, Class<?>[] types) {
        var typePart = Stream.of(types)
                .filter(t -> t != null && t != Object.class)
                .map(t -> toKebabCase(t.getSimpleName()))
                .distinct()
                .collect(Collectors.joining("-"));
        return typePart.isEmpty() ? operatorPrefix : operatorPrefix + "-" + typePart;
    }

    private static String toKebabCase(String value) {
        return value.replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase();
    }
}
