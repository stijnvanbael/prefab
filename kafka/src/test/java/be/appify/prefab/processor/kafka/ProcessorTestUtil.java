package be.appify.prefab.processor.kafka;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.springframework.core.io.ClassPathResource;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
        try {
            var resource = new ClassPathResource(name).getURL();
            return JavaFileObjects.forResource(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
