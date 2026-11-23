package be.appify.prefab.test.inmemory.persistence;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/** Property of an in-memory aggregate. Recognises {@code @Id} and {@code @Version} through annotations. */
final class InMemoryPersistentProperty extends AnnotationBasedPersistentProperty<InMemoryPersistentProperty> {

    InMemoryPersistentProperty(
            Property property,
            PersistentEntity<?, InMemoryPersistentProperty> owner,
            SimpleTypeHolder simpleTypeHolder
    ) {
        super(property, owner, simpleTypeHolder);
    }

    @Override
    protected Association<InMemoryPersistentProperty> createAssociation() {
        return new Association<>(this, null);
    }
}
