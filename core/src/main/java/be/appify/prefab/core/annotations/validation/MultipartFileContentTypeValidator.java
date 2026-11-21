package be.appify.prefab.core.annotations.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates {@link ContentType} on an uploaded file, so that a request is rejected before the file is stored.
 */
public class MultipartFileContentTypeValidator implements ConstraintValidator<ContentType, MultipartFile> {
    private Set<String> contentTypes = Set.of();

    /** Constructs a new MultipartFileContentTypeValidator. */
    public MultipartFileContentTypeValidator() {
    }

    @Override
    public void initialize(ContentType constraintAnnotation) {
        contentTypes = Set.of(constraintAnnotation.value());
    }

    @Override
    public boolean isValid(MultipartFile value, ConstraintValidatorContext context) {
        return value == null || contentTypes.contains(value.getContentType());
    }
}
