package be.appify.prefab.core.spring.data.jdbc;

import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

/**
 * Custom JdbcMappingContext that creates PrefabJdbcPersistentProperty instances, which add support for treating Java records as embedded
 * entities in Spring Data JDBC. This allows record properties to be mapped to columns in the database without requiring explicit @Embedded
 * annotations, simplifying the mapping of record types.
 */
public class PrefabJdbcMappingContext extends JdbcMappingContext {
    /**
     * Constructs a new PrefabJdbcMappingContext.
     *
     * @param namingStrategy
     *        the NamingStrategy to use for mapping property names to column names
     */
    public PrefabJdbcMappingContext(NamingStrategy namingStrategy) {
        super(namingStrategy);
    }

    @Override
    protected RelationalPersistentProperty createPersistentProperty(
            Property property,
            RelationalPersistentEntity<?> owner,
            SimpleTypeHolder simpleTypeHolder
    ) {
        return new PrefabJdbcPersistentProperty(property, owner, simpleTypeHolder, getNamingStrategy());
    }
}
