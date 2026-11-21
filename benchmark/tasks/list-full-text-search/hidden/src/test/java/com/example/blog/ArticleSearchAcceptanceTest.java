package com.example.blog;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ArticleSearchAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    private Api seed() throws Exception {
        var api = new Api(mockMvc);
        api.create("/articles", "{\"title\": \"Spring tips\", \"body\": \"Connecting to PostgreSQL made easy\"}");
        api.create("/articles", "{\"title\": \"Kafka basics\", \"body\": \"Topics and partitions explained\"}");
        api.create("/articles", "{\"title\": \"Gardening in spring\", \"body\": \"Plant tulips early\"}");
        return api;
    }

    private static List<String> titles(Api api, String query) throws Exception {
        return Api.read(api.get("/articles?size=50" + query).andExpect(status().isOk()), "$.content[*].title");
    }

    @Test
    void wordsMayOccurInEitherField() throws Exception {
        assertThat(titles(seed(), "&query=spring postgresql")).containsExactly("Spring tips");
    }

    @Test
    void matchesIgnoringCaseInBothFields() throws Exception {
        var api = seed();
        assertThat(titles(api, "&query=SPRING")).containsExactlyInAnyOrder("Spring tips", "Gardening in spring");
        assertThat(titles(api, "&query=partitions")).containsExactly("Kafka basics");
    }

    @Test
    void returnsAllArticlesWithoutQuery() throws Exception {
        assertThat(titles(seed(), "")).hasSize(3);
    }
}
