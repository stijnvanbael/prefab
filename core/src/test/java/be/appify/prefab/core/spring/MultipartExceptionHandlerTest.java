package be.appify.prefab.core.spring;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import static org.assertj.core.api.Assertions.assertThat;

class MultipartExceptionHandlerTest {
    private final MultipartExceptionHandler handler = new MultipartExceptionHandler();

    @Test
    void missingPartIsABadRequestThatNamesThePart() {
        var exception = new MissingServletRequestPartException("file");

        var response = handler.handle(exception);

        assertThat(handler.canHandle(exception)).isTrue();
        assertThat(response.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getMessage()).contains("'file'");
    }

    @Test
    void malformedMultipartRequestIsABadRequest() {
        var exception = new MultipartException("Failed to parse multipart servlet request");

        assertThat(handler.canHandle(exception)).isTrue();
        assertThat(handler.handle(exception).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void oversizedUploadIsLeftToTheDefaultHandling() {
        assertThat(handler.canHandle(new MaxUploadSizeExceededException(1024))).isFalse();
    }

    @Test
    void otherExceptionsAreNotHandled() {
        assertThat(handler.canHandle(new IllegalStateException("boom"))).isFalse();
    }
}
