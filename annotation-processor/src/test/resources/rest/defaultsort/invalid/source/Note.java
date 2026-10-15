package rest.defaultsort.invalid;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.SortOrder;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetList(defaultSort = {
        @SortOrder(field = "missing"),
        @SortOrder(field = "title", direction = SortOrder.Direction.DESC)})
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Note(
        @Id String id,
        @Version long version,
        String title,
        String author) {

    @Create
    public Note(String title, String author) {
        this(UUID.randomUUID().toString(), 0L, title, author);
    }
}
