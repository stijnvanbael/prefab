package rest.fulltextfilter.invalid.mixedtypeahead;

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
public record Manual(
        @Id String id,
        @Version long version,
        @Filter(operator = Filter.Operator.FULL_TEXT, parameter = "query", typeahead = false) String title,
        @Filter(operator = Filter.Operator.FULL_TEXT, parameter = "query") String body) {

    @Create
    public Manual(String title, String body) {
        this(UUID.randomUUID().toString(), 0L, title, body);
    }
}

