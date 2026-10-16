package be.appify.prefab.example.kafka.article;

import be.appify.prefab.example.kafka.article.application.ArticleQuery;
import be.appify.prefab.example.kafka.article.application.ArticleService;
import be.appify.prefab.test.IntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ArticleFullTextIntegrationTest {

    @Autowired
    ArticleClient articles;

    @Autowired
    ArticleService service;

    private final String category = "category-" + UUID.randomUUID();

    private void createSamples() throws Exception {
        articles.createArticle("Spring Boot basics", "Getting started with the framework", category, 100);
        articles.createArticle("Cooking pasta", "Boil water, then add the pasta", category, 5);
        articles.createArticle("Advanced pasta", "A Spring in your step with fresh pasta", category, 50);
    }

    private List<String> titles(ArticleQuery query) {
        return service.getList(query.category(category).sort(Sort.by("title"))).getContent().stream()
                .map(Article::title)
                .toList();
    }

    @Test
    void queryMatchesAnyOfTheFullTextFields() throws Exception {
        createSamples();

        assertThat(titles(ArticleQuery.newQuery().query("spring")))
                .containsExactly("Advanced pasta", "Spring Boot basics");
    }

    @Test
    void webSearchSyntaxSupportsExclusion() throws Exception {
        createSamples();

        assertThat(titles(ArticleQuery.newQuery().query("spring -boot"))).containsExactly("Advanced pasta");
    }

    @Test
    void fullTextCombinesWithRangeAndEqualityFilters() throws Exception {
        createSamples();

        assertThat(titles(ArticleQuery.newQuery().query("pasta").viewsGreaterThanOrEqual(10)))
                .containsExactly("Advanced pasta");
    }

    @Test
    void withoutQueryAllArticlesMatch() throws Exception {
        createSamples();

        assertThat(titles(ArticleQuery.newQuery())).hasSize(3);
    }
}
