package be.appify.prefab.core.spring.data;

import java.util.List;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Executes a Query by Example combined with range conditions. Implemented by the persistence modules because
 * Spring Data repositories cannot express range comparisons in an {@link Example}.
 */
public interface RangeQueryExecutor {

    /**
     * Finds all entities matching the example and all active range conditions.
     *
     * @param example
     *         the example to match
     * @param ranges
     *         the range conditions; conditions without a value are ignored
     * @param pageable
     *         the paging and sorting information
     * @param <T>
     *         the entity type
     * @return a page of matching entities
     */
    <T> Page<T> findAll(Example<T> example, List<RangeCondition> ranges, Pageable pageable);
}
