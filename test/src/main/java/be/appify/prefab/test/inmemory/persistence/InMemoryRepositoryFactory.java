package be.appify.prefab.test.inmemory.persistence;

import java.util.Optional;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractEntityInformation;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.ValueExpressionDelegate;

/**
 * Creates the in-memory repositories: {@link InMemoryPrefabRepository} as implementation and the query handling of
 * {@link InMemoryQueryLookupStrategy}. Deliberately not listed in a {@code spring.factories}: that would make Spring
 * Data believe a second store module is present and switch JDBC and MongoDB repositories to strict mode.
 */
final class InMemoryRepositoryFactory extends RepositoryFactorySupport {
    private final InMemoryStore store;
    private final ExamplePredicate examples;

    InMemoryRepositoryFactory(InMemoryStore store, ExamplePredicate examples) {
        this.store = store;
        this.examples = examples;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
        return new AggregateInformation<>(domainClass, store);
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
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(Key key, ValueExpressionDelegate delegate) {
        return Optional.of(new InMemoryQueryLookupStrategy(store));
    }

    private static final class AggregateInformation<T, ID> extends AbstractEntityInformation<T, ID> {
        private final InMemoryStore store;

        private AggregateInformation(Class<T> domainClass, InMemoryStore store) {
            super(domainClass);
            this.store = store;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ID getId(T entity) {
            return (ID) store.properties().idOf(entity).orElse(null);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<ID> getIdType() {
            return (Class<ID>) Object.class;
        }
    }
}
