package dbmigration.valuetypewrappingrecord;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record Product(
        @Id String id,
        @Version long version,
        AddressHolder address) {

    public record AddressHolder(Address value) {}

    public record Address(String street, String city) {}
}

