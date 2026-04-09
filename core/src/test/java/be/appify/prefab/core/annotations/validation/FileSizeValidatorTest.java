package be.appify.prefab.core.annotations.validation;

import be.appify.prefab.core.domain.Binary;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSizeValidatorTest {

    @TempDir
    File tempDir;

    @FileSize(max = 100)
    private static final class SmallSizeAnnotationHolder {}

    @FileSize(max = 0)
    private static final class ZeroSizeAnnotationHolder {}

    private static final FileSize SMALL_SIZE_ANNOTATION =
            SmallSizeAnnotationHolder.class.getDeclaredAnnotation(FileSize.class);

    private static final FileSize ZERO_SIZE_ANNOTATION =
            ZeroSizeAnnotationHolder.class.getDeclaredAnnotation(FileSize.class);

    @Test
    void fileSizeWithinLimitIsValid() throws IOException {
        var file = new File(tempDir, "small.txt");
        Files.writeString(file.toPath(), "hello"); // 5 bytes
        var validator = new FileSizeValidator();
        validator.initialize(SMALL_SIZE_ANNOTATION);
        var binary = new Binary("small.txt", "text/plain", file);
        assertTrue(validator.isValid(binary, null));
    }

    @Test
    void fileSizeExceedingLimitIsInvalid() throws IOException {
        var file = new File(tempDir, "large.txt");
        Files.writeString(file.toPath(), "a".repeat(101)); // 101 bytes, exceeds max 100
        var validator = new FileSizeValidator();
        validator.initialize(SMALL_SIZE_ANNOTATION);
        var binary = new Binary("large.txt", "text/plain", file);
        assertFalse(validator.isValid(binary, null));
    }

    @Test
    void fileSizeExactlyAtLimitIsValid() throws IOException {
        var file = new File(tempDir, "exact.txt");
        Files.writeString(file.toPath(), "a".repeat(100)); // exactly 100 bytes
        var validator = new FileSizeValidator();
        validator.initialize(SMALL_SIZE_ANNOTATION);
        var binary = new Binary("exact.txt", "text/plain", file);
        assertTrue(validator.isValid(binary, null));
    }

    @Test
    void emptyFileIsValidWhenMaxIsZero() throws IOException {
        var file = new File(tempDir, "empty.txt");
        Files.writeString(file.toPath(), ""); // 0 bytes
        var validator = new FileSizeValidator();
        validator.initialize(ZERO_SIZE_ANNOTATION);
        var binary = new Binary("empty.txt", "text/plain", file);
        assertTrue(validator.isValid(binary, null));
    }
}
