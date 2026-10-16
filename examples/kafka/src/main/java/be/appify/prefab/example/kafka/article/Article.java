package be.appify.prefab.example.kafka.article;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/** An article that can be searched with a single full-text query over its title and body. */
@Aggregate
@GetById
@GetList
public record Article(
        @Id Reference<Article> id,
        @Version long version,
        @Filter(operator = Filter.Operator.FULL_TEXT, parameter = "query") String title,
        @Filter(operator = Filter.Operator.FULL_TEXT, parameter = "query") String body,
        @Filter String category,
        @Filter(operator = Filter.Operator.GREATER_THAN_OR_EQUAL) int views
) {

    /** Creates a new article. */
    @Create
    public Article(String title, String body, String category, int views) {
        this(Reference.create(), 0L, title, body, category, views);
    }
}