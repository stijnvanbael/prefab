package be.appify.prefab.processor.event;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class BoundEventPublisherCompilationTest {
    @Test
    void boundPublisherRejectsDifferentGenericPayloadAtCompileTime() {
        var source = JavaFileObjects.forSourceString("example.Publication", """
                package example;
                import be.appify.prefab.core.domain.EventPublisher;
                import be.appify.prefab.core.domain.EventType;

                class Publication {
                    record Changed<T>(T payload) {}
                    static final EventPublisher<Changed<String>> CHANGES =
                            EventPublisher.bind(new EventType<Changed<String>>() {});

                    void publish() {
                        CHANGES.publish(new Changed<Integer>(42));
                    }
                }
                """);

        var compilation = javac().withOptions("-proc:none").compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("incompatible types");
    }

    @Test
    void legacyPublicationProducesDeprecationWarning() {
        var source = JavaFileObjects.forSourceString("example.Publication", """
                package example;
                import be.appify.prefab.core.domain.PublishesEvents;

                class Publication implements PublishesEvents {
                    void fire() {
                        publish("legacy");
                    }
                }
                """);

        var compilation = javac().withOptions("-proc:none", "-Xlint:deprecation").compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation).hadWarningContaining("deprecated");
    }

    @Test
    void aggregateWithStaticBoundPublisherGeneratesWithoutPersistingHandle() {
        var source = JavaFileObjects.forSourceString("example.Order", """
                package example;
                import be.appify.prefab.core.annotations.Aggregate;
                import be.appify.prefab.core.annotations.rest.Create;
                import be.appify.prefab.core.domain.EventPublisher;
                import be.appify.prefab.core.service.Reference;
                import org.springframework.data.annotation.Id;
                import org.springframework.data.annotation.Version;

                @Aggregate
                public record Order(@Id Reference<Order> id, @Version long version, String name) {
                    private static final EventPublisher<Created> CREATED = EventPublisher.bind(Created.class);
                    public record Created(String name) {}

                    @Create
                    public Order(String name) {
                        this(Reference.create(), 0L, name);
                        CREATED.publish(new Created(name));
                    }
                }
                """);

        var compilation = javac().withProcessors(new PrefabProcessor()).compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("example.application.OrderService")
                .contentsAsUtf8String().doesNotContain("EventPublisher");
    }
}
