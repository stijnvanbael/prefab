package be.appify.prefab.mongodb.spring.data.mongodb;

import be.appify.prefab.core.spring.data.FullTextCondition;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Pageable;
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
}
