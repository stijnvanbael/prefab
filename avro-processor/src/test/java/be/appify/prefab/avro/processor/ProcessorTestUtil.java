package be.appify.prefab.avro.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.tools.JavaFileObject;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

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

    public static void assertGeneratedSourceEqualsIgnoringWhitespace(
            Compilation compilation,
            String generatedTypeName,
            String expectedFileName
    ) {
        var actualCode = generatedSourceContentsOf(compilation, generatedTypeName);
        var expectedCode = contentsOf(expectedFileName);
        assertThat(actualCode).isEqualToIgnoringWhitespace(expectedCode);
    }

    private static String generatedSourceContentsOf(Compilation compilation, String generatedTypeName) {
        var expectedSuffix = "/" + generatedTypeName.replace('.', '/') + ".java";
        var generatedFile = compilation.generatedSourceFiles().stream()
                .filter(file -> file.toUri().getPath().endsWith(expectedSuffix))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Generated source file not found: " + generatedTypeName));
        try {
            return generatedFile.getCharContent(true).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
