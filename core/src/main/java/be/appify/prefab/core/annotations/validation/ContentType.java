package be.appify.prefab.core.annotations.validation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validation annotation to validate that a Binary field contains a valid content type (MIME type).
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = ContentTypeValidator.class)
@Documented
public @interface ContentType {
    /** Default error message */
    String DEFAULT_MESSAGE = "invalid content type";

    /**
     * The error message template
     * @return the error message template
     */
    String message() default DEFAULT_MESSAGE;

    /**
     * List of supported mime types, eg: text/html
     * @return array of supported mime types
     */
    String[] value();

    /**
     * The validation groups for conditional validation
     * @return the validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Payload type for clients to specify custom payload objects
     * @return the payload type
     */
    Class<? extends Payload>[] payload() default {};
}
