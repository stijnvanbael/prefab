package be.appify.prefab.core.annotations.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Validate that the file size is below the specified maximum. */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = FileSizeValidator.class)
@Documented
public @interface FileSize {
    /** Default error message */
    String DEFAULT_MESSAGE = "file too large";

    /**
     * Default error message
     * @return the error message template
     */
    String message() default DEFAULT_MESSAGE;

    /**
     * Maximum file size in bytes
     * @return the maximum file size in bytes
     */
    int max() default Integer.MAX_VALUE;

    /**
     * Allows the specification of validation groups
     * @return the validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Allows the specification of custom payload objects
     * @return the payload type
     */
    Class<? extends Payload>[] payload() default {};
}
