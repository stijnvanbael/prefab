package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class GenericAvroBindingTest {
    @Test
    void generatesIndependentSchemasConvertersAndRegistrations() throws Exception {
        var compilation = javac().withProcessors(new PrefabProcessor()).compile(
                JavaFileObjects.forSourceString("generic.Changed", """
                        package generic;
                        import be.appify.prefab.core.annotations.Event;
                        @Event(topic = "strings", serialization = Event.Serialization.AVRO, typeArguments = String.class)
                        @Event(topic = "numbers", serialization = Event.Serialization.AVRO, typeArguments = Integer.class)
                        public record Changed<T>(T payload) {}
                        """));
        assertThat(compilation).succeeded();
        var schemas = compilation.generatedSourceFiles().stream()
                .filter(file -> file.getName().endsWith("SchemaFactory.java")).toList();
        assertThat(schemas).hasSize(2);
        for (var schema : schemas) {
            var source = schema.getCharContent(false).toString();
            assertThat(source).contains("Schema.createRecord(\"Changed_")
                    .containsAnyOf("Schema.Type.STRING", "Schema.Type.INT");
        }
        var registrars = compilation.generatedSourceFiles().stream()
                .filter(file -> file.getName().endsWith("ReaderSchemaRegistrar.java")).toList();
        assertThat(registrars).hasSize(2);
        for (var registrar : registrars) {
            assertThat(registrar.getCharContent(false).toString())
                    .contains("registry.registerAvroConverters(type, writer::convert, reader::convert)")
                    .contains("new EventType<Changed<").doesNotContain("Changed.class");
        }
    }
}
