package be.appify.prefab.core.annotations.validation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = ContentTypeValidator.class)
@Documented
public @interface ContentType {
    String DEFAULT_MESSAGE = "invalid content type";

    String message() default DEFAULT_MESSAGE;

    /// List of supported mime types
    /// eg: text/html
    String[] value();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
