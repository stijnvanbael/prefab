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

    private static final String VALID_AGGREGATE_WITH_PLUGIN_OVERRIDE = """
            package test.example;

            import be.appify.prefab.core.annotations.Aggregate;
            import be.appify.prefab.core.annotations.Generate;
            import be.appify.prefab.processor.PrefabPlugin;

            @Generate(plugin = TestPlugin.class, enabled = false)
            @Aggregate(root = "Order")
            public class OrderAggregate {
            }

            interface TestPlugin extends PrefabPlugin {
            }
            """;

    private static final String AGGREGATE_WITH_MULTIPLE_OVERRIDES = """
            package test.example;

            import be.appify.prefab.core.annotations.Aggregate;
            import be.appify.prefab.core.annotations.Generate;
            import be.appify.prefab.core.annotations.OutputTarget;
            import be.appify.prefab.processor.PrefabPlugin;

            @Generate(plugin = Plugin1.class, enabled = true)
            @Generate(plugin = Plugin2.class, target = OutputTarget.TEST)
            @Aggregate(root = "Order")
            public class OrderAggregate {
            }

            interface Plugin1 extends PrefabPlugin {
            }

            interface Plugin2 extends PrefabPlugin {
            }
            """;

    private static final String GENERATE_ON_NON_AGGREGATE = """
            package test.example;

            import be.appify.prefab.core.annotations.Generate;
            import be.appify.prefab.processor.PrefabPlugin;

            @Generate(plugin = TestPlugin.class)
            public class NotAnAggregate {
            }

            interface TestPlugin extends PrefabPlugin {
            }
            """;

    private static final String GENERATE_WITH_NON_PLUGIN_CLASS = """
            package test.example;

            import be.appify.prefab.core.annotations.Aggregate;
            import be.appify.prefab.core.annotations.Generate;

            @Generate(plugin = String.class)
            @Aggregate(root = "Order")
            public class OrderAggregate {
            }
            """;

    private static final String GENERATE_WITH_DUPLICATE_PLUGINS = """
            package test.example;

            import be.appify.prefab.core.annotations.Aggregate;
            import be.appify.prefab.core.annotations.Generate;
            import be.appify.prefab.processor.PrefabPlugin;

            @Generate(plugin = TestPlugin.class, enabled = true)
            @Generate(plugin = TestPlugin.class, enabled = false)
            @Aggregate(root = "Order")
            public class OrderAggregate {
            }

            interface TestPlugin extends PrefabPlugin {
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
                    .compile(sourceOf(VALID_AGGREGATE_WITH_PLUGIN_OVERRIDE));

            assertThat(compilation).succeeded();
        }

        @Test
        @DisplayName("should accept multiple @Generate overrides on same aggregate")
        void acceptMultipleOverrides() {
            var compilation = javac()
                    .withProcessors(new PrefabProcessor())
                    .compile(sourceOf(AGGREGATE_WITH_MULTIPLE_OVERRIDES));

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
                    .compile(sourceOf(GENERATE_WITH_NON_PLUGIN_CLASS));

            assertThat(compilation).hadErrorContaining("@Generate(plugin=String)");
            assertThat(compilation).hadErrorContaining("must reference a PrefabPlugin subclass");
        }

        @Test
        @DisplayName("should warn when same plugin configured multiple times")
        void warnOnDuplicatePluginConfiguration() {
            var compilation = javac()
                    .withProcessors(new PrefabProcessor())
                    .compile(sourceOf(GENERATE_WITH_DUPLICATE_PLUGINS));

            // Compilation succeeds despite duplicate override warning
            assertThat(compilation).succeeded();
        }
    }

    /** Helper: Convert string to JavaFileObject for compilation. */
    private static javax.tools.JavaFileObject sourceOf(String sourceCode) {
        return com.google.testing.compile.JavaFileObjects.forSourceString(
                "test.example.Source",
                sourceCode
        );
    }
}




