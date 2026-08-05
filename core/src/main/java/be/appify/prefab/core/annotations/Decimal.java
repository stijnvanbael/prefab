package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Carries the Avro decimal logical-type metadata (precision and scale) for a {@code BigDecimal} field.
 *
 * <p>When Prefab generates a Java record from an Avro schema ({@code .avsc} file), it annotates every
 * {@code BigDecimal} field with this annotation so that downstream generators (e.g., schema factories)
 * can recover the original precision and scale without re-reading the AVSC file.
 *
 * <p>You can also place this annotation on hand-written {@code BigDecimal} parameters to explicitly
 * declare the Avro decimal precision and scale used during serialisation.
 *
 * @see be.appify.prefab.core.annotations.AvroSchema
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.CLASS)
public @interface Decimal {

    /**
     * The number of significant decimal digits.
     *
     * @return precision
     */
    int precision();

    /**
     * The number of digits to the right of the decimal point.
     *
     * @return scale
     */
    int scale();
}
