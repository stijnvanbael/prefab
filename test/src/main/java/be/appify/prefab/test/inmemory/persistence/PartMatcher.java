package be.appify.prefab.test.inmemory.persistence;

import be.appify.prefab.core.service.Reference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.util.ObjectUtils;

/**
 * One condition of a derived query, such as the {@code Name} or {@code PriceGreaterThan} of
 * {@code findByNameAndPriceGreaterThan}.
 */
final class PartMatcher {
    private final Part part;
    private final AggregateProperties properties;
    private final boolean ignoreCase;
    private final BiPredicate<Object, List<Object>> condition;

    /**
     * Creates the matcher of a part.
     *
     * @param part
     *         the part of the method name
     * @param method
     *         the description of the repository method, for error messages
     * @param properties
     *         reads aggregate properties
     * @throws IllegalArgumentException
     *         if the keyword of the part is not supported
     */
    PartMatcher(Part part, String method, AggregateProperties properties) {
        this.part = part;
        this.properties = properties;
        this.ignoreCase = part.shouldIgnoreCase() != Part.IgnoreCaseType.NEVER;
        this.condition = conditionOf(part.getType(), method);
    }

    int arguments() {
        return part.getNumberOfArguments();
    }

    /**
     * Binds the arguments of a call.
     *
     * @param values
     *         the arguments of the call that were not consumed yet; this part takes its share
     * @return the predicate for an aggregate
     */
    Predicate<Object> bind(Iterator<Object> values) {
        var arguments = new ArrayList<Object>();
        for (var index = 0; index < arguments(); index++) {
            arguments.add(coerce(values.next()));
        }
        var path = part.getProperty().toDotPath();
        return candidate -> condition.test(properties.read(candidate, path).orElse(null), arguments);
    }

    // Generated methods filter a Reference property by the plain identifier, which the real stores convert
    private Object coerce(Object value) {
        if (!Reference.class.equals(part.getProperty().getLeafType())) {
            return value;
        }
        return switch (value) {
            case String id -> Reference.fromId(id);
            case Collection<?> ids -> ids.stream().map(this::coerce).toList();
            default -> value;
        };
    }

    private BiPredicate<Object, List<Object>> conditionOf(Part.Type type, String method) {
        return switch (type) {
            case SIMPLE_PROPERTY -> (actual, args) -> same(actual, args.getFirst());
            case NEGATING_SIMPLE_PROPERTY -> (actual, args) -> actual != null && !same(actual, args.getFirst());
            case IN -> (actual, args) -> in(actual, args.getFirst());
            case NOT_IN -> (actual, args) -> actual != null && !in(actual, args.getFirst());
            case LIKE -> (actual, args) -> like(actual, args.getFirst());
            case NOT_LIKE -> (actual, args) -> actual != null && !like(actual, args.getFirst());
            case CONTAINING -> (actual, args) -> contains(actual, args.getFirst());
            case NOT_CONTAINING -> (actual, args) -> actual != null && !contains(actual, args.getFirst());
            case STARTING_WITH -> (actual, args) -> text(actual, args.getFirst(), String::startsWith);
            case ENDING_WITH -> (actual, args) -> text(actual, args.getFirst(), String::endsWith);
            case GREATER_THAN, AFTER -> (actual, args) -> actual != null && Comparisons.compare(actual, args.getFirst()) > 0;
            case GREATER_THAN_EQUAL ->
                    (actual, args) -> actual != null && Comparisons.compare(actual, args.getFirst()) >= 0;
            case LESS_THAN, BEFORE -> (actual, args) -> actual != null && Comparisons.compare(actual, args.getFirst()) < 0;
            case LESS_THAN_EQUAL -> (actual, args) -> actual != null && Comparisons.compare(actual, args.getFirst()) <= 0;
            case BETWEEN -> (actual, args) -> actual != null
                    && Comparisons.compare(actual, args.get(0)) >= 0 && Comparisons.compare(actual, args.get(1)) <= 0;
            case IS_NULL -> (actual, args) -> actual == null;
            case IS_NOT_NULL -> (actual, args) -> actual != null;
            case TRUE -> (actual, args) -> Boolean.TRUE.equals(actual);
            case FALSE -> (actual, args) -> Boolean.FALSE.equals(actual);
            case REGEX -> (actual, args) -> actual instanceof String text
                    && Pattern.compile(args.getFirst().toString(), flags()).matcher(text).find();
            default -> throw new IllegalArgumentException(
                    "%s uses the keyword %s, which the in-memory test backend does not support. Use "
                            .formatted(method, type) + "@IntegrationTest(containers = true) for tests that call it.");
        };
    }

    private boolean same(Object actual, Object expected) {
        if (actual instanceof Number number && expected instanceof Number other) {
            return Comparisons.compare(number, other) == 0;
        }
        return Objects.equals(folded(actual), folded(expected));
    }

    private boolean in(Object actual, Object candidates) {
        var values = candidates instanceof Collection<?> collection ? collection
                : List.of(ObjectUtils.toObjectArray(candidates));
        return values.stream().anyMatch(value -> same(actual, value));
    }

    private boolean like(Object actual, Object pattern) {
        if (!(actual instanceof String text)) {
            return false;
        }
        var regex = new StringBuilder();
        for (var character : pattern.toString().toCharArray()) {
            regex.append(switch (character) {
                case '%' -> ".*";
                case '_' -> ".";
                default -> Pattern.quote(String.valueOf(character));
            });
        }
        return Pattern.compile(regex.toString(), flags() | Pattern.DOTALL).matcher(text).matches();
    }

    private boolean contains(Object actual, Object expected) {
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(value -> same(value, expected));
        }
        return text(actual, expected, String::contains);
    }

    private boolean text(Object actual, Object expected, BiPredicate<String, String> test) {
        return actual instanceof String text && expected != null
                && test.test(folded(text).toString(), folded(expected).toString());
    }

    private Object folded(Object value) {
        return ignoreCase && value instanceof String text ? text.toLowerCase(Locale.ROOT) : value;
    }

    private int flags() {
        return ignoreCase ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0;
    }
}
