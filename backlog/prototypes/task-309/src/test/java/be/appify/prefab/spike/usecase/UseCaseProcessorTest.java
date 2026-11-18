package be.appify.prefab.spike.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UseCaseProcessorTest {
    @Test
    void useCaseMethod_generatesCompilableSpringEndpoint(@TempDir Path tempDirectory) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<>();
        var generatedSources = Files.createDirectories(tempDirectory.resolve("generated"));
        var classes = Files.createDirectories(tempDirectory.resolve("classes"));
        var workflowSource = Path.of(
                "src/main/java/be/appify/prefab/spike/usecase/importing/ProductImportWorkflow.java");

        try (var fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            var classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
            var task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    List.of(
                            "--release", "21",
                            "-classpath", classpath,
                            "-d", classes.toString(),
                            "-s", generatedSources.toString()),
                    null,
                    fileManager.getJavaFileObjectsFromPaths(List.of(workflowSource)));
            task.setProcessors(List.of(new UseCaseProcessor()));

            var compiled = task.call();
            var diagnosticMessages = diagnostics.getDiagnostics().toString();
            assertTrue(compiled, diagnosticMessages);
        }

        var generated = Files.readString(
                generatedSources.resolve(
                        "be/appify/prefab/spike/usecase/importing/http/ProductImportWorkflowEndpoint.java"));
        var expected = Files.readString(Path.of(
                "src/test/resources/generated/ProductImportWorkflowEndpoint.java"));
        assertEquals(normalizeLineEndings(expected), normalizeLineEndings(generated));
        assertTrue(generated.contains("return workflow.execute(request);"));
    }

    private static String normalizeLineEndings(String source) {
        return source.replace("\r\n", "\n");
    }
}
