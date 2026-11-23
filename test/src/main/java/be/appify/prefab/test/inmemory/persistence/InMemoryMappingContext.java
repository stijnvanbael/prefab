package be.appify.prefab.test.inmemory.persistence;

import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/** Describes the aggregates of the in-memory repositories: identifier, version and properties. */
public final class InMemoryMappingContext
        extends AbstractMappingContext<BasicPersistentEntity<?, InMemoryPersistentProperty>, InMemoryPersistentProperty> {

    /** Constructs a new InMemoryMappingContext. */
    public InMemoryMappingContext() {
    }

    // JDK types such as BigDecimal or Instant are values, and their private fields are not accessible
    @Override
    protected boolean shouldCreatePersistentEntityFor(TypeInformation<?> type) {
        return !type.getType().getName().startsWith("java.") && super.shouldCreatePersistentEntityFor(type);
    }

    @Override
    protected <T> BasicPersistentEntity<?, InMemoryPersistentProperty> createPersistentEntity(
            TypeInformation<T> typeInformation
    ) {
        return new BasicPersistentEntity<>(typeInformation);
    }

    @Override
    protected InMemoryPersistentProperty createPersistentProperty(
            Property property,
            BasicPersistentEntity<?, InMemoryPersistentProperty> owner,
            SimpleTypeHolder simpleTypeHolder
    ) {
        return new InMemoryPersistentProperty(property, owner, simpleTypeHolder);
    }
}
