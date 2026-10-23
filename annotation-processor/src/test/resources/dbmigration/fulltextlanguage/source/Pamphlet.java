package dbmigration.fulltextlanguage;

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
public record Pamphlet(
        @Id String id,
        @Version long version,
        @Filter(operator = Filter.Operator.FULL_TEXT, parameter = "query", language = "english", weight = Filter.Weight.A) String title,
        @Filter(operator = Filter.Operator.FULL_TEXT, parameter = "query", language = "english", weight = Filter.Weight.C) String bodyText,
        @Filter String category) {

    @Create
    public Pamphlet(String title, String bodyText, String category) {
        this(UUID.randomUUID().toString(), 0L, title, bodyText, category);
    }
}
