package be.appify.prefab.processor.kafka;

import com.google.testing.compile.JavaFileObjects;
import org.springframework.core.io.ClassPathResource;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ProcessorTestUtil {
    private ProcessorTestUtil() {
    }

    public static String contentsOf(String fileName) throws IOException {
        return new ClassPathResource(fileName).getContentAsString(StandardCharsets.UTF_8);
    }

    public static JavaFileObject sourceOf(String name) throws IOException {
        var resource = new ClassPathResource(name).getURL();
        return JavaFileObjects.forResource(resource);
    }
}
