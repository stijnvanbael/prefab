package be.appify.prefab.processor.event;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static com.google.common.truth.Truth.assertThat;

class GenericEventBindingTest {
    @Test
    void discoversRepeatedBindingsWithoutHandlersAndGeneratesTypedRegistrars() throws Exception {
        var source = JavaFileObjects.forSourceString("example.Changed", """
                package example;
                import be.appify.prefab.core.annotations.Event;
                import be.appify.prefab.core.annotations.PartitioningKey;
                @Event(topic = "strings", typeArguments = String.class)
                @Event(topic = "numbers", typeArguments = Integer.class)
                public record Changed<T>(@PartitioningKey String id, T payload) {}
                """);

        var compilation = javac().withProcessors(new PrefabProcessor()).compile(source);

        assertThat(compilation).succeeded();
        var registrars = compilation.generatedSourceFiles().stream()
                .filter(file -> file.getName().endsWith("EventTypeRegistrar.java")).toList();
        assertThat(registrars).hasSize(2);
        var generated = new StringBuilder();
        for (var registrar : registrars) {
            generated.append(registrar.getCharContent(false));
        }
        assertThat(generated.toString()).contains("EventType<Changed<String>>");
        assertThat(generated.toString()).contains("EventType<Changed<Integer>>");
        assertThat(generated.toString()).contains("registry.register(\"strings\"");
        assertThat(generated.toString()).contains("registry.register(\"numbers\"");
    }

    @Test
    void rejectsWrongArgumentCount() {
        var source = JavaFileObjects.forSourceString("example.Changed", """
                package example;
                import be.appify.prefab.core.annotations.Event;
                @Event(topic = "changes")
                public record Changed<T>(T payload) {}
                """);

        var compilation = javac().withProcessors(new PrefabProcessor()).compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("exactly 1 concrete arguments");
    }

    @Test
    void rejectsBoundViolations() {
        var source = JavaFileObjects.forSourceString("example.Changed", """
                package example;
                import be.appify.prefab.core.annotations.Event;
                @Event(topic = "changes", typeArguments = String.class)
                public record Changed<T extends Number>(T payload) {}
                """);

        var compilation = javac().withProcessors(new PrefabProcessor()).compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("violates bound");
    }

    @ParameterizedTest
    @CsvSource({
            "String.class, T extends Comparable<T>",
            "'{String.class, String.class}', 'T, U extends Comparable<? super T>'"
    })
    void resolvesSelfReferentialAndDependentBounds(String arguments, String parameters) {
        var source = JavaFileObjects.forSourceString("example.Changed", """
                package example;
                import be.appify.prefab.core.annotations.Event;
                @Event(topic = "changes", typeArguments = %s)
                public record Changed<%s>(String id, T payload) {}
                """.formatted(arguments, parameters));

        assertThat(javac().withProcessors(new PrefabProcessor()).compile(source)).succeeded();
    }

    @ParameterizedTest
    @CsvSource({
            "java.util.List.class, concrete non-generic classes",
            "int.class, concrete non-generic classes",
            "String[].class, concrete non-generic classes"
    })
    void rejectsUnsupportedArgumentsWithoutCrashingProcessor(String argument, String diagnostic) {
        var source = JavaFileObjects.forSourceString("example.Changed", """
                package example;
                import be.appify.prefab.core.annotations.Event;
                @Event(topic = "changes", typeArguments = %s)
                public record Changed<T>(T payload) {}
                """.formatted(argument));

        var compilation = javac().withProcessors(new PrefabProcessor()).compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining(diagnostic);
    }

    @Test
    void rejectsDuplicateConcreteBindings() {
        var source = JavaFileObjects.forSourceString("example.Changed", """
                package example;
                import be.appify.prefab.core.annotations.Event;
                @Event(topic = "first", typeArguments = String.class)
                @Event(topic = "second", typeArguments = String.class)
                public record Changed<T>(T payload) {}
                """);

        var compilation = javac().withProcessors(new PrefabProcessor()).compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Duplicate @Event binding");
    }

    @Test
    void reportsIncompleteAvroSupportRatherThanGeneratingErasedConverters() {
        var source = JavaFileObjects.forSourceString("example.Changed", """
                package example;
                import be.appify.prefab.core.annotations.Event;
                @Event(topic = "changes", typeArguments = String.class, serialization = Event.Serialization.AVRO)
                public record Changed<T>(T payload) {}
                """);

        var compilation = javac().withProcessors(new PrefabProcessor()).compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Generic Avro event bindings are not implemented");
    }

    @ParameterizedTest
    @CsvSource({"Changed", "Changed<?>", "Changed<Long>", "Changed<T>"})
    void rejectsHandlersWithoutADeclaredConcreteBinding(String parameter) {
        var source = JavaFileObjects.forSourceString("example.Handlers", """
                package example;
                import be.appify.prefab.core.annotations.Event;
                import be.appify.prefab.core.annotations.EventHandler;
                import org.springframework.stereotype.Component;
                @Event(topic = "strings", typeArguments = String.class)
                record Changed<T>(T payload) {}
                @Component
                public class Handlers<T> {
                    @EventHandler public void changed(%s event) {}
                }
                """.formatted(parameter));

        var compilation = javac().withProcessors(new PrefabProcessor()).compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("must use a declared concrete @Event binding");
    }
}
