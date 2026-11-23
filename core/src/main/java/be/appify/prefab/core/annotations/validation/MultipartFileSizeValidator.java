package be.appify.prefab.core.annotations.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates {@link FileSize} on an uploaded file, so that a request is rejected before the file is stored.
 */
public class MultipartFileSizeValidator implements ConstraintValidator<FileSize, MultipartFile> {
    private long maxFileSize;

    /** Constructs a new MultipartFileSizeValidator. */
    public MultipartFileSizeValidator() {
    }

    @Override
    public void initialize(FileSize constraintAnnotation) {
        this.maxFileSize = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(MultipartFile value, ConstraintValidatorContext context) {
        return value == null || value.getSize() <= maxFileSize;
    }
}
