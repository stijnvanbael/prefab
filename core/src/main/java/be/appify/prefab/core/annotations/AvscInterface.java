package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps an AVSC named type to a Java interface that the generated record or enum should implement.
 *
 * <p>Declare this on the top-level {@link Avsc} event contract to opt individual generated AVSC
 * records or enums into additional interfaces. Matching uses the original Avro {@code namespace}
 * and {@code name}, not the generated Java package or simple name, so mappings stay stable when
 * Prefab capitalises Java type names or keeps generated sources in the contract package.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Repeatable(AvscInterfaces.class)
public @interface AvscInterface {

    /**
     * The original Avro schema name to match.
     *
     * @return the Avro type name
     */
    String name();

    /**
     * The original Avro schema namespace to match.
     *
     * @return the Avro namespace, or {@code ""} for no namespace
     */
    String namespace() default "";

    /**
     * The Java interface that matching generated AVSC records or enums must implement.
     *
     * @return the Java interface to add to the generated type
     */
    Class<?> type();
}
