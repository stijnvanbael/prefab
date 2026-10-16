package be.appify.prefab.core.spring.data;

import java.util.List;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Executes a Query by Example combined with a full-text condition and, optionally, range conditions. Implemented by
 * the persistence modules because Spring Data repositories cannot express full-text matching. The range conditions
 * are accepted here so that both kinds of filters can be combined in one query, which is why this executor is used
 * instead of {@link RangeQueryExecutor} as soon as a full-text filter is present.
 */
public interface FullTextQueryExecutor {

    /**
     * Finds all entities matching the example, the full-text condition and all active range conditions.
     *
     * @param example
     *         the example to match
     * @param fullText
     *         the full-text condition; ignored when it has no search text
     * @param ranges
     *         the range conditions; conditions without a value are ignored
     * @param pageable
     *         the paging and sorting information
     * @param <T>
     *         the entity type
     * @return a page of matching entities
     */
    <T> Page<T> findAll(Example<T> example, FullTextCondition fullText, List<RangeCondition> ranges, Pageable pageable);
}
