package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.avro.processor.ProcessorTestUtil.generatedSourceOf;
import static be.appify.prefab.avro.processor.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class EventSnapshotTest {

    @Test
    void generatesSnapshotRecordsFromAvsc() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/avro/snapshot/source/Snapshots.java"));
        assertThat(compilation).succeeded();

        var source = generatedSourceOf(compilation, "event.avro.snapshot.UserCreatedV1");
        var nested = generatedSourceOf(compilation, "event.avro.snapshot.AddressV1");

        assertThat(source).contains("public record UserCreatedV1(");
        assertThat(source).contains("String firstName");
        assertThat(source).contains("Integer age");
        assertThat(source).contains("List<String> tags");
        assertThat(source).contains("AddressV1 address");
        assertThat(nested).contains("public record AddressV1(String line)");
    }

    @Test
    void rejectsMissingAvscFile() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(com.google.testing.compile.JavaFileObjects.forSourceString("event.Missing", """
                        package event;

                        @be.appify.prefab.core.annotations.EventSnapshot(avsc = "does/not/exist.avsc")
                        class Missing {
                        }
                        """));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("AVSC file not found");
    }
}
