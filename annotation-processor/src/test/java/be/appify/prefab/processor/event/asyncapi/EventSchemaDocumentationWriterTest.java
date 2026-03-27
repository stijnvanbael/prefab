package be.appify.prefab.processor.event.asyncapi;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.contentsOf;
import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class EventSchemaDocumentationWriterTest {

    @Test
    void simpleJsonEvent() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/asyncapi/simple/source/SimpleAsyncApiEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "META-INF/async-api/asyncapi.json")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/asyncapi/simple/expected/asyncapi.json"));
    }

    @Test
    void sealedInterfaceEvent() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/asyncapi/sealed/source/SealedAsyncApiEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "META-INF/async-api/asyncapi.json")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/asyncapi/sealed/expected/asyncapi.json"));
    }

    @Test
    void avroEvent() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/asyncapi/avro/source/AvroAsyncApiEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "META-INF/async-api/asyncapi.json")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("event/asyncapi/avro/expected/asyncapi.json"));
    }
}
