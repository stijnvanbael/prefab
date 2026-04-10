package be.appify.prefab.core.annotations.validation;

import be.appify.prefab.core.domain.Binary;
import java.io.File;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentTypeValidatorTest {

    @ContentType({"image/jpeg", "image/png"})
    private static final class AnnotationHolder {}

    private static final ContentType ANNOTATION =
            AnnotationHolder.class.getDeclaredAnnotation(ContentType.class);

    @Test
    void nullValueIsValid() {
        var validator = new ContentTypeValidator();
        validator.initialize(ANNOTATION);
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void matchingContentTypeIsValid() {
        var validator = new ContentTypeValidator();
        validator.initialize(ANNOTATION);
        var binary = new Binary("test.jpg", "image/jpeg", new File("test.jpg"));
        assertTrue(validator.isValid(binary, null));
    }

    @Test
    void nonMatchingContentTypeIsInvalid() {
        var validator = new ContentTypeValidator();
        validator.initialize(ANNOTATION);
        var binary = new Binary("test.txt", "text/plain", new File("test.txt"));
        assertFalse(validator.isValid(binary, null));
    }

    @Test
    void anotherMatchingContentTypeIsValid() {
        var validator = new ContentTypeValidator();
        validator.initialize(ANNOTATION);
        var binary = new Binary("test.png", "image/png", new File("test.png"));
        assertTrue(validator.isValid(binary, null));
    }
}
