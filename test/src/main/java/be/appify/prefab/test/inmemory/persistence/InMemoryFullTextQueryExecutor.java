package be.appify.prefab.test.inmemory.persistence;

import be.appify.prefab.core.spring.data.FullTextCondition;
import be.appify.prefab.core.spring.data.FullTextQueryExecutor;
import be.appify.prefab.core.spring.data.RangeCondition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * {@link FullTextQueryExecutor} for the in-memory repositories. Combines the example and range conditions with a
 * case insensitive word search on the searched properties. Matching and relevance ordering approximate PostgreSQL
 * text search, see {@link TextSearch}. The text search language of the condition is ignored: there is no stemming.
 * Results are ordered by relevance, highest first, when the sort asks for {@link FullTextCondition#RELEVANCE} or is
 * empty; other sort orders are applied after it.
 */
public class InMemoryFullTextQueryExecutor implements FullTextQueryExecutor {
    private final InMemoryStore store;
    private final InMemoryRangeQueryExecutor ranges;

    /**
     * Constructs a new InMemoryFullTextQueryExecutor.
     *
     * @param store
     *         the store that holds the aggregates
     * @param ranges
     *         selects the aggregates that match the example and the range conditions
     */
    public InMemoryFullTextQueryExecutor(InMemoryStore store, InMemoryRangeQueryExecutor ranges) {
        this.store = store;
        this.ranges = ranges;
    }

    @Override
    public <T> Page<T> findAll(
            Example<T> example,
            FullTextCondition fullText,
            List<RangeCondition> rangeConditions,
            Pageable pageable
    ) {
        var search = new TextSearch(fullText);
        var scored = new ArrayList<Scored<T>>();
        for (var candidate : ranges.matching(example, rangeConditions)) {
            search.score(valuesOf(candidate, fullText))
                    .ifPresent(score -> scored.add(new Scored<>(candidate, score)));
        }
        var sort = FullTextCondition.withoutRelevance(pageable.getSort());
        Comparator<Scored<T>> bySort = Comparator.comparing(Scored::row, store.properties().comparator(sort));
        Comparator<Scored<T>> order = fullText.ranksByRelevance(pageable.getSort())
                ? Comparator.<Scored<T>>comparingDouble(Scored::score).reversed().thenComparing(bySort)
                : bySort;
        var page = Pages.of(scored, pageable, order);
        return page.map(Scored::row);
    }

    private <T> TextSearch.Values valuesOf(T candidate, FullTextCondition fullText) {
        var texts = fullText.properties().stream()
                .map(property -> new TextSearch.PropertyText(property, textsOf(candidate, property)))
                .toList();
        return new TextSearch.Values(texts, fullText);
    }

    private List<String> textsOf(Object candidate, String property) {
        return store.properties().read(candidate, property)
                .map(value -> value instanceof Collection<?> collection
                        ? collection.stream().map(String::valueOf).toList()
                        : List.of(value.toString()))
                .orElse(List.of());
    }

    private record Scored<T>(T row, double score) {
    }
}
