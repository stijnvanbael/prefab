package be.appify.prefab.core.spring.data.jdbc;

/**
 * Marker interface for generated JDBC reading converters for polymorphic aggregate roots.
 *
 * <p>Any class implementing this interface is automatically discovered by the Prefab JDBC configuration and
 * registered in {@code JdbcCustomConversions}, so that Spring Data JDBC can use it to hydrate a sealed-interface
 * aggregate from a database row.</p>
 *
 * <p>The annotation processor generates one implementation per {@code @Aggregate}-annotated sealed interface.</p>
 */
public interface PolymorphicReadingConverter {
}
