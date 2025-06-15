package be.appify.prefab.core.annotations.validation;

import be.appify.prefab.core.domain.Binary;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.io.IOException;
import java.nio.file.Files;

public class FileSizeValidator implements ConstraintValidator<FileSize, Binary> {

    private int maxFileSize;

    @Override
    public void initialize(FileSize constraintAnnotation) {
        this.maxFileSize = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(Binary value, ConstraintValidatorContext context) {
        try {
            return Files.size(value.data().toPath()) <= maxFileSize;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
