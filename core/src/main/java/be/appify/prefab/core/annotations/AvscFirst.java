package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type as a trigger for AVSC-first event generation. The annotation processor reads the
 * referenced AVSC schema file and generates the corresponding Java event record along with all
 * required Avro converters.
 *
 * <p>Place this annotation on any class or interface that acts as a marker. The marker type itself
 * is not used at runtime; it exists solely to tell the annotation processor which AVSC schema to
 * process.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @AvscFirst(path = "avro/sale-created.avsc", topic = "sale")
 * public interface SaleCreatedAvro {}
 * }</pre>
 *
 * <p>The AVSC file must be placed on the classpath (e.g.,
 * {@code src/main/resources/avro/sale-created.avsc}). The generated event record is placed in the
 * package defined by the AVSC schema's {@code namespace} field.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AvscFirst {

    /**
     * Classpath-relative path to the AVSC schema file.
     *
     * @return the classpath-relative path to the AVSC file, e.g. {@code "avro/sale-created.avsc"}
     */
    String path();

    /**
     * The messaging topic the generated event will be published to or consumed from.
     *
     * @return the topic name
     */
    String topic();

    /**
     * The messaging platform. Defaults to {@link Event.Platform#DERIVED}.
     *
     * @return the platform
     */
    Event.Platform platform() default Event.Platform.DERIVED;
}
