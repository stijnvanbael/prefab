package be.appify.prefab.core.annotations.validation;

import be.appify.prefab.core.domain.Binary;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collections;
import java.util.Set;

/** Validator for the ContentType annotation */
public class ContentTypeValidator implements ConstraintValidator<ContentType, Binary> {
    private Set<String> contentTypes = Collections.emptySet();

    /** Constructs a new ContentTypeValidator. */
    public ContentTypeValidator() {
    }

    @Override
    public void initialize(ContentType constraintAnnotation) {
        contentTypes = Set.of(constraintAnnotation.value());
    }

    @Override
    public boolean isValid(Binary value, ConstraintValidatorContext context) {
        return value == null || contentTypes.contains(value.contentType());
    }
}
