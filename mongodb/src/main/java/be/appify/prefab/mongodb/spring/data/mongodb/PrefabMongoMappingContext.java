package be.appify.prefab.mongodb.spring.data.mongodb;

import be.appify.prefab.core.annotations.Aggregate;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.mongodb.core.mapping.BasicMongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * Custom {@link MongoMappingContext} that supports sealed interfaces annotated with
 * {@link Aggregate} as MongoDB entity types.
 *
 * <p>Spring Data MongoDB's default mapping context rejects interfaces as persistent entities.
 * This subclass overrides that behaviour to allow sealed {@link Aggregate} interfaces to be
 * registered and to create the appropriate {@link SealedInterfaceMongoPersistentEntity} for them.</p>
 *
 * <p>When a sealed {@link Aggregate} interface is encountered:</p>
 * <ol>
 *   <li>{@link #shouldCreatePersistentEntityFor} returns {@code true} so Spring Data registers it.</li>
 *   <li>{@link #createPersistentEntity} produces a {@link SealedInterfaceMongoPersistentEntity} that
 *       derives the collection name from the interface's simple name and delegates {@code @Id} lookup
 *       to the first permitted subtype.</li>
 * </ol>
 */
public class PrefabMongoMappingContext extends MongoMappingContext {

    /**
     * Constructs a new PrefabMongoMappingContext.
     */
    public PrefabMongoMappingContext() {
    }

    /**
     * Returns {@code true} for sealed interfaces annotated with {@link Aggregate}, in addition to
     * all types that the default context would register.
     */
    @Override
    protected boolean shouldCreatePersistentEntityFor(TypeInformation<?> type) {
        if (isSealedAggregate(type.getType())) {
            return true;
        }
        return super.shouldCreatePersistentEntityFor(type);
    }

    /**
     * Creates a {@link SealedInterfaceMongoPersistentEntity} for sealed {@link Aggregate} interfaces;
     * delegates to the standard {@link BasicMongoPersistentEntity} for all other types.
     */
    @Override
    protected <T> BasicMongoPersistentEntity<T> createPersistentEntity(TypeInformation<T> typeInformation) {
        if (isSealedAggregate(typeInformation.getType())) {
            return new SealedInterfaceMongoPersistentEntity<>(typeInformation, this);
        }
        return super.createPersistentEntity(typeInformation);
    }

    private static boolean isSealedAggregate(Class<?> type) {
        return type.isInterface() && type.isSealed() && type.isAnnotationPresent(Aggregate.class);
    }
}
