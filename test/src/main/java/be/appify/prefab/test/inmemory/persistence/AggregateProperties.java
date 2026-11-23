package be.appify.prefab.test.inmemory.persistence;

import be.appify.prefab.core.service.Reference;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.InstantiationAwarePropertyAccessor;
import org.springframework.util.ClassUtils;
import org.springframework.util.NumberUtils;

/**
 * Reads and rewrites properties of aggregates through the Spring Data mapping context, so that records and classes
 * are treated the same way as by the real repositories.
 */
public final class AggregateProperties {
    private static final EntityInstantiators INSTANTIATORS = new EntityInstantiators();

    private final MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>>
            mappingContext;

    /**
     * Constructs a new AggregateProperties.
     *
     * @param mappingContext
     *         the mapping context that describes the aggregates
     */
    public AggregateProperties(
            MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>>
                    mappingContext
    ) {
        this.mappingContext = mappingContext;
    }

    /**
     * Reads a property, possibly nested with dots.
     *
     * @param bean
     *         the object to read from
     * @param path
     *         the property name, or a dotted path to a nested property
     * @return the value, empty when the property does not exist or is {@code null}
     */
    public Optional<Object> read(Object bean, String path) {
        Optional<Object> current = Optional.of(bean);
        for (var segment : path.split("\\.")) {
            current = current.flatMap(value -> readSegment(value, segment));
        }
        return current;
    }

    private Optional<Object> readSegment(Object bean, String name) {
        return Optional.ofNullable(mappingContext.getPersistentEntity(bean.getClass()))
                .flatMap(entity -> Optional.ofNullable(entity.getPersistentProperty(name))
                        .map(property -> entity.getPropertyAccessor(bean).getProperty(property)));
    }

    /**
     * The identifier of an aggregate.
     *
     * @param bean
     *         the aggregate
     * @return the identifier, empty when it is not assigned
     */
    public Optional<Object> idOf(Object bean) {
        return Optional.ofNullable(entityOf(bean).getIdentifierAccessor(bean).getIdentifier());
    }

    /**
     * Describes the {@code @Version} of an aggregate.
     *
     * @param bean
     *         the aggregate
     * @return the version, empty when the aggregate has no version property
     */
    public Optional<Version> versionOf(Object bean) {
        var entity = entityOf(bean);
        return Optional.ofNullable(entity.getVersionProperty()).map(property -> {
            var value = entity.getPropertyAccessor(bean).getProperty(property);
            return value instanceof Number number
                    ? new Version(property.getType(), true, number.longValue())
                    : new Version(property.getType(), false, 0);
        });
    }

    /**
     * Copies an aggregate with another version.
     *
     * @param bean
     *         the aggregate
     * @param version
     *         the new version
     * @param <T>
     *         the aggregate type
     * @return the copy, which is the same instance for a mutable aggregate
     */
    public <T> T withVersion(T bean, long version) {
        @SuppressWarnings("unchecked")
        var entity = (PersistentEntity<T, ?>) entityOf(bean);
        var property = entity.getRequiredVersionProperty();
        PersistentPropertyAccessor<T> accessor = new InstantiationAwarePropertyAccessor<>(
                bean, entity::getPropertyAccessor, INSTANTIATORS);
        var type = ClassUtils.resolvePrimitiveIfNecessary(property.getType()).asSubclass(Number.class);
        accessor.setProperty(property, NumberUtils.convertNumberToTargetClass(version, type));
        return accessor.getBean();
    }

    /**
     * A comparator for the given sort. Null values sort last when ascending and first when descending, as in
     * PostgreSQL, unless the order says otherwise.
     *
     * @param sort
     *         the sort, may be unsorted
     * @return the comparator, which treats all aggregates as equal for an unsorted sort
     */
    public Comparator<Object> comparator(Sort sort) {
        return sort.stream()
                .map(this::comparatorOf)
                .reduce(Comparator::thenComparing)
                .orElse((left, right) -> 0);
    }

    private Comparator<Object> comparatorOf(Sort.Order order) {
        Comparator<Object> values = Comparator.comparing(
                value -> (Comparable<Object>) sortKey(value, order.isIgnoreCase()));
        var nullsFirst = switch (order.getNullHandling()) {
            case NULLS_FIRST -> true;
            case NULLS_LAST -> false;
            case NATIVE -> order.isDescending();
        };
        Comparator<Optional<Object>> optional = nullsFirst
                ? Comparator.comparing(value -> value.orElse(null), Comparator.nullsFirst(values))
                : Comparator.comparing(value -> value.orElse(null), Comparator.nullsLast(values));
        Comparator<Object> byProperty = (left, right) ->
                optional.compare(read(left, order.getProperty()), read(right, order.getProperty()));
        return order.isDescending() ? byProperty.reversed() : byProperty;
    }

    private static Object sortKey(Object value, boolean ignoreCase) {
        return switch (value) {
            case Reference<?> reference -> sortKey(reference.id(), ignoreCase);
            case String text -> ignoreCase ? text.toLowerCase() : text;
            case Comparable<?> comparable -> comparable;
            default -> value.toString();
        };
    }

    private PersistentEntity<?, ?> entityOf(Object bean) {
        return mappingContext.getRequiredPersistentEntity(bean.getClass());
    }

    /**
     * A version of an aggregate.
     *
     * @param type
     *         the declared type of the version property
     * @param assigned
     *         whether the aggregate has a version value
     * @param value
     *         the current value, only meaningful when assigned
     */
    public record Version(Class<?> type, boolean assigned, long value) {

        /**
         * Whether the aggregate was never saved: no version, or {@code 0} for a primitive.
         *
         * @return {@code true} for a new aggregate
         */
        public boolean isNew() {
            return !assigned || type.isPrimitive() && value == 0;
        }

        /**
         * The version of an aggregate when it is first saved: {@code 1} for a primitive, as {@code 0} means new, and
         * {@code 0} for a wrapper.
         *
         * @return the initial version
         */
        public long initial() {
            return type.isPrimitive() ? 1 : 0;
        }
    }
}
