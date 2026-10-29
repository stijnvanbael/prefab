package be.appify.prefab.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the value of an event field that was added after earlier messages were written. The value is applied when
 * a message written before the field existed is read, for both JSON and Avro.
 * <p>
 * The literal is parsed according to the component type. Supported component types are {@code String}, the primitive
 * types and their wrappers, enums (the constant name) and lists (only the empty list, {@code "[]"}). Nullable
 * components already default to {@code null} and must not declare a default.
 * <p>
 * For Avro the default is emitted on the generated schema field, so it is honoured by Avro's reader/writer schema
 * resolution.
 */
@Documented
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Default {

    /**
     * The default value as a literal.
     *
     * @return the literal
     */
    String value();
}
