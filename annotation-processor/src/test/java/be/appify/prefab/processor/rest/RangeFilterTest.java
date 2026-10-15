package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.Compilation;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangeFilterTest {

    private static final Compilation productCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("rest/rangefilter/source/Product.java"));

    private static final Compilation tenantCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("rest/rangefilter/tenant/source/Invoice.java"));

    private static final Compilation parentCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("rest/rangefilter/parent/source/Project.java"),
                    sourceOf("rest/rangefilter/parent/source/Ticket.java"));

    private static String source(Compilation compilation, String name) throws IOException {
        assertThat(compilation).succeeded();
        return compilation.generatedSourceFile(name)
                .orElseThrow(() -> new AssertionError("Not generated: " + name))
                .getCharContent(true)
                .toString();
    }

    private static void assertContains(String source, String fragment) {
        assertTrue(source.contains(fragment), "Expected to find <" + fragment + "> in:\n" + source);
    }

    private static void assertDoesNotContain(String source, String fragment) {
        assertFalse(source.contains(fragment), "Expected not to find <" + fragment + "> in:\n" + source);
    }

    @Test
    void controllerExposesRangeParametersWithSuffixedNamesAndRealTypes() throws Exception {
        var controller = source(productCompilation, "rest.rangefilter.infrastructure.http.ProductController");

        assertContains(controller, "String name");
        assertContains(controller, "BigDecimal priceGreaterThanOrEqual");
        assertContains(controller, "BigDecimal priceLessThan");
        assertContains(controller, "Integer stockGreaterThan");
        assertContains(controller, "Instant createdAtLessThanOrEqual");
        assertContains(controller, "service.getList(pageable, name, priceGreaterThanOrEqual, priceLessThan, "
                + "stockGreaterThan, createdAtLessThanOrEqual)");
    }

    @Test
    void serviceCombinesExampleAndRangeConditions() throws Exception {
        var service = source(productCompilation, "rest.rangefilter.application.ProductService");

        assertContains(service, "RangeQueryExecutor rangeQueryExecutor");
        assertContains(service, "rangeQueryExecutor.findAll(");
        assertContains(service, "Example.of(new Product(");
        assertContains(service, "withMatcher(\"name\"");
        assertContains(service, "new RangeCondition(\"price\", Filter.Operator.GREATER_THAN_OR_EQUAL, priceGreaterThanOrEqual)");
        assertContains(service, "new RangeCondition(\"price\", Filter.Operator.LESS_THAN, priceLessThan)");
        assertContains(service, "new RangeCondition(\"stock\", Filter.Operator.GREATER_THAN, stockGreaterThan)");
        assertContains(service, "new RangeCondition(\"createdAt\", Filter.Operator.LESS_THAN_OR_EQUAL, createdAtLessThanOrEqual)");
        assertDoesNotContain(service, "productRepository.findAll(");
    }

    @Test
    void rangeOnlyFieldsAreIgnoredByTheExampleMatcher() throws Exception {
        var service = source(productCompilation, "rest.rangefilter.application.ProductService");

        assertContains(service, "withIgnorePaths(\"id\", \"version\", \"price\", \"stock\", \"createdAt\")");
    }

    @Test
    void testClientExposesRangeParameters() throws Exception {
        var processor = new RestWriterTest.CapturingTestClientProcessor();
        var compilation = javac().withProcessors(processor)
                .compile(sourceOf("rest/rangefilter/source/Product.java"));
        assertThat(compilation).succeeded();
        var client = processor.capturedSources.stream()
                .filter(source -> source.contains("class ProductClient"))
                .findFirst()
                .orElseThrow();

        assertContains(client, "BigDecimal priceGreaterThanOrEqual");
        assertContains(client, "request.queryParam(\"priceGreaterThanOrEqual\", String.valueOf(priceGreaterThanOrEqual))");
        assertContains(client, "request.queryParam(\"stockGreaterThan\", String.valueOf(stockGreaterThan))");
        assertContains(client, "request.queryParam(\"name\", name)");
    }

    @Test
    void tenantServiceKeepsTenantMatcherAlongsideRanges() throws Exception {
        var service = source(tenantCompilation, "rest.rangefilter.tenant.application.InvoiceService");

        assertContains(service, "rangeQueryExecutor.findAll(");
        assertContains(service, "tenantContextProvider.currentTenantId()");
        assertContains(service, "withMatcher(\"organisationId\"");
        assertContains(service, "new RangeCondition(\"amount\", Filter.Operator.GREATER_THAN, amountGreaterThan)");
    }

    @Test
    void parentServiceKeepsParentMatcherAlongsideRanges() throws Exception {
        var service = source(parentCompilation, "rest.rangefilter.parent.application.TicketService");

        assertContains(service, "rangeQueryExecutor.findAll(");
        assertContains(service, "projectId != null ? new Reference<Project>(projectId) : null");
        assertContains(service, "withMatcher(\"project\"");
        assertContains(service, "new RangeCondition(\"priority\", Filter.Operator.LESS_THAN_OR_EQUAL, priorityLessThanOrEqual)");
    }

    @Test
    void rangeOperatorOnStringFieldIsRejected() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/rangefilter/invalid/source/Note.java"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("only supported on numeric and java.time fields");
    }
}
