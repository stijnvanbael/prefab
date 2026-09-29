package be.appify.prefab.processor.sns;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.springframework.core.io.ClassPathResource;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ProcessorTestUtil {
    private static final String CLASS_OUTPUT = "/CLASS_OUTPUT/";

    private ProcessorTestUtil() {
    }

    public static String contentsOf(String fileName) throws IOException {
        return new ClassPathResource(fileName).getContentAsString(StandardCharsets.UTF_8);
    }

    public static JavaFileObject sourceOf(String name) throws IOException {
        var resource = new ClassPathResource(name).getURL();
        return JavaFileObjects.forResource(resource);
    }

    public static String generatedSourceOf(Compilation compilation, String generatedTypeName) {
        return generatedSourceContentsOf(compilation, generatedTypeName);
    }

    public static void assertGeneratedSourceEqualsIgnoringWhitespace(
            Compilation compilation,
            String generatedTypeName,
            String expectedFileName
    ) throws IOException {
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

    /**
     * Returns a class loader that loads the classes produced by the given compilation, so generated code can be
     * exercised at runtime.
     */
    public static ClassLoader classLoaderOf(Compilation compilation) {
        var classFiles = compilation.generatedFiles().stream()
                .filter(file -> file.getKind() == JavaFileObject.Kind.CLASS)
                .collect(Collectors.toMap(ProcessorTestUtil::binaryName, Function.identity()));
        return new ClassLoader(ProcessorTestUtil.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                var classFile = classFiles.get(name);
                if (classFile == null) {
                    throw new ClassNotFoundException(name);
                }
                try (var input = classFile.openInputStream()) {
                    var bytes = input.readAllBytes();
                    return defineClass(name, bytes, 0, bytes.length);
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
        };
    }

    private static String binaryName(JavaFileObject classFile) {
        var path = classFile.toUri().getPath();
        return path.substring(path.indexOf(CLASS_OUTPUT) + CLASS_OUTPUT.length(), path.length() - ".class".length())
                .replace('/', '.');
    }
}
