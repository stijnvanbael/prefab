package be.appify.prefab.avro.processor;

import static be.appify.prefab.avro.processor.ProcessorTestUtil.classpathOptionsWith;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Negative compilation tests for invalid AVSC resources. */
class MalformedAvscDiagnosticsTest {

    @Test
    @DisplayName("a malformed AVSC file yields an actionable error naming the file instead of a processor crash")
    void malformedAvsc_reportsActionableError() throws IOException {
        var classpath = Files.createTempDirectory("prefab-malformed-avsc");
        Files.createDirectories(classpath.resolve("bad"));
        Files.writeString(classpath.resolve("bad/Broken.avsc"), "{ \"type\": \"record\", \"name\": ");

        var compilation = javac().withProcessors(new PrefabProcessor())
                .withOptions(classpathOptionsWith(classpath))
                .compile(JavaFileObjects.forSourceString("bad.BrokenAvsc", """
                        package bad;

                        import be.appify.prefab.core.annotations.Avsc;
                        import be.appify.prefab.core.annotations.AvscFile;
                        import be.appify.prefab.core.annotations.Event;

                        @Event(topic = "broken", serialization = Event.Serialization.AVRO)
                        @Avsc(files = @AvscFile(path = "bad/Broken.avsc", keyProperty = "name"))
                        public interface BrokenAvsc {
                        }
                        """));

        assertThat(compilation).failed();
        var messages = compilation.errors().stream().map(d -> d.getMessage(null)).toList();
        assertTrue(messages.stream().anyMatch(message -> message.contains("bad/Broken.avsc")
                        && !message.contains("Internal error")),
                () -> "Unexpected diagnostics: " + messages);
    }
}
