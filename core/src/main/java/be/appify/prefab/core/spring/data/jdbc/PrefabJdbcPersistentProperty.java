package be.appify.prefab.core.spring.data.jdbc;

import java.lang.annotation.Annotation;
import java.util.Map;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jdbc.core.mapping.BasicJdbcPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Custom PersistentProperty implementation that adds support for treating Java records as embedded entities in Spring Data JDBC. If a
 * property is of a record type and does not have an explicit @Embedded annotation, this class will synthesize an @Embedded annotation with
 * default settings, allowing the record's properties to be mapped to columns in the database without requiring explicit annotations.
 */
public class PrefabJdbcPersistentProperty extends BasicJdbcPersistentProperty {

    private Embedded syntheticEmbedded;
    private boolean syntheticResolved = false;

    protected PrefabJdbcPersistentProperty(
            Property property,
            RelationalPersistentEntity<?> owner,
            SimpleTypeHolder simpleTypeHolder,
            NamingStrategy namingStrategy
    ) {
        super(property, owner, simpleTypeHolder, namingStrategy);
    }

    private Embedded resolveSyntheticEmbedded() {
        if (!syntheticResolved) {
            try {
                Class<?> actualType = getActualType();
                if (super.findAnnotation(Embedded.class) == null
                        && !isCollectionLike()
                        && actualType.isRecord()
                        && !actualType.isAnnotationPresent(Table.class)) {
                    syntheticEmbedded = AnnotationUtils.synthesizeAnnotation(
                            Map.of("onEmpty", Embedded.OnEmpty.USE_NULL, "prefix", getName() + "_"),
                            Embedded.class, null);
                }
                syntheticResolved = true;
            } catch (Exception e) {
                return null;
            }
        }
        return syntheticEmbedded;
    }

    @Override
    public boolean isEmbedded() {
        return findAnnotation(Embedded.class) != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A findAnnotation(Class<A> annotationType) {
        A found = super.findAnnotation(annotationType);
        if (found != null) {
            return found;
        }
        if (annotationType == Embedded.class) {
            return (A) resolveSyntheticEmbedded();
        }
        return null;
    }
}