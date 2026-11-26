package be.appify.prefab.processor;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simulates an IDE that passes javac only the changed sources of a module, with the earlier output on the classpath.
 * Aggregated outputs (migrations, AsyncAPI, test helpers) must still describe the whole module.
 */
class IncrementalCompilationTest {
    @TempDir
    Path module;
    private Path classes;
    private Path generatedSources;

    @BeforeEach
    void setUp() throws IOException {
        classes = Files.createDirectories(module.resolve("target/classes"));
        generatedSources = Files.createDirectories(module.resolve("target/generated-sources/annotations"));
    }

    @Test
    void partialRecompileKeepsUnchangedAggregatesInRegeneratedMigration() throws IOException {
        var product = write("shop/Product.java", aggregate("Product", "name"));
        var category = write("shop/Category.java", aggregate("Category", "title"));
        compile(product, category);
        var migration = classes.resolve("db/migration/V1__generated.sql");
        assertThat(Files.readString(migration)).contains("\"product\"", "\"category\"");

        // The IDE deletes the outputs owned by the recompiled source, including the shared migration.
        deleteOutputsOf("Product");
        Files.delete(migration);
        compile(write("shop/Product.java", aggregate("Product", "label")));

        assertThat(Files.readString(migration)).contains("\"product\"", "\"label\"", "\"category\"", "\"title\"");
    }

    @Test
    void partialRecompileKeepsChannelsOfUnchangedEvents() throws IOException {
        var created = write("shop/OrderCreated.java", event("OrderCreated", "orders.created"));
        var shipped = write("shop/OrderShipped.java", event("OrderShipped", "orders.shipped"));
        compile(created, shipped);

        deleteOutputsOf("OrderCreated");
        Files.delete(classes.resolve("META-INF/async-api/asyncapi.json"));
        compile(write("shop/OrderCreated.java", event("OrderCreated", "orders.placed")));

        assertThat(asyncApi()).contains("orders.placed", "orders.shipped").doesNotContain("orders.created");
    }

    @Test
    void deletedSourceDisappearsFromAggregatedOutputs() throws IOException {
        var created = write("shop/OrderCreated.java", event("OrderCreated", "orders.created"));
        var shipped = write("shop/OrderShipped.java", event("OrderShipped", "orders.shipped"));
        compile(created, shipped);
        var shippedAssert = module.resolve("target/prefab-test-sources/shop/OrderShippedAssert.java");
        assertThat(shippedAssert).exists();

        Files.delete(shipped);
        deleteOutputsOf("OrderShipped");
        deleteOutputsOf("OrderCreated");
        compile(write("shop/OrderCreated.java", event("OrderCreated", "orders.placed")));

        assertThat(asyncApi()).contains("orders.placed").doesNotContain("orders.shipped");
        assertThat(shippedAssert).doesNotExist();
        assertThat(module.resolve("target/prefab-test-sources/shop/OrderCreatedAssert.java")).exists();
    }

    @Test
    void unchangedRecompileDoesNotTouchGeneratedTestSources() throws IOException {
        var created = write("shop/OrderCreated.java", event("OrderCreated", "orders.created"));
        compile(created);
        var helper = module.resolve("target/prefab-test-sources/shop/OrderCreatedAssert.java");
        var before = Files.getLastModifiedTime(helper);

        deleteOutputsOf("OrderCreated");
        compile(created);

        assertThat(Files.getLastModifiedTime(helper)).isEqualTo(before);
    }

    private static String aggregate(String name, String field) {
        return """
                package shop;

                import be.appify.prefab.core.annotations.Aggregate;
                import be.appify.prefab.core.annotations.DbMigration;
                import be.appify.prefab.core.service.Reference;
                import jakarta.validation.constraints.Size;
                import org.springframework.data.annotation.Id;
                import org.springframework.data.annotation.Version;

                @Aggregate
                @DbMigration
                public record %1$s(@Id Reference<%1$s> id, @Version long version, @Size(max = 50) String %2$s) {
                }
                """.formatted(name, field);
    }

    private static String event(String name, String topic) {
        return """
                package shop;

                import be.appify.prefab.core.annotations.Event;
                import be.appify.prefab.core.annotations.PartitioningKey;

                @Event(topic = "%s")
                public record %s(@PartitioningKey String id) {
                }
                """.formatted(topic, name);
    }

    private Path write(String path, String contents) throws IOException {
        var file = module.resolve("src/main/java").resolve(path);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, contents);
    }

    private String asyncApi() throws IOException {
        return Files.readString(classes.resolve("META-INF/async-api/asyncapi.json"));
    }

    /** Deletes the class and Filer outputs of a type before recompiling it, as JPS and Maven do. */
    private void deleteOutputsOf(String simpleName) throws IOException {
        for (var directory : List.of(classes, generatedSources)) {
            try (Stream<Path> files = Files.walk(directory)) {
                for (var file : files.filter(path -> path.getFileName().toString().startsWith(simpleName)).toList()) {
                    Files.delete(file);
                }
            }
        }
    }

    private void compile(Path... sources) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(classes));
            fileManager.setLocationFromPaths(StandardLocation.SOURCE_OUTPUT, List.of(generatedSources));
            var options = List.of("-classpath", classes + File.pathSeparator + System.getProperty("java.class.path"),
                    "-proc:full");
            var output = new StringWriter();
            var task = compiler.getTask(output, fileManager, diagnostics, options, null,
                    fileManager.getJavaFileObjects(sources));
            task.setProcessors(List.of(new PrefabProcessor()));
            assertThat(task.call()).as(() -> output + diagnostics.getDiagnostics().toString()).isTrue();
        }
    }
}
