package be.appify.prefab.test.inmemory.persistence;

import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.repository.support.KeyValueRepositoryFactory;
import org.springframework.data.keyvalue.repository.support.KeyValueRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;

/**
 * {@link KeyValueRepositoryFactoryBean} that creates {@link InMemoryRepositoryFactory}. Registered for every
 * repository interface by {@link InMemoryRepositoriesRegistrar}.
 *
 * @param <T>
 *         the repository type
 * @param <S>
 *         the aggregate type
 * @param <ID>
 *         the identifier type
 */
public class InMemoryRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
        extends KeyValueRepositoryFactoryBean<T, S, ID> {
    private InMemoryStore store;
    private ExamplePredicate examples;

    /**
     * Constructs a new InMemoryRepositoryFactoryBean.
     *
     * @param repositoryInterface
     *         the repository interface
     */
    public InMemoryRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    /**
     * Sets the store that holds the aggregates.
     *
     * @param store
     *         the store
     */
    public void setStore(InMemoryStore store) {
        this.store = store;
    }

    /**
     * Sets the evaluator of Query by Example.
     *
     * @param examples
     *         the evaluator
     */
    public void setExamples(ExamplePredicate examples) {
        this.examples = examples;
    }

    @Override
    protected KeyValueRepositoryFactory createRepositoryFactory(
            KeyValueOperations operations,
            Class<? extends AbstractQueryCreator<?, ?>> queryCreator,
            Class<? extends RepositoryQuery> repositoryQueryType
    ) {
        return new InMemoryRepositoryFactory(operations, store, examples);
    }
}
