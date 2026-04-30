package be.appify.prefab.processor;

import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class JavaFileWriterTest {

    @Test
    void noteEmittedWhenGenerationSkippedDueToExistingSourceFile() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/override/source/Widget.java"),
                        sourceOf("rest/override/source/application/WidgetService.java")
                );

        assertThat(compilation).hadNoteContaining("rest.override.application.WidgetService");
        assertThat(compilation).hadNoteContaining("already exists");
    }
}
