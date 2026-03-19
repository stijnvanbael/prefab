package be.appify.prefab.core.spring.data.jdbc;

import be.appify.prefab.core.service.Reference;
import java.sql.JDBCType;
import java.sql.SQLType;
import java.util.Map;
import org.springframework.data.jdbc.core.dialect.JdbcArrayColumns;

/**
 * JdbcArrayColumns wrapper that adds support for Prefab-specific types, such as {@link Reference}, when determining the SQL type for array
 * components. This allows Spring Data JDBC to properly handle arrays of these types when generating SQL queries and mapping results.
 */
public class PrefabJdbcArrayColumns implements JdbcArrayColumns {
    private final JdbcArrayColumns delegate;
    private final Map<Class<?>, SQLType> customTypeMappings = Map.of(
            Reference.class, JDBCType.VARCHAR
    );

    /**
     * Constructs a new PrefabJdbcArrayColumns that wraps the given delegate.
     *
     * @param delegate
     *         the JdbcArrayColumns to wrap
     */
    protected PrefabJdbcArrayColumns(JdbcArrayColumns delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isSupported() {
        return delegate.isSupported();
    }

    @Override
    public Class<?> getArrayType(Class<?> userType) {
        if (customTypeMappings.containsKey(userType)) {
            return String.class;
        }
        return delegate.getArrayType(userType);
    }

    @Override
    public SQLType getSqlType(Class<?> componentType) {
        if (customTypeMappings.containsKey(componentType)) {
            return customTypeMappings.get(componentType);
        }
        return delegate.getSqlType(componentType);
    }

    @Override
    public String getArrayTypeName(SQLType jdbcType) {
        return delegate.getArrayTypeName(jdbcType);
    }
}
