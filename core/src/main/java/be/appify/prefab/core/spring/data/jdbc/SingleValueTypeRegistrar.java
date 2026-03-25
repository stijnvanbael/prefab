package be.appify.prefab.core.spring.data.jdbc;

import java.util.List;

/**
 * A registrar for single-value types that should be treated as simple types by Spring Data JDBC.
 * <p>
 * Implement this interface and register it as a Spring bean to register additional single-value types
 * (types annotated with {@link be.appify.prefab.core.service.SingleValue}) with the Prefab JDBC dialect.
 * </p>
 *
 * @see PrefabJdbcDialect
 */
public interface SingleValueTypeRegistrar {
    /**
     * Returns the list of single-value type classes to register as simple types.
     *
     * @return the list of single-value type classes
     */
    List<Class<?>> singleValueTypes();
}
