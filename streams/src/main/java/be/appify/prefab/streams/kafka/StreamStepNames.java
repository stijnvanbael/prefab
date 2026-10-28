package be.appify.prefab.streams.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
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
    private static final int MAX_EXPLICIT_NAME_LENGTH = 200;
    private static final Pattern VALID_EXPLICIT_NAME = Pattern.compile("[a-zA-Z0-9._-]+");

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

    String nextGroupByName(Class<?> inputType) {
        return reserveName("group-by", inputType);
    }

    String nextAggregateName(Class<?> inputType, Class<?> resultType) {
        return reserveName("aggregate", inputType, resultType);
    }

    String nextWindowedAggregateName(Class<?> inputType, Class<?> resultType) {
        return reserveName("windowed-aggregate", inputType, resultType);
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

    String nextRepartitionName(Class<?> inputType) {
        return reserveName("repartition", inputType);
    }

    String reserveExplicitName(String name) {
        validateExplicitName(name);
        if (usageCountByName.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Repartition topic name '" + name + "' conflicts with a step or repartition topic already "
                    + "defined in this topology. Choose a unique name.");
        }
        usageCountByName.put(name, new AtomicInteger(1));
        return name;
    }

    private static void validateExplicitName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Repartition topic name must not be null or blank");
        }
        if (name.length() > MAX_EXPLICIT_NAME_LENGTH) {
            throw new IllegalArgumentException("Repartition topic name '" + name + "' is longer than "
                    + MAX_EXPLICIT_NAME_LENGTH + " characters");
        }
        if (name.equals(".") || name.equals("..") || !VALID_EXPLICIT_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid repartition topic name '" + name
                    + "'. Use only letters, digits, '.', '_' and '-' (and not just '.' or '..').");
        }
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
