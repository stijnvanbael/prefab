package be.appify.prefab.example.sale.invoice;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Aggregate
@DbMigration
@GetList
public record Invoice(
        @Id String id,
        @Version long version,
        BigDecimal total,
        // TODO: filter on date range
        Instant created,
        // TODO: unique constraint
        @Filter
        String invoiceNumber
) {
    @PersistenceCreator
    public Invoice {
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public long version() {
        return version;
    }
}
