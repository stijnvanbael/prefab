package be.appify.prefab.test.inmemory.persistence;

import java.util.Optional;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.repository.query.PredicateQueryCreator;
import org.springframework.data.keyvalue.repository.support.KeyValueRepositoryFactory;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.ValueExpressionDelegate;

/**
 * Creates the in-memory repositories: {@link InMemoryPrefabRepository} as implementation and the query handling of
 * {@link InMemoryQueryLookupStrategy}.
 */
final class InMemoryRepositoryFactory extends KeyValueRepositoryFactory {
    private final KeyValueOperations operations;
    private final InMemoryStore store;
    private final ExamplePredicate examples;

    InMemoryRepositoryFactory(KeyValueOperations operations, InMemoryStore store, ExamplePredicate examples) {
        super(operations, PredicateQueryCreator.class);
        this.operations = operations;
        this.store = store;
        this.examples = examples;
    }

    @Override
    protected Object getTargetRepository(RepositoryInformation information) {
        return new InMemoryPrefabRepository<>(information.getDomainType(), store, examples);
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return InMemoryPrefabRepository.class;
    }

    @Override
    protected RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {
        return RepositoryFragments.empty();
    }

    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(Key key, ValueExpressionDelegate delegate) {
        return Optional.of(new InMemoryQueryLookupStrategy(delegate, operations, store));
    }
}
