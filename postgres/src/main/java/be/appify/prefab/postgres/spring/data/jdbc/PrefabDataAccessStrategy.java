package be.appify.prefab.postgres.spring.data.jdbc;

import be.appify.prefab.core.annotations.Aggregate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.DelegatingDataAccessStrategy;
import org.springframework.data.jdbc.core.convert.Identifier;
import org.springframework.data.jdbc.core.convert.InsertSubject;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.relational.core.conversion.IdValueSource;
import org.springframework.data.relational.core.dialect.Escaper;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;

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

    private final Escaper likeEscaper;

    /**
     * Constructs a new PrefabDataAccessStrategy wrapping the given delegate.
     *
     * @param delegate
     *         the underlying {@link DataAccessStrategy} to delegate to
     * @param likeEscaper
     *         the dialect's LIKE escaper, used to resolve {@code ValueFunction} query values eagerly
     */
    public PrefabDataAccessStrategy(DataAccessStrategy delegate, Escaper likeEscaper) {
        super(delegate);
        this.likeEscaper = likeEscaper;
    }

    @Override
    public <T> long count(Query query, Class<T> domainType) {
        return super.count(resolveValueFunctions(query), domainType);
    }

    @Override
    public <T> boolean exists(Query query, Class<T> domainType) {
        return super.exists(resolveValueFunctions(query), domainType);
    }

    @Override
    public <T> Optional<T> findOne(Query query, Class<T> domainType) {
        return super.findOne(resolveValueFunctions(query), domainType);
    }

    @Override
    public <T> Iterable<T> findAll(Query query, Class<T> domainType) {
        return super.findAll(resolveValueFunctions(query), domainType);
    }

    @Override
    public <T> Stream<T> streamAll(Query query, Class<T> domainType) {
        return super.streamAll(resolveValueFunctions(query), domainType);
    }

    @Override
    public <T> Iterable<T> findAll(Query query, Class<T> domainType, Pageable pageable) {
        return super.findAll(resolveValueFunctions(query), domainType, pageable);
    }

    /**
     * Rebuilds the given {@link Query} with any {@code ValueFunction} criteria value resolved eagerly.
     * <p>
     * Works around a Spring Data JDBC 4.1.0 regression (spring-projects/spring-data-relational#2317): the
     * {@code contains}/{@code startsWith}/{@code endsWith} {@code ExampleMatcher}s and derived-query keywords
     * produce a lazily-evaluated {@code ValueFunction} value that is otherwise bound to the JDBC statement
     * unresolved, causing the query to compare against the lambda's {@code toString()} instead of the intended
     * escaped LIKE pattern.
     */
    private Query resolveValueFunctions(Query query) {
        var criteria = query.getCriteria()
                .map(c -> ValueFunctionResolvingCriteria.wrap(c, likeEscaper));
        if (criteria.isEmpty()) {
            return query;
        }
        return Query.query(criteria.get())
                .columns(query.getColumns().toArray(new SqlIdentifier[0]))
                .sort(query.getSort())
                .offset(query.getOffset())
                .limit(query.getLimit());
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
        return super.insert(instance, domainType, withTypeDiscriminator(domainType, identifier), idValueSource);
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
        if (isPolymorphicSubtype(domainType)) {
            insertSubjects = insertSubjects.stream()
                    .map(s -> InsertSubject.describedBy(s.getInstance(),
                            withTypeDiscriminator(domainType, s.getIdentifier())))
                    .toList();
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

    /**
     * Returns {@code true} if the given domain type is a direct implementor of a sealed interface annotated with
     * {@link Aggregate}, i.e. it is a concrete subtype of a polymorphic aggregate root.
     */
    private static boolean isPolymorphicSubtype(Class<?> domainType) {
        return PrefabPersistentEntity.findDirectSealedAggregateInterface(domainType) != null;
    }

    /**
     * Returns a new {@link Identifier} that includes a {@code type} column set to the simple name of the concrete
     * domain type. This is used when inserting polymorphic aggregate subtypes so that the discriminator column is
     * populated correctly.
     */
    private static <T> Identifier withTypeDiscriminator(Class<T> domainType, Identifier identifier) {
        if (!isPolymorphicSubtype(domainType)) {
            return identifier;
        }
        return identifier.withPart(SqlIdentifier.quoted("type"), domainType.getSimpleName(), String.class);
    }
}
