package be.appify.prefab.processor.kafka;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.Compilation;
import java.io.IOException;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.kafka.ProcessorTestUtil.generatedSourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class GenericKafkaBindingTest {
    @Test
    void generatesDifferentListenersWithOnlyTheirOwnTopics() {
        var compilation = javac().withProcessors(new PrefabProcessor()).compile(
                JavaFileObjects.forSourceString("example.Changed", """
                        package example;
                        import be.appify.prefab.core.annotations.Event;
                        @Event(topic = "strings", typeArguments = String.class, platform = Event.Platform.KAFKA)
                        @Event(topic = "numbers", typeArguments = Integer.class, platform = Event.Platform.KAFKA)
                        public record Changed<T>(String id, T payload) {}
                        """),
                JavaFileObjects.forSourceString("example.Handlers", """
                        package example;
                        import be.appify.prefab.core.annotations.EventHandler;
                        import org.springframework.stereotype.Component;
                        @Component
                        public class Handlers {
                            @EventHandler public void strings(Changed<String> event) {}
                            @EventHandler public void numbers(Changed<Integer> event) {}
                        }
                        """));

        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "example.infrastructure.kafka.HandlersKafkaConsumer");
        assertThat(source).contains("topics = \"strings\"", "topics = \"numbers\"",
                "Changed<String> event", "Changed<Integer> event", "prefab-event-type",
                "handlers.strings(event)", "handlers.numbers(event)");
    }

    @Test
    void listenersSharingATopicInvokeOnlyTheirConcreteHandler() throws ReflectiveOperationException {
        var compilation = javac().withProcessors(new PrefabProcessor()).compile(
                JavaFileObjects.forSourceString("example.Changed", """
                        package example;
                        import be.appify.prefab.core.annotations.Event;
                        @Event(topic = "shared", typeArguments = String.class, platform = Event.Platform.KAFKA)
                        @Event(topic = "shared", typeArguments = Integer.class, platform = Event.Platform.KAFKA)
                        public record Changed<T>(String id, T payload) {}
                        """),
                JavaFileObjects.forSourceString("example.Handlers", """
                        package example;
                        import be.appify.prefab.core.annotations.EventHandler;
                        import org.springframework.stereotype.Component;
                        @Component
                        public class Handlers {
                            public int strings;
                            public int numbers;
                            @EventHandler public void strings(Changed<String> event) { strings++; }
                            @EventHandler public void numbers(Changed<Integer> event) { numbers++; }
                        }
                        """));
        assertThat(compilation).succeeded();
        var loader = new CompilationClassLoader(compilation);
        var handlerClass = loader.loadClass("example.Handlers");
        var handlers = handlerClass.getConstructor().newInstance();
        var eventClass = loader.loadClass("example.Changed");
        var consumer = loader.loadClass("example.infrastructure.kafka.HandlersKafkaConsumer")
                .getConstructor(handlerClass).newInstance(handlers);
        var event = eventClass.getConstructor(String.class, Object.class).newInstance("id", "payload");

        for (var method : consumer.getClass().getDeclaredMethods()) {
            if (method.getName().startsWith("onChanged")) {
                method.invoke(consumer, event, "example.Changed<java.lang.String>");
            }
        }

        assertThat(handlerClass.getField("strings").get(handlers)).isEqualTo(1);
        assertThat(handlerClass.getField("numbers").get(handlers)).isEqualTo(0);
    }

    @Test
    void selectsPlatformFromTheConcreteBinding() {
        var compilation = javac().withProcessors(new PrefabProcessor()).compile(
                JavaFileObjects.forSourceString("example.Changed", """
                        package example;
                        import be.appify.prefab.core.annotations.Event;
                        @Event(topic = "strings", typeArguments = String.class, platform = Event.Platform.KAFKA)
                        @Event(topic = "numbers", typeArguments = Integer.class, platform = Event.Platform.PUB_SUB)
                        public record Changed<T>(String id, T payload) {}
                        """),
                JavaFileObjects.forSourceString("example.Handlers", """
                        package example;
                        import be.appify.prefab.core.annotations.EventHandler;
                        import org.springframework.stereotype.Component;
                        @Component
                        public class Handlers {
                            @EventHandler public void strings(Changed<String> event) {}
                            @EventHandler public void numbers(Changed<Integer> event) {}
                        }
                        """));

        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "example.infrastructure.kafka.HandlersKafkaConsumer");
        assertThat(source).contains("Changed<String> event", "topics = \"strings\"")
                .doesNotContain("Changed<Integer>", "topics = \"numbers\"", "handlers.numbers");
    }

    private static final class CompilationClassLoader extends ClassLoader {
        private final Compilation compilation;

        private CompilationClassLoader(Compilation compilation) {
            super(GenericKafkaBindingTest.class.getClassLoader());
            this.compilation = compilation;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            var file = compilation.generatedFile(StandardLocation.CLASS_OUTPUT, name.replace('.', '/') + ".class");
            if (file.isEmpty()) {
                throw new ClassNotFoundException(name);
            }
            try (var input = file.get().openInputStream()) {
                var bytes = input.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
