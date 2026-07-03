package be.appify.prefab.processor.event.asyncapi;

import be.appify.prefab.processor.PrefabProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.testing.compile.Compilation;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.StreamSupport;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventSchemaDocumentationWriterTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void publishedEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/asyncapi/simple/source/OrderCreated.java"));
        assertThat(compilation).succeeded();
        var asyncApi = readAsyncApi(compilation);

        assertEquals("2.6.0", asyncApi.at("/asyncapi").asText());
        assertEquals("Application Events", asyncApi.at("/info/title").asText());
        assertEquals("1.0.0", asyncApi.at("/info/version").asText());
        assertEquals("#/components/messages/OrderCreated", asyncApi.at("/channels/orders/publish/message/$ref").asText());
        assertEquals("#/components/schemas/OrderCreated", asyncApi.at("/components/messages/OrderCreated/payload/$ref").asText());
        assertEquals("object", asyncApi.at("/components/schemas/OrderCreated/type").asText());
        assertEquals("string", asyncApi.at("/components/schemas/OrderCreated/properties/orderId/type").asText());
        assertEquals("string", asyncApi.at("/components/schemas/OrderCreated/properties/customerId/type").asText());

        var required = StreamSupport.stream(asyncApi.at("/components/schemas/OrderCreated/required").spliterator(), false)
                .map(JsonNode::asText)
                .toList();
        assertEquals(2, required.size());
        assertTrue(required.contains("orderId"));
        assertTrue(required.contains("customerId"));
    }

    @Test
    void consumedEvent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/asyncapi/consumed/source/OrderReceived.java"),
                        sourceOf("event/asyncapi/consumed/source/Order.java")
                );
        assertThat(compilation).succeeded();
        var asyncApi = readAsyncApi(compilation);

        var channels = asyncApi.path("channels");
        var hasSubscribe = false;
        var hasPublish = false;
        for (var channel : channels) {
            hasSubscribe = hasSubscribe || !channel.path("subscribe").isMissingNode();
            hasPublish = hasPublish || !channel.path("publish").isMissingNode();
        }

        assertTrue(hasSubscribe);
        assertFalse(hasPublish);
    }

    @Test
    void docField() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/asyncapi/docfield/source/OrderDocumented.java"));
        assertThat(compilation).succeeded();
        var asyncApi = readAsyncApi(compilation);

        assertEquals(
                "Unique identifier of the order",
                asyncApi.at("/components/schemas/OrderDocumented/properties/orderId/description").asText()
        );
    }

    private static JsonNode readAsyncApi(Compilation compilation) {
        var file = compilation.generatedFile(
                        StandardLocation.CLASS_OUTPUT,
                        "",
                        "META-INF/async-api/asyncapi.json"
                )
                .orElseThrow(() -> new AssertionError("Generated asyncapi.json was not found"));

        try {
            return OBJECT_MAPPER.readTree(file.getCharContent(false).toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse generated asyncapi.json", e);
        }
    }
}
