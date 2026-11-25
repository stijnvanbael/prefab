package be.appify.prefab.processor.error;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.classpathOptionsWith;

/**
 * Negative compilation tests for the failure categories described in {@code docs}: expected input failures,
 * unexpected plugin failures, resource failures and fatal errors.
 */
class ProcessorFailureDiagnosticsTest {

    private static JavaFileObject aggregate(String name) {
        return JavaFileObjects.forSourceString("test.failure." + name, """
                package test.failure;

                import be.appify.prefab.core.annotations.Aggregate;
                import be.appify.prefab.core.annotations.rest.Create;
                import be.appify.prefab.core.service.Reference;
                import org.springframework.data.annotation.Id;
                import org.springframework.data.annotation.Version;

                @Aggregate
                public record %s(@Id Reference<%s> id, @Version long version, String name) {
                    @Create
                    public %s(String name) {
                        this(Reference.create(), 0L, name);
                    }
                }
                """.formatted(name, name, name));
    }

    private static Compilation compileWith(String failureMode, String... extraOptions) {
        var options = new java.util.ArrayList<String>();
        options.add("-A" + FailureInjectingTestPlugin.OPTION + "=" + failureMode);
        options.addAll(List.of(extraOptions));
        return javac().withProcessors(new PrefabProcessor()).withOptions(options)
                .compile(aggregate("Alpha"), aggregate("Beta"));
    }

    private static List<String> errors(Compilation compilation) {
        return compilation.errors().stream().map(d -> d.getMessage(null)).toList();
    }

    private static void assertContains(String message, String... fragments) {
        for (var fragment : fragments) {
            assertTrue(message.contains(fragment), () -> "Expected <" + fragment + "> in:\n" + message);
        }
    }

    private static void assertDoesNotContain(String message, String... fragments) {
        for (var fragment : fragments) {
            assertFalse(message.contains(fragment), () -> "Unexpected <" + fragment + "> in:\n" + message);
        }
    }

    @Test
    @DisplayName("an expected failure from a plugin is one actionable error per aggregate with element and fix")
    void expectedPluginFailure_reportsOneActionableErrorPerIndependentAggregate() {
        var compilation = compileWith("expected");

        assertThat(compilation).failed();
        assertEquals(2, compilation.errors().size(), () -> errors(compilation).toString());
        compilation.errors().forEach(error -> {
            assertContains(error.getMessage(null), "[@Aggregate]", "injected invalid input",
                    "Suggested fix: remove the injected failure",
                    "Caused by: java.lang.IllegalStateException: root cause");
            assertDoesNotContain(error.getMessage(null), "Internal error");
            assertNotNull(error.getSource());
        });
        assertEquals(2, compilation.errors().stream().map(d -> d.getSource().getName()).distinct().count());
    }

    @Test
    @DisplayName("an unexpected plugin failure names the plugin, keeps the cause chain and hints at the stack trace")
    void unexpectedPluginFailure_namesPluginAndCause() {
        var compilation = compileWith("unexpected");

        assertThat(compilation).failed();
        assertEquals(2, compilation.errors().size());
        var message = errors(compilation).getFirst();
        assertContains(message,
                "Internal error in plugin " + FailureInjectingTestPlugin.class.getName(),
                "java.lang.IllegalStateException: injected bug",
                "Caused by: java.lang.NullPointerException: inner cause",
                "-Aprefab.diagnostics.stacktrace=true");
        assertDoesNotContain(message, "\tat ");
        assertTrue(errors(compilation).stream().allMatch(error -> error.contains("Internal error")),
                () -> "no consequent compiler errors expected, but got " + errors(compilation));
    }

    @Test
    @DisplayName("-Aprefab.diagnostics.stacktrace=true adds the full stack trace of an unexpected failure")
    void unexpectedPluginFailure_withStackTraceOption_includesStackTrace() {
        var compilation = compileWith("unexpected", "-A" + ErrorReporter.STACKTRACE_OPTION + "=true");

        assertThat(compilation).failed();
        assertContains(errors(compilation).getFirst(),
                "\tat " + FailureInjectingTestPlugin.class.getName() + ".writeService",
                "Caused by: java.lang.NullPointerException: inner cause");
    }

    @Test
    @DisplayName("fatal JVM errors are never caught and swallowed")
    void fatalError_isNotCaught() {
        var thrown = assertThrows(Throwable.class, () -> compileWith("fatal"));

        var cause = thrown;
        while (cause.getCause() != null && !(cause instanceof AssertionError)) {
            cause = cause.getCause();
        }
        assertTrue(cause instanceof AssertionError, () -> "Expected an AssertionError but got " + thrown);
        assertEquals("injected fatal error", cause.getMessage());
    }

    @Test
    @DisplayName("an unsupported type yields an actionable error instead of an uncaught exception")
    void unsupportedType_reportsActionableError() {
        var compilation = javac().withProcessors(new PrefabProcessor()).compile(
                JavaFileObjects.forSourceString("test.failure.Generic", """
                        package test.failure;

                        import be.appify.prefab.core.annotations.Aggregate;
                        import be.appify.prefab.core.service.Reference;
                        import org.springframework.data.annotation.Id;
                        import org.springframework.data.annotation.Version;

                        @Aggregate
                        public record Generic<T extends Number & Comparable<T>>(
                                @Id Reference<Generic<T>> id, @Version long version, T value) {}
                        """));

        assertThat(compilation).failed();
        assertTrue(errors(compilation).stream().anyMatch(message -> message.contains("cannot map type")
                        && message.contains("Suggested fix") && !message.contains("Internal error")),
                () -> "Unexpected diagnostics: " + errors(compilation));
    }

    @Test
    @DisplayName("an invalid migration transition yields an error naming the file and a corrective action")
    void invalidMigrationTransition_reportsFileAndFix() throws IOException {
        var classpath = Files.createTempDirectory("prefab-failure-migration");
        var migrations = Files.createDirectories(classpath.resolve(Path.of("db", "migration")));
        Files.writeString(migrations.resolve("V1__baseline.sql"), "CREATE TABLE chapter (id VARCHAR(255));");
        Files.writeString(migrations.resolve("V2__broken.sql"), "ALTER TABLE chapter DROP COLUMN missing;");

        var compilation = javac().withProcessors(new PrefabProcessor())
                .withOptions(classpathOptionsWith(classpath))
                .compile(aggregate("Alpha"));

        assertThat(compilation).failed();
        assertTrue(errors(compilation).stream().anyMatch(message ->
                        message.contains("V2__broken.sql") && message.contains("cannot interpret database migration")
                                && message.contains("Cannot drop non-existing column: missing")
                                && message.contains("Suggested fix") && !message.contains("Internal error")),
                () -> "Unexpected diagnostics: " + errors(compilation));
    }
}
