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

class FullTextFilterTest {

    private static final Compilation articleCompilation = compile("rest/fulltextfilter/source/Article.java");

    private static Compilation compile(String... sources) {
        return javac().withProcessors(new PrefabProcessor())
                .compile(java.util.Arrays.stream(sources).map(source -> sourceOf(source)).toList());
    }

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
    void controllerExposesOneSharedStringParameter() throws Exception {
        var controller = source(articleCompilation, "rest.fulltextfilter.infrastructure.http.ArticleController");

        assertContains(controller, "String query");
        assertContains(controller, "String category");
        assertContains(controller, "BigDecimal priceGreaterThan");
        assertContains(controller, "service.getList(pageable, query, category, priceGreaterThan)");
        assertDoesNotContain(controller, "String title");
        assertDoesNotContain(controller, "String body");
    }

    @Test
    void serviceSearchesAllFieldsAndCombinesWithExampleAndRanges() throws Exception {
        var service = source(articleCompilation, "rest.fulltextfilter.application.ArticleService");

        assertContains(service, "FullTextQueryExecutor fullTextQueryExecutor");
        assertContains(service, "fullTextQueryExecutor.findAll(");
        assertContains(service, "new FullTextCondition(List.of(\"title\", \"body\"), query)");
        assertContains(service, "new RangeCondition(\"price\", Filter.Operator.GREATER_THAN, priceGreaterThan)");
        assertContains(service, "withMatcher(\"category\"");
        assertContains(service, "withIgnorePaths(\"id\", \"version\", \"title\", \"body\", \"price\")");
        assertDoesNotContain(service, "RangeQueryExecutor");
        assertDoesNotContain(service, "withMatcher(\"title\"");
    }

    @Test
    void fluentQueryHasOneSearchProperty() throws Exception {
        var query = source(articleCompilation, "rest.fulltextfilter.application.ArticleQuery");

        assertContains(query, "ArticleQuery query(String query)");
        assertContains(query, "Optional<String> query()");
        assertDoesNotContain(query, "title(");
        var service = source(articleCompilation, "rest.fulltextfilter.application.ArticleService");
        assertContains(service, "query.query().orElse(null)");
    }

    @Test
    void onlyFullTextFilterUsesEmptyRangeList() throws Exception {
        var compilation = compile("rest/fulltextfilter/onlyfulltext/source/Note.java");
        var service = source(compilation, "rest.fulltextfilter.onlyfulltext.application.NoteService");

        assertContains(service, "new FullTextCondition(List.of(\"text\"), text)");
        assertContains(service, "List.of(),");
    }

    @Test
    void tenantIsKeptAlongsideFullText() throws Exception {
        var service = source(compile("rest/fulltextfilter/tenant/source/Ticket.java"),
                "rest.fulltextfilter.tenant.application.TicketService");

        assertContains(service, "fullTextQueryExecutor.findAll(");
        assertContains(service, "tenantContextProvider.currentTenantId()");
        assertContains(service, "withMatcher(\"organisationId\"");
    }

    @Test
    void testClientExposesSearchParameter() throws Exception {
        var processor = new RestWriterTest.CapturingTestClientProcessor();
        var compilation = javac().withProcessors(processor)
                .compile(sourceOf("rest/fulltextfilter/source/Article.java"));
        assertThat(compilation).succeeded();
        var client = processor.capturedSources.stream()
                .filter(source -> source.contains("class ArticleClient"))
                .findFirst()
                .orElseThrow();

        assertContains(client, "String query");
        assertContains(client, "request.queryParam(\"query\", query)");
    }

    @Test
    void fuzzyFilterPassesFuzzinessAndThresholdToTheCondition() throws Exception {
        var service = source(compile("rest/fulltextfilter/fuzzy/source/Recipe.java"),
                "rest.fulltextfilter.fuzzy.application.RecipeService");

        assertContains(service, "new FullTextCondition(List.of(\"title\", \"body\"), query, true, 0.4)");
    }

    @Test
    void exactFilterKeepsTheExactCondition() throws Exception {
        var service = source(articleCompilation, "rest.fulltextfilter.application.ArticleService");

        assertDoesNotContain(service, ", true,");
    }

    @Test
    void similarityThresholdOutOfRangeIsRejected() {
        var compilation = compile("rest/fulltextfilter/invalid/threshold/source/Guide.java");

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("similarityThreshold must be between 0 (exclusive) and 1");
    }

    @Test
    void nonStringFieldIsRejected() {
        var compilation = compile("rest/fulltextfilter/invalid/nonstring/source/Memo.java");

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("FULL_TEXT is only supported on String fields");
    }

    @Test
    void multipleFullTextParametersAreRejected() {
        var compilation = compile("rest/fulltextfilter/invalid/twoparameters/source/Post.java");

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("only one FULL_TEXT request parameter is supported per aggregate");
    }

    @Test
    void parameterClashingWithAnotherFilterIsRejected() {
        var compilation = compile("rest/fulltextfilter/invalid/clash/source/Page.java");

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("clashes with another filter parameter");
    }
}
