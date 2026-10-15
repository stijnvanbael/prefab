package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.Compilation;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetListQueryTest {

    private static final Compilation productCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("rest/rangefilter/source/Product.java"));

    private static final Compilation parentCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("rest/rangefilter/parent/source/Project.java"),
                    sourceOf("rest/rangefilter/parent/source/Ticket.java"));

    private static final Compilation sortCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("rest/defaultsort/source/Note.java"));

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

    @Test
    void queryExposesPagingSortAndOneMethodPerFilter() throws Exception {
        var query = source(productCompilation, "rest.rangefilter.application.ProductQuery");

        assertContains(query, "public static ProductQuery newQuery()");
        assertContains(query, "public ProductQuery page(int page)");
        assertContains(query, "public ProductQuery pageSize(int pageSize)");
        assertContains(query, "public ProductQuery sort(Sort sort)");
        assertContains(query, "public ProductQuery name(String name)");
        assertContains(query, "public ProductQuery priceGreaterThanOrEqual(BigDecimal priceGreaterThanOrEqual)");
        assertContains(query, "public ProductQuery stockGreaterThan(Integer stockGreaterThan)");
        assertContains(query, "public ProductQuery createdAtLessThanOrEqual(Instant createdAtLessThanOrEqual)");
    }

    @Test
    void serviceOverloadDelegatesToExistingGetList() throws Exception {
        var service = source(productCompilation, "rest.rangefilter.application.ProductService");

        assertContains(service, "public Page<Product> getList(ProductQuery query)");
        assertContains(service, "PageRequest.of(query.page(), query.pageSize(), query.sort().orElse(Sort.unsorted()))");
        assertContains(service, "return getList(pageable, query.name().orElse(null), "
                + "query.priceGreaterThanOrEqual().orElse(null)");
    }

    @Test
    void serviceOverloadAppliesDefaultSortWhenNoSortIsGiven() throws Exception {
        var service = source(sortCompilation, "rest.defaultsort.application.NoteService");

        assertContains(service, "query.sort().orElse(Sort.by(new Sort.Order(Sort.Direction.ASC, \"author\"), "
                + "new Sort.Order(Sort.Direction.DESC, \"title\")))");
    }

    @Test
    void parentIsRequiredOnTheQuery() throws Exception {
        var query = source(parentCompilation, "rest.rangefilter.parent.application.TicketQuery");
        var service = source(parentCompilation, "rest.rangefilter.parent.application.TicketService");

        assertContains(query, "public TicketQuery projectId(String projectId)");
        assertContains(service, "query.projectId().orElseThrow(() -> new IllegalArgumentException(\"projectId is required\"))");
    }
}
