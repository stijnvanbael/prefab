package be.appify.prefab.test.inmemory.persistence;

import be.appify.prefab.core.spring.data.RangeCondition;
import be.appify.prefab.core.spring.data.RangeQueryExecutor;
import java.util.List;
import java.util.function.Predicate;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * {@link RangeQueryExecutor} for the in-memory repositories. Compares {@link Comparable} properties, and numbers of
 * different types by value. Aggregates without a value for a range property never match.
 */
public class InMemoryRangeQueryExecutor implements RangeQueryExecutor {
    private final InMemoryStore store;
    private final ExamplePredicate examples;

    /**
     * Constructs a new InMemoryRangeQueryExecutor.
     *
     * @param store
     *         the store that holds the aggregates
     * @param examples
     *         evaluates examples
     */
    public InMemoryRangeQueryExecutor(InMemoryStore store, ExamplePredicate examples) {
        this.store = store;
        this.examples = examples;
    }

    @Override
    public <T> Page<T> findAll(Example<T> example, List<RangeCondition> ranges, Pageable pageable) {
        var rows = matching(example, ranges);
        return Pages.of(rows, pageable, store.properties().comparator(pageable.getSort()));
    }

    <T> List<T> matching(Example<T> example, List<RangeCondition> ranges) {
        var matchesExample = examples.of(example);
        var matchesRanges = ranges.stream().filter(RangeCondition::isActive).map(this::matches)
                .reduce(Predicate::and).orElse(candidate -> true);
        return store.findAll(example.getProbeType()).stream()
                .filter(matchesExample)
                .filter(matchesRanges)
                .toList();
    }

    private Predicate<Object> matches(RangeCondition condition) {
        return candidate -> store.properties().read(candidate, condition.property())
                .map(actual -> satisfies(condition, Comparisons.compare(actual, condition.value())))
                .orElse(false);
    }

    private static boolean satisfies(RangeCondition condition, int comparison) {
        return switch (condition.operator()) {
            case GREATER_THAN -> comparison > 0;
            case GREATER_THAN_OR_EQUAL -> comparison >= 0;
            case LESS_THAN -> comparison < 0;
            case LESS_THAN_OR_EQUAL -> comparison <= 0;
            default -> throw new IllegalArgumentException("Not a range operator: " + condition.operator());
        };
    }
}
