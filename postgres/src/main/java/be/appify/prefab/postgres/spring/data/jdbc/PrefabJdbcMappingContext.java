package be.appify.prefab.postgres.spring.data.jdbc;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.spel.EvaluationContextProvider;
import org.springframework.data.spel.ExtensionAwareEvaluationContextProvider;

/**
 * Custom JdbcMappingContext that creates PrefabJdbcPersistentProperty instances, which add support for treating Java records as embedded
 * entities in Spring Data JDBC. This allows record properties to be mapped to columns in the database without requiring explicit @Embedded
 * annotations, simplifying the mapping of record types.
 */
public class PrefabJdbcMappingContext extends JdbcMappingContext {

    private final SqlIdentifierExpressionEvaluator sqlIdentifierExpressionEvaluator = new SqlIdentifierExpressionEvaluator(
            EvaluationContextProvider.DEFAULT);

    /**
     * Constructs a new PrefabJdbcMappingContext.
     *
     * @param namingStrategy
     *         the NamingStrategy to use for mapping property names to column names
     */
    public PrefabJdbcMappingContext(NamingStrategy namingStrategy) {
        super(namingStrategy);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.sqlIdentifierExpressionEvaluator.setProvider(new ExtensionAwareEvaluationContextProvider(applicationContext));
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.sqlIdentifierExpressionEvaluator.setEnvironment(environment);
        super.setEnvironment(environment);
    }

    @Override
    protected <T> RelationalPersistentEntity<T> createPersistentEntity(TypeInformation<T> typeInformation) {
        return new PrefabPersistentEntity<>(typeInformation, getNamingStrategy(), sqlIdentifierExpressionEvaluator);
    }

    @Override
    protected RelationalPersistentProperty createPersistentProperty(
            Property property,
            RelationalPersistentEntity<?> owner,
            SimpleTypeHolder simpleTypeHolder
    ) {
        var persistentProperty = new PrefabJdbcPersistentProperty(property, owner, simpleTypeHolder, getNamingStrategy());
        applyDefaults(persistentProperty);
        return persistentProperty;
    }
}
