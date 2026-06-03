package dbmigration.valuetypewrappingrecord;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import java.util.List;

import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public record ProductWithList(
        @Id String id,
        @Version long version,
        List<AddressHolder> addresses) {

    public record AddressHolder(Address value) {}

    public record Address(String street, String city) {}
}

