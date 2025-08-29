package be.appify.prefab.example.sale;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Search;

import java.math.BigDecimal;
import java.util.Optional;

@Aggregate
@DbMigration
@Search
public class Invoice {
    private final BigDecimal total;

    public Invoice(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal total() {
        return total;
    }

    @EventHandler
    public static Optional<Invoice> onSaleCompleted(SaleCompleted event) {
        if (event.type() == SaleType.INVOICE) {
            return Optional.of(new Invoice(event.items().stream()
                    .map(saleItem -> saleItem.price().multiply(saleItem.quantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)));
        }
        return Optional.empty();
    }
}
