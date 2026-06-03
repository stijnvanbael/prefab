package rest.createorupdate;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.HttpMethod;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Product(
        @Id String id,
        @Version long version,
        String name,
        String price
) {
    @Create(method = HttpMethod.PUT, path = "/{id}")
    public Product(String id, String name, String price) {
        this(id, 0L, name, price);
    }

    @Update(method = HttpMethod.PUT)
    public Product update(String name, String price) {
        return new Product(id, version, name, price);
    }
}
