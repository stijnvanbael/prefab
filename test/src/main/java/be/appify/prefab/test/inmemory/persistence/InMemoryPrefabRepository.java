package be.appify.prefab.test.inmemory.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Base implementation of every in-memory repository: CRUD, paging and sorting, and Query by Example, which Spring
 * Data KeyValue lacks. Saving checks and increments {@code @Version}, see {@link InMemoryStore}.
 *
 * @param <T>
 *         the aggregate type
 * @param <ID>
 *         the identifier type
 */
public class InMemoryPrefabRepository<T, ID> implements CrudRepository<T, ID>, PagingAndSortingRepository<T, ID> {
    private final Class<T> type;
    private final InMemoryStore store;
    private final ExamplePredicate examples;

    /**
     * Constructs a new InMemoryPrefabRepository.
     *
     * @param type
     *         the aggregate type, the sealed root for a sealed aggregate
     * @param store
     *         the store that holds the aggregates
     * @param examples
     *         evaluates examples
     */
    public InMemoryPrefabRepository(Class<T> type, InMemoryStore store, ExamplePredicate examples) {
        this.type = type;
        this.store = store;
        this.examples = examples;
    }

    @Override
    public <S extends T> S save(S entity) {
        return store.save(entity);
    }

    @Override
    public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
        var saved = new ArrayList<S>();
        entities.forEach(entity -> saved.add(save(entity)));
        return saved;
    }

    @Override
    public Optional<T> findById(ID id) {
        return store.findById(id, type);
    }

    @Override
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }

    @Override
    public Iterable<T> findAll() {
        return store.findAll(type);
    }

    @Override
    public Iterable<T> findAllById(Iterable<ID> ids) {
        return StreamSupport.stream(ids.spliterator(), false)
                .flatMap(id -> findById(id).stream())
                .toList();
    }

    @Override
    public long count() {
        return store.count(type);
    }

    @Override
    public void deleteById(ID id) {
        store.deleteById(id, type);
    }

    @Override
    public void delete(T entity) {
        store.delete(entity);
    }

    @Override
    public void deleteAllById(Iterable<? extends ID> ids) {
        ids.forEach(this::deleteById);
    }

    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        store.deleteAll(type);
    }

    @Override
    public Iterable<T> findAll(Sort sort) {
        return sorted(store.findAll(type), sort);
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        return page(store.findAll(type), pageable);
    }

    /**
     * Finds the aggregates that match an example.
     *
     * @param example
     *         the example
     * @param pageable
     *         the page to return, may be unpaged
     * @param <S>
     *         the aggregate type
     * @return the page of matching aggregates
     */
    public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
        return page(matching(example), pageable);
    }

    /**
     * Finds the aggregates that match an example.
     *
     * @param example
     *         the example
     * @param sort
     *         the order of the result
     * @param <S>
     *         the aggregate type
     * @return the matching aggregates
     */
    public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
        return sorted(matching(example), sort);
    }

    /**
     * Finds the aggregates that match an example.
     *
     * @param example
     *         the example
     * @param <S>
     *         the aggregate type
     * @return the matching aggregates, in no particular order
     */
    public <S extends T> List<S> findAll(Example<S> example) {
        return matching(example);
    }

    /**
     * Counts the aggregates that match an example.
     *
     * @param example
     *         the example
     * @param <S>
     *         the aggregate type
     * @return the number of matching aggregates
     */
    public <S extends T> long count(Example<S> example) {
        return matching(example).size();
    }

    /**
     * Whether any aggregate matches an example.
     *
     * @param example
     *         the example
     * @param <S>
     *         the aggregate type
     * @return {@code true} if at least one aggregate matches
     */
    public <S extends T> boolean exists(Example<S> example) {
        return !matching(example).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private <S extends T> List<S> matching(Example<S> example) {
        var predicate = examples.of(example);
        return store.findAll(type).stream()
                .filter(predicate)
                .filter(candidate -> example.getProbeType().isInstance(candidate))
                .map(candidate -> (S) candidate)
                .toList();
    }

    private <S> List<S> sorted(List<S> rows, Sort sort) {
        return Pages.of(rows, Pageable.unpaged(sort), store.properties().comparator(sort)).getContent();
    }

    private <S> Page<S> page(List<S> rows, Pageable pageable) {
        return Pages.of(rows, pageable, store.properties().comparator(pageable.getSort()));
    }
}
