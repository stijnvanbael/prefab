package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Addon annotation that enables AVSC-first event generation. Must be combined with {@link Event}
 * on the same type. The annotation processor reads each referenced AVSC schema file and generates
 * a corresponding Java event record along with all required Avro converters. Every generated
 * record implements the annotated interface, making it usable as that type at runtime.
 *
 * <p>Place this annotation together with {@link Event} on an interface that represents the event
 * contract. The topic, platform, and serialization are taken from {@link Event}; {@link Avsc} specifies
 * the AVSC schema files and optional per-file partitioning-key metadata.
 *
 * <p>Single-event usage:
 *
 * <pre>{@code
 * @Event(topic = "sale", serialization = Event.Serialization.AVRO)
 * @Avsc("avro/sale-created.avsc")
 * public interface SaleCreated {
 *     @PartitioningKey
 *     String saleId();
 * }
 * }</pre>
 *
 * <p>Multiple-event usage (all generated records implement {@code SaleEvent}):
 *
 * <pre>{@code
 * @Event(topic = "sale", serialization = Event.Serialization.AVRO)
 * @Avsc({"avro/sale-created.avsc", "avro/sale-paid.avsc", "avro/sale-cancelled.avsc"})
 * public interface SaleEvent {
 *     @PartitioningKey
 *     String saleId();
 * }
 * }</pre>
 *
 * <p>Sealed multiple-event usage – the interface may be declared {@code sealed} with a
 * {@code permits} clause that lists the names of the generated records. The annotation processor
 * generates the permitted records in round 1; javac resolves the {@code permits} clause in round 2
 * once the generated classes are available.
 *
 * <pre>{@code
 * @Event(topic = "sale", serialization = Event.Serialization.AVRO)
 * @Avsc(files = {
 *         @AvscFile(path = "avro/sale-created.avsc", keyProperty = "saleId"),
 *         @AvscFile(path = "avro/sale-paid.avsc", keyProperty = "paymentId")
 * })
 * public sealed interface SaleEvent permits SaleCreated, SalePaid {}
 * }</pre>
 *
 * <p>Each AVSC file must be placed on the classpath (e.g.,
 * {@code src/main/resources/avro/sale-created.avsc}). Generated event records are placed in the
 * package defined by each AVSC schema's {@code namespace} field.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Avsc {

    /**
     * One or more classpath-relative paths to AVSC schema files. A separate event record is generated for
     * each path; all generated records implement the annotated interface.
     *
     * <p>This is the preferred form when all referenced AVSC events share the same partitioning key. In
     * that case, declare a matching interface method annotated with {@link PartitioningKey}.
     *
     * @return the classpath-relative paths to the AVSC files
     */
    String[] value() default {};

    /**
     * Explicit AVSC schema declarations with optional partitioning-key metadata per schema.
     *
     * <p>Use this form when different AVSC files need different partitioning-key properties, or when only
     * some of the generated events should register a partitioning key extractor.
     *
     * @return the schema declarations
     */
    AvscFile[] files() default {};
}
