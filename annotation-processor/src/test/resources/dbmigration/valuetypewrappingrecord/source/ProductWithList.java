package dbmigration.valuetypewrappingrecord;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record ProductWithList(
        @Id String id,
        @Version long version,
        List<AddressHolder> addresses) {

    public record AddressHolder(Address value) {}

    public record Address(String street, String city) {}
}

