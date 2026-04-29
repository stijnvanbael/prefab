package be.appify.prefab.avro.processor;

import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.tools.JavaFileObject;
import org.springframework.core.io.ClassPathResource;

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
        URL resource;
        try {
            resource = new ClassPathResource(name).getURL();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return JavaFileObjects.forResource(resource);
    }
}
