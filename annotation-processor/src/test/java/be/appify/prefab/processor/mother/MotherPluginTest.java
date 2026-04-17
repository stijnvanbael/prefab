package be.appify.prefab.processor.mother;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class MotherPluginTest {

    @Test
    void compilationSucceedsWithExampleAnnotations() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/person/source/Person.java"));

        assertThat(compilation).succeeded();
    }

    @Test
    void requestRecordContainsSchemaExampleAnnotationFromExampleOnConstructorParam() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/person/source/Person.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mother.person.application.CreatePersonRequest")
                .contentsAsUtf8String()
                .contains("example = \"Alice\"");
        assertThat(compilation)
                .generatedSourceFile("mother.person.application.CreatePersonRequest")
                .contentsAsUtf8String()
                .contains("example = \"alice@example.com\"");
    }

    @Test
    void updateRequestRecordContainsSchemaExampleAnnotation() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/person/source/Person.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mother.person.application.PersonUpdateRequest")
                .contentsAsUtf8String()
                .contains("example = \"Bob\"");
    }

    @Test
    void responseRecordContainsSchemaExampleAnnotationFromAggregateField() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/person/source/Person.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mother.person.infrastructure.http.PersonResponse")
                .contentsAsUtf8String()
                .contains("example = \"Alice\"");
        assertThat(compilation)
                .generatedSourceFile("mother.person.infrastructure.http.PersonResponse")
                .contentsAsUtf8String()
                .contains("example = \"alice@example.com\"");
    }

    @Test
    void invalidExampleOnNumericFieldProducesCompilerError() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/invalideexample/source/BadExample.java"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("cannot be parsed as int");
    }

    @Test
    void compilationSucceedsForEventTypes() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/events/source/PersonEvent.java"));

        assertThat(compilation).succeeded();
    }

    @Test
    void motherGeneratedForMultiFieldRecordNestedInsideSingleValueType() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mother/nestedinwrapper/source/OrderEvent.java"));

        // The annotation processor must not throw when processing a single-value type whose
        // sole field is itself a multi-field record.  If writeNestedMothersFor does not recurse
        // through the single-value wrapper, or if typeDefaultValue generates invalid code for it,
        // the compilation will fail.
        assertThat(compilation).succeeded();
    }
}

