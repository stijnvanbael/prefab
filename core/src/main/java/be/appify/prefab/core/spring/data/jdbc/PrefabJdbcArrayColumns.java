package be.appify.prefab.core.spring.data.jdbc;

import be.appify.prefab.core.service.SingleValue;
import java.sql.JDBCType;
import java.sql.SQLType;
import org.springframework.data.jdbc.core.dialect.JdbcArrayColumns;

/**
 * JdbcArrayColumns wrapper that adds support for Prefab-specific types, such as types annotated with {@link SingleValue}, when determining
 * the SQL type for array components. This allows Spring Data JDBC to properly handle arrays of these types when generating SQL queries and
 * mapping results.
 */
public class PrefabJdbcArrayColumns implements JdbcArrayColumns {
    private final JdbcArrayColumns delegate;

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
        if (userType.isAnnotationPresent(SingleValue.class)) {
            return String.class;
        }
        return delegate.getArrayType(userType);
    }

    @Override
    public SQLType getSqlType(Class<?> componentType) {
        if (componentType.isAnnotationPresent(SingleValue.class)) {
            return JDBCType.VARCHAR;
        }
        return delegate.getSqlType(componentType);
    }

    @Override
    public String getArrayTypeName(SQLType jdbcType) {
        return delegate.getArrayTypeName(jdbcType);
    }
}
