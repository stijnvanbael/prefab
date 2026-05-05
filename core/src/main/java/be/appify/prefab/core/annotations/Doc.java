package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a record component, constructor parameter, or event field to provide a human-readable description.
 * <p>
 * The description is propagated to all generated API and schema artefacts:
 * <ul>
 *   <li>OpenAPI / Swagger UI: the annotation processor emits
 *       {@code @Schema(description = "...")} on the corresponding field in the generated request or response record.</li>
 *   <li>AsyncAPI documentation: the annotation processor includes a {@code "description"} key in the JSON Schema
 *       for the field of the generated event schema document.</li>
 *   <li>Avro schemas: the annotation processor sets the {@code "doc"} property on the generated
 *       {@link org.apache.avro.Schema.Field}.</li>
 * </ul>
 */
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.CLASS)
public @interface Doc {

    /**
     * A human-readable description of the field or parameter.
     *
     * @return the description
     */
    String value();
}

