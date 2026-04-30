package be.appify.prefab.processor.event.asyncapi;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class EventSchemaDocumentationWriterTest {
    @Test
    void publishedEvent() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/asyncapi/simple/source/OrderCreated.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedFile(
                        javax.tools.StandardLocation.CLASS_OUTPUT,
                        "META-INF/async-api/asyncapi.json"
                )
                .contentsAsUtf8String()
                .isEqualTo("""
                        {
                          "asyncapi": "2.6.0",
                          "info": {
                            "title": "Application Events",
                            "version": "1.0.0"
                          },
                          "channels": {
                            "orders": {
                              "publish": {
                                "message": {
                                  "$ref": "#/components/messages/OrderCreated"
                                }
                              }
                            }
                          },
                          "components": {
                            "messages": {
                              "OrderCreated": {
                                "payload": {
                                  "$ref": "#/components/schemas/OrderCreated"
                                }
                              }
                            },
                            "schemas": {
                              "OrderCreated": {
                                "type": "object",
                                "properties": {
                                  "orderId": {
                                    "type": "string"
                                  },
                                  "customerId": {
                                    "type": "string"
                                  }
                                },
                                "required": ["orderId", "customerId"]
                              }
                            }
                          }
                        }
                        """);
    }

    @Test
    void consumedEvent() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/asyncapi/consumed/source/OrderReceived.java"),
                        sourceOf("event/asyncapi/consumed/source/Order.java")
                );
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedFile(
                        javax.tools.StandardLocation.CLASS_OUTPUT,
                        "META-INF/async-api/asyncapi.json"
                )
                .contentsAsUtf8String()
                .contains("\"subscribe\"");
        assertThat(compilation).generatedFile(
                        javax.tools.StandardLocation.CLASS_OUTPUT,
                        "META-INF/async-api/asyncapi.json"
                )
                .contentsAsUtf8String()
                .doesNotContain("\"publish\"");
    }

    @Test
    void docField() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/asyncapi/docfield/source/OrderDocumented.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedFile(
                        javax.tools.StandardLocation.CLASS_OUTPUT,
                        "META-INF/async-api/asyncapi.json"
                )
                .contentsAsUtf8String()
                .contains("\"description\": \"Unique identifier of the order\"");
    }
}
