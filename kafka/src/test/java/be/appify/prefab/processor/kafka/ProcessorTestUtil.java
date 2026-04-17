package be.appify.prefab.processor.kafka;

import com.google.testing.compile.JavaFileObjects;
import org.springframework.core.io.ClassPathResource;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ProcessorTestUtil {
    private ProcessorTestUtil() {
    }

    public static String contentsOf(String fileName) {
        try {
            return new ClassPathResource(fileName).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaFileObject sourceOf(String name) {
        try {
            var resource = new ClassPathResource(name).getURL();
            return JavaFileObjects.forResource(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
