package be.appify.prefab.postgres.spring.data.jdbc;

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

    private static final ThreadLocal<SimpleTypeHolder> STASHED_HOLDER = new ThreadLocal<>();

    private final SimpleTypeHolder simpleTypeHolder;
    private Embedded syntheticEmbedded;
    private boolean syntheticResolved = false;

    /**
     * Constructs a new PrefabJdbcPersistentProperty
     *
     * @param property
     *         the property to wrap
     * @param owner
     *         the owner of the property
     * @param simpleTypeHolder
     *         the SimpleTypeHolder to use for determining simple types (stashed in a thread-local to work around super constructor
     *         ordering)
      * @param namingStrategy
     *         the NamingStrategy to use for mapping property names to column names
     */
    protected PrefabJdbcPersistentProperty(
            Property property,
            RelationalPersistentEntity<?> owner,
            SimpleTypeHolder simpleTypeHolder,
            NamingStrategy namingStrategy
    ) {
        super(property, owner, stash(simpleTypeHolder), namingStrategy);
        this.simpleTypeHolder = simpleTypeHolder;
    }

    private static SimpleTypeHolder stash(SimpleTypeHolder holder) {
        STASHED_HOLDER.set(holder);
        return holder;
    }

    private static String toSnakeCase(String value) {
        return value.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    private SimpleTypeHolder getSimpleTypeHolder() {
        // During super constructor, field isn't assigned yet — use stashed value
        return simpleTypeHolder != null ? simpleTypeHolder : STASHED_HOLDER.get();
    }

    private Embedded resolveSyntheticEmbedded() {
        if (!syntheticResolved) {
            Class<?> actualType = getActualType();
            SimpleTypeHolder holder = getSimpleTypeHolder();
            if (!isCollectionLike()
                    && actualType.isRecord()
                    && !actualType.isAnnotationPresent(Table.class)
                    && !holder.isSimpleType(actualType)) {
                syntheticEmbedded = AnnotationUtils.synthesizeAnnotation(
                        Map.of("onEmpty", Embedded.OnEmpty.USE_NULL, "prefix", toSnakeCase(getName()) + "_"),
                        Embedded.class, null);
            }
            syntheticResolved = true;
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
        if (annotationType == Embedded.class) {
            Embedded synthetic = resolveSyntheticEmbedded();
            if (synthetic != null) {
                return (A) synthetic;
            }
        }
        return super.findAnnotation(annotationType);
    }
}
