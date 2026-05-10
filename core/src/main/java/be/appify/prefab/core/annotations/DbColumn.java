package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a custom SQL column type for an aggregate field, bypassing Prefab's built-in type validation.
 *
 * <p>Use this annotation when a field's Java type is not in Prefab's supported set (e.g. {@code float[]},
 * custom value types, PostgreSQL extension types such as {@code pgvector}'s {@code vector(N)},
 * PostGIS geometry, or hstore). The annotation serves two purposes:</p>
 * <ol>
 *   <li><strong>Type validation bypass</strong> – the annotation processor accepts the field without
 *       throwing an {@link IllegalArgumentException}, regardless of the field's Java type.</li>
 *   <li><strong>DDL generation</strong> – when {@code @DbMigration} is enabled on the enclosing aggregate,
 *       the exact SQL type specified in {@link #type()} is emitted in the generated Flyway migration script.</li>
 * </ol>
 *
 * <p>Optionally, {@link #converter()} may reference a Spring {@code Converter} implementation that is
 * automatically registered as a {@code JdbcCustomConversions} contributor at application startup.
 * The converter class must have a no-argument constructor. If dependency injection is required,
 * annotate the converter with {@code @Component} and omit this attribute instead.</p>
 *
 * <h2>Example – pgvector embedding field</h2>
 * <pre>{@code
 * @Aggregate
 * public record MemoryEntry(
 *         @Id Reference<MemoryEntry> id,
 *         @Version long version,
 *         @DbColumn(type = "vector(1536)", converter = FloatArrayToVectorConverter.class)
 *         float[] embedding
 * ) { }
 * }</pre>
 *
 * @see DbMigration
 */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface DbColumn {

    /**
     * The exact SQL column type to emit in the generated Flyway migration DDL.
     *
     * <p>Examples: {@code "vector(1536)"}, {@code "geometry(Point,4326)"}, {@code "hstore"},
     * {@code "JSONB"}, {@code "TEXT"}.</p>
     *
     * <p>Must not be blank. Required when {@code @DbMigration} is enabled on the enclosing aggregate;
     * the value is used as-is in the generated {@code CREATE TABLE} or {@code ALTER TABLE} statement.</p>
     *
     * @return the SQL column type string
     */
    String type();

    /**
     * An optional converter class that converts between the field's Java type and the JDBC representation.
     *
     * <p>The class must implement Spring's {@code Converter<F, T>} and have a public no-argument constructor.
     * Prefab instantiates it automatically and registers it as a {@code JdbcCustomConversions} contributor,
     * so the user does not need to declare it as a {@code @Component} or configure it manually.</p>
     *
     * <p>Defaults to {@code void.class} (disabled). Provide a converter class when the field's Java type
     * requires explicit conversion to/from the JDBC type (e.g. {@code float[]} ↔ {@code PGobject}).</p>
     *
     * @return the converter class, or {@code void.class} if none
     */
    Class<?> converter() default void.class;
}

