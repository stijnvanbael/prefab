package be.appify.prefab.core.annotations;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Overrides the Avro schema metadata for a generated Java type.
 *
 * <p>When Prefab generates a Java record or enum from an Avro schema, it capitalises the first letter
 * of the type name to produce a valid Java identifier. If the original Avro schema name differs from
 * the capitalised Java name, this annotation is placed on the generated type so that schema factories
 * and converters can recover the original Avro name and namespace.
 *
 * <p>You can also use this annotation on hand-written types to explicitly declare the Avro schema
 * name and/or namespace that should be used during serialisation, overriding the default behaviour
 * that derives both from the Java class name and package.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface AvroSchema {
    /**
     * The original Avro schema name for this type, if it differs from the Java simple class name.
     * An empty string (the default) means "use the Java class name as-is".
     *
     * @return the Avro schema name, or {@code ""} to use the Java class name
     */
    String name() default "";
    /**
     * The Avro namespace for this type. An empty string (the default) means "use the Java package name".
     *
     * @return the Avro namespace, or {@code ""} to use the Java package name
     */
    String namespace() default "";
}