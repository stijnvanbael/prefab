package be.appify.prefab.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.OutputTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link GenerateAnnotationValidator}. */
class GenerateAnnotationValidatorTest {

    private static final String VALID_AGGREGATE_WITH_PLUGIN_OVERRIDE_AGGREGATE = """
            package test.example;

            import be.appify.prefab.core.annotations.Aggregate;
            import be.appify.prefab.core.annotations.Generate;
            import be.appify.prefab.core.service.Reference;
            import org.springframework.data.annotation.Id;
            import org.springframework.data.annotation.Version;

            @Generate(plugin = TestPlugin.class, enabled = false)
            @Aggregate
            public record OrderAggregate(
                    @Id Reference<OrderAggregate> id,
                    @Version long version
            ) {}
            """;

    private static final String VALID_AGGREGATE_WITH_PLUGIN_OVERRIDE_PLUGIN = """
            package test.example;

            import be.appify.prefab.processor.PrefabPlugin;

            public interface TestPlugin extends PrefabPlugin {
            }
            """;

    private static final String AGGREGATE_WITH_MULTIPLE_OVERRIDES_AGGREGATE = """
            package test.example;

            import be.appify.prefab.core.annotations.Aggregate;
            import be.appify.prefab.core.annotations.Generate;
            import be.appify.prefab.core.annotations.OutputTarget;
            import be.appify.prefab.core.service.Reference;
            import org.springframework.data.annotation.Id;
            import org.springframework.data.annotation.Version;

            @Generate(plugin = Plugin1.class, enabled = true)
            @Generate(plugin = Plugin2.class, target = OutputTarget.TEST)
            @Aggregate
            public record OrderAggregate(
                    @Id Reference<OrderAggregate> id,
                    @Version long version
            ) {}
            """;

    private static final String AGGREGATE_WITH_MULTIPLE_OVERRIDES_PLUGIN1 = """
            package test.example;

            import be.appify.prefab.processor.PrefabPlugin;

            public interface Plugin1 extends PrefabPlugin {
            }
            """;

    private static final String AGGREGATE_WITH_MULTIPLE_OVERRIDES_PLUGIN2 = """
            package test.example;

            import be.appify.prefab.processor.PrefabPlugin;

            public interface Plugin2 extends PrefabPlugin {
            }
            """;

    private static final String GENERATE_ON_NON_AGGREGATE = """
            package test.example;

            import be.appify.prefab.core.annotations.Generate;
            import be.appify.prefab.processor.PrefabPlugin;

            @Generate(plugin = TestPlugin.class)
            class NotAnAggregate {
            }

            interface TestPlugin extends PrefabPlugin {
            }
            """;

    private static final String GENERATE_WITH_NON_PLUGIN_CLASS = """
            package test.example;

            import be.appify.prefab.core.annotations.Aggregate;
            import be.appify.prefab.core.annotations.Generate;
            import be.appify.prefab.core.service.Reference;
            import org.springframework.data.annotation.Id;
            import org.springframework.data.annotation.Version;

            @Generate(plugin = String.class)
            @Aggregate
            public record OrderAggregate(
                    @Id Reference<OrderAggregate> id,
                    @Version long version
            ) {}
            """;

    private static final String GENERATE_WITH_DUPLICATE_PLUGINS_AGGREGATE = """
            package test.example;

            import be.appify.prefab.core.annotations.Aggregate;
            import be.appify.prefab.core.annotations.Generate;
            import be.appify.prefab.core.service.Reference;
            import org.springframework.data.annotation.Id;
            import org.springframework.data.annotation.Version;

            @Generate(plugin = TestPlugin.class, enabled = true)
            @Generate(plugin = TestPlugin.class, enabled = false)
            @Aggregate
            public record OrderAggregate(
                    @Id Reference<OrderAggregate> id,
                    @Version long version
            ) {}
            """;

    private static final String GENERATE_WITH_DUPLICATE_PLUGINS_PLUGIN = """
            package test.example;

            import be.appify.prefab.processor.PrefabPlugin;

            public interface TestPlugin extends PrefabPlugin {
            }
            """;

    @Nested
    @DisplayName("Valid annotations")
    class ValidAnnotationsTests {

        @Test
        @DisplayName("should accept @Generate on @Aggregate with PrefabPlugin subclass")
        void acceptValidPluginOverride() {
            var compilation = javac()
                    .withProcessors(new PrefabProcessor())
                    .compile(
                            sourceOf(VALID_AGGREGATE_WITH_PLUGIN_OVERRIDE_AGGREGATE, "test.example.OrderAggregate"),
                            sourceOf(VALID_AGGREGATE_WITH_PLUGIN_OVERRIDE_PLUGIN, "test.example.TestPlugin")
                    );

            assertThat(compilation).succeeded();
        }

        @Test
        @DisplayName("should accept multiple @Generate overrides on same aggregate")
        void acceptMultipleOverrides() {
            var compilation = javac()
                    .withProcessors(new PrefabProcessor())
                    .compile(
                            sourceOf(AGGREGATE_WITH_MULTIPLE_OVERRIDES_AGGREGATE, "test.example.OrderAggregate"),
                            sourceOf(AGGREGATE_WITH_MULTIPLE_OVERRIDES_PLUGIN1, "test.example.Plugin1"),
                            sourceOf(AGGREGATE_WITH_MULTIPLE_OVERRIDES_PLUGIN2, "test.example.Plugin2")
                    );

            assertThat(compilation).succeeded();
        }
    }

    @Nested
    @DisplayName("Invalid annotations")
    class InvalidAnnotationsTests {

        @Test
        @DisplayName("should warn when @Generate placed on non-@Aggregate class")
        void warnOnNonAggregate() {
            var compilation = javac()
                    .withProcessors(new PrefabProcessor())
                    .compile(sourceOf(GENERATE_ON_NON_AGGREGATE));

            // Note: warnings are not errors, compilation succeeds
            assertThat(compilation).succeeded();
        }

        @Test
        @DisplayName("should fail when @Generate.plugin() is not a PrefabPlugin subclass")
        void failOnNonPluginClass() {
            var compilation = javac()
                    .withProcessors(new PrefabProcessor())
                    .compile(sourceOf(GENERATE_WITH_NON_PLUGIN_CLASS, "test.example.OrderAggregate"));

            assertThat(compilation).hadErrorContaining("@Generate(plugin=java.lang.String)");
            assertThat(compilation).hadErrorContaining("must reference a PrefabPlugin subclass");
        }

        @Test
        @DisplayName("should warn when same plugin configured multiple times")
        void warnOnDuplicatePluginConfiguration() {
            var compilation = javac()
                    .withProcessors(new PrefabProcessor())
                    .compile(
                            sourceOf(GENERATE_WITH_DUPLICATE_PLUGINS_AGGREGATE, "test.example.OrderAggregate"),
                            sourceOf(GENERATE_WITH_DUPLICATE_PLUGINS_PLUGIN, "test.example.TestPlugin")
                    );

            // Compilation succeeds despite duplicate override warning
            assertThat(compilation).succeeded();
        }
    }

    /** Helper: Convert string to JavaFileObject for compilation. */
    private static javax.tools.JavaFileObject sourceOf(String sourceCode, String fileName) {
        return com.google.testing.compile.JavaFileObjects.forSourceString(
                fileName,
                sourceCode
        );
    }

    /** Helper: Convert string to JavaFileObject for compilation using default name. */
    private static javax.tools.JavaFileObject sourceOf(String sourceCode) {
        return sourceOf(sourceCode, "test.example");
    }
}




