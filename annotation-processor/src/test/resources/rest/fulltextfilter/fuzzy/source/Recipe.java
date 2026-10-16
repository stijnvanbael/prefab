package rest.fulltextfilter.fuzzy;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetList
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public record Recipe(
        @Id String id,
        @Version long version,
        @Filter(operator = Filter.Operator.FULL_TEXT, parameter = "query", fuzzy = true, similarityThreshold = 0.4) String title,
        @Filter(operator = Filter.Operator.FULL_TEXT, parameter = "query", fuzzy = true, similarityThreshold = 0.4) String body) {

    @Create
    public Recipe(String title, String body) {
        this(UUID.randomUUID().toString(), 0L, title, body);
    }
}
