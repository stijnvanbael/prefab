package rest.fulltextfilter;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetList
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)public record Article(
        @Id String id,
        @Version long version,
        @Filter(operator = Filter.Operator.FULL_TEXT, parameter = "query") String title,
        @Filter(operator = Filter.Operator.FULL_TEXT, parameter = "query") String body,
        @Filter(operator = Filter.Operator.EQUAL) String category,
        @Filter(operator = Filter.Operator.GREATER_THAN) BigDecimal price) {

    @Create
    public Article(String title, String body, String category, BigDecimal price) {
        this(UUID.randomUUID().toString(), 0L, title, body, category, price);
    }
}