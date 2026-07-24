package assertion;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Computed;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import java.util.List;
import java.util.UUID;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@GetList
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        String name,
        Money price,
        List<String> tags) {

    @Create
    public Product(String name, Money price) {
        this(Reference.create(), 0L, name, price, List.of());
    }

    @Update
    public Product update(String name, Money price) {
        return new Product(id, version, name, price, tags);
    }

    @Computed
    public int tagCount() {
        return tags.size();
    }

    public static record Money(double value) {
        @Computed
        public String formatted() {
            return "%.2f".formatted(value);
        }
    }
}
