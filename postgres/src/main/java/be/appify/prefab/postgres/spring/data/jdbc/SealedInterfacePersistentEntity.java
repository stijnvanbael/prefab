package be.appify.prefab.postgres.spring.data.jdbc;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
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
 * {@link be.appify.prefab.core.spring.data.jdbc.PolymorphicReadingConverter}; this entity is only used for SQL generation (table name, WHERE clause).</p>
 *
 * @param <T>
 *         the sealed interface type
 */
class SealedInterfacePersistentEntity<T> extends PrefabPersistentEntity<T> {

    private static final Logger log = LoggerFactory.getLogger(SealedInterfacePersistentEntity.class);

    /**
     * Helper record used solely to obtain a {@link java.lang.reflect.Field} for the {@code type} discriminator column
     * via reflection. Package-private so Spring Data's {@link TypeInformation} can introspect it, allowing
     * {@link org.springframework.data.mapping.model.AbstractPersistentProperty} to resolve the {@code type} property.
     */
    record TypeDiscriminatorRecord(String type) {}

    private final PrefabJdbcMappingContext mappingContext;
    private final NamingStrategy namingStrategy;
    private final SqlIdentifierExpressionEvaluator sqlIdentifierExpressionEvaluator;

    SealedInterfacePersistentEntity(
            TypeInformation<T> information,
            NamingStrategy namingStrategy,
            SqlIdentifierExpressionEvaluator sqlIdentifierExpressionEvaluator,
            PrefabJdbcMappingContext mappingContext
    ) {
        super(information, namingStrategy, sqlIdentifierExpressionEvaluator);
        this.namingStrategy = namingStrategy;
        this.sqlIdentifierExpressionEvaluator = sqlIdentifierExpressionEvaluator;
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
            log.debug("Could not resolve id property for subtype {} of {}: {}",
                    subtypes[0].getName(), getType().getName(), e.getMessage());
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
     * Returns {@code true} when at least one permitted subtype has an id property. Overriding this is critical:
     * {@code BasicPersistentEntity.hasIdProperty()} checks an internal field that is only populated during property
     * registration, which never happens for a sealed interface. Without this override,
     * {@code AggregatePath.TableInfo.computeIdColumnInfos()} exits early with an empty result, causing
     * "Identifier columns must not be empty" in {@code SqlGenerator}.
     */
    @Override
    public boolean hasIdProperty() {
        return getIdProperty() != null;
    }

    /**
     * Delegates property iteration to all permitted subtypes and also yields a synthetic {@code type} property so that
     * the discriminator column is included in Spring Data JDBC's generated SELECT queries.
     *
     * <p>Properties are deduplicated by name; the first subtype that declares a given property wins.</p>
     */
    @Override
    public void doWithProperties(PropertyHandler<RelationalPersistentProperty> handler) {
        collectSubtypeProperties().values().forEach(handler::doWithPersistentProperty);
        handler.doWithPersistentProperty(createTypeDiscriminatorProperty());
    }

    /**
     * Extends the default property iterable to include id properties from all permitted subtypes when filtering by
     * {@link Id}. This is a secondary safety net for code paths that call {@code getPersistentProperties} directly.
     */
    @Override
    public Iterable<RelationalPersistentProperty> getPersistentProperties(Class<? extends Annotation> annotationType) {
        Iterable<RelationalPersistentProperty> own = super.getPersistentProperties(annotationType);
        if (annotationType == Id.class) {
            RelationalPersistentProperty idProp = getIdProperty();
            if (idProp != null) {
                return () -> Stream.concat(
                        java.util.stream.StreamSupport.stream(own.spliterator(), false),
                        Stream.of(idProp)
                ).distinct().iterator();
            }
        }
        return own;
    }

    private LinkedHashMap<String, RelationalPersistentProperty> collectSubtypeProperties() {
        var properties = new LinkedHashMap<String, RelationalPersistentProperty>();
        for (Class<?> subtype : getType().getPermittedSubclasses()) {
            try {
                mappingContext.getRequiredPersistentEntity(subtype)
                        .doWithProperties((PropertyHandler<RelationalPersistentProperty>) property ->
                                properties.putIfAbsent(property.getName(), property));
            } catch (Exception e) {
                log.debug("Could not collect properties from subtype {} of {}: {}",
                        subtype.getName(), getType().getName(), e.getMessage());
            }
        }
        return properties;
    }

    /**
     * Resolves a persistent property by name by delegating to the first permitted subtype. Since the sealed
     * interface declares no fields itself, property lookup would otherwise always return {@code null}, causing
     * Spring Data's query-method derivation (e.g. {@code findByQuiz}) to fail with a
     * {@code PropertyReferenceException}.
     *
     * @param name
     *         the property name to look up
     * @return the property from the first permitted subtype, or {@code null} if not found
     */
    @Override
    public @Nullable RelationalPersistentProperty getPersistentProperty(String name) {
        RelationalPersistentProperty ownProperty = super.getPersistentProperty(name);
        if (ownProperty != null) {
            return ownProperty;
        }
        Class<?>[] subtypes = getType().getPermittedSubclasses();
        if (subtypes.length == 0) {
            return null;
        }
        try {
            return mappingContext.getRequiredPersistentEntity(subtypes[0]).getPersistentProperty(name);
        } catch (Exception e) {
            log.debug("Could not resolve property '{}' for subtype {} of {}: {}",
                    name, subtypes[0].getName(), getType().getName(), e.getMessage());
            return null;
        }
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
            log.debug("Could not delegate isNew() to concrete entity for {}, falling back to id-property check: {}",
                    bean.getClass().getName(), e.getMessage());
            var idProp = getIdProperty();
            if (idProp == null) {
                return true;
            }
            return getPropertyAccessor((T) bean).getProperty(idProp) == null;
        }
    }

    private RelationalPersistentProperty createTypeDiscriminatorProperty() {
        try {
            var field = TypeDiscriminatorRecord.class.getDeclaredField("type");
            var ownerTypeInfo = TypeInformation.of(TypeDiscriminatorRecord.class);
            var typeDiscriminatorOwner = new PrefabPersistentEntity<>(ownerTypeInfo, namingStrategy, sqlIdentifierExpressionEvaluator);
            var property = Property.of(ownerTypeInfo, field);
            return new TypeDiscriminatorPersistentProperty(property, typeDiscriminatorOwner);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Cannot create synthetic type discriminator property", e);
        }
    }

    /**
     * A {@link PrefabJdbcPersistentProperty} for the synthetic {@code type} discriminator column.
     *
     * <p>The super-constructor receives a {@link TypeDiscriminatorRecord} owner so that
     * {@link org.springframework.data.mapping.model.AbstractPersistentProperty} can resolve {@code type} via
     * {@link TypeInformation}. {@link #getOwner()} is then overridden to return the enclosing
     * {@link SealedInterfacePersistentEntity}, ensuring that the SQL generator qualifies the {@code type} column
     * with the sealed interface's table (e.g. {@code "quiz"}) rather than the helper record's table.</p>
     */
    private class TypeDiscriminatorPersistentProperty extends PrefabJdbcPersistentProperty {

        TypeDiscriminatorPersistentProperty(
                Property property,
                PrefabPersistentEntity<TypeDiscriminatorRecord> typeDiscriminatorOwner
        ) {
            super(property, typeDiscriminatorOwner, SimpleTypeHolder.DEFAULT, namingStrategy);
        }

        @Override
        public RelationalPersistentEntity<?> getOwner() {
            return SealedInterfacePersistentEntity.this;
        }
    }
}
