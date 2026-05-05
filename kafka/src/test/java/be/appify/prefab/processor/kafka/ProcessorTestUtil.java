package be.appify.prefab.processor.kafka;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.springframework.core.io.ClassPathResource;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

    public static Path compileDependencyClasspath(JavaFileObject... sources) {
        try {
            var outputDirectory = Files.createTempDirectory("prefab-kafka-test-dependency");
            compileSourcesToDirectory(outputDirectory, sources);
            return outputDirectory;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> classpathOptionsWith(Path additionalClasspathEntry) {
        var classpathEntries = new ArrayList<String>();
        classpathEntries.add(additionalClasspathEntry.toString());
        classpathEntries.add(System.getProperty("java.class.path"));
        return List.of("-classpath", String.join(java.io.File.pathSeparator, classpathEntries));
    }

    private static void compileSourcesToDirectory(Path outputDirectory, JavaFileObject... sources) {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("System Java compiler is not available");
        }
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(outputDirectory));
            var options = List.of("-classpath", System.getProperty("java.class.path"));
            var task = compiler.getTask(null, fileManager, diagnostics, options, null, List.of(sources));
            if (!Boolean.TRUE.equals(task.call())) {
                var errors = diagnostics.getDiagnostics().stream().map(Object::toString).toList();
                throw new IllegalStateException("Dependency compilation failed: " + String.join("\n", errors));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
