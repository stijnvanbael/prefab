package be.appify.prefab.postgres.spring.data.jdbc;

import org.jspecify.annotations.Nullable;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

/**
 * A {@link org.springframework.data.relational.core.mapping.RelationalPersistentEntity} for sealed interfaces
 * annotated with {@link be.appify.prefab.core.annotations.Aggregate}.
 *
 * <p>Spring Data JDBC cannot natively map a sealed interface to a table because interfaces have no declared fields.
 * This class bridges the gap by:</p>
 * <ul>
 *   <li>Deriving the table name from the sealed interface's simple name via the naming strategy (same name used
 *       by the generated DB migration, e.g. {@code "shape"} for {@code Shape}).</li>
 *   <li>Delegating the {@code id} property to the first permitted record subtype's persistent entity so that
 *       {@code findById}, {@code deleteById}, and other id-based operations work correctly.</li>
 * </ul>
 *
 * <p>The concrete subtype instantiation is always handled by the registered
 * {@link PolymorphicReadingConverter}; this entity is only used for SQL generation (table name, WHERE clause).</p>
 *
 * @param <T>
 *         the sealed interface type
 */
class SealedInterfacePersistentEntity<T> extends PrefabPersistentEntity<T> {

    private final PrefabJdbcMappingContext mappingContext;

    SealedInterfacePersistentEntity(
            TypeInformation<T> information,
            NamingStrategy namingStrategy,
            SqlIdentifierExpressionEvaluator sqlIdentifierExpressionEvaluator,
            PrefabJdbcMappingContext mappingContext
    ) {
        super(information, namingStrategy, sqlIdentifierExpressionEvaluator);
        this.mappingContext = mappingContext;
    }

    /**
     * Returns the {@code id} property of the first permitted subtype. All subtypes must share the same {@code id}
     * field name and type (enforced by the annotation processor), so the first subtype's property is representative.
     *
     * @return the id property, or {@code null} if none can be found
     */
    @Override
    public @Nullable RelationalPersistentProperty getIdProperty() {
        Class<?>[] subtypes = getType().getPermittedSubclasses();
        if (subtypes.length == 0) {
            return null;
        }
        try {
            return mappingContext.getRequiredPersistentEntity(subtypes[0]).getIdProperty();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public RelationalPersistentProperty getRequiredIdProperty() {
        RelationalPersistentProperty prop = getIdProperty();
        if (prop == null) {
            throw new MappingException(
                    "No id property found for polymorphic aggregate " + getType().getName()
                            + ". Ensure all permitted subtypes declare an @Id field.");
        }
        return prop;
    }

    /**
     * Checks whether the given instance is new (has a {@code null} id). Delegates to the concrete subtype's
     * persistent entity so the id accessor works against the actual runtime type.
     *
     * @param bean
     *         an instance of the sealed interface (actually a concrete subtype at runtime)
     * @return {@code true} if the id is {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean isNew(Object bean) {
        try {
            var concreteEntity = (org.springframework.data.relational.core.mapping.RelationalPersistentEntity<Object>)
                    mappingContext.getRequiredPersistentEntity(bean.getClass());
            return concreteEntity.isNew(bean);
        } catch (Exception e) {
            var idProp = getIdProperty();
            if (idProp == null) {
                return true;
            }
            return getPropertyAccessor((T) bean).getProperty(idProp) == null;
        }
    }
}
