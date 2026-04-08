package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type as a custom (user-defined) domain type that the Prefab annotation processor should not attempt to map
 * automatically.
 *
 * <p>By default, Prefab maps each aggregate field to a database column and, when Avro serialization is enabled, to an
 * Avro schema field. Types that are not recognised by the built-in whitelist will cause a compile-time error.
 * Annotating such a type with {@code @CustomType} opts out of the automatic handling:
 * <ul>
 *   <li><strong>Database migration</strong> – the field is skipped (no column is generated). To keep Spring Data happy
 *       you must either annotate the field on the aggregate with
 *       {@code @org.springframework.data.annotation.Transient}, or register a
 *       {@link be.appify.prefab.processor.PrefabPlugin} that returns a
 *       {@link be.appify.prefab.processor.dbmigration.DataType} from its {@code dataTypeOf()} method.</li>
 *   <li><strong>Avro events</strong> – the field is omitted from the generated Avro schema and converters. To include
 *       it, register a {@link be.appify.prefab.processor.PrefabPlugin} that implements {@code avroSchemaOf()},
 *       {@code toAvroValueOf()}, and {@code fromAvroValueOf()}.</li>
 *   <li><strong>REST responses</strong> – the field is passed through unchanged; Jackson (or the configured
 *       serializer) handles serialization as normal.</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * @CustomType
 * public record Either<L, R>(L left, R right) { }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomType {
}
