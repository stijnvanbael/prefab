package be.appify.prefab.core.annotations.validation;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

class MultipartFileValidatorsTest {

    @ContentType({"image/png", "image/jpeg"})
    @FileSize(max = 3)
    private static final class Constraints {
    }

    private static final ContentType CONTENT_TYPE = Constraints.class.getDeclaredAnnotation(ContentType.class);
    private static final FileSize FILE_SIZE = Constraints.class.getDeclaredAnnotation(FileSize.class);

    @Test
    void contentType_givenAllowedType_isValid() {
        assertThat(contentTypeValidator().isValid(file("image/png", 1), null)).isTrue();
    }

    @Test
    void contentType_givenOtherType_isInvalid() {
        assertThat(contentTypeValidator().isValid(file("application/pdf", 1), null)).isFalse();
    }

    @Test
    void fileSize_givenSizeAtTheMaximum_isValid() {
        assertThat(fileSizeValidator().isValid(file("image/png", 3), null)).isTrue();
    }

    @Test
    void fileSize_givenSizeAboveTheMaximum_isInvalid() {
        assertThat(fileSizeValidator().isValid(file("image/png", 4), null)).isFalse();
    }

    @Test
    void bothValidators_givenNoFile_leaveThatToNotNull() {
        assertThat(contentTypeValidator().isValid(null, null)).isTrue();
        assertThat(fileSizeValidator().isValid(null, null)).isTrue();
    }

    private static MockMultipartFile file(String contentType, int size) {
        return new MockMultipartFile("file", "file", contentType, new byte[size]);
    }

    private static MultipartFileContentTypeValidator contentTypeValidator() {
        var validator = new MultipartFileContentTypeValidator();
        validator.initialize(CONTENT_TYPE);
        return validator;
    }

    private static MultipartFileSizeValidator fileSizeValidator() {
        var validator = new MultipartFileSizeValidator();
        validator.initialize(FILE_SIZE);
        return validator;
    }
}
