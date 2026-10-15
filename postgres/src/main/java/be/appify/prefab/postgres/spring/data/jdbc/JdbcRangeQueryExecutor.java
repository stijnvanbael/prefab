package be.appify.prefab.postgres.spring.data.jdbc;

import be.appify.prefab.core.spring.data.RangeCondition;
import be.appify.prefab.core.spring.data.RangeQueryExecutor;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.repository.query.RelationalExampleMapper;

/**
 * {@link RangeQueryExecutor} for Spring Data JDBC. Spring Data JDBC repositories cannot take a {@link Criteria},
 * so the example is mapped to a {@link Query} and extended with the range criteria, which is then executed through
 * {@link JdbcAggregateOperations}.
 */
public class JdbcRangeQueryExecutor implements RangeQueryExecutor {

    private final JdbcAggregateOperations operations;
    private final RelationalExampleMapper exampleMapper;

    /**
     * Constructs a new JdbcRangeQueryExecutor.
     *
     * @param operations
     *         the JDBC aggregate operations used to execute the query
     * @param mappingContext
     *         the mapping context used to map the example to a query
     */
    public JdbcRangeQueryExecutor(
            JdbcAggregateOperations operations,
            MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext
    ) {
        this.operations = operations;
        this.exampleMapper = new RelationalExampleMapper(mappingContext);
    }

    @Override
    public <T> Page<T> findAll(Example<T> example, List<RangeCondition> ranges, Pageable pageable) {
        var criteria = Stream.concat(
                        exampleMapper.getMappedExample(example).getCriteria().stream(),
                        ranges.stream().filter(RangeCondition::isActive).map(JdbcRangeQueryExecutor::criteriaOf))
                .reduce(Criteria.empty(), (all, next) -> all.and(next), (left, right) -> left.and(right));
        return operations.findAll(Query.query(criteria), example.getProbeType(), pageable);
    }

    private static Criteria criteriaOf(RangeCondition condition) {
        var step = Criteria.where(condition.property());
        return switch (condition.operator()) {
            case GREATER_THAN -> step.greaterThan(condition.value());
            case GREATER_THAN_OR_EQUAL -> step.greaterThanOrEquals(condition.value());
            case LESS_THAN -> step.lessThan(condition.value());
            case LESS_THAN_OR_EQUAL -> step.lessThanOrEquals(condition.value());
            default -> throw new IllegalArgumentException("Not a range operator: " + condition.operator());
        };
    }
}
