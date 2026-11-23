package be.appify.prefab.test.inmemory.persistence;

import be.appify.prefab.core.service.Reference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.MatchMode;
import org.springframework.data.domain.ExampleMatcher.NullHandler;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.support.ExampleMatcherAccessor;

/**
 * Evaluates Query by Example in memory, which Spring Data KeyValue does not support. Understands the matchers the
 * generated list services use: exact, contains, starts with, ends with, regex, case insensitivity, ignored paths and
 * {@code matchingAll} / {@code matchingAny}.
 *
 * <p>Property values of an example are compared with {@link Object#equals(Object)}, except strings, which follow the
 * string matcher of their path. Nested records are compared property by property.</p>
 */
public final class ExamplePredicate {
    private final MappingContext<? extends KeyValuePersistentEntity<?, ?>, ? extends KeyValuePersistentProperty<?>>
            mappingContext;
    private final AggregateProperties properties;

    /**
     * Constructs a new ExamplePredicate factory.
     *
     * @param mappingContext
     *         the mapping context that describes the aggregates
     * @param properties
     *         reads aggregate properties
     */
    public ExamplePredicate(
            MappingContext<? extends KeyValuePersistentEntity<?, ?>, ? extends KeyValuePersistentProperty<?>>
                    mappingContext,
            AggregateProperties properties
    ) {
        this.mappingContext = mappingContext;
        this.properties = properties;
    }

    /**
     * Creates the predicate for an example.
     *
     * @param example
     *         the example
     * @return a predicate that accepts the aggregates that match the example
     */
    public Predicate<Object> of(Example<?> example) {
        var matcher = new ExampleMatcherAccessor(example.getMatcher());
        var criteria = new ArrayList<Criterion>();
        collect(example.getProbe(), "", matcher, criteria);
        var matchAll = example.getMatcher().getMatchMode() == MatchMode.ALL;
        return candidate -> matchAll
                ? criteria.stream().allMatch(criterion -> criterion.matches(candidate))
                : criteria.isEmpty() || criteria.stream().anyMatch(criterion -> criterion.matches(candidate));
    }

    private void collect(Object probe, String prefix, ExampleMatcherAccessor matcher, List<Criterion> criteria) {
        var entity = mappingContext.getRequiredPersistentEntity(probe.getClass());
        var accessor = entity.getPropertyAccessor(probe);
        entity.doWithProperties((SimplePropertyHandler) property -> {
            var path = prefix + property.getName();
            if (matcher.isIgnoredPath(path)) {
                return;
            }
            var value = accessor.getProperty(property);
            if (value == null) {
                if (matcher.getNullHandler() == NullHandler.INCLUDE) {
                    criteria.add(new Criterion(path, Optional.empty(), matcher));
                }
            } else if (isNested(value)) {
                collect(value, path + ".", matcher, criteria);
            } else {
                var transformed = matcher.getValueTransformerForPath(path).apply(Optional.of(value));
                criteria.add(new Criterion(path, transformed, matcher));
            }
        });
    }

    private static boolean isNested(Object value) {
        return value.getClass().isRecord() && !(value instanceof Reference<?>);
    }

    private final class Criterion {
        private final String path;
        private final Optional<Object> expected;
        private final ExampleMatcherAccessor matcher;

        private Criterion(String path, Optional<Object> expected, ExampleMatcherAccessor matcher) {
            this.path = path;
            this.expected = expected;
            this.matcher = matcher;
        }

        boolean matches(Object candidate) {
            var actual = properties.read(candidate, path);
            if (expected.isEmpty()) {
                return actual.isEmpty();
            }
            return actual.map(value -> expected.get() instanceof String text && value instanceof String other
                    ? matchesText(text, other)
                    : Objects.equals(expected.get(), value)).orElse(false);
        }

        private boolean matchesText(String expectedText, String actualText) {
            var ignoreCase = matcher.isIgnoreCaseForPath(path);
            var wanted = ignoreCase ? expectedText.toLowerCase(Locale.ROOT) : expectedText;
            var actual = ignoreCase ? actualText.toLowerCase(Locale.ROOT) : actualText;
            return switch (stringMatcher()) {
                case DEFAULT, EXACT -> actual.equals(wanted);
                case STARTING -> actual.startsWith(wanted);
                case ENDING -> actual.endsWith(wanted);
                case CONTAINING -> actual.contains(wanted);
                case REGEX -> Pattern.compile(expectedText, ignoreCase ? Pattern.CASE_INSENSITIVE : 0)
                        .matcher(actualText).find();
            };
        }

        private StringMatcher stringMatcher() {
            return matcher.getStringMatcherForPath(path);
        }
    }
}
