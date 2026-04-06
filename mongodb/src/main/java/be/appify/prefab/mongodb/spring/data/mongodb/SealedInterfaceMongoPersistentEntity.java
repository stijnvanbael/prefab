package be.appify.prefab.mongodb.spring.data.mongodb;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mongodb.core.mapping.BasicMongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.core.TypeInformation;

/**
 * A {@link org.springframework.data.mongodb.core.mapping.MongoPersistentEntity} for sealed interfaces annotated
 * with {@link be.appify.prefab.core.annotations.Aggregate}.
 *
 * <p>Spring Data MongoDB cannot natively map a sealed interface to a collection because interfaces have no
 * declared fields. This class bridges the gap by:</p>
 * <ul>
 *   <li>Deriving the collection name from the sealed interface's simple name (e.g. {@code "shape"} for
 *       {@code Shape}), consistent with the naming used by the annotation processor's DB migration.</li>
 *   <li>Delegating the {@code id} property lookup to the first permitted record subtype so that
 *       {@code findById}, {@code deleteById}, and other id-based operations work correctly.</li>
 * </ul>
 *
 * <p>Concrete subtype instantiation is handled by Spring Data MongoDB's native {@code _class} discriminator
 * written by {@code MappingMongoConverter}; this entity is only used for collection resolution and id access.</p>
 *
 * @param <T>
 *         the sealed interface type
 */
class SealedInterfaceMongoPersistentEntity<T> extends BasicMongoPersistentEntity<T> {

    private static final Logger log = LoggerFactory.getLogger(SealedInterfaceMongoPersistentEntity.class);

    private final MongoMappingContext mappingContext;

    SealedInterfaceMongoPersistentEntity(TypeInformation<T> information, MongoMappingContext mappingContext) {
        super(information);
        this.mappingContext = mappingContext;
    }

    /**
     * Returns the {@code @Id} property from the first permitted subtype. All subtypes share the same {@code id}
     * field (enforced by the annotation processor), so delegating to the first subtype is safe.
     *
     * @return the id property, or {@code null} if no subtype or no id property is found
     */
    @Override
    public @Nullable MongoPersistentProperty getIdProperty() {
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
    public MongoPersistentProperty getRequiredIdProperty() {
        MongoPersistentProperty prop = getIdProperty();
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
            var concreteEntity = (org.springframework.data.mongodb.core.mapping.MongoPersistentEntity<Object>)
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
}
