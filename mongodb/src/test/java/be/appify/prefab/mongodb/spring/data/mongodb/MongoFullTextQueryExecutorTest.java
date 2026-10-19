package be.appify.prefab.mongodb.spring.data.mongodb;

import be.appify.prefab.core.spring.data.FullTextCondition;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoFullTextQueryExecutorTest {

    record Article(String id, String title, String category) {
    }

    private Document queryOf(FullTextCondition condition) {
        var operations = mock(MongoOperations.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<MongoOperations> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(operations);
        when(operations.find(any(Query.class), eq(Article.class))).thenReturn(List.of());

        new MongoFullTextQueryExecutor(provider)
                .findAll(Example.of(new Article(null, null, "java")), condition, List.of(), Pageable.unpaged());

        var captor = ArgumentCaptor.forClass(Query.class);
        verify(operations).find(captor.capture(), eq(Article.class));
        return captor.getValue().getQueryObject();
    }

    @Test
    void fuzzyConditionDegradesToTheExactTextSearch() {
        var exact = queryOf(new FullTextCondition(List.of("title"), "kafak"));
        var fuzzy = queryOf(new FullTextCondition(List.of("title"), "kafak", true, 0.5));

        assertThat(fuzzy).isEqualTo(exact);
        assertThat(fuzzy.get("$text", Document.class).getString("$search")).isEqualTo("kafak");
    }

    @Test
    void textSearchIsCombinedWithTheExampleCriteria() {
        var query = queryOf(new FullTextCondition(List.of("title"), "kafka", true, 0.3));

        assertThat(query).containsKey("$text").containsKey("$example");
    }

    @Test
    void blankQueryAddsNoTextSearch() {
        var query = queryOf(new FullTextCondition(List.of("title"), " ", true, 0.3));

        assertThat(query).doesNotContainKey("$text");
    }

    private Query findQueryOf(FullTextCondition condition, Pageable pageable) {
        var operations = mock(MongoOperations.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<MongoOperations> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(operations);
        when(operations.find(any(Query.class), eq(Article.class))).thenReturn(List.of());

        new MongoFullTextQueryExecutor(provider)
                .findAll(Example.of(new Article(null, null, "java")), condition, List.of(), pageable);

        var captor = ArgumentCaptor.forClass(Query.class);
        verify(operations).find(captor.capture(), eq(Article.class));
        return captor.getValue();
    }

    @Test
    void relevanceSortOrdersByTextScore() {
        var query = findQueryOf(new FullTextCondition(List.of("title"), "kafka"),
                PageRequest.of(0, 10, Sort.by(FullTextCondition.RELEVANCE)));

        assertThat(query.getSortObject()).containsKey("score");
        assertThat(query.getFieldsObject()).containsKey("score");
        assertThat(query.getQueryObject()).containsKey("$text");
    }

    @Test
    void ordersByTextScoreByDefaultWhenNoSortIsGiven() {
        var query = findQueryOf(new FullTextCondition(List.of("title"), "kafka"), Pageable.unpaged());

        assertThat(query.getSortObject()).containsKey("score");
    }

    @Test
    void explicitSortOverridesTheDefaultRelevanceOrder() {
        var query = findQueryOf(new FullTextCondition(List.of("title"), "kafka"),
                PageRequest.of(0, 10, Sort.by("title")));

        assertThat(query.getSortObject()).containsOnlyKeys("title");
        assertThat(query.getFieldsObject()).doesNotContainKey("score");
    }

    @Test
    void relevanceCombinesWithOtherSortPropertiesAndIsNotSortedAsAField() {
        var query = findQueryOf(new FullTextCondition(List.of("title"), "kafka"),
                PageRequest.of(0, 10, Sort.by(FullTextCondition.RELEVANCE).and(Sort.by("title"))));

        assertThat(query.getSortObject().keySet()).containsExactly("score", "title");
    }

    @Test
    void relevanceSortIsIgnoredWithoutSearchText() {
        var query = findQueryOf(new FullTextCondition(List.of("title"), " "),
                PageRequest.of(0, 10, Sort.by(FullTextCondition.RELEVANCE)));

        assertThat(query.getSortObject()).isEmpty();
    }

    @Test
    void typeaheadMatchesTheLastWordAsWordStartRegexOnEverySearchedProperty() {
        var query = queryOf(new FullTextCondition(List.of("title", "body"), "kafka spri").withTypeahead());

        assertThat(query.get("$text", Document.class).getString("$search")).isEqualTo("kafka");
        var orClauses = query.getList("$and", Document.class).get(1).getList("$or", Document.class);
        assertThat(orClauses.stream().map(Document::keySet).toList())
                .containsExactly(java.util.Set.of("title"), java.util.Set.of("body"));
        assertThat(orClauses.stream().flatMap(or -> or.values().stream()).toList())
                .allSatisfy(value -> assertThat(value).isInstanceOfSatisfying(java.util.regex.Pattern.class,
                        pattern -> assertThat(pattern.pattern()).isEqualTo("\\bspri")));
    }

    @Test
    void typeaheadWithASingleWordUsesNoTextSearchAndNoScore() {
        var query = findQueryOf(new FullTextCondition(List.of("title"), "spri").withTypeahead(),
                PageRequest.of(0, 10, Sort.by(FullTextCondition.RELEVANCE)));

        assertThat(query.getQueryObject()).doesNotContainKey("$text").containsKey("$and");
        assertThat(query.getSortObject()).isEmpty();
    }

    @Test
    void typeaheadIgnoresACompletedLastWord() {
        var query = queryOf(new FullTextCondition(List.of("title"), "spri ").withTypeahead());

        assertThat(query).containsKey("$text").doesNotContainKey("$and");
    }
}
