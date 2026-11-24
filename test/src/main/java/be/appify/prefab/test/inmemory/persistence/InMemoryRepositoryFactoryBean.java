package be.appify.prefab.test.inmemory.persistence;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * Creates one in-memory repository. Registered for every repository interface by
 * {@link InMemoryRepositoriesRegistrar}.
 *
 * @param <T>
 *         the repository type
 * @param <S>
 *         the aggregate type
 * @param <ID>
 *         the identifier type
 */
public class InMemoryRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
        extends RepositoryFactoryBeanSupport<T, S, ID> {
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
    protected RepositoryFactorySupport createRepositoryFactory() {
        return new InMemoryRepositoryFactory(store, examples);
    }
}
