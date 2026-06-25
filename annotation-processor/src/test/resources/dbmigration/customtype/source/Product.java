package dbmigration.customtype;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.CustomType;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public record Product(
        @Id String id,
        @Version long version,
        String name,
        Result<String, Integer> result) {

    @CustomType
    public record Result<L, R>(L left, R right) {}
}
