package be.appify.prefab.mongodb.spring.data.mongodb;

import be.appify.prefab.core.spring.data.RangeCondition;
import be.appify.prefab.core.spring.data.RangeQueryExecutor;
import java.util.List;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

/**
 * {@link RangeQueryExecutor} for Spring Data MongoDB. Combines the example criteria with range criteria on the
 * same {@link Query}.
 */
public class MongoRangeQueryExecutor implements RangeQueryExecutor {

    private final MongoOperations operations;

    /**
     * Constructs a new MongoRangeQueryExecutor.
     *
     * @param operations
     *         the MongoDB operations used to execute the query
     */
    public MongoRangeQueryExecutor(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public <T> Page<T> findAll(Example<T> example, List<RangeCondition> ranges, Pageable pageable) {
        var type = example.getProbeType();
        var query = new Query(new Criteria().alike(example));
        ranges.stream()
                .filter(RangeCondition::isActive)
                .map(MongoRangeQueryExecutor::criteriaOf)
                .forEach(query::addCriteria);
        var results = operations.find(Query.of(query).with(pageable), type);
        return PageableExecutionUtils.getPage(results, pageable,
                () -> operations.count(Query.of(query).limit(-1).skip(-1), type));
    }

    private static Criteria criteriaOf(RangeCondition condition) {
        var criteria = Criteria.where(condition.property());
        return switch (condition.operator()) {
            case GREATER_THAN -> criteria.gt(condition.value());
            case GREATER_THAN_OR_EQUAL -> criteria.gte(condition.value());
            case LESS_THAN -> criteria.lt(condition.value());
            case LESS_THAN_OR_EQUAL -> criteria.lte(condition.value());
            default -> throw new IllegalArgumentException("Not a range operator: " + condition.operator());
        };
    }
}
