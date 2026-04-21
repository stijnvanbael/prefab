package be.appify.prefab.postgres.spring.data.jdbc;

import java.sql.SQLType;
import org.springframework.data.jdbc.core.dialect.JdbcArrayColumns;

/**
 * JdbcArrayColumns wrapper that adds support for single-value record types when determining the SQL type for array
 * components. A single-value record is a Java record with exactly one component.
 * <p>
 * This allows Spring Data JDBC to properly handle arrays of these types when generating SQL queries and mapping results,
 * using the SQL type appropriate for the record's single component field.
 * </p>
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
        if (isSingleFieldRecord(userType) && !SingleValueRecordSimpleTypeHolder.wrapsMultiFieldRecord(userType)) {
            return userType.getRecordComponents()[0].getType();
        }
        return delegate.getArrayType(userType);
    }

    @Override
    public SQLType getSqlType(Class<?> componentType) {
        if (isSingleFieldRecord(componentType) && !SingleValueRecordSimpleTypeHolder.wrapsMultiFieldRecord(componentType)) {
            return PrefabMappingJdbcConverter.sqlTypeFor(componentType.getRecordComponents()[0].getType());
        }
        return delegate.getSqlType(componentType);
    }

    @Override
    public String getArrayTypeName(SQLType jdbcType) {
        return delegate.getArrayTypeName(jdbcType);
    }

    private static boolean isSingleFieldRecord(Class<?> clazz) {
        return clazz.isRecord() && clazz.getRecordComponents().length == 1;
    }
}
