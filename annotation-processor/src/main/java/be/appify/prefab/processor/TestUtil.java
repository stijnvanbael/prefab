package be.appify.prefab.processor;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class TestUtil {
    public static String idOf(ResultActions result) {
        var location = result.andReturn().getResponse().getHeader("Location");
        assert location != null;
        return location.substring(location.lastIndexOf('/') + 1);
    }

    public static MockMultipartFile mockMultipartFile(MultipartFile file) {
        if (file instanceof MockMultipartFile) {
            return (MockMultipartFile) file;
        } else {
            try {
                return new MockMultipartFile(
                        file.getName(),
                        file.getOriginalFilename(),
                        file.getContentType(),
                        file.getBytes()
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
