package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.PrefabProcessor;
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

class AvscIncrementalCompilationTest {
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
    void schemaResourceEditAndFollowingMakeRecreateCompleteMigrationHistory() throws IOException {
        var schema = classes.resolve("avro/shipment-dispatched.avsc");
        Files.createDirectories(schema.getParent());
        Files.writeString(schema, schema("trackingNumber"));
        var event = writeSource("shop/ShipmentEvent.java", """
                package shop;

                import be.appify.prefab.core.annotations.Avsc;
                import be.appify.prefab.core.annotations.Event;

                @Event(topic = "shipping", serialization = Event.Serialization.AVRO)
                @Avsc("avro/shipment-dispatched.avsc")
                public interface ShipmentEvent {
                }
                """);
        var aggregate = writeSource("shop/CustomerOrderHistory.java", """
                package shop;

                import be.appify.prefab.core.annotations.Aggregate;
                import be.appify.prefab.core.annotations.DbMigration;
                import be.appify.prefab.core.service.Reference;
                import jakarta.validation.constraints.NotNull;
                import java.util.List;
                import org.springframework.data.annotation.Id;
                import org.springframework.data.annotation.Version;

                @Aggregate
                @DbMigration
                public record CustomerOrderHistory(
                        @Id Reference<CustomerOrderHistory> id,
                        @Version long version,
                        @NotNull List<Entry> entries
                ) {
                    public record Entry(@NotNull String product, int quantity, @NotNull List<Note> notes) {
                    }

                    public record Note(@NotNull String text, @NotNull String author) {
                    }
                }
                """);
        compile(event, aggregate);

        var migrationDirectory = classes.resolve("db/migration");
        var initialMigration = migrationDirectory.resolve("V1__generated.sql");
        var manifest = migrationDirectory.resolve("prefab-aggregates.txt");
        var generatedEvent = generatedSources.resolve("shop/ShipmentDispatched.java");
        assertThat(Files.readString(initialMigration))
                .contains("customer_order_history_entry", "REFERENCES \"customer_order_history\"");
        assertThat(Files.readString(generatedEvent)).contains("String trackingNumber");
        assertThat(manifest).exists();

        Files.writeString(schema, schema("trackingCode"));
        deleteGeneratedOutputs("ShipmentDispatched");
        compile(event);

        assertThat(Files.readString(generatedEvent))
                .contains("String trackingCode")
                .doesNotContain("trackingNumber");
        assertThat(initialMigration).exists();
        assertThat(migrationDirectory.resolve("V2__generated.sql")).doesNotExist();

        // JPS can remove shared generated resources before its following Make rebuilds every source.
        Files.delete(initialMigration);
        Files.delete(manifest);
        deleteGeneratedOutputs(null);
        compile(event, aggregate);

        assertThat(Files.readString(initialMigration))
                .contains("\"customer_order_history\"", "\"customer_order_history_entry\"",
                        "REFERENCES \"customer_order_history\"");
        assertThat(migrationDirectory.resolve("V2__generated.sql")).doesNotExist();
        assertThat(manifest).exists();
    }

    @Test
    void avscGeneratedEventAggregateWritesTestClientToPrefabTestSources() throws IOException {
        var schema = classes.resolve("avro/order-created.avsc");
        Files.createDirectories(schema.getParent());
        Files.writeString(schema, """
                {
                  "type": "record",
                  "name": "OrderCreated",
                  "namespace": "shop",
                  "fields": [
                    {"name": "orderId", "type": "string"}
                  ]
                }
                """);
        var event = writeSource("shop/OrderEvent.java", """
                package shop;

                import be.appify.prefab.core.annotations.Avsc;
                import be.appify.prefab.core.annotations.Event;

                @Event(topic = "orders.created", serialization = Event.Serialization.AVRO)
                @Avsc("avro/order-created.avsc")
                public interface OrderEvent {
                }
                """);
        var aggregate = writeSource("shop/Order.java", """
                package shop;

                import be.appify.prefab.core.annotations.Aggregate;
                import be.appify.prefab.core.annotations.rest.GetById;
                import be.appify.prefab.core.service.Reference;
                import jakarta.validation.constraints.NotNull;
                import org.springframework.data.annotation.Id;
                import org.springframework.data.annotation.Version;

                @Aggregate
                @GetById
                public record Order(
                        @Id Reference<Order> id,
                        @Version long version,
                        @NotNull String description,
                        OrderCreated lastEvent
                ) {
                }
                """);

        compile(event, aggregate);

        assertThat(module.resolve("target/prefab-test-sources/shop/OrderClient.java"))
                .exists();
        assertThat(classes.resolve("shop/OrderClient.java"))
                .doesNotExist();
    }

    private static String schema(String field) {
        return """
                {
                  "type": "record",
                  "name": "ShipmentDispatched",
                  "namespace": "shop",
                  "fields": [
                    {"name": "%s", "type": "string"}
                  ]
                }
                """.formatted(field);
    }

    private Path writeSource(String relativePath, String contents) throws IOException {
        var source = module.resolve("src/main/java").resolve(relativePath);
        Files.createDirectories(source.getParent());
        return Files.writeString(source, contents);
    }

    private void deleteGeneratedOutputs(String fileNameSubstring) throws IOException {
        try (Stream<Path> files = Files.walk(generatedSources)) {
            for (var source : files.filter(Files::isRegularFile)
                    .filter(path -> fileNameSubstring == null
                            || path.getFileName().toString().contains(fileNameSubstring))
                    .toList()) {
                var classFile = classes.resolve(generatedSources.relativize(source))
                        .resolveSibling(source.getFileName().toString().replaceFirst("\\.java$", ".class"));
                deleteClassFiles(classFile);
                Files.delete(source);
            }
        }
    }

    private static void deleteClassFiles(Path classFile) throws IOException {
        if (!Files.exists(classFile.getParent())) {
            return;
        }
        var classFilePrefix = classFile.getFileName().toString().replaceFirst("\\.class$", "") + "$";
        try (Stream<Path> files = Files.list(classFile.getParent())) {
            for (var output : files.filter(path -> path.equals(classFile)
                    || path.getFileName().toString().startsWith(classFilePrefix)).toList()) {
                Files.delete(output);
            }
        }
    }

    private void compile(Path... sources) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("System Java compiler is not available");
        }
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
