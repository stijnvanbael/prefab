package be.appify.prefab.core.spring.data.jdbc;

import java.util.List;

/**
 * Marker interface for Spring components that contribute converters registered automatically by
 * {@code @DbColumn(converter = ...)} annotations.
 *
 * <p>The Prefab annotation processor generates an implementation of this interface for each aggregate
 * package that declares at least one {@code @DbColumn} with a non-void converter class.
 * The generated class is annotated with {@code @Component} so it is auto-discovered by Spring's
 * component scan, and the converters it returns are registered in {@code JdbcCustomConversions}.</p>
 *
 * <p>Users never implement this interface directly.</p>
 */
public interface DbColumnConverterContributor {

    /**
     * Returns the converter instances to be registered in {@code JdbcCustomConversions}.
     *
     * @return an unmodifiable list of converter instances; never {@code null}
     */
    List<Object> converters();
}

