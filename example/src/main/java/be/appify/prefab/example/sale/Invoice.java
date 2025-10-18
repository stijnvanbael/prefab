package be.appify.prefab.example.sale;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Search;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Aggregate
@DbMigration
@Search
public class Invoice {
    @Id
    private String id;
    @Version
    private long version;
    private final BigDecimal total;

    @PersistenceCreator
    public Invoice(
            String id,
            long version,
            BigDecimal total
    ) {
        this.id = id;
        this.version = version;
        this.total = total;
    }

    public String id() {
        return id;
    }

    public long version() {
        return version;
    }

    public BigDecimal total() {
        return total;
    }

    @EventHandler
    public static Optional<Invoice> onSaleCompleted(SaleCompleted event) {
        if (event.type() == SaleType.INVOICE) {
            return Optional.of(new Invoice(UUID.randomUUID().toString(), 0, event.items().stream()
                    .map(saleItem -> saleItem.price().multiply(saleItem.quantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)));
        }
        return Optional.empty();
    }
}
