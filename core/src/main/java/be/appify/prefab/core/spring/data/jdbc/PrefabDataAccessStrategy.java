package be.appify.prefab.core.spring.data.jdbc;

import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.DelegatingDataAccessStrategy;
import org.springframework.data.jdbc.core.convert.Identifier;
import org.springframework.data.jdbc.core.convert.InsertSubject;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.relational.core.conversion.IdValueSource;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

/**
 * Custom {@link DataAccessStrategy} that intercepts child-entity delete and insert operations to avoid unnecessary
 * deletes and re-inserts when the child collections have not changed. It works together with
 * {@link PrefabJdbcAggregateTemplate}, which sets a thread-local with the set of child types whose collections are
 * unchanged before delegating to the default save pipeline.
 * <p>
 * When the thread-local is populated, any attempt to delete or insert an entity whose type is in the skip set is
 * silently ignored — the existing child rows in the database are already correct.
 */
public class PrefabDataAccessStrategy extends DelegatingDataAccessStrategy {

    /**
     * Thread-local set of child entity types whose collections are unchanged and should therefore not be
     * deleted/re-inserted during the current save operation. Populated and cleared by
     * {@link PrefabJdbcAggregateTemplate#save(Object)}.
     */
    static final ThreadLocal<Set<Class<?>>> SKIP_CHILD_TYPES = new ThreadLocal<>();

    /**
     * Constructs a new PrefabDataAccessStrategy wrapping the given delegate.
     *
     * @param delegate
     *         the underlying {@link DataAccessStrategy} to delegate to
     */
    public PrefabDataAccessStrategy(DataAccessStrategy delegate) {
        super(delegate);
    }

    @Override
    public <T> @Nullable Object insert(T instance, Class<T> domainType, Identifier identifier,
            IdValueSource idValueSource) {
        if (shouldSkip(domainType)) {
            // Return null intentionally: the child rows already exist in the database because the collection
            // was determined to be unchanged. Skipping the insert is safe because no new ID needs to be generated
            // for child entities without an explicit @Id column.
            return null;
        }
        return super.insert(instance, domainType, identifier, idValueSource);
    }

    @Override
    public <T> @Nullable Object[] insert(List<InsertSubject<T>> insertSubjects, Class<T> domainType,
            IdValueSource idValueSource) {
        if (shouldSkip(domainType)) {
            // Return an array of nulls of the correct length intentionally: the child rows already exist in the
            // database. A null per-element signals that no generated ID was produced, which is safe for child
            // entities without an explicit @Id column.
            return new Object[insertSubjects.size()];
        }
        return super.insert(insertSubjects, domainType, idValueSource);
    }

    @Override
    public void delete(Object rootId, PersistentPropertyPath<RelationalPersistentProperty> propertyPath) {
        if (shouldSkipPath(propertyPath)) {
            return;
        }
        super.delete(rootId, propertyPath);
    }

    @Override
    public void delete(Iterable<Object> rootIds, PersistentPropertyPath<RelationalPersistentProperty> propertyPath) {
        if (shouldSkipPath(propertyPath)) {
            return;
        }
        super.delete(rootIds, propertyPath);
    }

    private boolean shouldSkip(Class<?> domainType) {
        Set<Class<?>> skip = SKIP_CHILD_TYPES.get();
        return skip != null && skip.contains(domainType);
    }

    private boolean shouldSkipPath(PersistentPropertyPath<RelationalPersistentProperty> propertyPath) {
        Set<Class<?>> skip = SKIP_CHILD_TYPES.get();
        if (skip == null || propertyPath.isEmpty()) {
            return false;
        }
        Class<?> leafType = propertyPath.getLeafProperty().getActualType();
        return skip.contains(leafType);
    }
}
