package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a record component, constructor parameter, or event field to declare a representative example value.
 * <p>
 * The value is used in three places:
 * <ul>
 *   <li>Object Mother generation: the annotated value becomes the default in the generated {@code Mother} class
 *       instead of the generic type-based fallback.</li>
 *   <li>OpenAPI / Swagger UI: the annotation processor emits
 *       {@code @Schema(example = "...")} on the corresponding field in the generated request or response record.</li>
 *   <li>AsyncAPI documentation: the annotation processor includes an {@code "example"} key in the JSON Schema for
 *       the field of the generated event schema document.</li>
 * </ul>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE)
public @interface Example {
    /**
     * A representative example value for the field or parameter.
     *
     * @return the example value as a String
     */
    String value();
}

