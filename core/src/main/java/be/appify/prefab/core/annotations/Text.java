package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a {@code String} field to an unbounded {@code TEXT} column in the generated Flyway migration script.
 * <p>
 * By default, {@code String} fields are mapped to {@code VARCHAR(255)}. Use {@code @Text} when the value
 * may exceed 255 characters (e.g. descriptions, notes, free-text content).
 * </p>
 * <p>
 * Alternatively, use Jakarta Validation's {@code @Size(max = N)} to generate {@code VARCHAR(N)}.
 * </p>
 *
 * @see jakarta.validation.constraints.Size
 */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.SOURCE)
public @interface Text {
}

