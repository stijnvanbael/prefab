package be.appify.prefab.processor.test;

import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
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

    public static Path compileDependencyClasspath(JavaFileObject... sources) {
        try {
            var outputDirectory = Files.createTempDirectory("prefab-annotation-test-dependency");
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
