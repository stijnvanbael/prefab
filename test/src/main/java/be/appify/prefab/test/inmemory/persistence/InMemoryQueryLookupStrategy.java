package be.appify.prefab.test.inmemory.persistence;

import be.appify.prefab.core.annotations.rest.AutocompleteQuery;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.repository.query.KeyValuePartTreeQuery;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ValueExpressionDelegate;

/**
 * Decides how every query method of an in-memory repository is executed: generated autocomplete methods from their
 * runtime metadata, methods with a store specific query as a failure on invocation, and all other methods as derived
 * queries evaluated by Spring Data KeyValue.
 */
final class InMemoryQueryLookupStrategy implements QueryLookupStrategy {
    private static final Logger log = LoggerFactory.getLogger(InMemoryQueryLookupStrategy.class);

    private final ValueExpressionDelegate valueExpressions;
    private final KeyValueOperations operations;
    private final InMemoryStore store;

    InMemoryQueryLookupStrategy(
            ValueExpressionDelegate valueExpressions,
            KeyValueOperations operations,
            InMemoryStore store
    ) {
        this.valueExpressions = valueExpressions;
        this.operations = operations;
        this.store = store;
    }

    @Override
    public RepositoryQuery resolveQuery(
            Method method,
            RepositoryMetadata metadata,
            ProjectionFactory factory,
            NamedQueries namedQueries
    ) {
        var queryMethod = new QueryMethod(method, metadata, factory);
        var autocomplete = method.getAnnotation(AutocompleteQuery.class);
        if (autocomplete != null) {
            return new AutocompleteRepositoryQuery(queryMethod, autocomplete, store, metadata.getDomainType());
        }
        if (NativeQueryRepositoryQuery.isNative(method)) {
            var query = new NativeQueryRepositoryQuery(queryMethod, metadata.getRepositoryInterface(), method);
            log.warn("{} has a native query that cannot run in memory; calling it fails. "
                    + "Use @IntegrationTest(containers = true) for tests that call it.", query.description());
            return query;
        }
        return new KeyValuePartTreeQuery(queryMethod, valueExpressions, operations,
                ReferenceAwarePredicateQueryCreator.class);
    }
}
