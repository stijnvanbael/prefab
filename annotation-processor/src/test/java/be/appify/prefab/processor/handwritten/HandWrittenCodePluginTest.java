package be.appify.prefab.processor.handwritten;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.Compilation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.classpathOptionsWith;
import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class HandWrittenCodePluginTest {
    private static final String PREFIX = "handwritten/source/";
    private static final String LEGACY = "handwritten/legacy/";
    private static final List<String> JPA_STUBS = List.of(
            "handwritten/stubs/jakarta/persistence/Entity.java",
            "handwritten/stubs/jakarta/persistence/Table.java");
    private static final String ORDER_TABLE = """
            CREATE TABLE "order" (
              "id" VARCHAR (255) NOT NULL,
              "customer" VARCHAR (255) NOT NULL,
              PRIMARY KEY("id")
            );
            """;

    @Test
    void reportsHandlerThatMapsAnEndpointPrefabAlreadyExposes() {
        var compilation = compile(PREFIX + "Order.java", PREFIX + "ClashingOrderController.java");

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("GET /orders/{id}")
                .inFile(sourceOf(PREFIX + "ClashingOrderController.java"));
        assertThat(compilation).hadErrorContaining("aggregate Order (get by id)");
        assertThat(compilation).hadErrorContaining("POST /orders");
        assertThat(compilation).hadErrorContaining("Suggested fix: remove the handler and use the generated"
                + " OrderController");
    }

    @Test
    void reportsHandlerWithoutHttpMethodThatCoversAnExposedPath() {
        var compilation = compile(PREFIX + "Order.java", PREFIX + "AnyMethodController.java");

        assertThat(compilation).hadErrorContaining("maps GET /orders");
    }

    @Test
    void doesNotReportControllerThatOnlyAddsEndpoints() {
        var compilation = compile(PREFIX + "Order.java", PREFIX + "OrderSummaryController.java");

        assertThat(compilation).succeeded();
        assertThat(findings(compilation)).isEmpty();
    }

    @Test
    void doesNotReportControllerThatTakesOverTheGeneratedOne() {
        var compilation = compile(PREFIX + "Order.java", PREFIX + "OrderController.java");

        assertThat(compilation).succeeded();
        assertThat(findings(compilation)).isEmpty();
    }

    @Test
    void doesNotReportSuppressedController() {
        var compilation = compile(PREFIX + "Order.java", PREFIX + "LegacyOrderController.java");

        assertThat(compilation).succeeded();
        assertThat(findings(compilation)).isEmpty();
    }

    @Test
    void reportsEntityWithTheNameOfAnAggregate() {
        var compilation = compile(withStubs(PREFIX + "Order.java", LEGACY + "Order.java"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("duplicates aggregate Order");
        assertThat(compilation).hadErrorContaining("Suggested fix: delete the entity");
    }

    @Test
    void reportsEntityMappedToTheTableOfAnAggregate() {
        var compilation = compile(withStubs(PREFIX + "Order.java", LEGACY + "OrderRow.java"));

        assertThat(compilation).hadErrorContaining("OrderRow: this hand-written entity duplicates aggregate Order");
    }

    @Test
    void doesNotReportEntitiesOfOtherTables() {
        var compilation = compile(withStubs(PREFIX + "Order.java", LEGACY + "Invoice.java"));

        assertThat(compilation).succeeded();
        assertThat(findings(compilation)).isEmpty();
    }

    @Test
    void reportsRepositoryForAnAggregate() {
        var compilation = compile(PREFIX + "Order.java", LEGACY + "LegacyOrderRepository.java");

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("this hand-written repository duplicates the OrderRepository");
        assertThat(compilation).hadErrorContaining("@RepositoryMixin(Order.class)");
    }

    @Test
    void doesNotReportRepositoryMixinsOrRepositoriesOfOtherTypes() {
        var compilation = compile(withStubs(PREFIX + "Order.java", LEGACY + "OrderQueries.java",
                LEGACY + "Invoice.java", LEGACY + "InvoiceRepository.java"));

        assertThat(compilation).succeeded();
        assertThat(findings(compilation)).isEmpty();
    }

    @Test
    void severityOptionTurnsFindingsIntoWarnings() {
        var compilation = compile(List.of("-Aprefab.handwritten.severity=warning"),
                PREFIX + "Order.java", PREFIX + "ClashingOrderController.java");

        assertThat(compilation).succeeded();
        assertThat(compilation).hadWarningContaining("GET /orders/{id}");
    }

    @Test
    void canBeDisabledWithAProcessorOption() {
        var compilation = compile(List.of("-Aprefab.plugin.handwrittencode.enabled=false"),
                PREFIX + "Order.java", PREFIX + "ClashingOrderController.java");

        assertThat(compilation).succeeded();
        assertThat(findings(compilation)).isEmpty();
    }

    @Test
    void reportsHandWrittenMigrationThatChangesAManagedTable() throws IOException {
        var migrations = migrations(Map.of("V1__init_orders.sql", ORDER_TABLE));

        var compilation = compile(classpathOptionsWith(migrations), PREFIX + "Order.java");

        assertThat(compilation).succeeded();
        assertThat(compilation).hadWarningContaining("db/migration/V1__init_orders.sql is a hand-written migration");
        assertThat(compilation).hadWarningContaining("CREATE TABLE `order` (aggregate Order)");
        assertThat(compilation).hadWarningContaining("Suggested fix: change the @Aggregate model instead");
    }

    @Test
    void migrationFindingsCanBeMadeErrors() throws IOException {
        var migrations = migrations(Map.of("V1__init_orders.sql", ORDER_TABLE));
        var options = new ArrayList<>(classpathOptionsWith(migrations));
        options.add("-Aprefab.handwritten.severity=error");

        var compilation = compile(options, PREFIX + "Order.java");

        assertThat(compilation).hadErrorContaining("is a hand-written migration");
    }

    @Test
    void reportsOnlyMigrationsThatAreNotExempt() throws IOException {
        var migrations = migrations(Map.of(
                "V1__generated.sql", ORDER_TABLE,
                "V2__add_notes.sql", "-- prefab:manual\nALTER TABLE \"order\" ADD COLUMN \"notes\" TEXT;\n",
                "V3__backfill.sql", "UPDATE \"order\" SET \"customer\" = lower(\"customer\");\n",
                "V4__audit_log.sql", "CREATE TABLE audit_log (id VARCHAR(255) PRIMARY KEY);\n",
                "V5__add_status_manual.sql", "ALTER TABLE \"order\" ADD COLUMN \"status\" VARCHAR (20);\n",
                "V6__add_priority.sql", "ALTER TABLE \"order\" ADD COLUMN \"priority\" BIGINT;\n"));

        var compilation = compile(classpathOptionsWith(migrations), PREFIX + "Order.java");

        assertThat(findings(compilation))
                .singleElement()
                .asString()
                .contains("db/migration/V6__add_priority.sql")
                .contains("ALTER TABLE `order` (aggregate Order)");
    }

    private static Compilation compile(String... resources) {
        return compile(List.of(), resources);
    }

    private static Compilation compile(List<String> resources) {
        return compile(List.of(), resources.toArray(String[]::new));
    }

    private static Compilation compile(List<String> options, String... resources) {
        var sources = Stream.of(resources).map(resource -> (JavaFileObject) sourceOf(resource)).toList();
        return javac().withProcessors(new PrefabProcessor()).withOptions(options).compile(sources);
    }

    private static List<String> withStubs(String... resources) {
        return Stream.concat(JPA_STUBS.stream(), Stream.of(resources)).toList();
    }

    private static List<String> findings(Compilation compilation) {
        return compilation.diagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR
                        || diagnostic.getKind() == Diagnostic.Kind.WARNING)
                .map(diagnostic -> diagnostic.getMessage(null))
                .filter(message -> message.startsWith("[Prefab]"))
                .toList();
    }

    /** Creates a class path entry that holds the given migrations plus the marker file the processor probes for. */
    private static Path migrations(Map<String, String> migrations) throws IOException {
        var classpath = Files.createTempDirectory("prefab-handwritten-migrations");
        var directory = Files.createDirectories(classpath.resolve("db").resolve("migration"));
        Files.writeString(directory.resolve("V1__baseline.sql"), "");
        for (var migration : migrations.entrySet()) {
            Files.writeString(directory.resolve(migration.getKey()), migration.getValue());
        }
        return classpath;
    }
}
